/**
 * @author padres-dev-4187
 *
 */
package pstb.benchmark;

import java.util.Arrays;

import org.apache.logging.log4j.Logger;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import pstb.util.NetworkProtocol;

/**
 * The Broker Object
 * Handles everything regarding the Broker:
 * 	- Creating it
 * 	- Accessing it's URI
 * 	- Starting it
 * 
 * @author padres-dev-4187
 */
public class PSBrokerPADRES implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	private String host;
	private Integer port;
	private NetworkProtocol protocol;
	private String brokerName;
	private String[] neighbourURIs;
	
	private BrokerCore actualBroker;
	private BrokerConfig bConfig;
	
	private final String logHeader = "Broker: ";
	private Logger brokerLogger;
	
	/**
	 * Broker Constructor
	 * 
	 * @param newHost - this broker's host
	 * @param newPort - this broker's port
	 * @param newProtocol - this broker's protocol
	 * @param givenName - this broker's name
	 */
	public PSBrokerPADRES(String newHost, Integer newPort, NetworkProtocol newProtocol, String givenName)
	{
		host = newHost;
		port = newPort;
		protocol = newProtocol;
		brokerName = givenName;
	}
	
	/**
	 * Sets the URIs associated with this Broker's neighbours
	 *  
	 * @param givenNeighbourURIs - the given URIs
	 */
	public void setNeighbourURIs(String [] givenNeighbourURIs)
	{
		neighbourURIs = givenNeighbourURIs;
	}
	
	/**
	 * Sets the Logger this Broker will use
	 *  
	 * @param phybrokerlogger - the given Logger
	 */
	public void setBrokerLogger(Logger phybrokerlogger)
	{
		brokerLogger = phybrokerlogger;
	}
	
	/**
	 * Gets the URIs associated with this Broker's neighbours
	 * 
	 * @return This neighbour's URIs
	 */
	public String[] getNeighbourURIS()
	{
		return this.neighbourURIs;
	}
	
	/**
	 * Gets the name of this Broker
	 * 
	 * @return this Broker's name
	 */
	public String getName()
	{
		return this.brokerName;
	}
	
	/**
	 * Creates the PADRES BrokerCore
	 * - arguably the actual Broker
	 * 
	 * @return false if there's an error; true otherwise
	 */
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
		
		brokerLogger.info("The neighbour URIs are " + Arrays.toString(neighbourURIs));
		
		bConfig.setBrokerURI(this.getBrokerURI());
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
	
	/**
	 * Starts the BrokerCore
	 * 
	 * @return false if error; true otherwise
	 */
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
	
	/**
	 * Develops the URI of this Broker
	 * 
	 * @return this broker's URI
	 */
	public String getBrokerURI()
	{
		return protocol.toString() + "://" + host + ":" + port.toString() + "/" + brokerName; 
	}
	
}
