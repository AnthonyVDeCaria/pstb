/**
 * 
 */
package pstb.startup.topology;

import java.util.ArrayList;

/**
 * @author padres-dev-4187
 *
 */
public class ClientNotes {
	private ArrayList<String> connections;
	private String requestedWorkload;
	
	public ClientNotes()
	{
		connections = new ArrayList<String>();
		requestedWorkload = new String();
	}
	
	/**
	 * Sets the Client's connections
	 * 
	 * @param newConnections -  the connections to set
	 */
	public void setConnections(ArrayList<String> newConnections) {
		this.connections = newConnections;
	}
	
	/**
	 * Sets the Client's workload
	 * 
	 * @param newWorkload - the String of the workload file to set
	 */
	public void setWorkload(String workload) {
		this.requestedWorkload = workload;
	}
	
	/**
	 * Gets the Client's connections
	 * 
	 * @return the connections
	 */
	public ArrayList<String> getConnections() {
		return connections;
	}

	public String getRequestedWorkload() {
		return requestedWorkload;
	}
}
