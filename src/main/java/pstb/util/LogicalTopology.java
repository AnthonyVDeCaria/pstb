package pstb.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import org.apache.logging.log4j.Logger;

import pstb.startup.NonMutuallyConnectedNodes;

/**
 * @author padres-dev-4187
 * 
 * The Logical Topology of the network.
 * Contains a collection of all the NodeRole PubSubGroups - I.e. the needed broker and client nodes - that make this topology.
 */
public class LogicalTopology {
	private HashMap<String, ArrayList<String>> brokers;
	private HashMap<String, ClientNotes> clients;
	
	private enum VisitedState{ NOTVISITED, VISITED }
	private ArrayList<NonMutuallyConnectedNodes> problemNodes;
	
	private final String logHeader = "Logical Topology: "; 
	private Logger logger = null;
	
	/**
	 * Empty constructor
	 */
	public LogicalTopology(Logger log)
    {
		logger = log;
		brokers = new HashMap<String, ArrayList<String>>();
		clients = new HashMap<String, ClientNotes>();
		problemNodes = new ArrayList<NonMutuallyConnectedNodes>();
    }
	
	/**
	 * Adds a new node to the topology
	 * 
	 * @param roles - a list of all of this node's roles
	 * @param name - the name of this node
	 * @param connections - what brokers connected to
	 */
	public boolean addNewNodeToTopo(ArrayList<NodeRole> roles, String name, ArrayList<String> connections)
	{
		if(roles.contains(NodeRole.B))
		{
			if(roles.size() > 1)
			{
				return false;
			}
			else
			{
				addNewBroker(name, connections);
			}
		}
		else
		{
			ClientNotes newNote = new ClientNotes();
			
			newNote.setConnections(connections);
			
			ArrayList<ClientRole> newRoles = new ArrayList<ClientRole>();
			roles.forEach((roleI)->{
				ClientRole test = ClientRole.valueOf(roleI.toString());
				newRoles.add(test);
			});
			newNote.setRoles(newRoles);
			
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
	
	public HashMap<String, ArrayList<String>> getBrokers()
	{
		return brokers;
	}
	
	public HashMap<String, ClientNotes> getClients()
	{
		return clients;
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
	 * Looks at all of the nodes in the Broker ("B") group and their connections.
	 * If one node lists another in its connections, but it is not reciprocated
	 * this function makes a note of it.
	 * 
	 * @returns true if everything is mutually connected; false if not; null if there is an error
	 */
	public Boolean confirmBrokerMutualConnectivity()
	{
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
	 * Forces all non-mutually connected nodes to become mutually connected
	 * If none exist, it throws an exception
	 * (Expects confirmBrokerMutualConnectivity() to be run first)
	 * Once the fix is complete - the non-mutually connected list is lost
	 * 
	 * @return false on failure; true otherwise
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
		logger.info("All nodes mutually connected.");
		return true;
	}
	
	/**
	 * Sees if the Logical Topology is connected
	 * It does so by first seeing if all the brokers are connected to each other
	 * and then by seeing if the other roles are attached to existing brokers
	 * 
	 * @return true if yes; false if no
	 */
	public boolean confirmTopoConnectivity()
	{
		boolean topoConnected = true;
		
		HashMap<String, VisitedState> visitedBrokerNodes = new HashMap<String, VisitedState>();
		
		brokers.forEach((name, connections)->visitedBrokerNodes.put(name, VisitedState.NOTVISITED));
		
		logger.info("Topology: Beginning to check broker connectivity.");
		boolean attemptFinishedProperly = attemptToReachAllBrokerNodes(visitedBrokerNodes);
		
        if(!attemptFinishedProperly)
		{
        	logger.error("Topology: Found a node missing connections.");
        	topoConnected = false;
		}
        else
        {
        	if(visitedBrokerNodes.containsValue(VisitedState.NOTVISITED))
        	{
        		logger.error("Topology: Not all nodes were reached.");
            	topoConnected = false;
        	}
        	else
        	{
        		logger.info("Topology: All brokers are connected.");
        		
        		logger.debug("Topology: Looking at client connections...");
        		try
        		{
        			clients.forEach((clientI, clientIsNotes) -> {
        				ArrayList<String> clientIsConnections = clientIsNotes.getConnections();
        				for(int j = 0; j < clientIsConnections.size(); j++)
        				{
        					String brokerJ = clientIsConnections.get(j);
        					
        					if(!brokers.containsKey(brokerJ))
        					{
        						throw new IllegalArgumentException("Broker " + brokerJ
    																	+ " doesn't exist for client " + clientI + ".");
        					}
        				}
            		});
        		}
        		catch (IllegalArgumentException e)
        		{
        			logger.error("Topology: Not all brokers exist for some clients", e);
        			topoConnected = false;
        		}
        		
        		logger.info("Topology: All clients are connected to existing brokers");
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
		
		// To reach all nodes, this code uses a Breadth-First Search
		
		Queue<String> queue = new LinkedList<String>();
		
		String startingNode = randomlySelectBrokerNode();
		logger.debug("Starting at node: " + startingNode);		
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
	
	/**
	 * Returns a random node from the Broker ("B") group
	 * 
	 * @param brokerGroup - the "B" group
	 * @returns the name of a broker node
	 */
	private String randomlySelectBrokerNode()
	{
		Random generator = new Random();
		String[] brokerNodes = brokers.keySet().toArray(new String[brokers.size()]);
		int i = generator.nextInt(brokerNodes.length);
		return  brokerNodes[i];
	}
	
	/**
	 * Returns the size of the network - i.e. how many brokers and clients there are
	 * 
	 * @return the size of the network
	 */
	public int size() 
	{
		return brokers.size() + clients.size();
	}
}
