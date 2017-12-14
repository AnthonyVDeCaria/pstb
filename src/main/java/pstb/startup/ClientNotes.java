/**
 * 
 */
package pstb.startup;

import java.util.ArrayList;

/**
 * @author padres-dev-4187
 *
 */
public class ClientNotes {
	private ArrayList<String> connections;
	private ArrayList<ClientRole> roles;
	
	public ClientNotes()
	{
		connections = new ArrayList<String>();
		roles = new ArrayList<ClientRole>();
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
	 * Sets the Client's roles
	 * 
	 * @param newRoles - the roles to set
	 */
	public void setRoles(ArrayList<ClientRole> newRoles) {
		this.roles = newRoles;
	}
	
	/**
	 * Adds a new role for the Client
	 * 
	 * @param newRole - the new ClientRole to add
	 */
	public void addNewRole(ClientRole newRole)
	{
		roles.add(newRole);
	}
	
	/**
	 * Gets the Client's connections
	 * 
	 * @return the connections
	 */
	public ArrayList<String> getConnections() {
		return connections;
	}
	
	/**
	 * Gets the Client's roles
	 * 
	 * @return the roles
	 */
	public ArrayList<ClientRole> getRoles() {
		return roles;
	}
}
