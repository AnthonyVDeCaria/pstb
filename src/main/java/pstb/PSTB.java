/**
 * @author padres-dev-4187
 *
 */
package pstb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.startup.TopologyFileParser;
import pstb.util.LogicalTopology;
import pstb.util.NodeRole;
import pstb.util.PubSubGroup;
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
			PubSubGroup broker = network.getGroup(NodeRole.B);
			
			logger.info("Starting Topology Testing.");
			boolean reciCheck = network.confirmBrokerMutualConnectivity(broker);
			if(!reciCheck)
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
						network.fixReciprocations(network.getGroup(NodeRole.B));
					}
					catch(IllegalArgumentException e)
					{
						logger.warn("Problem with fixing reciprocations", e);
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
