package pstb.startup.topology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 * 
 * The Logical Topology of the network.
 * Contains a collection of all the NodeRole PubSubGroups - I.e. the needed broker and client nodes - that make this topology.
 */
public class LogicalTopology {
    private TreeMap<String, ArrayList<String>> brokers;
    private TreeMap<String, ClientNotes> clients;
    
    private ArrayList<NonMutuallyConnectedNodes> problemNodes;
    
    private final String logHeader = "Logical Topology: "; 
    private Logger logger = LogManager.getRootLogger();
    
    /**
     * Empty constructor
     */
    public LogicalTopology()
    {
        brokers = new TreeMap<String, ArrayList<String>>();
        clients = new TreeMap<String, ClientNotes>();
        problemNodes = new ArrayList<NonMutuallyConnectedNodes>();
    }
    
    /**
     * Adds a new node to the topology, assuming that all the roles make sense.
     * I.e. if its a broker node, there is only the Broker role.
     * 
     * @param role - this node's role
     * @param name - the name of this node
     * @param connections - what brokers this node is connected to
     * @param workloadFile - if needed, the workload file of this node 
     */
    public boolean addNewNodeToTopo(NodeRole role, String name, ArrayList<String> connections, String workloadFileString)
    {
        if(role == null || name == null || name.isEmpty())
        {
            logger.error(logHeader + "An key input was given as null or empty!");
            return false;
        }
        
        if(role.equals(NodeRole.B))
        {
            addNewBroker(name, connections);
        }
        else
        {
            if(connections == null || connections.isEmpty())
            {
                logger.error(logHeader + "this client wasn't given broker connections!");
                return false;
            }
            
            if(workloadFileString == null || workloadFileString.isEmpty())
            {
                logger.error(logHeader + "no workload was given for this client!");
                return false;
            }
            
            ClientNotes newNote = new ClientNotes();
            
            newNote.setConnections(connections);
            newNote.setWorkload(workloadFileString);
            
            addNewClient(name, newNote);
        }
        
        return true;
    }
    
    private void addNewBroker(String name, ArrayList<String> connections)
    {
        brokers.put(name, connections);
    }
    
    private void addNewClient(String name, ClientNotes note)
    {
        clients.put(name, note);
    }
    
    public TreeMap<String, ArrayList<String>> getBrokers()
    {
        return brokers;
    }
    
    public TreeMap<String, ClientNotes> getClients()
    {
        return clients;
    }
    
    /**
     * Returns the size of the network - i.e. how many brokers and clients there are
     * 
     * @return the size of the network
     */
    public int networkSize() 
    {
        return brokers.size() + clients.size();
    }
    
    /**
     * Returns the number of brokers there are in the network
     * 
     * @return the number of brokers
     */
    public int numBrokers()
    {
        return brokers.size();
    }
    
    /**
     * Returns the number of clients there are in the network
     * 
     * @return the number of clients
     */
    public int numClients()
    {
        return clients.size();
    }
    
    /**
     * Logs all the logical brokers that are not mutually connected.
     */
    public void logNonMutuallyConnectedNodes()
    {
        if(!problemNodes.isEmpty())
        {
            problemNodes.forEach((pair)->{
                String n = pair.getProblematicNode();
                String mC = pair.getMissingConnection();
                logger.info(logHeader + "Node " + n + " doesn't reciprocate Node " + mC + "'s connection.");
            });
        }
        else
        {
            logger.info("All B Nodes are mutually connected.");
        }
    }
    
    /**
     * Looks at all of the brokers and their connections.
     * If broker A lists broker B in its connections, but it is not reciprocated, this function makes a note of it.
     * If broker A lists broker C in its connections, but broker C doesn't exist in the broker space, 
     * this function returns an error.
     * 
     * @returns true if everything is mutually connected; false if not; null if there is an error
     */
    public Boolean confirmBrokerMutualConnectivity()
    {    
        int numBrokers = brokers.size();
        if(numBrokers == 1)
        {
            logger.error(logHeader + "A single broker is mutually connected - returning true.");
            return true;
        }
        else if(numBrokers == 0)
        {
            logger.error(logHeader + "No brokers exist!");
            return false;
        }
        
        boolean allBrokersMC = true;
        Iterator<String> brokerNodeIt = brokers.keySet().iterator();
        for(; brokerNodeIt.hasNext();)
        {
            String nodeI = brokerNodeIt.next();
            ArrayList<String> isConnections = brokers.get(nodeI);
            for(int j = 0 ; j < isConnections.size() ; j++)
            {
                String nodeJ = isConnections.get(j);
                ArrayList<String> nodeJsConnections = brokers.get(nodeJ);
                if(nodeJsConnections == null)
                {
                    logger.error(logHeader + "Broker " + nodeI + " references Broker " + nodeJ + " which doesn't exist!");
                    return null;
                }
                if(!nodeJsConnections.contains(nodeI))
                {
                    allBrokersMC = false;
                    logger.warn(logHeader + "Node " + nodeJ + " does not reciprocate Node " + nodeI + "'s connection.");
                    NonMutuallyConnectedNodes problemPair = new NonMutuallyConnectedNodes(nodeJ, nodeI);
                    problemNodes.add(problemPair);
                }
            }
        }
        return allBrokersMC;
    }
    
    /**
     * Forces all non-mutually connected nodes to become mutually connected.
     * Once it does so, it destroys the non-mutually connected list.
     * If no mutually connected nodes exist, it returns false. 
     * 
     * @return false if there all nodes are mutually connected; true otherwise
     */
    public boolean forceMutualConnectivity()
    {
        if(problemNodes.isEmpty())
        {
            logger.error("No non-mutually connected nodes exist!");
            return false;
        }
        
        problemNodes.forEach((problemPair)->{
            String node = problemPair.getProblematicNode();
            String missingConnection = problemPair.getMissingConnection();
            
            ArrayList<String> connections = brokers.get(node);
            connections.add(missingConnection);
            brokers.put(node, connections);
        });
        
        problemNodes.clear();
        logger.info(logHeader + "All nodes mutually connected.");
        return true;
    }
    
    /**
     * Sees if the Logical Topology is connected.
     * First, by seeing if all the brokers are connected.
     * Second, by seeing if the clients connect to existing brokers.
     * 
     * @return true if yes; false if no
     */
    public boolean confirmTopoConnectivity()
    {
        int numBrokers = brokers.size();
        if(numBrokers == 1)
        {
            logger.error(logHeader + "A single broker is connected - returning true.");
            return true;
        }
        else if(numBrokers == 0)
        {
            logger.error(logHeader + "No brokers exist!");
            return false;
        }
        
        boolean topoConnected = true;
        
        HashMap<String, VisitedState> visitedBrokerNodes = new HashMap<String, VisitedState>();
        brokers.forEach((name, connections)->visitedBrokerNodes.put(name, VisitedState.NOTVISITED));
        
        logger.info(logHeader + "Beginning to check broker connectivity.");
        boolean attemptFinishedProperly = attemptToReachAllBrokerNodes(visitedBrokerNodes);
        
        if(!attemptFinishedProperly)
        {
            logger.error(logHeader + "Found a node missing connections!");
            topoConnected = false;
        }
        else
        {
            if(visitedBrokerNodes.containsValue(VisitedState.NOTVISITED))
            {
                logger.error(logHeader + "Not all nodes were reached!");
                topoConnected = false;
            }
            else
            {
                logger.info(logHeader + "All brokers are connected.");
                
                logger.debug(logHeader + "Looking at client connections...");
                try
                {
                    clients.forEach((clientI, clientIsNotes) -> {
                        ArrayList<String> clientIsConnections = clientIsNotes.getConnections();
                        for(int j = 0; j < clientIsConnections.size(); j++)
                        {
                            String brokerJ = clientIsConnections.get(j);
                            
                            if(!brokers.containsKey(brokerJ))
                            {
                                throw new IllegalArgumentException("Broker " + brokerJ + " doesn't exist for client " + clientI + "!");
                            }
                        }
                    });
                }
                catch (IllegalArgumentException e)
                {
                    logger.error("Topology: Not all brokers exist for some clients: ", e);
                    topoConnected = false;
                }
                logger.info("Topology: All clients are connected to existing brokers.");
            }
        }
        return topoConnected;
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
    private boolean attemptToReachAllBrokerNodes(HashMap<String, VisitedState> brokerVisitedList)
    {
        boolean nodesHaveConnections = true;
        Queue<String> queue = new LinkedList<String>();
        
        String[] brokerNodes = brokers.keySet().toArray(new String[brokers.size()]);
        String startingNode = PSTBUtil.randomlySelectString(brokerNodes);
        logger.debug(logHeader + "Starting at node: " + startingNode +".");    
        brokerVisitedList.put(startingNode, VisitedState.VISITED);
        queue.add(startingNode);
        
        while (!queue.isEmpty())
        {
            String element = queue.remove();
            ArrayList<String> connections = brokers.get(element);
            
            if(connections == null)
            {
                /*
                 * Since TopologyFileParser.parse() should be run first, 
                 * this bit of code should never be called
                 * However, it's here in case there are changes in the future.
                 */
                nodesHaveConnections = false;
                break;
            }
            else
            {
                for(int i = 0 ; i < connections.size() ; i++)
                {
                    String iNodeLabel = connections.get(i);
                    if(brokerVisitedList.get(iNodeLabel) == VisitedState.NOTVISITED)
                    {
                        queue.add(iNodeLabel);
                        brokerVisitedList.put(iNodeLabel, VisitedState.VISITED);
                    }
                }
            }
        }
        
        return nodesHaveConnections;
    }
}
