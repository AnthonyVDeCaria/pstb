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

import pstb.startup.BenchmarkConfig;
import pstb.startup.TopologyFileParser;
import pstb.startup.WorkloadFileParser;
import pstb.util.LogicalTopology;
import pstb.util.UI;

public class PSTB {
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
			endProgram(1, simpleUserInput);
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
				endProgram(1, simpleUserInput);
			}
		}
		
		if(benchmarkRules.checkForNullFields())
		{
			logger.error("Errors loading the properties file!");
			endProgram(1, simpleUserInput);
		}
		
		logger.info("No errors loading the Properties file!");
		
		WorkloadFileParser parseWLF = new WorkloadFileParser();
		
		logger.info("Parsing Workload Files...");
		
		logger.info("Parsing Publisher Workload...");
		boolean pubCheck = parseWLF.parsePublisherFiles(benchmarkRules.getPubWorkloadFilesPaths());
		logger.info("Parsing Subscriber Workload...");
		boolean subCheck = parseWLF.parseSubscriberFile(benchmarkRules.getSubWorkloadFilePath());
		
		if(!pubCheck)
		{
			logger.error("Publisher Workload File failed parsing!");
			endProgram(3, simpleUserInput);
		}
		if(!subCheck)
		{
			logger.error("Subscriber Workload File failed parsing!");
			endProgram(3, simpleUserInput);
		}
		
		logger.info("All workload files valid!!");
		
		boolean allToposOk = true;
		ArrayList<String> allTopoFiles = benchmarkRules.getTopologyFilesPaths();
		ArrayList<LogicalTopology> allTopos = new ArrayList<LogicalTopology>();
		
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
						endProgram(2, simpleUserInput);
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
							endProgram(2, simpleUserInput);
						}
					}
				}
				
				boolean topoCheck = network.confirmTopoConnectivity();
				if(!topoCheck)
				{
					allToposOk = false;
					allTopos.clear();
					logger.error("Topology Check Failed for topology " + topoI + "!");
				}
				else
				{
					allTopos.add(network);
					logger.info("Topology Check Complete for topology " + topoI + "!");
				}
			}
		}
		
		if(!allToposOk)
		{
			logger.error("Error with topology files!");
			allTopos.clear();
			endProgram(2, simpleUserInput);
		}
		
		logger.info("All topologies valid!!");
		
		endProgram(0, simpleUserInput);
	}
}


