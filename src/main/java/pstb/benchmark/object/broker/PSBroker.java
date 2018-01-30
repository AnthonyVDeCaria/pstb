package pstb.benchmark.object.broker;

import org.apache.logging.log4j.LogManager;

import pstb.benchmark.object.PSNode;
import pstb.benchmark.process.broker.PSTBBrokerProcess;
import pstb.startup.config.NetworkProtocol;

public abstract class PSBroker extends PSNode implements java.io.Serializable 
{
	// Constants
	private static final long serialVersionUID = 1L;
	
	// Variables needed by user to run experiment
	protected String host;
	protected Integer port;
	protected String[] neighbourURIs;
	
	public PSBroker(NetworkProtocol givenProtocol, String givenHost, Integer givenPort, String givenName)
	{
		super();
		
		protocol = givenProtocol;
		nodeName = givenName;
		
		host = givenHost;
		port = givenPort;
		neighbourURIs = null;
		
		nodeLog = LogManager.getLogger(PSTBBrokerProcess.class);
	}
	
	/**
	 * Develops the URI of this Broker
	 * 
	 * @return this broker's URI
	 */
	public abstract String getBrokerURI();
	
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
	 * Gets the URIs associated with this Broker's neighbours
	 * 
	 * @return This neighbour's URIs
	 */
	public String[] getNeighbourURIS()
	{
		return neighbourURIs;
	}
}
