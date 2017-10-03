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
	private HashMap<String, PSBrokerPADRES> genBrokers;
	private HashMap<String, PSClientPADRES> genClients;
	private ArrayList<ProcessBuilder> phyBrokers;
	private ArrayList<ProcessBuilder> phyClients;
	
	private int MEMORY = 1024;
	private String CLIENT_INT = "java -Xmx" + MEMORY + "M -Xverify:none "
								+ "-cp target/NumPub-0.0.1-SNAPSHOT-jar-with-dependencies.jar "
								+ "ca.utoronto.msrg.numpub.examples.cfd.CFDClient ";
	private String BROKER_INT = "screen -dmS broker java -Xmx1024M -Djava.rmi.server.codebase=file:${PADRES_HOME}/build/ " 
									+ "-cp target/NumPub-0.0.1-SNAPSHOT-jar-with-dependencies.jar -Djava.awt.headless=true " 
									+ "-Djava.security.policy=${PADRES_HOME}/etc/java.policy " 
									+ "ca.utoronto.msrg.padres.broker.brokercore.BrokerCore ";
	
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
		genBrokers = new HashMap<String, PSBrokerPADRES>();
		genClients = new HashMap<String, PSClientPADRES>();
		phyBrokers = new ArrayList<ProcessBuilder>();
		phyClients = new ArrayList<ProcessBuilder>();
		
		brokerList = new PubSubGroup();
		publisherList = new PubSubGroup();
		subscriberList = new PubSubGroup();
	}
	
	/**
	 * 
	 * @param distributed
	 * @param givenTopo
	 * @param givenWorkload
	 * @param givenProtocol
	 * @return
	 */
	public boolean developPhysicalTopology(boolean distributed, LogicalTopology givenTopo, NetworkProtocol givenProtocol)
	{
		brokerList = givenTopo.getGroup(NodeRole.B);
		publisherList = givenTopo.getGroup(NodeRole.P);
		subscriberList = givenTopo.getGroup(NodeRole.S);
		
		protocol = givenProtocol;
		
		boolean checkGB = generateBrokers(distributed);
		if(!checkGB)
		{
			logger.error(logHeader + "Error generating physical Brokers");
			return false;
		}
		
		boolean checkGC = generateClients();
		if(!checkGC)
		{
			logger.error(logHeader + "Error generating physical Clients");
			return false;
		}
		
		logger.info(logHeader + "Physical Topology developed successfully");
		return true;
	}
	
	private boolean generateBrokers(boolean givenDis)
	{
		Set<String> setBL = brokerList.keySet();
		Iterator<String> iteratorBL = setBL.iterator();
		
		for( ; iteratorBL.hasNext() ; )
		{
			String brokerI = iteratorBL.next();
			String hostName = getHost(givenDis);
			Integer port = getPort();
			
			PSBrokerPADRES actBrokerI = new PSBrokerPADRES(hostName, port, protocol, brokerI);
			
			genBrokers.put(brokerI, actBrokerI);
		}
		
		Set<String> setGB = genBrokers.keySet();
		Iterator<String> iteratorGB = setGB.iterator();
		for( ; iteratorGB.hasNext(); )
		{
			String brokerIName = iteratorGB.next();
			PSBrokerPADRES brokerI = genBrokers.get(brokerIName);
			
			ArrayList<String> neededURIs = new ArrayList<String>();
			
			ArrayList<String> bIConnectedNodes = brokerList.getNodeConnections(brokerIName);
			
			for(int j = 0 ; j < bIConnectedNodes.size() ; j++)
			{
				String brokerJName = bIConnectedNodes.get(j);
				PSBrokerPADRES actBrokerJ = genBrokers.get(brokerJName);
				if(actBrokerJ == null)
				{
					logger.error(logHeader + "couldn't find " + brokerJName + " in genBrokers that " + brokerIName 
									+ " is connected to.");
					return false;
				}
				neededURIs.add(actBrokerJ.createBrokerURI());
			}
			
			brokerI.setNeighbourURIs((String[]) bIConnectedNodes.toArray());
			
			genBrokers.put(brokerIName, brokerI);
		}
		
		logger.info(logHeader + "All brokers generated");
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
	
	private boolean generateClients()
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
			
			if(genClients.containsKey(subscriberNameI))
			{
				genClients.get(subscriberNameI).addNewClientRole(NodeRole.S);
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
		
		logger.info(logHeader + "All clients generated");
		return true;
	}
	
	private boolean addNewPhyClient(Iterator<String> clientIterator, PubSubGroup clientList, NodeRole givenNR)
	{
		String clientIName = clientIterator.next();
		PSClientPADRES clientI = new PSClientPADRES();
		
		ArrayList<String> clientIConnections = clientList.getNodeConnections(clientIName);
		ArrayList<String> clientIBrokerURIs = new ArrayList<String>();
		
		int numClientIConnections = clientIConnections.size();
		
		if(numClientIConnections <= 0)
		{
			logger.error(logHeader + "Client " + clientIName + " has no connections");
			return false;
		}
		
		for(int j = 0; j < numClientIConnections ; j++)
		{
			String brokerJName = clientIConnections.get(j);
			
			boolean doesBrokerJExist = genBrokers.containsKey(brokerJName);
			
			if(!doesBrokerJExist)
			{
				logger.error(logHeader + "Client " + clientIName + " references a broker " + brokerJName + " that doesn't exist");
				return false;
			}
			
			String brokerJURI = genBrokers.get(brokerJName).createBrokerURI();
			clientIBrokerURIs.add(brokerJURI);
		}
		
		clientI.addClientName(clientIName);
		clientI.addConnectedBrokers(clientIBrokerURIs);
		clientI.addNewClientRole(givenNR);
		
		genClients.put(clientIName, clientI);
		return true;
	}
	
	public boolean addRunLengthToAllClients(Long givenRL)
	{
		if(genClients.isEmpty())
		{
			logger.error(logHeader + " addRunLengthToAllClients() needs clients to be created first.\n" +
							"Please run developPhysicalTopology().");
			return false;
		}
		
		genClients.forEach((clientName, actualClient)->{
			actualClient.addRL(givenRL);
		});
		
		return true;
	}
	
	public boolean addWorkloadToAllClients(Workload givenWorkload)
	{
		if(genClients.isEmpty())
		{
			logger.error(logHeader + " addRunLengthToAllClients() needs clients to be created first.\n" +
							"Please run developPhysicalTopology().");
			return false;
		}
		
		genClients.forEach((clientName, actualClient)->{
			actualClient.addWorkload(givenWorkload);
		});
		
		return true;
	}
	
	public boolean startBrokerAndClientProcesses()
	{
		if(genBrokers.isEmpty())
		{
			logger.error(logHeader + " startBrokers() needs brokers to be created first.\n" +
							"Please run developPhysicalTopology() first.");
			return false;
		}
		
		if(genClients.isEmpty())
		{
			logger.error(logHeader + " startRun() needs clients to be created first.\n" +
							"Please run developPhysicalTopology().");
			return false;
		}
		
		Set<String> setPB = genBrokers.keySet();
		Iterator<String> iteratorPB = setPB.iterator();
		for( ; iteratorPB.hasNext() ; )
		{
			String brokerIName = iteratorPB.next();
			PSBrokerPADRES brokerI = genBrokers.get(brokerIName);
			
			
		}
		
		Set<String> setPC = genClients.keySet();
		Iterator<String> iteratorPC = setPC.iterator();
		for( ; iteratorPC.hasNext() ; )
		{
			String clientIName = iteratorPC.next();
			PSClientPADRES clientI = genClients.get(clientIName);
			
			
		}
		
		
		logger.info(logHeader + " ");
		return true;
	}
	
	public boolean startRun()
	{
		if(phyBrokers.isEmpty())
		{
			logger.error(logHeader + " startRun() needs broker processes to be created first.\n" +
							"Please run startBrokerAndClientProcesses() first.");
			return false;
		}
		
		if(phyClients.isEmpty())
		{
			logger.error(logHeader + " startRun() needs client processes to be created first.\n" +
							"Please run startBrokerAndClientProcesses() first.");
			return false;
		}
		
		logger.info(logHeader + "All brokers started");
		return true;
	}
}
