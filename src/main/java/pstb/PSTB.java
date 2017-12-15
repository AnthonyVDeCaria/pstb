package pstb;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.creation.PhysicalTopology;
import pstb.creation.PhysicalTopology.ActiveProcessRetVal;
import pstb.startup.DistributedState;
import pstb.startup.LogicalTopology;
import pstb.startup.NetworkProtocol;
import pstb.startup.Workload;
import pstb.startup.parsing.BenchmarkConfig;
import pstb.startup.parsing.DistributedFileParser;
import pstb.startup.parsing.TopologyFileParser;
import pstb.startup.parsing.WorkloadFileParser;
import pstb.startup.parsing.WorkloadFileParser.WorkloadFileType;
import pstb.util.PSTBError;
import pstb.util.PSTBUtil;
import pstb.util.UI;

/**
 * @author padres-dev-4187
 * 
 * The main PSTB function and its helpers.
 */
public class PSTB {
	private static final String DEFAULT_ANALYSIS_FILE_STRING = "etc/defaultAnalysis.txt";
	private static final String DEFAULT_BENCHMARK_PROPERTIES_FILE_STRING = "etc/defaultBenchmark.properties";
	private static final int EXCEUTED_PROPERLY_VALUE = 0;
	private static final Logger logger = LogManager.getRootLogger();
	
	/**
	 * @author padres-dev-4187
	 * 
	 * The main function for the entire PSTB program
	 * 
	 * @param args - the main function arguments 
	 */
	public static void main(String[] args)
	{
		// Startup
		logger.info("Starting program.");
		Scanner userInput = new Scanner(System.in);
		Properties defaultProp = null;
		
		logger.info("Starting Properties Parsing...");
		try
		{
			defaultProp = loadProperties(DEFAULT_BENCHMARK_PROPERTIES_FILE_STRING, null);
		}
		catch (IOException e)
		{
			logger.error("Properties: Couldn't find default properties file", e);
			endProgram(PSTBError.M_BENCHMARK, userInput);
		}
				
		BenchmarkConfig benchmarkRules = new BenchmarkConfig(logger);
		
		String customBenchProp = "Would you like to use a custom benchmark properties file Y/n?";
		boolean customBenchPropAns = UI.getYNAnswerFromUser(customBenchProp, userInput);
		if (!customBenchPropAns)
		{
			logger.debug("Loading the default Properties file...");
			benchmarkRules.setBenchmarkConfig(defaultProp);
		}
		else
		{
			try
			{
				String userPropFileString = UI.getAndCheckFilePathFromUser("Please input the path to this properties file", userInput);
				logger.debug("Loading the user Properties file...");
				Properties userProp = loadProperties(userPropFileString, defaultProp);
				benchmarkRules.setBenchmarkConfig(userProp);
			}
			catch (IOException e)
			{
				logger.error("Couldn't find user properties file", e);
				endProgram(PSTBError.M_BENCHMARK, userInput);
			}
		}
		
		if(benchmarkRules.checkForNullFields())
		{
			logger.error("Errors loading the properties file!");
			endProgram(PSTBError.M_BENCHMARK, userInput);
		}
		
		logger.info("Properties file loaded successfully!!");
				
		WorkloadFileParser parseWLF = new WorkloadFileParser(logger);
		parseWLF.setPubWorkloadFilesStrings(benchmarkRules.getPubWorkloadFilesStrings());
		parseWLF.setSubWorkloadFileString(benchmarkRules.getSubWorkloadFileString());
		
		logger.info("Parsing Workload Files...");
		
		logger.debug("Parsing Publisher Workload...");
		boolean pubCheck = parseWLF.parseWorkloadFiles(WorkloadFileType.P);
		logger.debug("Parsing Subscriber Workload...");
		boolean subCheck = parseWLF.parseWorkloadFiles(WorkloadFileType.S);
		
		if(!pubCheck)
		{
			logger.error("Publisher Workload File failed parsing!");
			endProgram(PSTBError.M_WORKLOAD, userInput);
		}
		if(!subCheck)
		{
			logger.error("Subscriber Workload File failed parsing!");
			endProgram(PSTBError.M_WORKLOAD, userInput);
		}
		
		logger.info("All workload files valid!!");
		
		Workload askedWorkload = parseWLF.getWorkload();
		
		boolean allToposOk = true;
		ArrayList<String> allTopoFiles = benchmarkRules.getTopologyFilesStrings();
		HashMap<String, LogicalTopology> allLTs = new HashMap<String, LogicalTopology>();
		int numBrokersLargestTopo = 0;
		int numNodesLargestTopo = 0;
		
		logger.info("Starting to parse Topology Files...");
		for(int i = 0 ; i < allTopoFiles.size() ; i++)
		{
			String topoI = allTopoFiles.get(i);
			TopologyFileParser parseTopo = new TopologyFileParser(topoI);
			
			logger.debug("Parsing Topology File " + topoI + "...");
			boolean parseCheck = parseTopo.parse();
			if(!parseCheck)
			{
				allToposOk = false;
				numBrokersLargestTopo = 0;
				logger.error("Parse Failed for file " + topoI + "!");
			}
			else
			{
				logger.debug("Parse Complete for file " + topoI + ".");
				
				LogicalTopology network = parseTopo.getLogicalTopo();
				
				logger.debug("Starting Topology Testing with topology " + topoI + "...");
				Boolean mCCheck = network.confirmBrokerMutualConnectivity();
				if(mCCheck == null)
				{
					logger.error("Error with topology " + topoI + "!");
					endProgram(PSTBError.M_TOPO_LOG, userInput);
				}
				else if(!mCCheck.booleanValue())
				{
					logger.warn("Topology File " + topoI + " has one way connections.");
					
					String fixBroker = topoI + " is not mutually connected\n"
							+ "Would you like us to fix this internally before testing topology Y/n?\n"
							+ "Answering 'n' will terminate the program.";
					
					boolean fixBrokerAns = UI.getYNAnswerFromUser(fixBroker, userInput);
					if (!fixBrokerAns)
					{
						endProgram(PSTBError.M_TOPO_LOG, userInput);
					}
					else
					{
						network.forceMutualConnectivity();
					}
				}
				
				boolean topoCheck = network.confirmTopoConnectivity();
				if(!topoCheck)
				{
					allToposOk = false;
					allLTs.clear();
					logger.error("Topology Check Failed for topology " + topoI + "!");
				}
				else
				{
					int numBrokersTopoI = network.numBrokers();
					if(numBrokersTopoI > numBrokersLargestTopo)
					{
						numBrokersLargestTopo = numBrokersTopoI;
					}
					int numNodesTopoI = network.networkSize();
					if(numNodesTopoI > numNodesLargestTopo)
					{
						numNodesLargestTopo = numNodesTopoI;
					}
					
					allLTs.put(topoI, network);
					logger.debug("Topology Check Complete for topology " + topoI + ".");
				}
			}
		}
		
		if(!allToposOk)
		{
			logger.error("Error with topology files!");
			allLTs.clear();
			numBrokersLargestTopo = 0;
			numNodesLargestTopo = 0;
			endProgram(PSTBError.M_TOPO_LOG, userInput);
		}
		
		logger.info("All topologies valid!!");
		
		HashMap<String, ArrayList<Integer>> disHostsAndPorts = new HashMap<String, ArrayList<Integer>>();
		String disUsername = new String();
		
		if(benchmarkRules.distributedRequested())
		{
			logger.info("Attempting to parse distributed ...");
			
			DistributedFileParser dfp = new DistributedFileParser(benchmarkRules.getDistributedFileString());
			
			boolean dfpCheck = dfp.parse(numBrokersLargestTopo);
			if(!dfpCheck)
			{
				logger.error("Error with distributed file!");
				endProgram(PSTBError.M_DISTRIBUTED, userInput);
			}
			
			disHostsAndPorts = dfp.getHostsAndPorts();
			
			if(numNodesLargestTopo > disHostsAndPorts.size())
			{
				logger.warn("Not enough hosts given for each node - there will be multiple nodes on a single machine.");
			}
			
			String distributeJarPrompt = "Please input the associated username:";
			disUsername = UI.getInputFromUser(distributeJarPrompt, userInput);
			
			distributeJarPrompt = "Would you like to distribute the PSTB jar to all nodes Y/n?";
			boolean distributeJar = UI.getYNAnswerFromUser(distributeJarPrompt, userInput);
			if(distributeJar)
			{
				String [] command = {"scripts/addPSTBToMachines.sh", disUsername};
				
				Boolean addToNodesCheck = PSTBUtil.createANewProcess(command, logger, false,
																			"Couldn't launch process to give all nodes PSTB: ", 
																			"Added PSTB to all nodes!", 
																			"Failed adding PSTB to all nodes!");
				
				if(addToNodesCheck == null || !addToNodesCheck.booleanValue())
				{
					endProgram(PSTBError.M_DISTRIBUTED, userInput);
				}
			}
		}
		
		// Benchmark
		Long currTime = System.currentTimeMillis();
		String currTimeString = PSTBUtil.DATE_FORMAT.format(currTime);
		
		String localUsername = System.getProperty("user.name");
		
		ArrayList<NetworkProtocol> askedProtocols = benchmarkRules.getProtocols();
		HashMap<String, DistributedState> askedDistributed = benchmarkRules.getDistributed();
		Iterator<String> iteratorLT = allLTs.keySet().iterator();
		
		logger.info("Beginning to create Physical Topology...");
		
		for( ; iteratorLT.hasNext() ; )
		{
			String topologyI = iteratorLT.next();
			
			for(int protocolI = 0 ; protocolI < askedProtocols.size() ; protocolI++)
			{
				DistributedState givenDS = askedDistributed.get(topologyI);
				PhysicalTopology localPT = null;
				PhysicalTopology disPT = null;
				try
				{
					localPT = new PhysicalTopology();
					disPT = new PhysicalTopology();
				}
				catch (UnknownHostException e)
				{
					logger.error("Couldn't create PhysicalTopolgies: ", e);
					endProgram(PSTBError.M_TOPO_PHY, userInput);
				}
				boolean checkLocalPT = true;
				boolean checkDisPT = true;
				
				if(givenDS.equals(DistributedState.No) || givenDS.equals(DistributedState.Both) )
				{
					checkLocalPT = localPT.developPhysicalTopology(false, allLTs.get(topologyI), askedProtocols.get(protocolI),
																		topologyI, disHostsAndPorts, currTimeString);
				}
				if(givenDS.equals(DistributedState.Yes) || givenDS.equals(DistributedState.Both) )
				{
					checkDisPT = disPT.developPhysicalTopology(true, allLTs.get(topologyI), askedProtocols.get(protocolI), 
																	topologyI, disHostsAndPorts, currTimeString);
				}
				
				if(!checkDisPT || !checkLocalPT)
				{
					logger.error("Error creating physical topology!");
					endProgram(PSTBError.M_TOPO_PHY, userInput);
				}
				
				logger.info("Beginning experiment...");
				boolean successfulExperiment = true;
				
				if(localPT.doObjectsExist())
				{
					localPT.setUsername(localUsername);
					successfulExperiment = conductExperiment(localPT, benchmarkRules.getRunLengths(),
															benchmarkRules.getNumRunsPerExperiment(), askedWorkload);
				}
				if (disPT.doObjectsExist())
				{
					disPT.setUsername(disUsername);
					successfulExperiment = conductExperiment(disPT, benchmarkRules.getRunLengths(),
															benchmarkRules.getNumRunsPerExperiment(), askedWorkload);
				}
				
				if(!successfulExperiment)
				{
					logger.error("Error conducting the experiment!");
					endProgram(PSTBError.M_EXPERIMENT, userInput);
				}
			}
		}
		
		// Analysis
		String analysisPrompt = "Would you like to run an analysis on the data genertated Y/n?";
		boolean analysis = UI.getYNAnswerFromUser(analysisPrompt, userInput);
		if(analysis)
		{
			String analysisFileString = DEFAULT_ANALYSIS_FILE_STRING;
			String customAnalysisFilePrompt = null;
			boolean customAnalysisFileAns = customBenchPropAns;
			
			if(!customBenchPropAns)
			{
				customAnalysisFilePrompt = "Would you like to use a custom analysis file Y/n?";
				customAnalysisFileAns = UI.getYNAnswerFromUser(customAnalysisFilePrompt, userInput);
			}
			
			if(customAnalysisFileAns)
			{
				customAnalysisFilePrompt = "Please input the path to the new analysis file";
				analysisFileString = UI.getAndCheckFilePathFromUser(customAnalysisFilePrompt, userInput);
			}
			
			String[] kill = {"./analysis.sh", "256", analysisFileString};
			
			Boolean analyszeCheck = PSTBUtil.createANewProcess(kill, logger, false, "Couldn't run analysis :", "Analysis successfull.", 
																	"Analysis failed!");
			if(analyszeCheck == null || !analyszeCheck.booleanValue())
			{
				logger.error("Analysis failed!");
				endProgram(PSTBError.M_ANALYSIS, userInput);
			}
			logger.info("Analysis successfull!!");
		}
		
		logger.info("Benchmark complete!!!");
		endProgram(EXCEUTED_PROPERLY_VALUE, userInput);
	}
	
	/**
	 * Extracts the properties from a given properties file and stores it in a new Property object.
	 * Throws an IOExpection if it cannot find the file along the path specified
	 * 
	 * @param propertyFilePath - the path to the properties file
	 * @param defaultProperties - any existing properties files (if none exist, add null).
	 * @return a loaded properties file
	 * @throws IOException
	 */
	private static Properties loadProperties(String propertyFilePath, Properties defaultProperties) throws IOException {
		Properties properties = new Properties(defaultProperties);
		
		FileInputStream propFileStream = new FileInputStream(propertyFilePath);
		properties.load(propFileStream);
		propFileStream.close();
		
		return properties;
	}
	
	/**
	 * Ends the main program
	 * 
	 * @param errorClassification - the type of error that occurred - including no error
	 * @param scannerSystemIn - the UI scanner used throughout the main function
	 * @see PSTBError
	 */
	private static void endProgram(int errorClassification, Scanner scannerSystemIn)
	{
		logger.info("Ending program.");
		scannerSystemIn.close();
		System.exit(errorClassification);
	}
	
	/**
	 * Given a set of parameters, conducts a particular Benchmark Experiment 
	 * 
	 * @param givenPT - the Experiment's PhysicalTopolgy
	 * @param givenRLs - the length a given run of the Experiment should be
	 * @param givenNumberOfRunsPerExperiment - the number of times an Experiment should run, given its other parameters
	 * @param givenWorkload - the Experiment's Workload
	 * @param givenAnalyzer - the Benchmark's analyzer
	 * @return false if there is a failure; true otherwise
	 */
	private static boolean conductExperiment(PhysicalTopology givenPT, ArrayList<Long> givenRLs, Integer givenNumberOfRunsPerExperiment,
												Workload givenWorkload)
	{
		// We'll need this to collect the diaries latter, so...
		Boolean givenPTDis = givenPT.getDistributed();
		
		if(givenPTDis == null)
		{
			logger.error("Error with givenPT - distributed value not set properly!");
			return false;
		}
		
		// Loop through the Run Lengths
		for(int runLengthI = 0 ; runLengthI < givenRLs.size(); runLengthI++)
		{
			Long iTHRunLengthMilli = givenRLs.get(runLengthI);
			Long iTHRunLengthNano = iTHRunLengthMilli * PSTBUtil.MILLISEC_TO_NANOSEC;
			Long sleepLengthMilli = iTHRunLengthMilli / 10;
			Long bufferTimeNano = iTHRunLengthNano / 10;
			
			boolean functionCheck = givenPT.addRunLengthToAllNodes(iTHRunLengthNano);
			if(!functionCheck)
			{
				logger.error("Error setting Run Length!");
				return false;
			}
			
			functionCheck = givenPT.addWorkloadToAllClients(givenWorkload);
			if(!functionCheck)
			{
				logger.error("Error setting Workload!");
				return false;
			}
			
			// Loop through the runs
			for(int runI = 0 ; runI < givenNumberOfRunsPerExperiment ; runI++)
			{
				givenPT.setRunNumber(runI);
				
				CountDownLatch start = new CountDownLatch(1);
																
				functionCheck = givenPT.prepareRun(start);
				if(!functionCheck)
				{
					logger.error("Couldn't prepare experiment!");
					return false;
				}
				
				functionCheck = givenPT.startRun();
				if(!functionCheck)
				{
					logger.error("Error starting run!");
					return false;
				}
				logger.debug("Run starting...");
				
				try 
				{
					start.await();
				} 
				catch (InterruptedException e) 
				{
					logger.error("Interrupted waiting for start signal: ", e);
				}
				logger.info("Start signal receieved.");
				
				PSTBUtil.synchronizeRun();
				logger.info("Synchronization complete.");
				
				Long startTime = System.nanoTime();
				PhysicalTopology.ActiveProcessRetVal valueCAP = givenPT.checkActiveProcesses();
				while(!valueCAP.equals(ActiveProcessRetVal.FloatingBrokers))
				{
					// If ActiveProcesses had an error... well...
					if(valueCAP.equals(ActiveProcessRetVal.Error))
					{
						logger.error("Run " + runI + " ran into an error!");
						killAllProcesses(givenPTDis.booleanValue(), givenPT.getUser());
						return false;
					}
					// If all of our processes have finished, we have an error - brokers should never self-terminate
					else if(valueCAP.equals(ActiveProcessRetVal.AllOff))
					{
						logger.error("Run " + runI + " has no more client or broker processes!");
						killAllProcesses(givenPTDis.booleanValue(), givenPT.getUser());
						return false;
					}
					// If there are brokers with no clients, has the experiment already finished?
					else if(valueCAP.equals(ActiveProcessRetVal.FloatingBrokers) )
					{
						Long currentTime = System.nanoTime();
						if((currentTime - startTime) < (iTHRunLengthNano - bufferTimeNano))
						{
							logger.error("Run " + runI + " finished early!");
							killAllProcesses(givenPTDis.booleanValue(), givenPT.getUser());
							return false;
						}
						else
						{
							break; // We finished already - there's no need to sleep again
						}
					}
					// Should the experiment be over already?
					else
					{
						Long currentTime = System.nanoTime();
						if((currentTime - startTime) > (iTHRunLengthNano + bufferTimeNano))
						{
							logger.error("Run " + runI + " hasn't finished within the experiment period!");
							killAllProcesses(givenPTDis.booleanValue(), givenPT.getUser());
							return false;
						}
					}
					
					// So that we don't continuously check, let's put this thread to sleep for a tenth of the run
					try 
					{				
						logger.trace("Pausing main.");
						Thread.sleep(sleepLengthMilli);
					} 
					catch (InterruptedException e) 
					{
						logger.error("Error sleeping in main:", e);
						killAllProcesses(givenPTDis.booleanValue(), givenPT.getUser());
						return false;
					}
					
					valueCAP = givenPT.checkActiveProcesses();
				}
				
				logger.info("Run ended.");
				
				Boolean resetCheck = killAllProcesses(givenPTDis.booleanValue(), givenPT.getUser());
				if(resetCheck == null || resetCheck.booleanValue() == false)
				{
					logger.error("Error reseting run " + runI + "!");
					return false;
				}
				
				givenPT.destroyAllActiveNodes();
				givenPT.resetSystemAfterRun();
				logger.info("Run " + runI + " complete.");
			}
		}
		
		logger.info("Experiment successful.");
		return true;
	}
	
	private static Boolean killAllProcesses(boolean distributed, String username)
	{
		ArrayList<String> command = new ArrayList<String>();
		
		if(distributed)
		{
			command.add("scripts/killAllNodes.sh");
			command.add(username);
		}
		else
		{
			command.add("scripts/killAllNodesOnThisMachine.sh");
		}
		
		String[] kill = command.toArray(new String[0]);
		
		return PSTBUtil.createANewProcess(kill, logger, false,
												"Couldn't run kill process :", 
												"Kill process successfull.", 
												"Kill process failed!"
											);
	}
}


