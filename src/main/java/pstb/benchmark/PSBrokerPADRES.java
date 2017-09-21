/**
 * @author padres-dev-4187
 *
 */
package pstb.benchmark;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import pstb.util.NetworkProtocol;

public class PSBrokerPADRES {
	private BrokerCore actualBroker; 
	
	private String host;
	private Integer port;
	private NetworkProtocol protocol;
	
	private String brokerName;
	
	private final String logHeader = "Broker: ";
	private static final Logger brokerLogger = LogManager.getRootLogger();
	
	public PSBrokerPADRES(String newHost, Integer newPort, NetworkProtocol newProtocol)
	{
		host = newHost;
		port = newPort;
		protocol = newProtocol;
	}
	
	public boolean developBroker(String givenName)
	{
		brokerLogger.info(logHeader + "");
		return false;
	}
	
	public String createBrokerURI()
	{
		return protocol.toString() + "://" + host + ":" + port.toString() + "/" + brokerName; 
	}
}
