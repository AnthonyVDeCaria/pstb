package pstb.creation.topology;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

import pstb.benchmark.object.broker.PSBroker;
import pstb.benchmark.object.broker.PSBrokerPADRES;
import pstb.benchmark.object.client.PSClient;
import pstb.benchmark.object.client.padres.PSClientPADRES;
import pstb.startup.config.NetworkProtocol;
import pstb.startup.config.SupportedEngines.PSEngine;
import pstb.startup.topology.LogicalTopology;
import pstb.startup.workload.PSAction;

/**
 * @author padres-dev-4187
 * 
 * Handles creating the PhysicalTopology
 * from the Broker and Client Objects
 * to the Broker and Client Processes
 */
public class PADRESTopology extends PhysicalTopology {
	// Constants - Supported Protocols
	public static final Set<NetworkProtocol> SUPPORTED_PROTOCOLS = EnumSet.of(NetworkProtocol.socket, NetworkProtocol.rmi);
	
	/**
	 * Constructor
	 * 
	 * @param givenTopo - the LogicalTopology that will be used as a basis
	 * @param givenProtocol - the 
	 * @param givenTFS
	 * @param givenHostsAndPorts
	 * @param givenBST
	 * @param givenWorkload
	 * @throws UnknownHostException
	 */
	public PADRESTopology(LogicalTopology givenTopo,  
			NetworkProtocol givenProtocol, String givenUser, HashMap<String, ArrayList<Integer>> givenHostsAndPorts, 
			HashMap<String, ArrayList<PSAction>> givenWorkload, 
			String givenBST, String givenTFS) throws UnknownHostException
	{
		super(givenTopo, givenProtocol, givenUser, givenHostsAndPorts, givenWorkload, givenBST, givenTFS);
		
		logHeader = "PADRES Topology: ";
		BROKER_PROCESS_CLASS_NAME = "pstb.benchmark.process.PSTBProcess";
		CLIENT_PROCESS_CLASS_NAME = "pstb.benchmark.process.PSTBProcess";
	}
	
	@Override
	public PSBroker createPSBrokerObject(String givenName, String givenHost, Integer givenPort)
	{
		return new PSBrokerPADRES(protocol, givenHost, givenPort, givenName);
	}
	
	@Override
	public void handleAdjacentBrokers(PSBroker givenBroker, ArrayList<String> connectedBrokersNames, String[] connectedBrokersURIs)
	{
		givenBroker.setNeighbourURIs(connectedBrokersURIs);
	}
	
	@Override
	public PSClient createPSClientObject()
	{
		return new PSClientPADRES();
	}
	
	@Override
	public PSEngine getEngine()
	{
		return PSEngine.PADRES;
	}
}