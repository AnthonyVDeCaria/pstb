package pstb.creation;

import pstb.benchmark.broker.padres.PSTBBrokerPADRES;
import pstb.benchmark.client.padres.PSTBClientPADRES;
import pstb.creation.server.padres.PADRESServer;
import pstb.startup.config.NetworkProtocol;
import pstb.startup.topology.ClientNotes;
import pstb.startup.topology.LogicalTopology;
import pstb.startup.workload.PADRESAction;
import pstb.util.PSTBUtil;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author padres-dev-4187
 * 
 * Handles creating the PhysicalTopology
 * from the Broker and Client Objects
 * to the Broker and Client Processes
 */
public class PADRESTopology extends PhysicalTopology {
	private HashMap<String, PSTBBrokerPADRES> brokerObjects;
	private HashMap<String, PSTBClientPADRES> clientObjects;
	
	private HashMap<String, ArrayList<PADRESAction>> masterWorkload;
	private PADRESServer masterServer;
	
	private final String logHeader = "PADRES Topology: ";
	private final Logger logger = LogManager.getRootLogger();
	
	/**
	 * Empty Constructor
	 */
	public PADRESTopology() throws UnknownHostException
	{
		super();
		
		brokerObjects = new HashMap<String, PSTBBrokerPADRES>();
		clientObjects = new HashMap<String, PSTBClientPADRES>();
		
		masterWorkload = new HashMap<String, ArrayList<PADRESAction>>();
		masterServer = null;
	}
	
	/**
	 * Sees if a Broker Object exists.
	 * (Basically an extension of the HashMap's containsKey() function)
	 * 
	 * @param givenName - the name
	 * @return true if it does; false if it doesn't
	 */
	public boolean doesBrokerObjectExist(String givenName)
	{
		return brokerObjects.containsKey(givenName);
	}
	
	/**
	 * Sees if a Client Object exists.
	 * (Basically an extension of the HashMap's containsKey() function)
	 * 
	 * @param givenName - the name
	 * @return true if it does; false if it doesn't
	 */
	public boolean doesClientObjectExist(String givenName)
	{
		return clientObjects.containsKey(givenName);
	}
	
	/**
	 * Determines if the Broker and Client objects have been created
	 * 
	 * @return true is they are; false if they aren't
	 */
	public boolean doAnyObjectsExist()
	{
		return !brokerObjects.isEmpty() && !clientObjects.isEmpty();
	}
	
	/**
	 * Retrieves a Broker Object.
	 * (Basically an extension of the HashMap's get() function)
	 * 
	 * @param givenName - the name
	 * @return the broker object - which could be null if none exists
	 */
	public PSTBBrokerPADRES getParticularBroker(String givenName)
	{
		return brokerObjects.get(givenName);
	}
	
	/**
	 * Retrieves a Client Object.
	 * (Basically an extension of the HashMap's get() function)
	 * 
	 * @param givenName - the name
	 * @return the client object - which could be null if none exists
	 */
	public PSTBClientPADRES getParticularClient(String givenName)
	{
		return clientObjects.get(givenName);
	}
	
	/**
	 * Develops a list of all the Client Object names
	 * 
	 * @return a list of client names
	 */
	public ArrayList<String> getAllClientObjectNames()
	{
		return new ArrayList<String>(clientObjects.keySet());
	}
	
	/**
	 * This code develops the Physical Topology
	 * I.e. creates all the Broker and Client Objects
	 * using information from the BenchmarkProperties file
	 * 
	 * @param givenDistributed - the distributed flag
	 * @param givenTopo - the LogicalTopology that we must make physical
	 * @param givenProtocol - the messaging protocol
	 * @param givenHostsAndPorts 
	 * @return false if an error occurs, true otherwise
	 */
	public boolean developPhysicalTopology(boolean givenDistributed, LogicalTopology givenTopo, NetworkProtocol givenProtocol,
												String givenTFP, HashMap<String, ArrayList<Integer>> givenHostsAndPorts, 
												String givenBST, HashMap<String, ArrayList<PADRESAction>> pADRESWorkload)
	{
		startingTopology = givenTopo;
		masterWorkload = pADRESWorkload;
		distributed = givenDistributed;
		
		protocol = givenProtocol;
		topologyFilePath = PSTBUtil.cleanTFS(givenTFP);
		benchmarkStartTime = givenBST;
		
		hostsAndPorts = givenHostsAndPorts;
		freeHosts = new ArrayList<String>(givenHostsAndPorts.keySet());
		givenMachines = new ArrayList<String>(givenHostsAndPorts.keySet());
		freeHosts.forEach((host)->{
			hostIsPortJ.put(host, 0);
		});
		
		boolean checkGB = developBrokers();
		if(!checkGB)
		{
			logger.error(logHeader + "Error generating physical Brokers!");
			return false;
		}
		
		boolean checkGC = developClients();
		if(!checkGC)
		{
			logger.error(logHeader + "Error generating physical Clients!");
			return false;
		}
		
		logger.info(logHeader + "Physical Topology developed successfully.");
		return true;
	}
	
	/**
	 * "Develops" (creates) the Broker Objects
	 * 
	 * @return false if there's an error, true otherwise
	 */
	private boolean developBrokers()
	{
		logger.debug(logHeader + "Attempting to develop broker objects");
		
		HashMap<String, ArrayList<String>> brokerList = startingTopology.getBrokers();
		
		// Loop through the brokerList -> that way we can create a bunch of unconnected brokerObjects
		Iterator<String> iteratorBL = brokerList.keySet().iterator();
		for( ; iteratorBL.hasNext() ; )
		{
			String brokerI = iteratorBL.next();
			String hostName = getBrokerHost();
			Integer port = getBrokerPort(hostName);
			
			PSTBBrokerPADRES actBrokerI = new PSTBBrokerPADRES(hostName, port, protocol, brokerI);
			
			brokerObjects.put(brokerI, actBrokerI);
			
			nodeMachine.put(brokerI, hostName);
		}
		
		// Now loop through the brokerObject, accessing the brokerList to find a given broker's connections
		Iterator<String> iteratorBO = brokerObjects.keySet().iterator();
		for( ; iteratorBO.hasNext() ; )
		{
			String brokerIName = iteratorBO.next();
			PSTBBrokerPADRES brokerI = brokerObjects.get(brokerIName);
			ArrayList<String> neededURIs = new ArrayList<String>();
			
			ArrayList<String> bIConnectedNodes = brokerList.get(brokerIName);
			for(int j = 0 ; j < bIConnectedNodes.size() ; j++)
			{
				String brokerJName = bIConnectedNodes.get(j);
				PSTBBrokerPADRES actBrokerJ = brokerObjects.get(brokerJName);
				if(actBrokerJ == null)
				{
					logger.error(logHeader + "couldn't find " + brokerJName + " in genBrokers that " + brokerIName 
									+ " is connected to!");
					nodeMachine.clear();
					return false;
				}
				neededURIs.add(actBrokerJ.getBrokerURI());
			}
			
			String[] nURIs = (String[]) neededURIs.toArray(new String[neededURIs.size()]);
			
			brokerI.setNeighbourURIs(nURIs);
			
			brokerI.setDistributed(distributed);
			brokerI.setTopologyFilePath(topologyFilePath);
			brokerI.setBenchmarkStartTime(benchmarkStartTime);
						
			brokerObjects.put(brokerIName, brokerI);
		}
		
		logger.debug(logHeader + "All broker objects developed");
		return true;
	}
	
	/**
	 * "Develops" (creates) the Client objects
	 * 
	 * @return false if there's an error, true otherwise 
	 */
	private boolean developClients()
	{
		HashMap<String, ClientNotes> clientList = startingTopology.getClients();
		Iterator<String> cliIterator = clientList.keySet().iterator();
		
		// Loop through the publisherList, creating every client that's there
		for( ; cliIterator.hasNext() ; )
		{
			PSTBClientPADRES clientI = new PSTBClientPADRES();
			
			String clientIName = cliIterator.next();
			ClientNotes clientINotes = clientList.get(clientIName);
			
			ArrayList<String> clientIConnections = clientINotes.getConnections();
			
			String workloadFileString = clientINotes.getRequestedWorkload();
			ArrayList<PADRESAction> clientIWorkload = masterWorkload.get(workloadFileString);
			if(clientIWorkload == null)
			{
				logger.error(logHeader + "Client " + clientIName + " is requesting a non-existant workload!");
				return false;
			}
			
			ArrayList<String> clientIBrokerURIs = new ArrayList<String>();
			int numClientIConnections = clientIConnections.size();
			
			if(numClientIConnections <= 0)
			{
				logger.error(logHeader + "Client " + clientIName + " has no connections!");
				return false;
			}
			
			for(int j = 0; j < numClientIConnections ; j++)
			{
				String brokerJName = clientIConnections.get(j);
				
				boolean doesBrokerJExist = brokerObjects.containsKey(brokerJName);
				
				if(!doesBrokerJExist)
				{
					logger.error(logHeader + "Client " + clientIName + " references a broker " + brokerJName + " that doesn't exist");
					return false;
				}
				
				String brokerJURI = brokerObjects.get(brokerJName).getBrokerURI();
				clientIBrokerURIs.add(brokerJURI);
			}
			
			clientI.setClientName(clientIName);
			clientI.setConnectedBrokers(clientIBrokerURIs);
			clientI.setDistributed(distributed);
			clientI.setNetworkProtocol(protocol);
			clientI.setTopologyFilePath(topologyFilePath);
			clientI.setBenchmarkStartTime(benchmarkStartTime);
			clientI.setWorkload(clientIWorkload);
										
			clientObjects.put(clientIName, clientI);
			
			String clientIMachine = getClientMachine();
			nodeMachine.put(clientIName, clientIMachine);
		}
		
		logger.debug(logHeader + "All clients developed");
		return true;
	}
	
	/**
	 * Adds the runLength value to all Clients
	 * @see BenchmarkConfig
	 * 
	 * @param givenRL - the runLength value to add
	 * @return false if there's an error; true otherwise
	 */
	public boolean addRunLengthToAllNodes(Long givenRL)
	{
		if(clientObjects.isEmpty())
		{
			logger.error(logHeader + "addRunLengthToAll() needs clients to be created first. " +
							"Please run developPhysicalTopology().");
			return false;
		}
		
		if(brokerObjects.isEmpty())
		{
			logger.error(logHeader + "addRunLengthToAll() needs brokers to be created first. " +
							"Please run developPhysicalTopology().");
			return false;
		}
		
		clientObjects.forEach((clientName, actualClient)->{
			actualClient.setRunLength(givenRL);
		});
		
		brokerObjects.forEach((brokerName, actualBroker)->{
			actualBroker.setRunLength(givenRL);
		});
		
		return true;
	}
	
	/**
	 * Prepares the topology for a run
	 * 
	 * @param givenStartSignal - the signal that will be used to let key threads know the run has started
	 * @return false if there is an issue; true otherwise
	 */
	public boolean prepareRun(CountDownLatch givenStartSignal)
	{
		if(brokerObjects.isEmpty())
		{
			logger.error(logHeader + "generateBrokerAndClientProcesses() needs brokers to be created first. " +
							"Please run developPhysicalTopology() first!");
			return false;
		}
		
		if(clientObjects.isEmpty())
		{
			logger.error(logHeader + "generateBrokerAndClientProcesses() needs clients to be created first. " +
							"Please run developPhysicalTopology()!");
			return false;
		}
		
		if(runNumber.compareTo(INIT_RUN_NUMBER) <= 0)
		{
			logger.error(logHeader + "generateBrokerAndClientProcesses() needs a runNumber. Please run setRunNumber()!");
			return false;
		}
		
		setupServer(givenStartSignal, runNumber);
		
		boolean retVal = generateBrokerAndClientProcesses();
		return retVal;
	}
	
	/**
	 * Prepares the PSTBServer for the run
	 * 
	 * @param givenStartSignal - the signal that will be used to let key threads know the run has started
	 */
	private void setupServer(CountDownLatch givenStartSignal, int givenRunNumber)
	{
		masterServer = new PADRESServer(givenRunNumber);
		masterServer.generatePort();
		masterServer.setBrokerData(brokerObjects);
		masterServer.setClientData(clientObjects);
		masterServer.setStartSignal(givenStartSignal);
	}
	
	/**
	 * Generates processes using the Broker and Client objects created in developPhysicalTopology()
	 * ... by which I mean call a function to generate ProcessBuilder.
	 * @param start 
	 * 
	 * @return false on error; true otherwise
	 */
	private boolean generateBrokerAndClientProcesses()
	{
		addRunNumberToAllNodes(); // I'm cheating, but this should always return true as we're already checking above for clients.
		
		Iterator<String> iteratorBO = brokerObjects.keySet().iterator();
		for( ; iteratorBO.hasNext() ; )
		{
			String brokerIName = iteratorBO.next();
			PSTBBrokerPADRES brokerI = brokerObjects.get(brokerIName);
			
			ProcessBuilder brokerIProcess = generateNodeProcess(brokerIName, brokerI, true);
			if(brokerIProcess == null)
			{
				logger.error(logHeader + "Couldn't create broker process for " + brokerIName + "!");
				return false;
			}
			else
			{
				brokerProcesses.put(brokerIName, brokerIProcess);
			}
		}
		
		Iterator<String> iteratorCO = clientObjects.keySet().iterator();
		for( ; iteratorCO.hasNext() ; )
		{
			String clientIName = iteratorCO.next();
			PSTBClientPADRES clientI = clientObjects.get(clientIName);
			
			ProcessBuilder clientIProcess = generateNodeProcess(clientIName, clientI, false);
			if(clientIProcess == null)
			{
				logger.error(logHeader + "Couldn't create client process for " + clientIName + "!");
				return false;
			}
			else
			{
				clientProcesses.put(clientIName, clientIProcess);
			}
		}
		
		logger.info(logHeader + "Successfully generated broker and client processes.");
		return true;
	}
	
	/**
	 * Adds a Run Number to all nodes
	 * 
	 * @return false if there's an error; true otherwise
	 */
	private boolean addRunNumberToAllNodes()
	{
		if(clientObjects.isEmpty())
		{
			logger.error(logHeader + "addRunNumberToAll() needs clients to be created first. " +
							"Please run developPhysicalTopology().");
			return false;
		}
		
		if(brokerObjects.isEmpty())
		{
			logger.error(logHeader + "addRunNumberToAll() needs brokers to be created first. " +
							"Please run developPhysicalTopology().");
			return false;
		}
		
		clientObjects.forEach((clientName, actualClient)->{
			actualClient.setRunNumber(runNumber);
		});
		
		brokerObjects.forEach((brokerName, actualBroker)->{
			actualBroker.setRunNumber(runNumber);
		});
		
		return true;
	}
	
	/**
	 * Generates the ProcessBuilder for the requested node
	 * 
	 * @param nodeName - the name of the node
	 * @param isBroker - is this node a Broker?
	 * @return null on error; the requesting ProcessBuilder otherwise
	 */
	private ProcessBuilder generateNodeProcess(String nodeName, Object node, boolean isBroker)
	{
		if(masterServer == null)
		{
			return null;
		}
		
		ArrayList<String> command = new ArrayList<String>();
		
		if(!distributed)
		{
			command.add("./startNode.sh");
			command.add("false");
		}
		else
		{
			command.add("./startRemoteNode.sh");
			
			String machine = nodeMachine.get(nodeName);
			command.add(machine);
		}
		
		if(isBroker)
		{
			command.add("1");
			command.add(MEM_BROKER.toString());
			command.add(nodeName);
			
			PSTBBrokerPADRES broker = (PSTBBrokerPADRES) node;
			String context = broker.generateContext();
			command.add(context);
		}
		else
		{
			command.add("0");
			command.add(MEM_CLIENT.toString());
			command.add(nodeName);
			
			PSTBClientPADRES client = (PSTBClientPADRES) node;
			String diary = client.generateDiaryName();
			command.add(diary);
		}
		
		command.add(ipAddress);
		
		Integer port = masterServer.getPort();
		if(port == null)
		{
			return null;
		}
		command.add(port.toString());
		
		if(user.isEmpty())
		{
			return null;
		}
		command.add(user);
		
		String[] finalCommand = command.toArray(new String[0]);
		
		ProcessBuilder createdProcessShell = new ProcessBuilder(finalCommand);
		
		createdProcessShell.redirectErrorStream(true);
		
		return createdProcessShell;
	}
	
	/**
	 * Starts the run. 
	 * I.e. Starts the PSTBServer and all of the ProcessBuilders.
	 * 
	 * @return false on failure; true otherwise
	 */
	public boolean startRun()
	{
		if(brokerProcesses.isEmpty())
		{
			logger.error(logHeader + "startRun() needs broker processes to be created first! Please run prepareRun()!");
			return false;
		}
		
		if(clientProcesses.isEmpty())
		{
			logger.error(logHeader + "startRun() needs client processes to be created first! Please run prepareRun()!");
			return false;
		}
		
		if(masterServer == null)
		{
			logger.error(logHeader + "startRun() needs a PSTBServer to be created first! Please run prepareRun()!");
			return false;
		}
		
		masterServer.start();
				
		boolean retVal = startProcesses();
		return retVal;
	}
	
	/**
	 * Resets everything after a run is finished
	 */
	public void resetSystemAfterRun()
	{
		destroyAllActiveNodes();
		brokerProcesses.clear();
		clientProcesses.clear();
		masterServer = null;
		runNumber = INIT_RUN_NUMBER;
	}

}
