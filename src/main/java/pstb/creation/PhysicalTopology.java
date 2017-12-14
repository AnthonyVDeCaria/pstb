package pstb.creation;

import pstb.benchmark.broker.PSBrokerPADRES;
import pstb.benchmark.client.PSClientPADRES;
import pstb.creation.server.PSTBServer;
import pstb.startup.ClientNotes;
import pstb.startup.LogicalTopology;
import pstb.startup.NetworkProtocol;
import pstb.startup.Workload;
import pstb.util.PSTBUtil;

import java.io.IOException;
import java.net.InetAddress;
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
public class PhysicalTopology {
	private HashMap<String, PSBrokerPADRES> brokerObjects;
	private HashMap<String, PSClientPADRES> clientObjects;
	private HashMap<String, ProcessBuilder> brokerProcesses;
	private HashMap<String, ProcessBuilder> clientProcesses;
	private HashMap<String, Process> activeBrokers;
	private HashMap<String, Process> activeClients;
	
	private LogicalTopology startingTopology;
	private PSTBServer masterServer;
	
	private NetworkProtocol protocol;
	private Boolean distributed;
	private String topologyFilePath;
	private String benchmarkStartTime;
	private Integer runNumber;
	private final Integer INIT_RUN_NUMBER = -1;
	
	private String user;
	
	private final Integer MEM_CLIENT = 256;
	private final Integer MEM_BROKER = 256;
	
	private String ipAddress;
	private final Integer LOCAL_START_PORT = 1100;
	private Integer localPortNum = LOCAL_START_PORT;
	private HashMap<String, ArrayList<Integer>> hostsAndPorts;
	private ArrayList<String> freeHosts;
	private int freeHostI = -1;
	private HashMap<String, Integer> hostIsPortJ;
	
	private ArrayList<String> givenMachines;
	private int givenMachineI = -1;
	private HashMap<String, String> nodeMachine;
	
	private final int INIT_TERMINATION_VALUE = 9999;
	
	private final String logHeader = "Physical Topology: ";
	private final Logger logger = LogManager.getRootLogger();
	
	/**
	 * Empty Constructor
	 */
	public PhysicalTopology() throws UnknownHostException
	{
		brokerObjects = new HashMap<String, PSBrokerPADRES>();
		clientObjects = new HashMap<String, PSClientPADRES>();
		brokerProcesses = new HashMap<String, ProcessBuilder>();
		clientProcesses = new HashMap<String, ProcessBuilder>();
		activeBrokers = new HashMap<String, Process>();
		activeClients = new HashMap<String, Process>();
		
		startingTopology = new LogicalTopology();
		masterServer = null;
		
		runNumber = INIT_RUN_NUMBER;
		protocol = null;
		distributed = null;
		topologyFilePath = new String();
		benchmarkStartTime = new String();
		
		hostsAndPorts = new HashMap<String, ArrayList<Integer>>();
		freeHosts = new ArrayList<String>();
		hostIsPortJ = new HashMap<String, Integer>();
		
		givenMachines = new ArrayList<String>();
		nodeMachine = new HashMap<String, String>();
		
		InetAddress masterAddress = InetAddress.getLocalHost();
		ipAddress = masterAddress.getHostAddress();
	}
	
	/**
	 * Sets the Run Number
	 * 
	 * @param runNum - the given Run Number
	 */
	public void setRunNumber(Integer runNum)
	{
		runNumber = runNum;
	}
	
	public void setUsername(String givenUsername) 
	{
		user = givenUsername;
	}
	
	/**
	 * Gets the Run Number
	 * 
	 * @return the Run Number
	 */
	public Integer getRunNumber()
	{
		return this.runNumber;
	}
	
	/**
	 * Gets the Distributed Boolean
	 * 
	 * @return the Distributed Boolean
	 */
	public Boolean getDistributed()
	{
		return distributed;
	}
	
	/**
	 * Gets the NetworkProtocol
	 * 
	 * @return the NetworkProtocol
	 */
	public NetworkProtocol getProtocol() 
	{
		return protocol;
	}
	
	/**
	 * Gets the Topology File Path
	 * 
	 * @return the TopologyFilePath
	 */
	public String getTopologyFilePath() 
	{
		return topologyFilePath;
	}
	
	public String getUser() 
	{
		return user;
	}
	
	/**
	 * Determines if the Broker and Client objects have been created
	 * 
	 * @return true is they are; false if they aren't
	 */
	public boolean doObjectsExist()
	{
		return !brokerObjects.isEmpty() && !clientObjects.isEmpty();
	}
	
	/**
	 * Determines if the Broker and Client ProcessBuilders have been created
	 * 
	 * @return true is they are; false if they aren't
	 */
	public boolean doProcessBuildersExist()
	{
		return !brokerProcesses.isEmpty() && !clientProcesses.isEmpty();
	}
	
	/**
	 * Returns the current number of brokers in the Logical Topology
	 * 
	 * @return the number of Broker Objects
	 */
	public Integer numberOfLogicalBrokers()
	{
		return startingTopology.getBrokers().size();
	}
	
	public boolean doesBrokerObjectExist(String brokerName)
	{
		return brokerObjects.containsKey(brokerName);
	}
	
	public PSBrokerPADRES getParticularBroker(String brokerName)
	{
		return brokerObjects.get(brokerName);
	}
	
	public boolean doesClientObjectExist(String clientName)
	{
		return clientObjects.containsKey(clientName);
	}
	
	public PSClientPADRES getParticularClient(String clientName)
	{
		return clientObjects.get(clientName);
	}
	
	public Process getParticularNodeProcess(String nodeName)
	{
		if(activeBrokers.containsKey(nodeName))
		{
			return activeBrokers.get(nodeName);
		}
		else if(activeClients.containsKey(nodeName))
		{
			return activeClients.get(nodeName);
		}
		else
		{
			return null;
		}
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
												String givenBST)
	{
		startingTopology = givenTopo;
		
		protocol = givenProtocol;
		distributed = givenDistributed;
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
			
			PSBrokerPADRES actBrokerI = new PSBrokerPADRES(hostName, port, protocol, brokerI);
			
			brokerObjects.put(brokerI, actBrokerI);
			
			nodeMachine.put(brokerI, hostName);
		}
		
		// Now loop through the brokerObject, accessing the brokerList to find a given broker's connections
		Iterator<String> iteratorBO = brokerObjects.keySet().iterator();
		for( ; iteratorBO.hasNext() ; )
		{
			String brokerIName = iteratorBO.next();
			PSBrokerPADRES brokerI = brokerObjects.get(brokerIName);
			ArrayList<String> neededURIs = new ArrayList<String>();
			
			ArrayList<String> bIConnectedNodes = brokerList.get(brokerIName);
			for(int j = 0 ; j < bIConnectedNodes.size() ; j++)
			{
				String brokerJName = bIConnectedNodes.get(j);
				PSBrokerPADRES actBrokerJ = brokerObjects.get(brokerJName);
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
	 * Gets a host for broker development
	 * 
	 * @param distributed
	 * @return the host's name
	 */
	private String getBrokerHost()
	{
		String hostName = "localhost";
		
		if(distributed)
		{
			freeHostI++;
			
			if(freeHostI >= freeHosts.size())
			{
				freeHostI = 0;
			}
			
			hostName = freeHosts.get(freeHostI);
		}
		
		return hostName;
	}
	
	/**
	 * Gets a port for broker development 
	 * 
	 * @param givenHost - the 
	 * @return the port number 
	 */
	private Integer getBrokerPort(String givenHost)
	{
		Integer retVal = localPortNum;
		
		if(givenHost.equals(PSTBUtil.LOCAL))
		{
			localPortNum++;
		}
		else
		{
			Integer j = hostIsPortJ.get(givenHost);
			ArrayList<Integer> portsForGivenHost = hostsAndPorts.get(givenHost);
			retVal = portsForGivenHost.get(j);
			
			j++;
			if(j >= portsForGivenHost.size())
			{
				hostIsPortJ.remove(givenHost);
				freeHosts.remove(givenHost);
			}
			else
			{
				hostIsPortJ.put(givenHost, j);
			}
		}
		
		return retVal;
	}
	
	/**
	 * "Develops" (creates) the Client objects
	 * By which I mean loops through the publisher and subscriber lists
	 * calling for Clients to be created or modified
	 * @see developAndStoreClientObject for the actual Client object creation
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
			PSClientPADRES clientI = new PSClientPADRES();
			
			String clientIName = cliIterator.next();
			ClientNotes clientINotes = clientList.get(clientIName);
			
			ArrayList<String> clientIConnections = clientINotes.getConnections();
			
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
			clientI.addConnectedBrokers(clientIBrokerURIs);
			clientI.setClientRoles(clientINotes.getRoles());
			clientI.setDistributed(distributed);
			clientI.setNetworkProtocol(protocol);
			clientI.setTopologyFilePath(topologyFilePath);
			clientI.setBenchmarkStartTime(benchmarkStartTime);
										
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
	 * Adds a Workload to all Clients
	 * @see Workload
	 * 
	 * @param givenWorkload - the Workload to add
	 * @return false if there's an error; true otherwise
	 */
	public boolean addWorkloadToAllClients(Workload givenWorkload)
	{
		if(clientObjects.isEmpty())
		{
			logger.error(logHeader + "addRunLengthToAllClients() needs clients to be created first. " +
							"Please run developPhysicalTopology().");
			return false;
		}
		
		clientObjects.forEach((clientName, actualClient)->{
			actualClient.addWorkload(givenWorkload);
		});
		
		return true;
	}
	
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
	 * Gets a list of all the client names
	 * 
	 * @return a list of client names
	 */
	public ArrayList<String> getAllClientNames()
	{
		return new ArrayList<String>(clientObjects.keySet());
	}
	
	private String getClientMachine()
	{
		String machineName = "localhost";
		
		if(distributed)
		{
			givenMachineI++;
			
			if(givenMachineI >= givenMachines.size())
			{
				givenMachineI = 0;
			}
			
			machineName = givenMachines.get(givenMachineI);
		}
		
		return machineName;
	}
	
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
		
		setupServer(givenStartSignal);
		
		boolean retVal = generateBrokerAndClientProcesses();
		return retVal;
	}
	
	private void setupServer(CountDownLatch givenStartSignal)
	{
		masterServer = new PSTBServer();
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
			PSBrokerPADRES brokerI = brokerObjects.get(brokerIName);
			
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
			PSClientPADRES clientI = clientObjects.get(clientIName);
			
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
			
			if(isBroker)
			{
				command.add("1");
				command.add(MEM_BROKER.toString());
				command.add(nodeName);
				
				PSBrokerPADRES broker = (PSBrokerPADRES) node;
				String context = broker.generateContext();
				command.add(context);
			}
			else
			{
				command.add("0");
				command.add(MEM_CLIENT.toString());
				command.add(nodeName);
				
				PSClientPADRES client = (PSClientPADRES) node;
				String diary = client.generateDiaryName();
				
				command.add(diary);
			}
		}
		else
		{
			command.add("./startRemoteNode.sh");
			command.add(user);
			
			String machine = nodeMachine.get(nodeName);
			command.add(machine);
			
			if(isBroker)
			{
				command.add("1");
				command.add(MEM_BROKER.toString());
				command.add(nodeName);
				
				PSBrokerPADRES broker = (PSBrokerPADRES) node;
				String context = broker.generateContext();
				
				command.add(context);
			}
			else
			{
				command.add("0");
				command.add(MEM_CLIENT.toString());
				command.add(nodeName);
				
				PSClientPADRES client = (PSClientPADRES) node;
				String diary = client.generateDiaryName();
				
				command.add(diary);
			}
		}
		
		command.add(ipAddress);
		
		Integer port = masterServer.getPort();
		if(port == null)
		{
			return null;
		}
		command.add(port.toString());
		
		if(user == null || user.isEmpty())
		{
			return null;
		}
		command.add(user);
		
		String[] finalCommand = command.toArray(new String[0]);
		
		ProcessBuilder createdProcessShell = new ProcessBuilder(finalCommand);
		
		createdProcessShell.redirectErrorStream(true);
		
		return createdProcessShell;
	}
	
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
	 * Launches all of the Processes generated in generateBrokerAndClientProcesses()
	 * ... by which I mean call the function that actually starts the Processes
	 * @see generateBrokerAndClientProcesses
	 * @see startProcesses
	 * 
	 * @return false if there's an error; true otherwise
	 */
	private boolean startProcesses()
	{		
		Iterator<String> brokerIterator = brokerProcesses.keySet().iterator();
		for( ; brokerIterator.hasNext() ; )
		{
			String brokerIName = brokerIterator.next();
			ProcessBuilder brokerIProcess = brokerProcesses.get(brokerIName);
			
			boolean handleCheck = startGivenNodeProcess(brokerIName, brokerIProcess, true);
			if(!handleCheck)
			{
				logger.error(logHeader + "Couldn't start broker" + brokerIName + "!");
				return false;
			}
		}
		
		Iterator<String> clientIterator = clientProcesses.keySet().iterator();
		for( ; clientIterator.hasNext() ; )
		{
			String clientIName = clientIterator.next();
			ProcessBuilder clientIProcess = clientProcesses.get(clientIName);
			
			boolean handleCheck = startGivenNodeProcess(clientIName, clientIProcess, false);
			if(!handleCheck)
			{
				logger.error(logHeader + "Couldn't start client " + clientIName + "!");
				return false;
			}
		}
		
		logger.info(logHeader + "Everything launched.");
		return true;
	}
	
	private boolean startGivenNodeProcess(String nodeName, ProcessBuilder nodeProcess, boolean isBroker) 
	{
		Process temp;
		try 
		{
			temp = nodeProcess.start();
		} 
		catch (IOException e) 
		{
			logger.error(logHeader + "Couldn't start node " + nodeName + ": ", e);
			return false;
		}
		logger.info(logHeader + "Node " + nodeName + " process started.");
		
		if(isBroker)
		{
			activeBrokers.put(nodeName, temp);
		}
		else
		{
			activeClients.put(nodeName, temp);
		}
		
		return true;
	}

	/**
	 * The values checkActiveProcesses can return
	 */
	public enum ActiveProcessRetVal
	{
		Error, StillRunning, AllOff, FloatingBrokers
	}
	
	/**
	 * Checks the Active Processes:
	 * 1) What is still active?
	 * 2) Where there any errors?
	 * 
	 * @return The given ActiveProcessRetVal associated with the scenario
	 */
	public ActiveProcessRetVal checkActiveProcesses()
	{	
		ArrayList<String> completedBrokers = new ArrayList<String>();
		ArrayList<String> completedClients = new ArrayList<String>();
		
		Boolean brokerCheck;
		Boolean clientCheck;
		boolean brokersOK = true;
		boolean clientsOK = true;
		
		Iterator<String> iteratorAB = activeBrokers.keySet().iterator();
		for( ; iteratorAB.hasNext() ; )
		{
			String brokerIName = iteratorAB.next();
			Process brokerI = activeBrokers.get(brokerIName);
			
			brokerCheck = isNodeActive(true, brokerIName, brokerI);
			if(brokerCheck == null)
			{
				brokersOK = false;
			}
			else if(!brokerCheck.booleanValue())
			{
				completedBrokers.add(brokerIName);
			}
		}
		
		Iterator<String> iteratorAC = activeClients.keySet().iterator();
		for( ; iteratorAC.hasNext() ; )
		{
			String clientIName = iteratorAC.next();
			Process clientI = activeClients.get(clientIName);
			
			clientCheck = isNodeActive(false, clientIName, clientI);
			if(clientCheck == null)
			{
				clientsOK = false;
			}
			else if(!clientCheck.booleanValue())
			{
				completedClients.add(clientIName);
			}
		}
		
		if(!brokersOK || !clientsOK)
		{
			return ActiveProcessRetVal.Error;
		}
		
		completedBrokers.forEach((finishedBroker)->{
			activeBrokers.remove(finishedBroker);
		});
		completedClients.forEach((finishedClient)->{
			activeClients.remove(finishedClient);
		});
		
		if(activeBrokers.isEmpty() && activeClients.isEmpty())
		{
			logger.info(logHeader + "No active processes exist");
			return ActiveProcessRetVal.AllOff;
		}
		else if(!activeBrokers.isEmpty() && activeClients.isEmpty())
		{
			logger.trace(logHeader + "No active clients exist, but brokers remain");
			return ActiveProcessRetVal.FloatingBrokers;
		}
		else if(activeBrokers.isEmpty() && !activeClients.isEmpty())
		{
			logger.error(logHeader + "No brokers exist, but there are still clients that do");
			return ActiveProcessRetVal.Error;
		}
		else
		{
			return ActiveProcessRetVal.StillRunning;
		}
	}
	
	public Boolean isNodeActive(boolean isBroker, String nodeIName, Process nodeI)
	{
		Integer checkProcessInt = null;
		String nodeType = new String();
		if(isBroker)
		{
			checkProcessInt = checkProcessI("pstb.benchmark.broker.PhysicalBroker", nodeIName, nodeI);
			nodeType = "Broker ";
		}
		else
		{
			checkProcessInt = checkProcessI("pstb.benchmark.client.PhysicalClient", nodeIName, nodeI);
			nodeType = "Client ";
		}
		
		if(checkProcessInt == null)
		{
			logger.error(logHeader + "Error running checkProcessI!");
			return null;
		}
		else if(checkProcessInt != 0 && checkProcessInt != INIT_TERMINATION_VALUE)
		{
			logger.error(logHeader + nodeType + nodeIName + " terminated with an error!");
			return null;
		}
		else if(checkProcessInt == 0)
		{
			return false;
		}
		
		return true;
	}
	
	private Integer checkProcessI(String nodeIClass, String nodeIName, Process nodeI)
	{
		int retVal = INIT_TERMINATION_VALUE;
		
		ArrayList<String> command = new ArrayList<String>();
		
		if(!distributed)
		{
			command.add("scripts/checkProcess.sh");
			command.add(nodeIClass + " -n " + nodeIName);
		}
		else
		{
			command.add("scripts/checkMachine.sh"); 
			command.add(user);
			command.add(nodeMachine.get(nodeIName));
			command.add("\"" + nodeIClass + " -n " + nodeIName + "\"");
		}
		
		logger.debug("Command is " + command.toString());
		
		String[] finalCommand = command.toArray(new String [0]);
		Boolean doesProcessExist = PSTBUtil.createANewProcess(finalCommand, logger, false,
											logHeader + "Couldn't create a new process to see if " + nodeIName + " was still running: ",
											logHeader + "Process " + nodeIName + " is still running.", 
											logHeader + "Process " + nodeIName + " is no longer running.");
		
		if(doesProcessExist == null)
		{
			logger.error(logHeader + "Error running initial checking function!");
			return null;
		}
		else if(doesProcessExist.booleanValue())
		{
			return retVal;
		}
		
		try
		{
			retVal = nodeI.exitValue();
		}
	    catch(IllegalThreadStateException e)
	    {
	    	logger.error(logHeader + "Process doesn't exist, but we're being told to wait for a Process!");
			return null;
	    }
		
		logger.debug(logHeader + "exitValue = " + retVal + "."); 
		
		if(retVal != 0 && retVal != INIT_TERMINATION_VALUE)
		{
			logger.error(logHeader + "node " + nodeIName + " terminated with error " + retVal + "!");
		}
		
		return retVal;
	}
	
	/**
	 * Kills all existing processes
	 */
	public void destroyAllProcesses()
	{
		activeClients.forEach((clientI, processI) ->{
			processI.destroy();
		});
		
		activeBrokers.forEach((brokerI, processI) ->{
			processI.destroy();
		});
		
		activeClients.clear();
		activeBrokers.clear();
	}
	
	/**
	 * Clears the process builders
	 */
	public void clearProcessBuilders()
	{
		brokerProcesses.clear();
		clientProcesses.clear();
		masterServer = null;
	}

}
