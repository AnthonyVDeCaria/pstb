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

import pstb.benchmark.PhysicalTopology;
import pstb.benchmark.PhysicalTopology.ActiveProcessRetVal;
import pstb.startup.BenchmarkConfig;
import pstb.startup.TopologyFileParser;
import pstb.startup.WorkloadFileParser;
import pstb.startup.WorkloadFileParser.WorkloadFileType;
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
 */
public class PSTB {
	private static final Long NANO_SEC_NEEDED_TO_CLEAN_BROKER = new Long(6000000000L);
	
	private static final Logger logger = LogManager.getLogger(PSTB.class);
	
	/**
	 * Extracts the properties from a given properties file 
	 * and stores it in a new Property object.
	 * Throws an IOExpection if it cannot find the file along the path specified
	 * @param propertyFilePath - the path to the properties file
	 * @param defaultProperties - any existing properties files. 
	 *        If none exist, add null.
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
	 * @param errorClassification - the type of error that occurred - including no error
	 * @param scannerSystemIn - the UI scanner used for the yes or no answers
	 * @see PSTBError
	 */
	private static void endProgram(Integer errorClassification, Scanner scannerSystemIn)
	{
		logger.info("Ending program.");
		scannerSystemIn.close();
		System.exit(errorClassification);
	}
	
	/**
	 * The main function
	 * @param args - the arguments to the main function
	 */
	public static void main(String[] args)
	{
		logger.info("Starting program.");
		Scanner userInput = new Scanner(System.in);
		Properties defaultProp = null;
		
		logger.info("Starting Properties Parsing...");
		try
		{
			defaultProp = loadProperties("etc/defaultBenchmark.properties", null);
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
				logger.debug("Loading the user Properties file...");
				Properties userProp = loadProperties("src/test/java/userBenchmark.properties", defaultProp);
				benchmarkRules.setBenchmarkConfig(userProp);
			}
			catch (IOException e)
			{
				logger.error("Properties: Couldn't find user properties file", e);
				endProgram(PSTBError.ERROR_BENCHMARK, userInput);
			}
		}
		
		if(benchmarkRules.checkForNullFields())
		{
			logger.error("Errors loading the properties file!");
			endProgram(PSTBError.ERROR_BENCHMARK, userInput);
		}
		
		logger.info("No errors loading the Properties file!");
				
		WorkloadFileParser parseWLF = new WorkloadFileParser(logger);
		parseWLF.setPubWorkloadFilesPaths(benchmarkRules.getPubWorkloadFilesPaths());
		parseWLF.setSubWorkloadFilePath(benchmarkRules.getSubWorkloadFilePath());
		
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
		ArrayList<String> allTopoFiles = benchmarkRules.getTopologyFilesPaths();
		HashMap<String, LogicalTopology> allLTs = new HashMap<String, LogicalTopology>();
		
		logger.info("Starting to disect Topology Files...");
		
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
				logger.debug("Parse Complete for file " + topoI + "!");
				
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
						try
						{
							network.forceMutualConnectivity();
						}
						catch(IllegalArgumentException e)
						{
							logger.error("Topology: Problem forcing mutual connectivity for topology " 
									+ topoI, e);
							endProgram(PSTBError.ERROR_TOPO_LOG, userInput);
						}
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
					logger.debug("Topology Check Complete for topology " + topoI + "!");
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
		
		ArrayList<NetworkProtocol> askedProtocols = benchmarkRules.getProtocols();
		HashMap<String, DistributedState> askedDistributed = benchmarkRules.getDistributed();
		Iterator<String> iteratorLT = allLTs.keySet().iterator();
		
		logger.info("Beginning to create Physical Topology");
		
		for( ; iteratorLT.hasNext() ; )
		{
			for(int protocolI = 0 ; protocolI < askedProtocols.size() ; protocolI++)
			{
				PhysicalTopology localPT = new PhysicalTopology(logger);
				PhysicalTopology disPT = new PhysicalTopology(logger);
				boolean checkLocalPT = true;
				boolean checkDisPT = true;
				
				String topologyI = iteratorLT.next();
				
				if(askedDistributed.get(topologyI).equals(DistributedState.No) 
						|| askedDistributed.get(topologyI).equals(DistributedState.Both) )
				{
					checkLocalPT = localPT.developPhysicalTopology(false, allLTs.get(topologyI), askedProtocols.get(protocolI),
																		topologyI);
				}
				if(askedDistributed.get(topologyI).equals(DistributedState.Yes) 
						|| askedDistributed.get(topologyI).equals(DistributedState.Both) )
				{
					checkDisPT = disPT.developPhysicalTopology(true, allLTs.get(topologyI), askedProtocols.get(protocolI), 
																	topologyI);
				}
				
				if(!checkDisPT || !checkLocalPT)
				{
					logger.error("Error creating physical topology");
					endProgram(PSTBError.ERROR_TOPO_PHY, userInput);
				}
				
				logger.info("Beginning experiment");
				boolean successfulExperiment = true;
				
				if(!localPT.doObjectsExist())
				{
					successfulExperiment = runExperiment(localPT, benchmarkRules.getRunLengths(), 
															benchmarkRules.getNumRunsPerExperiment(), askedWorkload);
				}
				else
				{
					successfulExperiment = runExperiment(disPT, benchmarkRules.getRunLengths(), 
															benchmarkRules.getNumRunsPerExperiment(), askedWorkload);
				}
				
				if(!successfulExperiment)
				{
					endProgram(PSTBError.ERROR_RUN, userInput);
				}
			}
		}
		
		endProgram(0, userInput);
	}
	
	private static boolean runExperiment(PhysicalTopology givenPT, ArrayList<Long> givenRLs, 
											Integer givenNumberOfRunsPerExperiment, Workload givenWorkload)
	{
		for(int runLengthI = 0 ; runLengthI < givenRLs.size(); runLengthI++)
		{
			Long currentRunLength = givenRLs.get(runLengthI) * PSTBUtil.MIN_TO_NANOSEC;
			Long sleepLength = null;
			
			boolean functionCheck = givenPT.addRunLengthToClients(currentRunLength);
			if(!functionCheck)
			{
				logger.error("Error setting Run Length");
				return false;
			}
			
			functionCheck = givenPT.addWorkloadToAllClients(givenWorkload);
			if(!functionCheck)
			{
				logger.error("Error setting Workload");
				return false;
			}
			
			for(int runI = 0 ; runI < givenNumberOfRunsPerExperiment ; runI++)
			{
				givenPT.setRunNumber(runI);
								
				functionCheck = givenPT.generateBrokerAndClientProcesses();
				if(!functionCheck)
				{
					logger.error("Error generating processes");
					return false;
				}
				
				functionCheck = givenPT.startProcesses();
				if(!functionCheck)
				{
					logger.error("Error starting run");
					return false;
				}
				
				Long startTime = System.nanoTime();
				PhysicalTopology.ActiveProcessRetVal valueCAP = null;
				sleepLength = (long) (currentRunLength / 10 / PSTBUtil.MILLISEC_TO_NANOSEC.doubleValue());
				Long currentTime = System.nanoTime();
				
				while( (currentTime - startTime) < currentRunLength)
				{
					valueCAP = givenPT.checkActiveProcesses();
					if(valueCAP.equals(ActiveProcessRetVal.Error) || valueCAP.equals(ActiveProcessRetVal.AllOff))
					{
						logger.error("Run had an error");
						givenPT.killAllProcesses();
						return false;
					}
					else if(valueCAP.equals(ActiveProcessRetVal.FloatingBrokers) )
					{
						currentTime = System.nanoTime();
						if((currentTime - startTime) < currentRunLength)
						{
							logger.error("Error - run finished early");
							return false;
						}
						else
						{
							// We finished already - there's no need to sleep again
							break;
						}
					}
					
					try 
					{				
						logger.trace("Pausing main");
						Thread.sleep(sleepLength);
					} 
					catch (InterruptedException e) 
					{
						logger.error("Error sleeping in main", e);
						givenPT.killAllProcesses();
						return false;
					}
					
					currentTime = System.nanoTime();
				}
				
				logger.info("Run successful");
				
				/*
				 * Wait for the clients to finish
				 */
				valueCAP = givenPT.checkActiveProcesses();
				while(!valueCAP.equals(ActiveProcessRetVal.FloatingBrokers) && !valueCAP.equals(ActiveProcessRetVal.AllOff))
				{
					valueCAP = givenPT.checkActiveProcesses();
					if(valueCAP.equals(ActiveProcessRetVal.Error))
					{
						logger.error("Error finishing run");
						givenPT.killAllProcesses();
						return false;
					}
					
					try
					{				
						logger.trace("Pausing main");
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
				
				Long waitTime = NANO_SEC_NEEDED_TO_CLEAN_BROKER * givenPT.numberOfLogicalBrokers();
				sleepLength = (long) (waitTime / 10 / PSTBUtil.MILLISEC_TO_NANOSEC.doubleValue());
				try 
				{				
					logger.trace("Pausing main");
					Thread.sleep(sleepLength);
				} 
				catch (InterruptedException e) 
				{
					logger.error("Error sleeping in main", e);
					return false;
				}
				
				givenPT.clearProcessBuilders();
				logger.info("Cooldown complete");
			}
		}
		
		logger.info("Experiment successful");
		return true;
	}
}


