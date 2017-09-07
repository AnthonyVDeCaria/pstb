/**
 * @author padres-dev-4187
 *
 */
package pstb;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
		logger.info("Starting programm - getting properties.");
		
		Properties defaultProp = null;
		
		try
		{
			defaultProp = loadProperties("etc/defaultBenchmark.properties", null);
		}
		catch (IOException e)
		{
			logger.error("Couldn't load default properties file", e);
		}
		
		if(defaultProp != null)
		{
			String topologyFileName = null;
			
			String customBenchProp = "Would you like to use a custom benchmark properties file Y/n?";
			boolean customBenchPropAns = UI.getYNAnswerFromUser(customBenchProp);
			if (!customBenchPropAns)
			{
				topologyFileName = defaultProp.getProperty("pstb.topologyFileLocation");
			}
			else
			{
				try
				{
					Properties userProp = loadProperties("src/test/java/userBenchmark.properties", defaultProp);
					topologyFileName = userProp.getProperty("pstb.topologyFileLocation");
				}
				catch (IOException e)
				{
					logger.error("Couldn't load user properties file", e);
				}
			}
			
			if(topologyFileName != null)
			{
				logger.info("Starting Topology File Parse.");
				TopologyFileParser parserTopo = new TopologyFileParser();
				boolean parseCheck = parserTopo.parse(topologyFileName);
				if(!parseCheck)
				{
					logger.error("Parse Failed!");
				}
				else
				{
					logger.info("Parse Complete.");
					LogicalTopology network = parserTopo.getLogicalTopo();
					
					logger.info("Starting Topology Testing.");
					boolean mCCheck = network.confirmBrokerMutualConnectivity();
					if(!mCCheck)
					{
						logger.info("Topology File has one way connections.");
						
						String fixBroker = "Would you like to fix Y/n?\n"
								+ "Answering 'n' will terminate this program.";
						boolean fixBrokerAns = UI.getYNAnswerFromUser(fixBroker);
						if (!fixBrokerAns)
						{
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
								logger.warn("Problem forcing mutual connectivity", e);
							}
						}
					}
					
					boolean topoCheck = network.confirmTopoConnectivity();
					if(!topoCheck)
					{
						logger.error("Topology Check Failed!");
					}
					else
					{
						logger.info("Topology Check Complete.");
					}
				}
			}
		}
	}
}
