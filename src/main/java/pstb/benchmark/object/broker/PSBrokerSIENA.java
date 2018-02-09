/**
 * 
 */
package pstb.benchmark.object.broker;

import java.util.HashMap;

import pstb.startup.config.NetworkProtocol;

/**
 * The SIENA Broker
 * 
 * @author padres-dev-4187
 */
public class PSBrokerSIENA extends PSBroker {
	// Constants
	private static final long serialVersionUID = 1L;
	
	private HashMap<String, String> neighbourURIsAndIds;

	public PSBrokerSIENA(NetworkProtocol givenProtocol, String givenHost, Integer givenPort, String givenName) 
	{
		super(givenProtocol, givenHost, givenPort, givenName);
		
		neighbourURIsAndIds = new HashMap<String, String>();
	}
	
	@Override
	public String getBrokerURI()
	{
		return protocol.toString() + ":" + host + ":" + port.toString(); 
	}
	
	public void updateIdMap(String givenURI, String givenID)
	{
		neighbourURIsAndIds.put(givenURI, givenID);
	}
	
	public String getID(String givenURI)
	{
		return neighbourURIsAndIds.get(givenURI);
	}

}
