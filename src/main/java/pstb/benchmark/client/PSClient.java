/**
 * @author padres-dev-4187
 *
 */
package pstb.benchmark.client;

import java.util.ArrayList;

public interface PSClient {
	
	public boolean initialize(String givenName);
	
	public boolean shutdown();

	public boolean connect(ArrayList<String> givenBrokerURIs);
	
	public boolean disconnect();
	
	public void startRun();
	
//	public boolean handleAction(ClientAction givenAction, PSAction predicates);
}
