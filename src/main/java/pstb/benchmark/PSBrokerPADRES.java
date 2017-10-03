/**
 * @author padres-dev-4187
 *
 */
package pstb.benchmark;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import pstb.util.NetworkProtocol;

public class PSBrokerPADRES implements java.io.Serializable {
	private String host;
	private Integer port;
	private NetworkProtocol protocol;
	private String brokerName;
	private String[] neighbourURIs;
	private Long runLength;
	
	private BrokerCore actualBroker;
	private BrokerConfig bConfig;
	
	private final String logHeader = "Broker: ";
	private static final Logger brokerLogger = LogManager.getRootLogger();
	
	public PSBrokerPADRES(String newHost, Integer newPort, NetworkProtocol newProtocol, String givenName)
	{
		host = newHost;
		port = newPort;
		protocol = newProtocol;
		brokerName = givenName;
		runLength = new Long(0);
	}
	
	public void setNeighbourURIs(String [] givenNeighbourURIs)
	{
		neighbourURIs = givenNeighbourURIs;
	}

	public void setRunLength(Long runLength)
	{
		this.runLength = runLength;
	}
	
	public String[] getNeighbourURIS()
	{
		return this.neighbourURIs;
	}
	
	public Long getRunLength()
	{
		return runLength;
	}
	
	public boolean createBroker()
	{
		brokerLogger.info(logHeader + "Starting new broker " + brokerName);
		
		try
		{
			bConfig = new BrokerConfig();
		}
		catch(BrokerCoreException e)
		{
			brokerLogger.error(logHeader + "Error creating new broker config for broker " + brokerName, e);
			return false;
		}
		
		bConfig.setBrokerURI(this.createBrokerURI());
		bConfig.setNeighborURIs(neighbourURIs);
		
		try
		{
			actualBroker = new BrokerCore(bConfig);
		}
		catch (BrokerCoreException e)
		{
			brokerLogger.error(logHeader + "Cannot create new broker " + brokerName, e);
			return false;
		}
		
		brokerLogger.info(logHeader + " Created broker " + brokerName);
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
