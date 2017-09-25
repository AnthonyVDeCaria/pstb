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
	
	private PubSubGroup brokerList; 
	private PubSubGroup publisherList;
	private PubSubGroup subscriberList; 
	
	private static final Integer PORTSTART = 1100;
	private Integer portNum = PORTSTART;
	
	private final String logHeader = "Physical Topology: ";
	private static final Logger logger = LogManager.getRootLogger();
	
	/**
	 * Empty Constructor
	 */
	public PhysicalTopology() 
	{
		phyClients = new HashMap<String, PSClientPADRES>();
		phyBrokers = new HashMap<String, PSBrokerPADRES>();
	}
	
	/**
	 * 
	 * @param distributed
	 * @param givenTopo
	 * @param givenWorkload
	 * @param givenProtocol
	 * @return
	 */
	public boolean developPhysicalTopology(boolean distributed, LogicalTopology givenTopo, Workload givenWorkload,
												NetworkProtocol givenProtocol)
	{
		brokerList = givenTopo.getGroup(NodeRole.B);
		publisherList = givenTopo.getGroup(NodeRole.P);
		subscriberList = givenTopo.getGroup(NodeRole.S);
		
		workload = givenWorkload;
		protocol = givenProtocol;
		
		boolean checkPPB = propogatePhyBrokers(distributed);
		if(!checkPPB)
		{
			logger.error(logHeader + "Error creating physical Brokers");
			return false;
		}
		
		boolean checkPPC = propogatePhyClients();
		if(!checkPPC)
		{
			logger.error(logHeader + "Error creating physical Clients");
			return false;
		}
		
		logger.info(logHeader + "Creating physical topology successful");
		return true;
	}
	
	public boolean startBrokers()
	{
		if(phyBrokers.isEmpty())
		{
			logger.error(logHeader + " startBrokers() needs brokers to be created first.\n" +
							"Please run developPhysicalTopology first.");
			return false;
		}

		Set<String> setPB = phyBrokers.keySet();
		Iterator<String> iteratorPB = setPB.iterator();
		for( ; iteratorPB.hasNext() ; )
		{
			String brokerI = iteratorPB.next();
			PSBrokerPADRES actualBrokerI = phyBrokers.get(brokerI);
			
			boolean checkBrokerStart = actualBrokerI.startBroker();
			if(!checkBrokerStart)
			{
				logger.error(logHeader + "Error starting broker " + brokerI);
				return false;
			}
		}
		
		logger.info(logHeader + "All brokers started");
		return true;
	}
	
	public boolean addRunLengthToAllClients(Long givenRL)
	{
		if(phyClients.isEmpty())
		{
			logger.error(logHeader + " addRunLengthToAllClients() needs clients to be created first.\n" +
							"Please run developPhysicalTopology().");
			return false;
		}
		
		phyClients.forEach((clientName, actualClient)->{
			actualClient.addRL(givenRL);
		});
		
		return true;
	}
	
	public boolean addIMPToAllClients(Long givenIMP)
	{
		if(phyClients.isEmpty())
		{
			logger.error(logHeader + " addIMPToAllClients() needs clients to be created first.\n" +
							"Please run developPhysicalTopology().");
			return false;
		}
		
		phyClients.forEach((clientName, actualClient)->{
			actualClient.addRL(givenIMP);
		});
		
		return true;
	}
	
	public boolean connectClients()
	{
		if(phyClients.isEmpty())
		{
			logger.error(logHeader + " connectClients() needs clients to be created first.\n" +
							"Please run developPhysicalTopology().");
			return false;
		}

		Set<String> setPC = phyClients.keySet();
		Iterator<String> iteratorPC = setPC.iterator();
		for( ; iteratorPC.hasNext() ; )
		{
			String clientI = iteratorPC.next();
			PSClientPADRES actualClientI = phyClients.get(clientI);
			
			boolean checkClientConnect = actualClientI.connect();
			if(!checkClientConnect)
			{
				logger.error(logHeader + "Error connetcing client " + clientI);
				return false;
			}
		}
		
		logger.info(logHeader + "All clients connected");
		return true;
	}
	
	public boolean startRun()
	{
		if(phyClients.isEmpty())
		{
			logger.error(logHeader + " startRun() needs clients to be created first.\n" +
							"Please run developPhysicalTopology().");
			return false;
		}
		
		Set<String> setPC = phyClients.keySet();
		Iterator<String> iteratorPC = setPC.iterator();
		for( ; iteratorPC.hasNext() ; )
		{
			String clientI = iteratorPC.next();
			PSClientPADRES actualClientI = phyClients.get(clientI);
			
			boolean checkStartRun = actualClientI.startRun();
			if(!checkStartRun)
			{
				logger.error(logHeader + "Error connetcing client " + clientI);
				return false;
			}
		}
		
		return true;
	}
	
	private boolean propogatePhyBrokers(boolean givenDis)
	{
		Set<String> setBL = brokerList.keySet();
		Iterator<String> iteratorBL = setBL.iterator();
		
		for( ; iteratorBL.hasNext() ; )
		{
			String brokerI = iteratorBL.next();
			String hostName = getHost(givenDis);
			Integer port = getPort();
			
			PSBrokerPADRES actBrokerI = new PSBrokerPADRES(hostName, port, protocol, brokerI);
			
			phyBrokers.put(brokerI, actBrokerI);
		}
		
		Set<String> setPB = phyBrokers.keySet();
		Iterator<String> iteratorPB = setPB.iterator();
		for( ; iteratorPB.hasNext(); )
		{
			String brokerI = iteratorPB.next();
			ArrayList<String> neededURIs = new ArrayList<String>();
			
			ArrayList<String> bIConnectedNodes = brokerList.getNodeConnections(brokerI);
			
			for(int j = 0 ; j < bIConnectedNodes.size() ; j++)
			{
				String connectedBrokerJ = bIConnectedNodes.get(j);
				PSBrokerPADRES actBrokerJ = phyBrokers.get(connectedBrokerJ);
				if(actBrokerJ == null)
				{
					logger.error(logHeader + "couldn't find " + connectedBrokerJ + " in phyBrokers");
					return false;
				}
				neededURIs.add(actBrokerJ.createBrokerURI());
			}
			
			boolean checkCreateBroker = phyBrokers.get(brokerI).createBroker(neededURIs);
			if(!checkCreateBroker)
			{
				logger.error(logHeader + " couldn't createBroker" + brokerI);
				return false;
			}
		}
		
		logger.info("All brokers created");
		return true;
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
		
		boolean clientInitCheck = clientI.initialize(clientNameI, clientIBrokerURIs, workload);
		if(!clientInitCheck)
		{
			logger.error(logHeader + "Couldn't initialize client " + clientNameI);
			return false;
		}
		
		phyClients.put(clientNameI, clientI);
		return true;
	}
}
