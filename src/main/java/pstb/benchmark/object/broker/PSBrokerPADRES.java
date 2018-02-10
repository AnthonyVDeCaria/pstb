package pstb.benchmark.object.broker;

import java.util.Arrays;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import pstb.startup.config.NetworkProtocol;

/**
 * The PADRES Broker
 * 
 * Handles the Broker for the PADRES engine.
 * I.e. functions for creating it, and starting it.
 * Also details how its URI is constructed.
 * 
 * @author padres-dev-4187
 */
public class PSBrokerPADRES extends PSBroker 
{
	// Constants
	private static final long serialVersionUID = 1L;
	
	// Variables set on creation
	private BrokerCore actualBroker;
	private BrokerConfig bConfig;
	
	/**
	 * Broker Constructor
	 * 
	 * @param givenHost - this broker's host
	 * @param givenPort - this broker's port
	 * @param givenProtocol - this broker's protocol
	 * @param givenName - this broker's name
	 */
	public PSBrokerPADRES(NetworkProtocol givenProtocol, String givenHost, Integer givenPort, String givenName)
	{
		super(givenProtocol, givenHost, givenPort, givenName);
		
		actualBroker = null;
		bConfig = null;
		
		logHeader = "PBroker: ";
	}
	
	@Override
	public String getBrokerURI()
	{
		return protocol.toString() + "://" + host + ":" + port.toString() + "/" + nodeName; 
	}
	
	/**
	 * Creates the PADRES BrokerCore - arguably the actual Broker
	 * 
	 * @return false if there's an error; true otherwise
	 */
	public boolean createBroker()
	{
		nodeLog.info(logHeader + "Starting new broker " + nodeName);
		
		try
		{
			bConfig = new BrokerConfig();
		}
		catch(BrokerCoreException e)
		{
			nodeLog.error(logHeader + "Error creating new broker config for broker " + nodeName, e);
			return false;
		}
		
		nodeLog.info("The neighbour URIs are " + Arrays.toString(neighbourURIs));
		
		bConfig.setBrokerURI(this.getBrokerURI());
		bConfig.setNeighborURIs(neighbourURIs);
		
		try
		{
			actualBroker = new BrokerCore(bConfig);
		}
		catch (BrokerCoreException e)
		{
			nodeLog.error(logHeader + "Cannot create new broker " + nodeName, e);
			return false;
		}
		
		nodeLog.info(logHeader + " Created broker " + nodeName);
		return true;
	}
	
	/**
	 * Starts the BrokerCore
	 * 
	 * @return false if error; true otherwise
	 */
	public boolean startBroker()
	{
		if(actualBroker == null)
		{
			nodeLog.error(logHeader + "No BrokerCore object exists for broker " + nodeName + " !");
			return false;
		}
		
		nodeLog.info(logHeader + "attempting to add broker " + nodeName + " to network");
		try
		{
			actualBroker.initialize();
		}
		catch (BrokerCoreException e)
		{
			nodeLog.error(logHeader + "Error starting broker " + nodeName, e);
			return false;
		}
		nodeLog.info(logHeader + "Added broker " + nodeName + " to network");
		return true;
	}
}
