/**
 * @author padres-dev-4187
 *
 */
package pstb.benchmark;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import pstb.util.NetworkProtocol;

public class PADRESBroker {
	
	private BrokerCore actualBroker; 
	private String host;
	private Integer port;
	private NetworkProtocol protocol;
	
	public PADRESBroker()
	{
		
	}
	
	public boolean developBroker()
	{
		return false;
	}
	
}
