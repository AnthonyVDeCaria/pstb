package pstb.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.Logger;

import pstb.startup.NonMutuallyConnectedNodes;
import pstb.util.PubSubGroup;

/**
 * @author padres-dev-4187
 * 
 * The Logical Topology.
 * Contains a collection of all the NodeRole PubSubGroups that make this topology.
 * I.e. the needed broker and client nodes.
 *
 */
public class LogicalTopology {
	private HashMap<NodeRole, PubSubGroup> network;
	
	private enum VisitedState{ NOTVISITED, VISITED }
	private ArrayList<NonMutuallyConnectedNodes> problemNodes = new ArrayList<NonMutuallyConnectedNodes>();
	
	private Logger logger = null;
	
	/**
	 * Empty constructor
	 */
	public LogicalTopology(Logger log)
    {
		logger = log;
		network = new HashMap<NodeRole, PubSubGroup>();
    }
	
	/**
	 * Adds a new group of nodes to this topology
	 * 
	 * @param role - the role this group will relate to
	 * @param input - an existing group; be it empty or filled
	 */
	public void addGroup(NodeRole role, PubSubGroup input)
	{
		this.network.put(role, input);
	}
	
	/**
	 * Gets the current group a particular role
	 * 
	 * @param role - the role this group 
	 * @returns the group
	 */
	public PubSubGroup getGroup(NodeRole role)
	{
		return network.get(role);
	}
	
	/**
	 * Prints out all the "B" nodes that are not mutually connected
	 */
	public void printNonMutuallyConnectedNodes()
	{
		if(!problemNodes.isEmpty())
		{
			problemNodes.forEach((pair)->{
				String n = pair.getProblematicNode();
				String mC = pair.getMissingConnection();
				System.out.println("Node " + n + " doesn't reciprocate Node " + mC + "'s connection.");
			});
		}
		else
		{
			System.out.println("All B Nodes are mutually connected.");
		}
	}
	
	/**
	 * Allows an action to be resolved on every group in the topology
	 * (A port of the HashMap's forEach)
	 * @param action - the action that's to be applied to every node
	 * @see HashMap
	 */
	public void forEach(BiConsumer<? super NodeRole,? super PubSubGroup> action)
	{
		network.forEach(action);
	}
	
	/**
	 * Adds a new node to the topology
	 * This means it will add the new node to all the groups it belongs to. 
	 * @param roles - a list of all of this node's roles
	 * @param name - the name of this node
	 * @param connections - what iArrays.toString(v))t's connected to
	 */
	public void addNewNodeToTopo(String[] roles, String name, ArrayList<String> connections)
	{
		for(int i = 0 ; i < roles.length ; i++)
		{
			NodeRole role = NodeRole.valueOf(roles[i]);
			PubSubGroup wantedGroup = getGroup(role);
			if(wantedGroup == null)
			{
				wantedGroup = new PubSubGroup();
				addGroup(role, wantedGroup);
			}
			wantedGroup.addNewNode(name, connections);
		}
	}
	
	/**
	 * Looks at all of the nodes in the Broker ("B") group and their connections.
	 * If one node lists another in its connections, but it is not reciprocated
	 * this function makes a note of it.
	 * 
	 * @param brokerGroup - the "B" group
	 * @returns true if everything is mutually connected; false if not
	 */
	public boolean confirmBrokerMutualConnectivity()
	{
		boolean allBrokersMC = true;
		PubSubGroup brokerGroup = getGroup(NodeRole.B);
		for(Iterator<String> brokerNodeIt = brokerGroup.keySet().iterator(); brokerNodeIt.hasNext();)
		{
			String node = brokerNodeIt.next();
			ArrayList<String> connections = brokerGroup.getNodeConnections(node);
			for(int i = 0 ; i < connections.size() ; i++)
			{
				String iTHNode = connections.get(i);
				ArrayList<String> iTHNodesConnections = brokerGroup.getNodeConnections(iTHNode);
				if(!iTHNodesConnections.contains(node))
				{
					allBrokersMC = false;
					logger.warn("Topology: Node " + iTHNode + 
							" does not reciprocate Node " + node + "'s connection.");
					NonMutuallyConnectedNodes problemPair = new NonMutuallyConnectedNodes(iTHNode, node);
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
	 */
	public void forceMutualConnectivity()
	{
		if(problemNodes.isEmpty())
		{
			throw new IllegalArgumentException("No non-mutually connected nodes exist. "
					+ "Was confirmBrokerMutualConnectivity() run first?");
		}
		else
		{
			PubSubGroup brokerGroup = getGroup(NodeRole.B);
			problemNodes.forEach((problemPair)->{
				String node = problemPair.getProblematicNode();
				String missingConnection = problemPair.getMissingConnection();
				
				ArrayList<String> connections = brokerGroup.getNodeConnections(node);
				connections.add(missingConnection);
				brokerGroup.addNewNode(node,connections);
			});
			problemNodes.clear();
			logger.info("All nodes mutually connected!");
		}
	}
	
	/**
	 * Returns a random node from the Broker ("B") group
	 * 
	 * @param brokerGroup - the "B" group
	 * @returns the name of a broker node
	 */
	private String randomlySelectBrokerNode(PubSubGroup brokerGroup)
	{
		Random generator = new Random();
		String[] brokerNodes = brokerGroup.keySet().toArray(new String[brokerGroup.size()]);
		int i = generator.nextInt(brokerNodes.length);
		return  brokerNodes[i];
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
	private boolean attemptToReachAllBrokerNodes(PubSubGroup brokerGroup, HashMap<String, VisitedState> brokerVisitedList)
	{
		boolean nodesHaveConnections = true;
		
		Queue<String> queue = new LinkedList<String>();
		
		String startingNode = randomlySelectBrokerNode(brokerGroup);
		logger.debug("Starting at node: " + startingNode);		
		brokerVisitedList.put(startingNode, VisitedState.VISITED);
		queue.add(startingNode);
		
		/*
		 * The BFS
		 */
		while (!queue.isEmpty())
		{
			String element = queue.remove();
			ArrayList<String> connections = brokerGroup.getNodeConnections(element);
			
			if(connections == null)
			{
				/*
				 * Since TopologyFileParser.parse should be run first, 
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
	 * Look at all the connections of a given group
	 * and see if their resulting matches in the broker "B" group are there
	 * 
	 * @param givenGroup - the given role group
	 * @param brokerGroup - the "B" group
	 * @return true if yes; false if no
	 */
	private boolean confirmBrokerNodeExistance(PubSubGroup givenGroup, PubSubGroup brokerGroup)
	{
		boolean neededBrokerNodesExist = false;
		try
		{
			givenGroup.forEach((clientNode, cNSBrokerList)->{
				for(int i = 0 ; i < cNSBrokerList.size() ; i++)
				{
					String iThBrokerNode = cNSBrokerList.get(i);
					if(!brokerGroup.checkNodeIsPresent(iThBrokerNode))
					{
						throw new IllegalArgumentException("Broker " + iThBrokerNode + 
								" doesn't exist for client " + clientNode +".");
					}
				}
			});
			neededBrokerNodesExist = true;
		}
		catch (IllegalArgumentException e)
		{
			logger.error("Topology: Couldn't find all brokers.", e);
		}
			
		return neededBrokerNodesExist;
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
		PubSubGroup brokerGroup = getGroup(NodeRole.B);
		
		brokerGroup.forEach((name, connections)->visitedBrokerNodes.put(name, VisitedState.NOTVISITED));
		
		logger.info("Topology: Beginning to check broker connectivity.");
		boolean attemptFinishedProperly = attemptToReachAllBrokerNodes(brokerGroup, visitedBrokerNodes);
		
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
        		
        		logger.info("Topology: Looking at client connections...");
        		try
        		{
        			network.forEach((k, v) -> {
            			if(k != NodeRole.B)
            			{
            				boolean vHasItsNodes = confirmBrokerNodeExistance(v, brokerGroup);
            				if(!vHasItsNodes)
            				{
            					throw new IllegalArgumentException();
            				}
            			}
            		});
        			logger.info("Topology: All clients are connected to existing brokers");
        		}
        		catch (IllegalArgumentException e)
        		{
        			logger.error("Topology: Not all brokers exist for some clients");
        			topoConnected = false;
        		}
        	}
        }
		return topoConnected;
    }
}
