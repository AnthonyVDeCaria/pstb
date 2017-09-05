/**
 * @author padres-dev-4187
 * 
 * This class deals with all of the nodes grouped together by a role - such as B or A.
 * It stores a node's name and what it's connected to.
 * 
 */
package pstb.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.function.BiConsumer;

public class PubSubGroup
{
	private HashMap<String, ArrayList<String>> nodes;

	/**
	 * Empty constructor
	 */
	public PubSubGroup()
    {
		nodes = new HashMap<String, ArrayList<String>>();
    }

	/**
	 * Adds a new node to this group
	 * @param name - the name of the new node
	 * @param connections - the names of the nodes it's connected to
	 */
	public void addNewNode(String name, ArrayList<String> connections) 
	{
		this.nodes.put(name, connections);
	}
	
	/**
	 * Adds a new node to this group
	 * @param name - the name of the new node
	 * @param connections - the names of the nodes it's connected to
	 */
	public void addNewNode(String name, String... connections) 
	{
		ArrayList<String> aLConn = new ArrayList<String>(Arrays.asList(connections));
		addNewNode(name, aLConn);
	}

	/**
	 * Gets a node's connections 
	 * @param nodeName - name of the desired node
	 * @return the node's connections
	 */
	public ArrayList<String> getNodeConnections(String nodeName) 
	{
		return nodes.get(nodeName);
	}
	
	/**
	 * Sees if a node is in this group
	 * @param nodeName - name of the desired node
	 * @return true if it is; false if it's not
	 */
	public boolean checkNodeIsPresent(String nodeName) 
	{
		return nodes.containsKey(nodeName);
	}
	
	/**
	 * Allows an action to be resolved on every node in the group
	 * (A port of the HashMap's forEach)
	 * @param action - the action that's to be applied to every node
	 * @see HashMap
	 */
	public void forEach(BiConsumer<? super String,? super ArrayList<String>> action)
	{
		nodes.forEach(action);
	}
	
	/**
	 * Returns a Set of all the nodes in this group
	 * (A port of the HashMap's keySet())
	 * @returns a Set of all keys 
	 * @see HashMap
	 * @see Set
	 */
	public Set<String> keySet()
	{
		return nodes.keySet();
	}
	
	/**
	 * Returns the number of nodes in this group
	 * (A port of the HashMap's size())
	 * @returns the size of the group
	 * @see HashMap
	 */
	public int size()
	{
		return nodes.size();
	}
	
	/**
	 * Prints out every node and it's connections
	 */
	public void print() 
	{
		this.nodes.forEach((k, v) -> {
			System.out.println("Node " + k + " is connected to: " + v);
		});
	}
}
