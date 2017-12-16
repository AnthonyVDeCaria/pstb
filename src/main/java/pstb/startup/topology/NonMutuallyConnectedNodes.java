package pstb.startup.topology;

/**
 * @author padres-dev-4187
 * 
 * This class helps the mutually connected functions by creating a class that contains both strings.
 * While it would appear that a HashMap would be sufficient, a given Node might be missing several connections.
 * A HashMap would override these individual connections, leaving just the last connection added.
 * 
 * @see cconfirmBrokerMutualConnectivity
 * @see fixMutualConnectivty
 */
public class NonMutuallyConnectedNodes {
	private String problematicNode;
	private String missingConnection;
	
	/**
	 * Empty Constructor
	 */
	public NonMutuallyConnectedNodes()
	{
		setProblematicNode(new String());
		setMissingConnection(new String());
	}
	
	/**
	 * Full Constructor
	 */
	public NonMutuallyConnectedNodes(String newPN, String newMC)
	{
		setProblematicNode(newPN);
		setMissingConnection(newMC);
	}
	
	/**
	 * Gets the problematic node for this pair
	 * 
	 * @return the problematic node
	 */
	public String getProblematicNode() {
		return problematicNode;
	}

	/**
	 * Gets the missingConnection for this pair
	 * 
	 * @return the missingConnection
	 */
	public String getMissingConnection() {
		return missingConnection;
	}
	
	/**
	 * Sets the problematicNode for this pair
	 * 
	 * @param pN - the name of the problematic node
	 */
	public void setProblematicNode(String pN) {
		this.problematicNode = pN;
	}

	/**
	 * Sets the missingConnection for this pair
	 * 
	 * @param missingConnection - the name of the missingConnection
	 */
	public void setMissingConnection(String mC) {
		this.missingConnection = mC;
	}

}
