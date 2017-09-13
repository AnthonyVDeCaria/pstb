/**
 * @author padres-dev-4187
 *
 */
package pstb.experiment;

public interface PSClient {
	
	public boolean initializeClient(String ClientName, String BrokerName);

	public boolean addClientToNetwork(String host, int port);
	
	public boolean advertise(String predicates);
	
	public boolean unadvertise(int id);
	
	public boolean subscribe(String predicates);
	
	public int unsubscribe(int id);
	
	public int publish(String predicates);
	
	public void registerListener();

	public void leaveNetwork();
	
	public void shutdownClient();
}
