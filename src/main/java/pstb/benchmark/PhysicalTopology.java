/**
 * @author padres-dev-4187
 *
 */
package pstb.benchmark;

import pstb.util.LogicalTopology;
import pstb.util.NetworkProtocol;
import pstb.util.NodeRole;
import pstb.util.PubSubGroup;
import pstb.util.Workload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PhysicalTopology {
	private HashMap<String, PSBrokerPADRES> phyBrokers;
	private HashMap<String, PSClientPADRES> phyClients;
	
	private Workload workload;
	private NetworkProtocol protocol;
	private Integer runLength;
	private Long idealMessagePeriod;
	
	private PubSubGroup brokerList; 
	private PubSubGroup publisherList;
	private PubSubGroup subscriberList; 
	
	private static final Integer portStart = 1100;
	private Integer portNum = portStart;
	
	private final String logHeader = "Physical Topology: ";
	private static final Logger logger = LogManager.getRootLogger();
	
	/**
	 * 
	 */
	public PhysicalTopology() 
	{
		phyClients = new HashMap<String, PSClientPADRES>();
		phyBrokers = new HashMap<String, PSBrokerPADRES>();
	}
	
	public boolean initializePhysicalTopology(boolean distributed, LogicalTopology givenTopo, Workload givenWorkload,
												NetworkProtocol givenProtocol, Integer givenRL, Integer givenIMR)
	{
		brokerList = givenTopo.getGroup(NodeRole.B);
		publisherList = givenTopo.getGroup(NodeRole.P);
		subscriberList = givenTopo.getGroup(NodeRole.S);
		
		workload = givenWorkload;
		protocol = givenProtocol;
		runLength = givenRL;
		idealMessagePeriod = convertIMRToIMP(givenIMR);
		
		propogatePhyClients();
		
		return true;
	}
	
	private Long convertIMRToIMP(Integer idealMessageRate)
	{
		return (long)((1 / (double)idealMessageRate) * 60 * 1000);
	}
	
	private boolean propogatePhyBrokers(boolean givenDis)
	{
		Set<String> bSet = brokerList.keySet();
		Iterator<String> bIterator = bSet.iterator();
		
		for( ; bIterator.hasNext() ; )
		{
			String bI = bIterator.next();
			String hostName = getHost(givenDis);
			Integer port = getPort();
		}
		
		
		return false;
	}
	
	private String getHost(boolean distributed)
	{
		String hostName = "localhost";
		
		if(distributed)
		{
			// TODO: handle distributed systems
		}
		
		return hostName;
	}
	
	private Integer getPort()
	{
		Integer retVal = portNum;
		portNum++;
		return retVal;
	}
	
	private boolean propogatePhyClients()
	{
		Set<String> pubSet = publisherList.keySet();
		Iterator<String> pubIterator = pubSet.iterator();
		
		Set<String> subSet = subscriberList.keySet();
		Iterator<String> subIterator = subSet.iterator();
		
		for( ; pubIterator.hasNext() ; )
		{
			boolean addPubCheck = addNewPhyClient(pubIterator, publisherList, NodeRole.P);
			if(!addPubCheck)
			{
				return false;
			}
		}
		
		for( ; subIterator.hasNext() ; )
		{
			String subscriberNameI = subIterator.next();
			
			if(phyClients.containsKey(subscriberNameI))
			{
				phyClients.get(subscriberNameI).addNewClientRole(NodeRole.S);
			}
			else
			{
				boolean addSubCheck = addNewPhyClient(subIterator, subscriberList, NodeRole.S);
				if(!addSubCheck)
				{
					return false;
				}
			}
		}
		
		return true;
	}
	
	private boolean addNewPhyClient(Iterator<String> clientIterator, PubSubGroup clientList, NodeRole givenNR)
	{
		String clientNameI = clientIterator.next();
		PSClientPADRES clientI = new PSClientPADRES();
		clientI.addNewClientRole(givenNR);
		
		ArrayList<String> clientIConnections = clientList.getNodeConnections(clientNameI);
		ArrayList<String> clientIBrokerURIs = new ArrayList<String>();
		for(int i = 0; i < clientIConnections.size() ; i++)
		{
			String brokerI = clientIConnections.get(i);
			String brokerIURI = phyBrokers.get(brokerI).createBrokerURI();
			clientIBrokerURIs.add(brokerIURI);
		}
		
		boolean clientInitCheck = clientI.initialize(clientNameI, clientIBrokerURIs, workload, idealMessagePeriod, 
														runLength);
		if(!clientInitCheck)
		{
			logger.error(logHeader + "Couldn't initialize client " + clientNameI);
			return false;
		}
		
		phyClients.put(clientNameI, clientI);
		return true;
	}
}
