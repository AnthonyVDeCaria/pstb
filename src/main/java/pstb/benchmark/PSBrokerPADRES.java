/**
 * @author padres-dev-4187
 *
 */
package pstb.benchmark;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import pstb.util.NetworkProtocol;

public class PSBrokerPADRES {
	private BrokerCore actualBroker; 
	
	private String host;
	private Integer port;
	private NetworkProtocol protocol;
	private String brokerName;
	
	private final String logHeader = "Broker: ";
	private static final Logger brokerLogger = LogManager.getRootLogger();
	
	public PSBrokerPADRES(String newHost, Integer newPort, NetworkProtocol newProtocol, String givenName)
	{
		host = newHost;
		port = newPort;
		protocol = newProtocol;
		brokerName = givenName;
	}
	
	public boolean createBroker(ArrayList<String> neededURIs)
	{
		brokerLogger.info(logHeader + "Starting new broker " + brokerName);
		String brokerCoreArg = "-uri " + this.createBrokerURI() + " -n ";
		for(int i = 0 ; i < neededURIs.size() ; i++)
		{
			brokerCoreArg += neededURIs.get(i);
			if(i != neededURIs.size()-1)
			{
				brokerCoreArg += ", ";
			}
		}
		
		try
		{
			actualBroker = new BrokerCore(brokerCoreArg);
		}
		catch(BrokerCoreException e)
		{
			brokerLogger.error(logHeader + "Error creating broker " + brokerName, e);
			return false;
		}
		
		brokerLogger.info(logHeader + " Created broker "+ brokerName);
		return true;
	}
	
	public boolean startBroker()
	{
		brokerLogger.info(logHeader + "attempting to add broker " + brokerName + " to network");
		try
		{
			actualBroker.initialize();
		}
		catch (BrokerCoreException e)
		{
			brokerLogger.error(logHeader + "Error starting broker " + brokerName, e);
			return false;
		}
		brokerLogger.info(logHeader + "Added broker " + brokerName + " to network");
		return true;
	}
	
	public String createBrokerURI()
	{
		return protocol.toString() + "://" + host + ":" + port.toString() + "/" + brokerName; 
	}
}
