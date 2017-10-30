package pstb;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.analysis.AnalysisFileParser;
import pstb.analysis.Analyzer;
import pstb.benchmark.PhysicalTopology;
import pstb.benchmark.PhysicalTopology.ActiveProcessRetVal;
import pstb.startup.BenchmarkConfig;
import pstb.startup.TopologyFileParser;
import pstb.startup.WorkloadFileParser;
import pstb.startup.WorkloadFileParser.WorkloadFileType;
import pstb.util.DistributedFlagValue;
import pstb.util.DistributedState;
import pstb.util.LogicalTopology;
import pstb.util.NetworkProtocol;
import pstb.util.PSTBError;
import pstb.util.PSTBUtil;
import pstb.util.UI;
import pstb.util.Workload;

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
			endProgram(PSTBError.ERROR_BENCHMARK, userInput);
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
				endProgram(PSTBError.ERROR_BENCHMARK, userInput);
			}
		}
		
		if(benchmarkRules.checkForNullFields())
		{
			logger.error("Errors loading the properties file!");
			endProgram(PSTBError.ERROR_BENCHMARK, userInput);
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
			endProgram(PSTBError.ERROR_WORKLOAD, userInput);
		}
		if(!subCheck)
		{
			logger.error("Subscriber Workload File failed parsing!");
			endProgram(PSTBError.ERROR_WORKLOAD, userInput);
		}
		
		logger.info("All workload files valid!!");
		
		Workload askedWorkload = parseWLF.getWorkload();
		
		boolean allToposOk = true;
		ArrayList<String> allTopoFiles = benchmarkRules.getTopologyFilesStrings();
		HashMap<String, LogicalTopology> allLTs = new HashMap<String, LogicalTopology>();
		
		logger.info("Starting to parse Topology Files...");
		for(int i = 0 ; i < allTopoFiles.size(); i++)
		{
			String topoI = allTopoFiles.get(i);
			TopologyFileParser parseTopo = new TopologyFileParser(topoI, logger);
			
			logger.debug("Parsing Topology File " + topoI + "...");
			boolean parseCheck = parseTopo.parse();
			if(!parseCheck)
			{
				allToposOk = false;
				logger.error("Parse Failed for file " + topoI + "!");
			}
			else
			{
				logger.debug("Parse Complete for file " + topoI + ".");
				
				LogicalTopology network = parseTopo.getLogicalTopo();
				
				logger.debug("Starting Topology Testing with topology " + topoI + "...");
				boolean mCCheck = network.confirmBrokerMutualConnectivity();
				if(!mCCheck)
				{
					logger.warn("Topology File " + topoI + " has one way connections.");
					
					String fixBroker = topoI + " is not mutually connected\n"
							+ "Would you like us to fix this internally before testing topology Y/n?\n"
							+ "Answering 'n' will terminate the program.";
					
					boolean fixBrokerAns = UI.getYNAnswerFromUser(fixBroker, userInput);
					if (!fixBrokerAns)
					{
						endProgram(PSTBError.ERROR_TOPO_LOG, userInput);
					}
					else
					{
						network.forceMutualConnectivity();
//						if(!checkFMC)
//						{
//							logger.error("Problem forcing mutual connectivity for topology " + topoI + "!");
//							endProgram(PSTBError.ERROR_TOPO_LOG, userInput);
//						}
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
					allLTs.put(topoI, network);
					logger.debug("Topology Check Complete for topology " + topoI + ".");
				}
			}
		}
		
		if(!allToposOk)
		{
			logger.error("Error with topology files!");
			allLTs.clear();
			endProgram(PSTBError.ERROR_TOPO_LOG, userInput);
		}
		
		logger.info("All topologies valid!!");
		
		// Benchmark
		ArrayList<NetworkProtocol> askedProtocols = benchmarkRules.getProtocols();
		HashMap<String, DistributedState> askedDistributed = benchmarkRules.getDistributed();
		Iterator<String> iteratorLT = allLTs.keySet().iterator();
		Analyzer brain = new Analyzer(logger);
		
		logger.info("Beginning to create Physical Topology...");
		
		for( ; iteratorLT.hasNext() ; )
		{
			for(int protocolI = 0 ; protocolI < askedProtocols.size() ; protocolI++)
			{
				String topologyI = iteratorLT.next();
				DistributedState givenDS = askedDistributed.get(topologyI);
				PhysicalTopology localPT = new PhysicalTopology(logger);
				PhysicalTopology disPT = new PhysicalTopology(logger);
				boolean checkLocalPT = true;
				boolean checkDisPT = true;
				
				if(givenDS.equals(DistributedState.No) || givenDS.equals(DistributedState.Both) )
				{
					checkLocalPT = localPT.developPhysicalTopology(false, allLTs.get(topologyI), askedProtocols.get(protocolI),
																		topologyI);
				}
				if(givenDS.equals(DistributedState.Yes) || givenDS.equals(DistributedState.Both) )
				{
					checkDisPT = disPT.developPhysicalTopology(true, allLTs.get(topologyI), askedProtocols.get(protocolI), 
																	topologyI);
				}
				
				if(!checkDisPT || !checkLocalPT)
				{
					logger.error("Error creating physical topology!");
					endProgram(PSTBError.ERROR_TOPO_PHY, userInput);
				}
				
				logger.info("Beginning experiment...");
				boolean successfulExperiment = true;
				
				if(localPT.doObjectsExist())
				{
					successfulExperiment = conductExperiment(localPT, benchmarkRules.getRunLengths(), 
															benchmarkRules.getNumRunsPerExperiment(), askedWorkload, brain);
				}
				else
				{
					successfulExperiment = conductExperiment(disPT, benchmarkRules.getRunLengths(), 
															benchmarkRules.getNumRunsPerExperiment(), askedWorkload, brain);
				}
				
				if(!successfulExperiment)
				{
					logger.error("Error conducting the experiment!");
					endProgram(PSTBError.ERROR_EXPERIMENT, userInput);
				}
			}
		}
		
		// Analysis
		logger.info("Printing all diaries to file...");
		brain.recordAllDiaries();
		logger.info("All diaries now in files.");
		
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
		
		logger.info("Parsing analysis file...");
		AnalysisFileParser brainReader = new AnalysisFileParser();
		brainReader.setAnalysisFileString(analysisFileString);
		
		boolean analysisParseCheck = brainReader.parse();
		if(!analysisParseCheck)
		{
			logger.error("Error parsing the analysis file!");
			endProgram(PSTBError.ERROR_ANALYSIS, userInput);
		}
		
		logger.info("Got requested analysises.");
		
		logger.info("Beginning to execute these analysises...");
		boolean analysisCheck = brain.executeAnalysis(brainReader.getRequestedAnalysis());
		if(!analysisCheck)
		{
			logger.error("Analysis Failed!");
			endProgram(PSTBError.ERROR_ANALYSIS, userInput);
		}
		
		logger.info("Analysis complete.");
		
		logger.info("Storing this analysis into a file...");
		brain.recordAllAnalyzedInformation();
		
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
	private static boolean conductExperiment(PhysicalTopology givenPT, ArrayList<Long> givenRLs, 
												Integer givenNumberOfRunsPerExperiment, Workload givenWorkload, 
												Analyzer givenAnalyzer)
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
			Long sleepLength = null;
			
			boolean functionCheck = givenPT.addRunLengthToClients(iTHRunLengthNano);
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
								
				functionCheck = givenPT.generateBrokerAndClientProcesses();
				if(!functionCheck)
				{
					logger.error("Error generating processes!");
					return false;
				}
				
				functionCheck = givenPT.startProcesses();
				if(!functionCheck)
				{
					logger.error("Error starting run!");
					return false;
				}
				
				logger.debug("Run starting...");
				
				Long startTime = System.nanoTime();
				PhysicalTopology.ActiveProcessRetVal valueCAP = null;
				sleepLength = (long) (iTHRunLengthNano / 10 / PSTBUtil.MILLISEC_TO_NANOSEC.doubleValue());
				Long currentTime = System.nanoTime();
				
				while( (currentTime - startTime) < iTHRunLengthNano)
				{
					valueCAP = givenPT.checkActiveProcesses();
					// If ActiveProcesses had an error... well...
					if(valueCAP.equals(ActiveProcessRetVal.Error))
					{
						logger.error("Run " + runI + " ran into an error!");
						givenPT.killAllProcesses();
						return false;
					}
					// If all of our processes have finished, we have an error - brokers should never self-terminate
					else if(valueCAP.equals(ActiveProcessRetVal.AllOff))
					{
						logger.error("Run " + runI + " has no more client or broker processes!");
						givenPT.killAllProcesses();
						return false;
					}
					// If there are brokers with no clients, has the experiment already finished?
					else if(valueCAP.equals(ActiveProcessRetVal.FloatingBrokers) )
					{
						currentTime = System.nanoTime();
						if((currentTime - startTime) < iTHRunLengthNano)
						{
							logger.error("Run " + runI + " finished early!");
							return false;
						}
						else
						{
							break; // We finished already - there's no need to sleep again
						}
					}
					
					// So that we don't continuously check, let's put this thread to sleep
					try 
					{				
						logger.trace("Pausing main.");
						Thread.sleep(sleepLength);
					} 
					catch (InterruptedException e) 
					{
						logger.error("Error sleeping in main:", e);
						givenPT.killAllProcesses();
						return false;
					}
					
					currentTime = System.nanoTime();
				}
				
				logger.info("Run ended.");
				
				logger.debug("Waiting for clients to finish...");
				valueCAP = givenPT.checkActiveProcesses();
				while(!valueCAP.equals(ActiveProcessRetVal.FloatingBrokers) && !valueCAP.equals(ActiveProcessRetVal.AllOff))
				{
					valueCAP = givenPT.checkActiveProcesses();
					if(valueCAP.equals(ActiveProcessRetVal.Error))
					{
						logger.error("Error waiting for run to finish!");
						givenPT.killAllProcesses();
						return false;
					}
					
					try
					{				
						logger.trace("Pausing main.");
						Thread.sleep(sleepLength);
					} 
					catch (InterruptedException e) 
					{
						logger.error("Error sleeping in main", e);
						givenPT.killAllProcesses();
						return false;
					}
				}
				
				givenPT.killAllProcesses();
				logger.info("Run complete.");
				
				logger.debug("Collecting diaries...");
				ArrayList<String> clientNames = givenPT.getAllClientNames();
				NetworkProtocol proto = givenPT.getProtocol();
				String disFlag = null;
				if(givenPTDis.booleanValue() == true)
				{
					disFlag = DistributedFlagValue.D.toString();
				}
				else
				{
					disFlag = DistributedFlagValue.L.toString();
				}
				
				boolean givenDiaryCollected = true;
				for(int i = 0; i < clientNames.size() ; i++)
				{
					String diaryName = givenPT.getTopologyFilePath() + "-"
										+ disFlag + "-"
										+ proto.toString() + "-"
										+ iTHRunLengthMilli + "-"
										+ runI + "-"
										+ clientNames.get(i);
					givenDiaryCollected = givenAnalyzer.collectDiaryAndAddToBookshelf(diaryName);
					if(!givenDiaryCollected)
					{
						logger.error("Error collecting diary " + diaryName + "!");
						return false;
					}
					logger.trace("Collected diary " + diaryName + ".");
				}
				
				logger.info("All diaries collected.");
				givenPT.clearProcessBuilders();
			}
		}
		
		logger.info("Experiment successful.");
		return true;
	}
}


