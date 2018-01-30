/**
 * 
 */
package pstb.creation.topology;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

import pstb.benchmark.object.broker.PSBroker;
import pstb.benchmark.object.broker.PSBrokerSIENA;
import pstb.benchmark.object.client.PSClient;
import pstb.benchmark.object.client.siena.PSClientSIENA;
import pstb.startup.config.NetworkProtocol;
import pstb.startup.config.SupportedEngines.PSEngine;
import pstb.startup.topology.LogicalTopology;
import pstb.startup.workload.PSAction;

/**
 * @author padres-dev-4187
 *
 */
public class SIENATopology extends PhysicalTopology {
	// Constants - Supported Protocols
	public static final Set<NetworkProtocol> SUPPORTED_PROTOCOLS = EnumSet.of(NetworkProtocol.ka, NetworkProtocol.tfp, NetworkProtocol.ufp);
	
	/**
	 * SIENATopology Constructor
	 * 
	 * @param givenTopo - the LogicalTopology this PhysicalTopology is build on top of
	 * @param givenProtocol - the NetworkProtocol this experiment will use
	 * @param givenUser
	 * @param givenHostsAndPorts
	 * @param givenWorkload
	 * @param givenBST
	 * @param givenTFS
	 * @throws UnknownHostException
	 */
	public SIENATopology(LogicalTopology givenTopo,  
			NetworkProtocol givenProtocol, String givenUser, HashMap<String, ArrayList<Integer>> givenHostsAndPorts, 
			HashMap<String, ArrayList<PSAction>> givenWorkload, 
			String givenBST, String givenTFS) throws UnknownHostException 
	{
		super(givenTopo, givenProtocol, givenUser, givenHostsAndPorts, givenWorkload, givenBST, givenTFS);
		
		logHeader = "SIENA Topology: ";
		BROKER_PROCESS_CLASS_NAME = "siena.StartDVDRPServer -id";
		CLIENT_PROCESS_CLASS_NAME = "pstb.benchmark.process.PSTBProcess";
	}
	
	@Override
	public PSBroker createPSBrokerObject(String givenName, String givenHost, Integer givenPort)
	{
		return new PSBrokerSIENA(protocol, givenHost, givenPort, givenName);
	}
	
	@Override
	public void handleAdjacentBrokers(PSBroker givenBroker, ArrayList<String> connectedBrokersNames, String[] connectedBrokersURIs)
	{
		PSBrokerSIENA convertedBroker = (PSBrokerSIENA) givenBroker;
		
		convertedBroker.setNeighbourURIs(connectedBrokersURIs);
		
		for(int i = 0 ; i < connectedBrokersURIs.length ; i++)
		{
			convertedBroker.updateIdMap(connectedBrokersURIs[i], connectedBrokersNames.get(i));
		}
		
	}
	
	@Override
	public PSClient createPSClientObject()
	{
		return new PSClientSIENA();
	}
	
	@Override
	public PSEngine getEngine()
	{
		return PSEngine.SIENA;
	}
}
