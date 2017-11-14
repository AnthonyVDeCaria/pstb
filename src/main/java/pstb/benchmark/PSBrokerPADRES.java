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
import pstb.util.DistributedFlagValue;
import pstb.util.NetworkProtocol;
import pstb.util.PSTBUtil;

/**
 * The Broker Object
 * 
 * Handles everything regarding the Broker: Creating it, Accessing it's URI, and Starting it.
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
	
	private String topologyFilePath;
	private Boolean distributed;
	private Long runLength;
	private Integer runNumber;
	
	private final Boolean INIT_DISTRIBUTED = null;
	private final Long INIT_RUN_LENGTH = new Long(0);
	private final Integer INIT_RUN_NUMBER = new Integer(-1);
	
	private BrokerCore actualBroker;
	private BrokerConfig bConfig;
	
	private final String logHeader = "Broker: ";
	private Logger logger;
	
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
		logger = phybrokerlogger;
	}
	
	/**
	 * Sets a Topology file path (that the diary will need)
	 * 
	 * @param givenTFP - the topologyFilePath to set
	 */
	public void setTopologyFilePath(String givenTFP) 
	{
		topologyFilePath = givenTFP;
	}
	
	/**
	 * Sets a distributed boolean (that the diary will need)
	 * 
	 * @param givenDis - the Distributed value to set
	 */
	public void setDistributed(Boolean givenDis)
	{
		distributed = givenDis;
	}
	
	/**
	 * Sets the runLength value
	 * 
	 * @param givenRL - the rL value to be set
	 */
	public void setRunLength(Long givenRL)
	{
		runLength = givenRL;
	}
	
	/**
	 * Adds the run number
	 * 
	 * @param givenRN - the given run number
	 */
	public void setRunNumber(Integer givenRN)
	{
		runNumber = givenRN;
	}
	
	/**
	 * Gets the URIs associated with this Broker's neighbours
	 * 
	 * @return This neighbour's URIs
	 */
	public String[] getNeighbourURIS()
	{
		return neighbourURIs;
	}
	
	/**
	 * Gets the name of this Broker
	 * 
	 * @return this Broker's name
	 */
	public String getName()
	{
		return brokerName;
	}
	
	/**
	 * Gets the run length
	 * 
	 * @return the run length
	 * 
	 */
	public Long getRunLength()
	{
		return runLength;
	}
	
	/**
	 * Creates the PADRES BrokerCore
	 * - arguably the actual Broker
	 * 
	 * @return false if there's an error; true otherwise
	 */
	public boolean createBroker()
	{
		logger.info(logHeader + "Starting new broker " + brokerName);
		
		try
		{
			bConfig = new BrokerConfig();
		}
		catch(BrokerCoreException e)
		{
			logger.error(logHeader + "Error creating new broker config for broker " + brokerName, e);
			return false;
		}
		
		logger.info("The neighbour URIs are " + Arrays.toString(neighbourURIs));
		
		bConfig.setBrokerURI(this.getBrokerURI());
		bConfig.setNeighborURIs(neighbourURIs);
		
		try
		{
			actualBroker = new BrokerCore(bConfig);
		}
		catch (BrokerCoreException e)
		{
			logger.error(logHeader + "Cannot create new broker " + brokerName, e);
			return false;
		}
		
		logger.info(logHeader + " Created broker " + brokerName);
		return true;
	}
	
	/**
	 * Starts the BrokerCore
	 * 
	 * @return false if error; true otherwise
	 */
	public boolean startBroker()
	{
		logger.info(logHeader + "attempting to add broker " + brokerName + " to network");
		try
		{
			actualBroker.initialize();
		}
		catch (BrokerCoreException e)
		{
			logger.error(logHeader + "Error starting broker " + brokerName, e);
			return false;
		}
		logger.info(logHeader + "Added broker " + brokerName + " to network");
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
	
	public String generateContext()
	{
		//Check that we have everything
		if(runLength.equals(INIT_RUN_LENGTH) 
				|| runNumber.equals(INIT_RUN_NUMBER) 
				|| topologyFilePath.isEmpty()
				|| distributed.equals(INIT_DISTRIBUTED)
			)
		{
			logger.error(logHeader + "Not pieces of context has been set");
			return null;
		}
		// We do

		// Check that we can access the distributed
		DistributedFlagValue distributedFlag = null;
		if(distributed.booleanValue() == true)
		{
			distributedFlag = DistributedFlagValue.D;
		}
		else if(distributed.booleanValue() == false)
		{
			distributedFlag = DistributedFlagValue.L;
		}
		else
		{
			logger.error(logHeader + "error with distributed");
			return null;
		}
		// We can - it's now in a flag enum
		// WHY a flag enum: to collapse the space to two values
		
		// Convert the nanosecond runLength into milliseconds
		// WHY: neatness / it's what the user gave us->why confuse them?
		Long milliRunLength = (long) (runLength / PSTBUtil.MILLISEC_TO_NANOSEC.doubleValue());
		
		return topologyFilePath + "-"
				+ distributedFlag.toString() + "-"
				+ protocol.toString() + "-"
				+ milliRunLength.toString() + "-"
				+ runNumber.toString() + "-"
				+ brokerName;
	}
}
