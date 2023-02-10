/**
 * 
 */
package pstb.creation.topology;

import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import pstb.benchmark.object.broker.PSBroker;
import pstb.benchmark.object.broker.PSBrokerSIENA;
import pstb.benchmark.object.client.PSClient;
import pstb.benchmark.object.client.siena.PSClientSIENA;
import pstb.startup.config.ExperimentType;
import pstb.startup.config.NetworkProtocol;
import pstb.startup.config.SupportedEngines.PSEngine;
import pstb.startup.distributed.Machine;
import pstb.startup.topology.LogicalTopology;
import pstb.startup.topology.VisitedState;
import pstb.startup.workload.PSAction;
import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 *
 */
public class SIENATopology extends PhysicalTopology {
    // Constants - Supported Protocols
    public static final Set<NetworkProtocol> SUPPORTED_PROTOCOLS = EnumSet.of(NetworkProtocol.ka, NetworkProtocol.tcp, NetworkProtocol.udp);
    
    /**
     * SIENATopology Constructor
     * 
     * @param givenTopo - the LogicalTopology this PhysicalTopology is build on top of
     * @param givenProtocol - the NetworkProtocol this experiment will use
     * @param givenUser
     * @param givenMachines
     * @param givenWorkload
     * @param givenBST
     * @param givenTFS
     * @throws UnknownHostException
     */
    public SIENATopology(ExperimentType givenMode, LogicalTopology givenTopo,  
            NetworkProtocol givenProtocol, String givenUser, ArrayList<Machine> givenMachines, 
            HashMap<String, ArrayList<PSAction>> givenWorkload, 
            String givenBST, String givenTFS, ServerSocket givenSS) throws Exception 
    {
        super(givenMode, givenTopo, givenProtocol, givenUser, givenMachines, givenWorkload, givenBST, givenTFS, givenSS);
        
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
    
    /**
     * Attempts to reach all of the nodes in the Broker ("B") group
     * It attempts to do so using a variation of the Breadth First Search
     * If a node doesn't have any connections; the attempt fails and returns false
     * (Technically this should be caught by TopologyFileParser;
     * however, this exists as a just in case)
     * 
     * @param brokerGroup - the "B" group
     * @param brokerVisitedList - a map of all the nodes and if they have been visited
     * @return true if the attempt runs through successfully; false if not
     */
    @Override
    protected boolean connectAllBrokers() 
    {    
        Queue<String> queue = new LinkedList<String>();
        
        HashMap<String, VisitedState> visitedBrokerNodes = new HashMap<String, VisitedState>();
        activeBrokers.forEach((name, connections)->visitedBrokerNodes.put(name, VisitedState.NOTVISITED));
        
        String[] brokerNodes = activeBrokers.keySet().toArray(new String[activeBrokers.size()]);
        String startingNode = PSTBUtil.randomlySelectString(brokerNodes);
        
        logger.debug(logHeader + "Starting at node: " + startingNode +".");        
        visitedBrokerNodes.put(startingNode, VisitedState.VISITED);
        queue.add(startingNode);
        
        while (!queue.isEmpty())
        {
            String elementI = queue.remove();
            visitedBrokerNodes.put(elementI, VisitedState.VISITED);
            
            PSBrokerSIENA brokerI = (PSBrokerSIENA) brokerObjects.get(elementI);
            String brokerIURI = brokerI.getBrokerURI();
            
            String[] connections = brokerI.getNeighbourURIS();
            if(connections == null)
            {
                /*
                 * This bit of code should never be called
                 * However, it's here in case there are changes in the future.
                 */
                return false;
            }
            else
            {
                for(int j = 0 ; j < connections.length ; j++)
                {
                    String brokerJURI = connections[j];
                    String brokerJID = brokerI.getID(brokerJURI);
                    if(visitedBrokerNodes.get(brokerJID) == VisitedState.NOTVISITED)
                    {
                        queue.add(brokerJID);
                        
                        String[] connectCommand = {"./connectSBrokers.sh", brokerIURI, brokerJID, brokerJURI};
                        
                        Boolean connectCheck = PSTBUtil.createANewProcess(connectCommand, logger, true, true,
                            "Problem connecting brokers " + brokerIURI + " & " + brokerJURI + ":", 
                            "Connected brokers " + brokerIURI + " & " + brokerJURI + ".",
                            "Couldn't connect brokers " + brokerIURI + " & " + brokerJURI + "!");
                        if(connectCheck == null || !connectCheck.booleanValue())
                        {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
}
