package pstb;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.benchmark.throughput.AttributeRatio;
import pstb.benchmark.throughput.MessageSize;
import pstb.benchmark.throughput.NumAttributes;
import pstb.creation.topology.PADRESTopology;
import pstb.creation.topology.PhysicalTopology;
import pstb.creation.topology.PhysicalTopology.ActiveProcessRetVal;
import pstb.creation.topology.SIENATopology;
import pstb.startup.DistributedFileParser;
import pstb.startup.config.BenchmarkConfig;
import pstb.startup.config.BenchmarkMode;
import pstb.startup.config.DistributedState;
import pstb.startup.config.NetworkProtocol;
import pstb.startup.config.SupportedEngines.PSEngine;
import pstb.startup.topology.LogicalTopology;
import pstb.startup.topology.TopologyFileParser;
import pstb.startup.workload.PSAction;
import pstb.startup.workload.WorkloadFileParser;
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
	private static final int TOO_FEW_ANALYSIS_ARGS = 2;
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
		
		logger.debug("Starting Properties Parsing...");
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
		
		logger.debug("Properties file loaded successfully!!");
			
		ArrayList<String> workloadFilesStrings = benchmarkRules.getWorkloadFilesStrings();
		ArrayList<PSEngine> requestedEngines = benchmarkRules.getEngines();
		boolean pRequested = benchmarkRules.padresRequested();
		boolean sRequested = benchmarkRules.sienaRequested();
		HashMap<String, ArrayList<PSAction>> PADRESWorkload = new HashMap<String, ArrayList<PSAction>>();
		HashMap<String, ArrayList<PSAction>> SIENAWorkload = new HashMap<String, ArrayList<PSAction>>();
		
		logger.debug("Parsing Workload Files...");
		boolean allWorkloadsOk = true;
		for(int i = 0 ; i < workloadFilesStrings.size() ; i++)
		{
			String workloadFileI = workloadFilesStrings.get(i);
			WorkloadFileParser parseWLFI = new WorkloadFileParser(workloadFileI, requestedEngines);
			
			logger.debug("Parsing Workload File " + workloadFileI + "...");
			boolean parseCheck = parseWLFI.parse();
			if(!parseCheck)
			{
				allWorkloadsOk = false;
				logger.error("Parse Failed for file " + workloadFileI + "!");
			}
			else
			{
				logger.debug("Parse Complete for file " + workloadFileI + ".");
				
				if(pRequested)
				{
					ArrayList<PSAction> workloadI = parseWLFI.getPADRESWorkload();
					PADRESWorkload.put(workloadFileI, workloadI);
				}
				
				if(sRequested)
				{
					ArrayList<PSAction> workloadI = parseWLFI.getSIENAWorkload();
					SIENAWorkload.put(workloadFileI, workloadI);
				}
			}
		}
		if(!allWorkloadsOk)
		{
			logger.error("Error with topology files!");
			endProgram(PSTBError.M_WORKLOAD, userInput);
		}
		logger.debug("All workload files valid!!");
		
		ArrayList<String> allTopoFiles = benchmarkRules.getTopologyFilesStrings();
		HashMap<String, LogicalTopology> allLTs = new HashMap<String, LogicalTopology>();
		int numBrokersLargestTopo = 0;
		int numNodesLargestTopo = 0;
		Set<String> givenWorkloadFilesStrings = null;
		if(pRequested)
		{
			givenWorkloadFilesStrings = PADRESWorkload.keySet();
		}
		else if(sRequested)
		{
			givenWorkloadFilesStrings = SIENAWorkload.keySet();
		}
		// for now else isn't needed
		
		logger.debug("Starting to parse Topology Files...");
		boolean allToposOk = true;
		for(int i = 0 ; i < allTopoFiles.size() ; i++)
		{
			String topoI = allTopoFiles.get(i);
			TopologyFileParser parseTopo = new TopologyFileParser(topoI, givenWorkloadFilesStrings);
			
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
					
				}
				logger.debug("Topology Check Complete for topology " + topoI + ".");
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
		logger.debug("All topologies valid!!");
		
		HashMap<String, ArrayList<Integer>> disHostsAndPorts = new HashMap<String, ArrayList<Integer>>();
		String disUsername = new String();
		
		if(benchmarkRules.distributedRequested())
		{
			logger.debug("Attempting to parse distributed ...");
			
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
		HashMap<String, DistributedState> askedDistributed = benchmarkRules.getDistributed();
		ArrayList<BenchmarkMode> askedModes = benchmarkRules.getModes();
		Iterator<String> iteratorLT = allLTs.keySet().iterator();
		
		logger.info("Beginning to create Physical Topology...");
		int numModes = askedModes.size();
		for(int i = 0 ; i < numModes ; i++)
		{
			BenchmarkMode modeI = askedModes.get(i);
			
			for( ; iteratorLT.hasNext() ; )
			{
				String topologyI = iteratorLT.next();
				LogicalTopology actualTopologyI = allLTs.get(topologyI);
				DistributedState givenDS = askedDistributed.get(topologyI);
				ArrayList<NetworkProtocol> askedProtocols = benchmarkRules.getProtocols();
				
				for(int j = 0 ; j < askedProtocols.size() ; j++)
				{
					NetworkProtocol givenProtocolJ = askedProtocols.get(j);
					
					PhysicalTopology local = null;
					PhysicalTopology dis = null;
					boolean checkLocalPT = true;
					boolean checkDisPT = true;
					
					if(PADRESTopology.SUPPORTED_PROTOCOLS.contains(givenProtocolJ))
					{
						try
						{
							local = new PADRESTopology(modeI, actualTopologyI, givenProtocolJ, null, null, PADRESWorkload, 
									currTimeString, topologyI);
							dis = new PADRESTopology(modeI, actualTopologyI, givenProtocolJ, disUsername, disHostsAndPorts, 
									PADRESWorkload, currTimeString, topologyI);
						}
						catch (UnknownHostException e)
						{
							logger.error("Couldn't create PhysicalTopolgies: ", e);
							endProgram(PSTBError.M_TOPO_PHY, userInput);
						}
					}
					else if(SIENATopology.SUPPORTED_PROTOCOLS.contains(givenProtocolJ))
					{
						try
						{
							local = new SIENATopology(modeI, actualTopologyI,  givenProtocolJ, null, null, SIENAWorkload, 
									currTimeString, topologyI);
							dis = new SIENATopology(modeI, actualTopologyI,  givenProtocolJ, disUsername, disHostsAndPorts, SIENAWorkload, 
									currTimeString, topologyI);
						}
						catch (UnknownHostException e)
						{
							logger.error("Couldn't create PhysicalTopolgies: ", e);
							endProgram(PSTBError.M_TOPO_PHY, userInput);
						}
					}
					
					if(givenDS.equals(DistributedState.No) || givenDS.equals(DistributedState.Both) )
					{
						checkLocalPT = local.developTopologyObjects(false);
					}
					if(givenDS.equals(DistributedState.Yes) || givenDS.equals(DistributedState.Both) )
					{
						checkDisPT = dis.developTopologyObjects(true);
					}
					
					if(!checkDisPT || !checkLocalPT)
					{
						logger.error("Error creating physical topology!");
						endProgram(PSTBError.M_TOPO_PHY, userInput);
					}
					
					logger.info("Beginning experiment...");
					boolean successfulExperiment = true;
					if(modeI.equals(BenchmarkMode.Scenario))
					{
						if(local.doAnyObjectsExist())
						{
							successfulExperiment = conductNormalExperiment(local, benchmarkRules.getRunLengths(),
																		benchmarkRules.getNumRunsPerExperiment());
						}
						if(dis.doAnyObjectsExist())
						{
							successfulExperiment = conductNormalExperiment(dis, benchmarkRules.getRunLengths(),
																		benchmarkRules.getNumRunsPerExperiment());
						}
					}
					else if(modeI.equals(BenchmarkMode.Throughput))
					{
						if(local.doAnyObjectsExist())
						{
							successfulExperiment = conductThroughputExperiment(local, benchmarkRules.getPeriodLength());
						}
						if(dis.doAnyObjectsExist())
						{
							successfulExperiment = conductThroughputExperiment(dis, benchmarkRules.getPeriodLength());
						}
					}
					
					if(!successfulExperiment)
					{
						logger.error("Error conducting the experiment!");
						endProgram(PSTBError.M_EXPERIMENT, userInput);
					}
				}
			}
		}
		
		// Analysis
		ArrayList<String> analyzeCommand = new ArrayList<String>();
		analyzeCommand.add("./analysis.sh");
		analyzeCommand.add("256");
		
		String printDiariesPrompt = "Would you like to print all diaries Y/n?";
		boolean printDiaries = UI.getYNAnswerFromUser(printDiariesPrompt, userInput);
		if(printDiaries)
		{
			analyzeCommand.add("-p");
		}
		
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
			
			analyzeCommand.add("-f");
			analyzeCommand.add(analysisFileString);
		}
		
		if(analyzeCommand.size() > TOO_FEW_ANALYSIS_ARGS)
		{
			String[] analyze = analyzeCommand.toArray(new String [0]);
			
			Boolean analyszeCheck = PSTBUtil.createANewProcess(analyze, logger, false, 
					"Couldn't run analysis :", 
					"Analysis successfull.", 
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
	 * @param givenPT - the Experiment's PhysicalTopology
	 * @param givenRLs - the length a given run of the Experiment should be
	 * @param givenNumberOfRunsPerExperiment - the number of times an Experiment should run, given its other parameters
	 * @param givenWorkload - the Experiment's Workload
	 * @param givenAnalyzer - the Benchmark's analyzer
	 * @return false if there is a failure; true otherwise
	 */
	private static boolean conductNormalExperiment(PhysicalTopology givenPT, ArrayList<Long> givenRLs, Integer givenNumberOfRunsPerExperiment)
	{
		// We'll need this in case there is an error, so...
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
			
			// Loop through the runs
			for(int runI = 0 ; runI < givenNumberOfRunsPerExperiment ; runI++)
			{
				CountDownLatch start = new CountDownLatch(1);
																
				boolean prepCheck = givenPT.prepareNormalRun(start, iTHRunLengthNano, runI);
				if(!prepCheck)
				{
					logger.error("Couldn't prepare experiment!");
					return false;
				}
				
				boolean runCheck = givenPT.startRun();
				if(!runCheck)
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
				
				PhysicalTopology.ActiveProcessRetVal valueCAP = givenPT.checkActiveProcesses();
				Long startTime = System.nanoTime();
				while(!valueCAP.equals(ActiveProcessRetVal.FloatingBrokers))
				{
					// If ActiveProcesses had an error... well...
					if(valueCAP.equals(ActiveProcessRetVal.Error))
					{
						logger.error("Run " + runI + " ran into an error!");
						PSTBUtil.killAllProcesses(givenPTDis.booleanValue(), givenPT.getUser(), logger);
						return false;
					}
					// If all of our processes have finished, we have an error - brokers should never self-terminate
					else if(valueCAP.equals(ActiveProcessRetVal.AllOff))
					{
						logger.error("Run " + runI + " has no more client or broker processes!");
						PSTBUtil.killAllProcesses(givenPTDis.booleanValue(), givenPT.getUser(), logger);
						return false;
					}
					// If there are brokers with no clients, has the experiment already finished?
					else if(valueCAP.equals(ActiveProcessRetVal.FloatingBrokers) )
					{
						Long currentTime = System.nanoTime();
						if((currentTime - startTime) < (iTHRunLengthNano - bufferTimeNano))
						{
							logger.error("Run " + runI + " finished early!");
							PSTBUtil.killAllProcesses(givenPTDis.booleanValue(), givenPT.getUser(), logger);
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
							PSTBUtil.killAllProcesses(givenPTDis.booleanValue(), givenPT.getUser(), logger);
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
						PSTBUtil.killAllProcesses(givenPTDis.booleanValue(), givenPT.getUser(), logger);
						return false;
					}
					
					valueCAP = givenPT.checkActiveProcesses();
				}
				
				logger.info("Run ended.");
				
				PSTBUtil.killAllProcesses(givenPTDis.booleanValue(), givenPT.getUser(), logger);
				
				givenPT.destroyAllActiveNodes();
				givenPT.resetSystemAfterRun();
				logger.info("Run " + runI + " complete.");
			}
		}
		
		logger.info("Experiment successful.");
		return true;
	}
	
	/**
	 * Given a set of parameters, conducts a particular Benchmark Experiment 
	 * 
	 * @param givenPT - the Experiment's PhysicalTopology
	 * @param givenRLs - the length a given run of the Experiment should be
	 * @return false if there is a failure; true otherwise
	 */
	private static boolean conductThroughputExperiment(PhysicalTopology givenPT, Long givenPL)
	{
		// We'll need this in case there is an error, so...
		Boolean givenPTDis = givenPT.getDistributed();
		if(givenPTDis == null)
		{
			logger.error("Error with givenPT - distributed value not set properly!");
			return false;
		}
		
		for(MessageSize msI : MessageSize.values())
		{
			for(NumAttributes naI : NumAttributes.values())
			{
				boolean oneAttributeComplete = false;
				for(AttributeRatio arI : AttributeRatio.values())
				{
					logger.info("Variables are: " + arI + " " + naI + " " + msI + ".");
					
					CountDownLatch start = new CountDownLatch(1);
					
					boolean run = true;
					if(naI.equals(NumAttributes.One))
					{
						if(oneAttributeComplete)
						{
							run = false;
						}
						else
						{
							oneAttributeComplete = true;
						}
					}
					
					if(run)
					{
						boolean prepCheck = givenPT.prepareThroughputRun(start, givenPL, arI, naI, msI);
						if(!prepCheck)
						{
							logger.error("Couldn't prepare experiment!");
							return false;
						}
						
						boolean runCheck = givenPT.startRun();
						if(!runCheck)
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
								
						PhysicalTopology.ActiveProcessRetVal valueCAP = givenPT.checkActiveProcesses();
						while(!valueCAP.equals(ActiveProcessRetVal.FloatingBrokers))
						{
							// If ActiveProcesses had an error... well...
							if(valueCAP.equals(ActiveProcessRetVal.Error))
							{
								logger.error("Run ran into an error!");
								PSTBUtil.killAllProcesses(givenPTDis.booleanValue(), givenPT.getUser(), logger);
								return false;
							}
							// If all of our processes have finished, we have an error - brokers should never self-terminate
							else if(valueCAP.equals(ActiveProcessRetVal.AllOff))
							{
								logger.error("Run has no more client or broker processes!");
								PSTBUtil.killAllProcesses(givenPTDis.booleanValue(), givenPT.getUser(), logger);
								return false;
							}
									
							// So that we don't continuously check, let's put this thread to sleep for two seconds
							try 
							{				
								logger.trace("Pausing main.");
								Thread.sleep(2000);
							} 
							catch (InterruptedException e) 
							{
								logger.error("Error sleeping in main:", e);
								PSTBUtil.killAllProcesses(givenPTDis.booleanValue(), givenPT.getUser(), logger);
								return false;
							}
									
							valueCAP = givenPT.checkActiveProcesses();
						}	
						logger.debug("Run ended.");
								
						PSTBUtil.killAllProcesses(givenPTDis.booleanValue(), givenPT.getUser(), logger);
								
						givenPT.destroyAllActiveNodes();
						givenPT.resetSystemAfterRun();
						logger.info("Run complete.");
					}
				}
			}			
		}

		logger.info("Experiment successful.");
		return true;
	}
}


