/**
 * @author padres-dev-4187
 *
 */
package pstb.benchmark;

import java.util.ArrayList;

import pstb.util.ClientAction;

public interface PSClient {
	
	public boolean initialize(String givenName);
	
	public boolean shutdown();

	public boolean connect(ArrayList<String> givenBrokerURIs);
	
	public boolean disconnect();
	
	public void startRun();
	
	public boolean handleAction(ClientAction givenAction, String predicates);
	
	public void listen();

	public void addDiaryEntry();
}
