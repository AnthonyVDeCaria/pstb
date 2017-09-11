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

import pstb.startup.BenchmarkVariables;
import pstb.startup.TopologyFileParser;
import pstb.util.LogicalTopology;
import pstb.util.UI;

public class PSTB {
	private static final Logger logger = LogManager.getRootLogger();
	
	private static Properties loadProperties(String propertyFile, Properties defaultProperties) throws IOException {
		Properties properties = new Properties(defaultProperties);
		
		FileInputStream propFileStream = new FileInputStream(propertyFile);
		properties.load(propFileStream);
		propFileStream.close();
		
		return properties;
	}
	
	public static void main(String[] args)
	{
		logger.info("Starting program.");
		Scanner simpleUserInput = new Scanner(System.in);
		
		logger.info("Starting Properties Parsing.");
		Properties defaultProp = null;
		
		try
		{
			defaultProp = loadProperties("etc/defaultBenchmark.properties", null);
		}
		catch (IOException e)
		{
			logger.error("Properties: Couldn't load default properties file", e);
		}
		
		if(defaultProp != null)
		{			
			BenchmarkVariables benchmarkRules = new BenchmarkVariables();
			
			String customBenchProp = "Would you like to use a custom benchmark properties file Y/n?";
			boolean customBenchPropAns = UI.getYNAnswerFromUser(customBenchProp, simpleUserInput);
			if (!customBenchPropAns)
			{
				benchmarkRules.setBenchmarkVariable(defaultProp);
			}
			else
			{
				try
				{
					Properties userProp = loadProperties("src/test/java/userBenchmark2.properties", defaultProp);
					benchmarkRules.setBenchmarkVariable(userProp);
				}
				catch (IOException e)
				{
					logger.error("Properties: Couldn't load user properties file", e);
				}
			}
			
			if(benchmarkRules.checkForNullFields())
			{
				logger.error("Error with properties file.");
			}
			else
			{
				logger.info("Starting to disect Topology Files.");
				
				boolean allToposOk = true;
				ArrayList<String> allTopoFiles = benchmarkRules.getTopologyFilesPaths();
				
				for(int i = 0 ; i < allTopoFiles.size(); i++)
				{
					TopologyFileParser parserTopo = new TopologyFileParser();
					String topoI = allTopoFiles.get(i);
					
					logger.info("Parsing Topology File " + topoI + "...");
					boolean parseCheck = parserTopo.parse(topoI);
					
					if(!parseCheck)
					{
						allToposOk = false;
						logger.error("Parse Failed for file " + topoI + "!");
					}
					else
					{
						logger.info("Parse Complete for file " + topoI + "!");
						
						LogicalTopology network = parserTopo.getLogicalTopo();
						
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
								logger.info("Ending program.");
								simpleUserInput.close();
								System.exit(0);
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
									logger.info("Ending program.");
									System.exit(0);
								}
							}
						}
						
						boolean topoCheck = network.confirmTopoConnectivity();
						if(!topoCheck)
						{
							allToposOk = false;
							logger.error("Topology Check Failed for topology " + topoI + "!");
						}
						else
						{
							logger.info("Topology Check Complete for topology " + topoI + "!");
						}
					}
				}
				
				if(allToposOk)
				{
					logger.info("All topologies valid!!");
				}
			}
		}
		
		logger.info("Ending program.");
		simpleUserInput.close();
	}
}
