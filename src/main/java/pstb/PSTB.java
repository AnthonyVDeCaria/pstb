/**
 * @author padres-dev-4187
 *
 */
package pstb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.startup.TopologyFileParser;
import pstb.util.LogicalTopology;
import pstb.util.UI;

public class PSTB {
	private static final Logger logger = LogManager.getRootLogger();
	
	public static void main(String[] args)
	{
		String topologyFileName = "src/test/java/topologyTest.txt";
		
		TopologyFileParser parserTopo = new TopologyFileParser();
		
		logger.info("Starting Parse.");
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
