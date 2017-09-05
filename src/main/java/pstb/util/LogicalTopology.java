/**
 * @author padres-dev-4187
 * 
 * The main Logical Topology class.
 * Contains a collection of all the NodeRole groups for this topology.
 *
 */
package pstb.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.startup.NonMutuallyConnectedNodes;
import pstb.util.PubSubGroup;

public class LogicalTopology {
	private HashMap<NodeRole, PubSubGroup> network;
	
	private enum ConnectedState{ NOTVISITED, CONNECTED }
	private ArrayList<NonMutuallyConnectedNodes> problemNodes = new ArrayList<NonMutuallyConnectedNodes>();
	
	private static final Logger logger = LogManager.getRootLogger();
	
	/**
	 * Empty constructor
	 */
	public LogicalTopology()
    {
		network = new HashMap<NodeRole, PubSubGroup>();
    }
	
	/**
	 * Adds a new group of nodes to this topology
	 * @param role - the role this group will relate to
	 * @param input - an existing group; be it empty or filled
	 */
	public void addGroup(NodeRole role, PubSubGroup input)
	{
		this.network.put(role, input);
	}
	
	/**
	 * Gets the current group a particular role 
	 * @param role - the role this group 
	 * @returns the group
	 */
	public PubSubGroup getGroup(NodeRole role)
	{
		return network.get(role);
	}
	
	/**
	 * Prints out all the "B" nodes that do not reciprocate each other
	 */
	public void printNonReciprocatingNodes()
	{
		if(!problemNodes.isEmpty())
		{
			problemNodes.forEach((l)->{
				String n = l.getProblematicNode();
				String mC = l.getMissingConnection();
				System.out.println("Node " + n + " doesn't reciprocate Node " + mC);
			});
		}
		else
		{
			System.out.println("All B Nodes reciprocate each other");
		}
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
	 * @param brokerGroup - the "B" group
	 * @returns true if everything has a reciprocal; false if not
	 */
	public boolean confirmBrokerMutualConnectivity(PubSubGroup brokerGroup)
	{
		boolean allBrokersMC = true;
		for(Iterator<String> it = brokerGroup.keySet().iterator(); it.hasNext();)
		{
			String node = it.next();
			ArrayList<String> connections = brokerGroup.getNodeConnections(node);
			for(int i = 0 ; i < connections.size() ; i++)
			{
				String iTHNode = connections.get(i);
				ArrayList<String> iTHNodesConnections = brokerGroup.getNodeConnections(iTHNode);
				boolean sharedNode = iTHNodesConnections.contains(node);
				if(!sharedNode)
				{
					allBrokersMC = false;
					logger.warn("Topology: Node " + iTHNode + " doesn't reciprocate Node " + node + "'s connection.");
					NonMutuallyConnectedNodes problemPair = new NonMutuallyConnectedNodes(iTHNode, node);
					problemNodes.add(problemPair);
				}
			}
		}
		return allBrokersMC;
	}
	
	/**
	 * Fixes all of the non-reciprocal nodes
	 * If none exist, it throws an exception
	 * Once the fix is complete - the non-reciprocal node list is lost
	 * @param brokerGroup - the "B" group
	 */
	public void fixReciprocations(PubSubGroup brokerGroup)
	{
		if(problemNodes.isEmpty())
		{
			throw new IllegalArgumentException("Unkown how many Non-reciprocating Nodes exit. "
					+ "Please run checkForBrokerReciprocates first.");
		}
		else
		{
			problemNodes.forEach((problemPair)->{
				String node = problemPair.getProblematicNode();
				String missingConnection = problemPair.getMissingConnection();
				
				ArrayList<String> connections = brokerGroup.getNodeConnections(node);
				connections.add(missingConnection);
				brokerGroup.addNewNode(node,connections);
			});
			problemNodes.clear();
			logger.info("Reci fix complete!");
		}
	}
	
	/**
	 * Returns a random node from the Broker ("B") group
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
	 * @param queue - a queue containing the starter node
	 * @param brokerGroup - the "B" group
	 * @param visited - a map of all the nodes and if they have been visited
	 * @returns true if there is no error with the nodes; false if there is
	 */
	private boolean attemptToReachAllBrokerNodes(Queue<String> queue, PubSubGroup brokerGroup, HashMap<String, ConnectedState> visited)
	{
		boolean nodesHaveConnections = true;
		while (!queue.isEmpty())
		{
			String element = queue.remove();
			ArrayList<String> connections = brokerGroup.getNodeConnections(element);
			
			if(connections == null)
			{
				nodesHaveConnections = false;
				break;
			}
			else
			{
				for(int i = 0 ; i < connections.size() ; i++)
				{
					String iNodeLabel = connections.get(i);
					if(visited.get(iNodeLabel) == ConnectedState.NOTVISITED)
					{
						queue.add(connections.get(i));
						visited.put(iNodeLabel, ConnectedState.CONNECTED);
					}
				}
			}
        }
		return nodesHaveConnections;
	}
	
	/**
	 * Look at all the connections of a given group
	 * and see if their resulting matches in the broker "B" group are there
	 * @param givenGroup - the given role group
	 * @param brokerGroup - the "B" group
	 * @returns true if yes; false if no
	 */
	private boolean confirmBrokerNodeExistance(PubSubGroup givenGroup, PubSubGroup brokerGroup)
	{
		boolean neededBrokerNodesExist = false;
		try
		{
			givenGroup.forEach((k, v)->{
				for(int i = 0 ; i < v.size() ; i++)
				{
					if(!brokerGroup.checkNodeIsPresent(v.get(i)))
					{
						throw new IllegalArgumentException("Broker " + v.get(i) + " doesn't exist for client " + k +".");
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
	 * @returns true if yes; false if no
	 */
	public boolean confirmTopoConnectivity()
	{
		boolean topoConnected = true;
		Queue<String> queue = new LinkedList<String>();
		HashMap<String, ConnectedState> visitedBrokers = new HashMap<String, ConnectedState>();
		PubSubGroup brokerGroup = getGroup(NodeRole.B);
		
		brokerGroup.forEach((name, connections)->visitedBrokers.put(name, ConnectedState.NOTVISITED));
		
		String startingNode = randomlySelectBrokerNode(brokerGroup);
		logger.debug("Starting at node: " + startingNode);
				
		visitedBrokers.put(startingNode, ConnectedState.CONNECTED);
		queue.add(startingNode);
		
		boolean attemptFinishedProperly = attemptToReachAllBrokerNodes(queue, brokerGroup, visitedBrokers);
		
        if(!attemptFinishedProperly)
		{
        	logger.error("Topology: Found a node missing connections");
        	topoConnected = false;
		}
        else
        {
        	if(visitedBrokers.containsValue(ConnectedState.NOTVISITED))
        	{
        		logger.error("Topology: Not all nodes were reached");
            	topoConnected = false;
        	}
        	else
        	{
        		logger.info("Topology: The brokers are connected.");
        		logger.info("Topology: Looking at the clients...");
        		
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
