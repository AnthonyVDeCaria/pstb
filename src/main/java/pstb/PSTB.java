/**
 * @author padres-dev-4187
 *
 */
package pstb;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
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
import pstb.util.UI;
import pstb.util.Workload;

public class PSTB {
	private static final Long MIN_TO_NANOSEC = new Long((long) 6e+10);
	
	private static final Logger logger = LogManager.getRootLogger();
	
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
	 * @param simpleUserInput - the UI scanner used for the yes or no answers
	 * @see TODO: error handling on exit
	 */
	private static void endProgram(Integer errorClassification, Scanner simpleUserInput)
	{
		logger.info("Ending program.");
		simpleUserInput.close();
		System.exit(errorClassification);
	}
	
	/**
	 * The main function
	 * @param args - the arguments to the main function
	 */
	public static void main(String[] args)
	{
		logger.info("Starting program.");
		Scanner simpleUserInput = new Scanner(System.in);
		Properties defaultProp = null;
		
		logger.info("Starting Properties Parsing...");
		try
		{
			defaultProp = loadProperties("etc/defaultBenchmark.properties", null);
		}
		catch (IOException e)
		{
			logger.error("Properties: Couldn't find default properties file", e);
			endProgram(PSTBError.ERROR_BENCHMARK, simpleUserInput);
		}
				
		BenchmarkConfig benchmarkRules = new BenchmarkConfig();
		
		String customBenchProp = "Would you like to use a custom benchmark properties file Y/n?";
		boolean customBenchPropAns = UI.getYNAnswerFromUser(customBenchProp, simpleUserInput);
		if (!customBenchPropAns)
		{
			logger.info("Loading the default Properties file...");
			benchmarkRules.setBenchmarkConfig(defaultProp);
		}
		else
		{
			try
			{
				logger.info("Loading the user Properties file...");
				Properties userProp = loadProperties("src/test/java/userBenchmark.properties", defaultProp);
				benchmarkRules.setBenchmarkConfig(userProp);
			}
			catch (IOException e)
			{
				logger.error("Properties: Couldn't find user properties file", e);
				endProgram(PSTBError.ERROR_BENCHMARK, simpleUserInput);
			}
		}
		
		if(benchmarkRules.checkForNullFields())
		{
			logger.error("Errors loading the properties file!");
			endProgram(PSTBError.ERROR_BENCHMARK, simpleUserInput);
		}
		
		logger.info("No errors loading the Properties file!");
		
		WorkloadFileParser parseWLF = new WorkloadFileParser();
		parseWLF.setPubWorkloadFilesPaths(benchmarkRules.getPubWorkloadFilesPaths());
		parseWLF.setSubWorkloadFilePath(benchmarkRules.getSubWorkloadFilePath());
		
		logger.info("Parsing Workload Files...");
		
		logger.info("Parsing Publisher Workload...");
		boolean pubCheck = parseWLF.parseWorkloadFiles(WorkloadFileType.P);
		logger.info("Parsing Subscriber Workload...");
		boolean subCheck = parseWLF.parseWorkloadFiles(WorkloadFileType.S);
		
		if(!pubCheck)
		{
			logger.error("Publisher Workload File failed parsing!");
			endProgram(PSTBError.ERROR_WORKLOAD, simpleUserInput);
		}
		if(!subCheck)
		{
			logger.error("Subscriber Workload File failed parsing!");
			endProgram(PSTBError.ERROR_WORKLOAD, simpleUserInput);
		}
		
		logger.info("All workload files valid!!");
		
		Workload askedWorkload = parseWLF.getWorkload();
		
		boolean allToposOk = true;
		ArrayList<String> allTopoFiles = benchmarkRules.getTopologyFilesPaths();
		ArrayList<LogicalTopology> allLTs = new ArrayList<LogicalTopology>();
		
		logger.info("Starting to disect Topology Files...");
		
		for(int i = 0 ; i < allTopoFiles.size(); i++)
		{
			String topoI = allTopoFiles.get(i);
			TopologyFileParser parseTopo = new TopologyFileParser(topoI);
			
			logger.info("Parsing Topology File " + topoI + "...");
			
			boolean parseCheck = parseTopo.parse();
			if(!parseCheck)
			{
				allToposOk = false;
				logger.error("Parse Failed for file " + topoI + "!");
			}
			else
			{
				logger.info("Parse Complete for file " + topoI + "!");
				
				LogicalTopology network = parseTopo.getLogicalTopo();
				
				logger.info("Starting Topology Testing with topology " + topoI + "...");
				
				boolean mCCheck = network.confirmBrokerMutualConnectivity();
				if(!mCCheck)
				{
					logger.info("Topology File " + topoI + " has one way connections.");
					
					String fixBroker = topoI + " is not mutually connected\n"
							+ "Would you like us to fix this internally before testing topology Y/n?\n"
							+ "Answering 'n' will terminate the program.";
					
					boolean fixBrokerAns = UI.getYNAnswerFromUser(fixBroker, simpleUserInput);
					if (!fixBrokerAns)
					{
						endProgram(PSTBError.ERROR_TOPO_LOG, simpleUserInput);
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
							endProgram(PSTBError.ERROR_TOPO_LOG, simpleUserInput);
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
					allLTs.add(network);
					logger.info("Topology Check Complete for topology " + topoI + "!");
				}
			}
		}
		
		if(!allToposOk)
		{
			logger.error("Error with topology files!");
			allLTs.clear();
			endProgram(PSTBError.ERROR_TOPO_LOG, simpleUserInput);
		}
		
		logger.info("All topologies valid!!");
		
		ArrayList<NetworkProtocol> askedProtocols = benchmarkRules.getProtocols();
		ArrayList<DistributedState> askedDistributed = benchmarkRules.getDistributed();
		
		logger.info("Beginning to create Physical Topology");
		
		for(int topologyIndex = 0 ; topologyIndex < allLTs.size(); topologyIndex++)
		{
			for(int protocolIndex = 0 ; protocolIndex < askedProtocols.size() ; protocolIndex++)
			{
				PhysicalTopology localPT = new PhysicalTopology();
				PhysicalTopology disPT = new PhysicalTopology();
				boolean checkLocalPT = true;
				boolean checkDisPT = true;
				
				if(askedDistributed.get(topologyIndex).equals(DistributedState.No) 
						|| askedDistributed.get(topologyIndex).equals(DistributedState.Both) )
				{
					checkLocalPT = localPT.developPhysicalTopology(false, allLTs.get(topologyIndex), 
																	askedProtocols.get(protocolIndex));
				}
				if(askedDistributed.get(topologyIndex).equals(DistributedState.Yes) 
						|| askedDistributed.get(topologyIndex).equals(DistributedState.Both) )
				{
					checkDisPT = disPT.developPhysicalTopology(true, allLTs.get(topologyIndex), askedProtocols.get(protocolIndex));
				}
				
				if(!checkDisPT || !checkLocalPT)
				{
					logger.error("Error creating physical topology");
					endProgram(PSTBError.ERROR_TOPO_PHY, simpleUserInput);
				}
				
				logger.info("Beginning experiment");
				boolean successfulExperiment = true;
				
				if(!localPT.isEmpty())
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
					endProgram(PSTBError.ERROR_RUN, simpleUserInput);
				}
			}
		}
		
		endProgram(0, simpleUserInput);
	}
	
	private static boolean runExperiment(PhysicalTopology givenPT, ArrayList<Long> givenRLs, 
											Integer givenNRPE, Workload givenWorkload)
	{
		for(int iRL = 0 ; iRL < givenRLs.size(); iRL++)
		{
			Long iTHRunLength = givenRLs.get(iRL)*MIN_TO_NANOSEC;
			
			boolean functionCheck = givenPT.addRunLengthToClients(iTHRunLength);
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
			
			for(int iNRPE = 0 ; iNRPE < givenNRPE ; iNRPE++)
			{
				functionCheck = givenPT.generateBrokerAndClientProcesses();
				if(!functionCheck)
				{
					logger.error("Error developing processes");
					return false;
				}
				
				functionCheck = givenPT.launchProcesses();
				if(!functionCheck)
				{
					logger.error("Error launching run");
					return false;
				}
				
				Long startTime = System.nanoTime();
				Long currentTime = System.nanoTime();
				while( (currentTime - startTime) < iTHRunLength)
				{
					PhysicalTopology.ActiveProcessRetVal response = givenPT.checkActiveProcesses();
					if(response.equals(ActiveProcessRetVal.Error))
					{
						logger.error("Run had an error");
						givenPT.killAllProcesses();
						return false;
					}
					else if(response.equals(ActiveProcessRetVal.AllOff))
					{
						currentTime = System.nanoTime();
						if((currentTime - startTime) < iTHRunLength)
						{
							logger.error("Error - run finished early");
							return false;
						}
						else
						{
							break;
						}
					}
					
					currentTime = System.nanoTime();
				}
				logger.info("Run successful");
				givenPT.killAllProcesses();
			}
		}
		
		logger.info("Experiment successful");
		return true;
	}
}


