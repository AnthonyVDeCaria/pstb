package pstb.benchmark;

import pstb.util.LogicalTopology;
import pstb.util.NetworkProtocol;
import pstb.util.PSTBUtil;
import pstb.util.ClientDiary;
import pstb.util.ClientNotes;
import pstb.util.Workload;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

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
	private ArrayList<ProcessBuilder> brokerPBs;
	private ArrayList<ProcessBuilder> clientPBs;
	private ArrayList<Process> activeBrokers;
	private ArrayList<Process> activeClients;
	
	private int CLIENT_MEMORY = 256;
	private int BROKER_MEMORY = 256;
	private final String DISTRIBUTED_INIT_P1 = "ssh -t -t ";
	private final String DISTRIBUTED_INIT_P2 = "<<-EOF\n" + "cd ~/PSTB"; 
	private final String CLIENT_INT = "java -Xmx" + CLIENT_MEMORY + "M -Xverify:none "
										+ "-cp target/pstb-0.0.1-SNAPSHOT-jar-with-dependencies.jar "
										+ "pstb.benchmark.PhysicalClient ";
	private final String BROKER_INT = "java -Xmx" + BROKER_MEMORY + "M "
										+ "-cp target/pstb-0.0.1-SNAPSHOT-jar-with-dependencies.jar -Djava.awt.headless=true "
										+ "pstb.benchmark.PhysicalBroker ";
	
	private LogicalTopology startingTopology;
	
	private NetworkProtocol protocol;
	private Boolean distributed;
	private String topologyFilePath;
	
	private Integer runNumber;
	private final Integer INIT_RUN_NUMBER = -1;
	
	private final Integer LOCAL_START_PORT = 1100;
	private Integer localPortNum = LOCAL_START_PORT;
	private final String LOCAL = "localhost";
	private HashMap<String, ArrayList<Integer>> brokerPorts;
	private ArrayList<String> freeBrokerHosts;
	private int freeBrokerHostI = -1;
	private HashMap<String, Integer> jForBrokerPorts;
	
	private ArrayList<String> clientMachines;
	private int clientMachineI = -1;
	private HashMap<String, String> nodeLocation;
	private HashMap<String, Integer> nodeObjectPort;
	private HashMap<String, Socket> nodeSocket;
	private HashMap<String, Integer> currentObjectPort;
	private final Integer STARTING_OBJECT_PORT = 100000;
	
	private final String logHeader = "Physical Topology: ";
	private Logger logger = null;
	
	private final int INIT_TERMINATION_VALUE = 9999;
	
	private final Long MILLI_SEC_NEEDED_TO_START_BROKER = new Long(2000L);
	private final Long MILLI_SEC_NEEDED_TO_START_CLIENT = new Long(2000L); 
	
	/**
	 * Empty Constructor
	 */
	public PhysicalTopology(Logger log) 
	{
		brokerObjects = new HashMap<String, PSBrokerPADRES>();
		clientObjects = new HashMap<String, PSClientPADRES>();
		brokerPBs = new ArrayList<ProcessBuilder>();
		clientPBs = new ArrayList<ProcessBuilder>();
		activeBrokers = new ArrayList<Process>();
		activeClients = new ArrayList<Process>();
		
		startingTopology = new LogicalTopology();
		
		runNumber = INIT_RUN_NUMBER;
		protocol = null;
		distributed = null;
		topologyFilePath = new String();
		
		brokerPorts = new HashMap<String, ArrayList<Integer>>();
		freeBrokerHosts = new ArrayList<String>();
		jForBrokerPorts = new HashMap<String, Integer>();
		
		clientMachines = new ArrayList<String>();
		nodeLocation = new HashMap<String, String>();
		nodeObjectPort = new HashMap<String, Integer>();
		currentObjectPort = new HashMap<String, Integer>();
		nodeSocket = new HashMap<String, Socket>();
		
		logger = log;
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
		return !brokerPBs.isEmpty() && !clientPBs.isEmpty();
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
	
	/**
	 * This code develops the Physical Topology
	 * I.e. creates all the Broker and Client Objects
	 * using information from the BenchmarkProperties file
	 * 
	 * @param givenDistributed - the distributed flag
	 * @param givenTopo - the LogicalTopology that we must make physical
	 * @param givenProtocol - the messaging protocol
	 * @param disFileHostsAndPorts 
	 * @return false if an error occurs, true otherwise
	 */
	public boolean developPhysicalTopology(boolean givenDistributed, LogicalTopology givenTopo, NetworkProtocol givenProtocol,
												String givenTFP, HashMap<String, ArrayList<Integer>> disFileHostsAndPorts)
	{
		startingTopology = givenTopo;
		
		protocol = givenProtocol;
		distributed = givenDistributed;
		topologyFilePath = PSTBUtil.cleanTPF(givenTFP);
		
		brokerPorts = disFileHostsAndPorts;
		freeBrokerHosts = new ArrayList<String>(disFileHostsAndPorts.keySet());
		freeBrokerHosts.forEach((host)->{
			jForBrokerPorts.put(host, 0);
			
			currentObjectPort.put(host, STARTING_OBJECT_PORT);
		});
		
		clientMachines = new ArrayList<String>(disFileHostsAndPorts.keySet());
		
		boolean checkGB = developBrokers();
		if(!checkGB)
		{
			logger.error(logHeader + "Error generating physical Brokers");
			return false;
		}
		
		brokerObjects.forEach((brokerName, brokerObject)->{
			System.out.println(brokerName + " " + brokerObject.getBrokerURI());
		});
		
		boolean checkGC = developClients();
		if(!checkGC)
		{
			logger.error(logHeader + "Error generating physical Clients");
			return false;
		}
		
		logger.info(logHeader + "Physical Topology developed successfully");
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
			
			// Now that the new object is created, let's add it to the host map
			boolean handleCheck = handleNodeLocationAndObjectPort(hostName, brokerI);
			if(!handleCheck)
			{
				return false;
			}
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
					nodeLocation.clear();
					return false;
				}
				neededURIs.add(actBrokerJ.getBrokerURI());
			}
			
			String[] nURIs = (String[]) neededURIs.toArray(new String[neededURIs.size()]);
			
			brokerI.setNeighbourURIs(nURIs);
			
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
			freeBrokerHostI++;
			
			if(freeBrokerHostI >= freeBrokerHosts.size())
			{
				freeBrokerHostI = 0;
			}
			
			hostName = freeBrokerHosts.get(freeBrokerHostI);
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
		
		if(givenHost.equals(LOCAL))
		{
			localPortNum++;
		}
		else
		{
			Integer j = jForBrokerPorts.get(givenHost);
			ArrayList<Integer> portsForGivenHost = brokerPorts.get(givenHost);
			retVal = portsForGivenHost.get(j);
			
			j++;
			if(j >= portsForGivenHost.size())
			{
				jForBrokerPorts.remove(givenHost);
				freeBrokerHosts.remove(givenHost);
			}
			else
			{
				jForBrokerPorts.put(givenHost, j);
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
				logger.error(logHeader + "Client " + clientIName + " has no connections");
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
							
			clientObjects.put(clientIName, clientI);
			
			String clientIMachine = getClientMachine();
			boolean clientHandleCheck = handleNodeLocationAndObjectPort(clientIMachine, clientIName);
			if(!clientHandleCheck)
			{
				return false;
			}
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
	public boolean addRunLengthToClients(Long givenRL)
	{
		if(clientObjects.isEmpty())
		{
			logger.error(logHeader + "addRunLengthToAllClients() needs clients to be created first. " +
							"Please run developPhysicalTopology().");
			return false;
		}
		
		clientObjects.forEach((clientName, actualClient)->{
			actualClient.setRunLength(givenRL);
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
	
	private boolean addRunNumberToAllClients()
	{
		if(clientObjects.isEmpty())
		{
			logger.error(logHeader + "addRunNumberToAllClients() needs clients to be created first. " +
							"Please run developPhysicalTopology().");
			return false;
		}
		
		clientObjects.forEach((clientName, actualClient)->{
			actualClient.setRunNumber(runNumber);
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
			clientMachineI++;
			
			if(clientMachineI >= clientMachines.size())
			{
				clientMachineI = 0;
			}
			
			machineName = clientMachines.get(clientMachineI);
		}
		
		return machineName;
	}
	
	private boolean handleNodeLocationAndObjectPort(String hostName, String objectName)
	{
		nodeLocation.put(objectName, hostName);
		
		Integer objectSocketPort = getObjectPort(hostName);
		if(distributed && objectSocketPort == null)
		{
			logger.error(logHeader + "Error storing node " + objectName + "!");
			nodeLocation.clear();
			return false;
		}
		else
		{
			nodeObjectPort.put(objectName, objectSocketPort);
		}
		
		return true;
	}
	
	private Integer getObjectPort(String hostName) 
	{
		Integer retVal = null;
		Integer newVal = null;
		
		if(!hostName.equals(LOCAL))
		{
			retVal = currentObjectPort.get(hostName);
			
			if(retVal != null)
			{
				newVal = retVal++;
				currentObjectPort.put(hostName, newVal);
			}
			else
			{
				logger.error(logHeader + "Couldn't find host " + hostName + " in port matrix!");
			}
		}
		
		return retVal;
	}
	
	/**
	 * Generates processes using the Broker and Client objects 
	 * created in developPhysicalTopology()
	 * 
	 * @return false on error; true otherwise
	 */
	public boolean generateBrokerAndClientProcesses()
	{
		if(brokerObjects.isEmpty())
		{
			logger.error(logHeader + "generateBrokerAndClientProcesses() needs brokers to be created first. " +
							"Please run developPhysicalTopology() first.");
			return false;
		}
		
		if(clientObjects.isEmpty())
		{
			logger.error(logHeader + "generateBrokerAndClientProcesses() needs clients to be created first. " +
							"Please run developPhysicalTopology().");
			return false;
		}
		
		if(runNumber.compareTo(INIT_RUN_NUMBER) <= 0)
		{
			logger.error(logHeader + "generateBrokerAndClientProcesses() needs a runNumber. Please run setRunNumber().");
			return false;
		}
		
		addRunNumberToAllClients(); // I'm cheating, but this should always return true as we're already checking above for clients.
		
		Iterator<String> iteratorBO = brokerObjects.keySet().iterator();
		for( ; iteratorBO.hasNext() ; )
		{
			String brokerIName = iteratorBO.next();
			PSBrokerPADRES brokerI = brokerObjects.get(brokerIName);
			
			String brokerCommand = new String();
			
			boolean objectFileSentCheck = false;
			
			if(!distributed)
			{
				FileOutputStream fileOut = null;
				try 
				{
					fileOut = new FileOutputStream("/tmp/" + brokerIName + ".bro");
				} 
				catch (FileNotFoundException e) 
				{
					logger.error(logHeader + "Couldn't create output file for broker object " + brokerIName + ": ", e);
					return false;
				}
				
				objectFileSentCheck = PSTBUtil.sendObject(brokerI, brokerIName, fileOut, logger, logHeader);
				try 
				{
					fileOut.close();
				} 
				catch (IOException e) 
				{
					logger.error(logHeader + "error closing FileOutputStream: ", e);
					return false;
				}
				
				brokerCommand = BROKER_INT + " -n " + brokerIName + " -r " + runNumber.toString() + " -p " + "null";
			}
			else
			{
				String host = nodeLocation.get(brokerIName);
				Integer port = nodeObjectPort.get(brokerIName);
				Socket socketConnection = null;
				
				try 
				{
					socketConnection = new Socket(host, port);
				} 
				catch (IOException e) 
				{
					logger.error(logHeader + "Cannot create socket for broker " + brokerIName + ": ", e);
					return false;
				}
				nodeSocket.put(brokerIName, socketConnection);
				
				OutputStream socketOut = null;
				try 
				{
					socketOut = socketConnection.getOutputStream();
				} 
				catch (IOException e) 
				{
					logger.error(logHeader + "Couldn't create socket for broker object " + brokerIName + ": ", e);
					return false;
				}
				
				objectFileSentCheck = PSTBUtil.sendObject(brokerI, brokerIName, socketOut, logger, logHeader);
				try 
				{
					socketOut.close();
				} 
				catch (IOException e) 
				{
					logger.error(logHeader + "error closing socket OutputStream: ", e);
					return false;
				}
				
				brokerCommand = DISTRIBUTED_INIT_P1 + host + DISTRIBUTED_INIT_P2 + BROKER_INT + " -n " + brokerIName 
									+ " -r " + runNumber.toString() + " -p " + port.toString();
			}
			
			if(!objectFileSentCheck)
			{
				return false;
			}
			
			ProcessBuilder actualBrokerI = new ProcessBuilder(brokerCommand.split("\\s+"));
			brokerPBs.add(actualBrokerI);
		}
		
		Iterator<String> iteratorCO = clientObjects.keySet().iterator();
		for( ; iteratorCO.hasNext() ; )
		{
			String clientIName = iteratorCO.next();
			PSClientPADRES clientI = clientObjects.get(clientIName);
			
			String clientCommand = new String();
			
			boolean objectFileSentCheck = false;
			
			if(!distributed)
			{
				FileOutputStream fileOut = null;
				try
				{
					fileOut = new FileOutputStream("/tmp/" + clientIName + ".cli");
				}
				catch (FileNotFoundException e) 
				{
					logger.error(logHeader + "Couldn't create output file for client object " + clientIName + ": ", e);
					return false;
				}
				
				objectFileSentCheck = PSTBUtil.sendObject(clientI, clientIName, fileOut, logger, logHeader);
				try 
				{
					fileOut.close();
				} 
				catch (IOException e) 
				{
					logger.error(logHeader + "error closing FileOutputStream: ", e);
					return false;
				}
				clientCommand = CLIENT_INT + " -n " + clientIName + " -r " + runNumber.toString() + " -p " + "null";
			}
			else
			{
				String host = nodeLocation.get(clientIName);
				Integer port = nodeObjectPort.get(clientIName);
				Socket socketConnection = null;
				
				try 
				{
					socketConnection = new Socket(host, port);
				} 
				catch (IOException e) 
				{
					logger.error(logHeader + "Cannot create socket for broker " + clientIName + ": ", e);
					return false;
				}
				nodeSocket.put(clientIName, socketConnection);
				
				OutputStream socketOut = null;
				try 
				{
					socketOut = socketConnection.getOutputStream();
				} 
				catch (IOException e) 
				{
					logger.error(logHeader + "Couldn't create socket for client object " + clientIName + ": ", e);
					return false;
				}
				
				objectFileSentCheck = PSTBUtil.sendObject(clientI, clientIName, socketOut, logger, logHeader);
				try 
				{
					socketOut.close();
				} 
				catch (IOException e) 
				{
					logger.error(logHeader + "error closing socket OutputStream: ", e);
					return false;
				}
				
				clientCommand = DISTRIBUTED_INIT_P1 + host + DISTRIBUTED_INIT_P2 + CLIENT_INT + " -n " + clientIName 
									+ " -r " + runNumber.toString() + " -p " + port.toString();
			}
			
			if(!objectFileSentCheck)
			{
				return false;
			}
			
			ProcessBuilder actualClientI = new ProcessBuilder(clientCommand.split("\\s+"));
			clientPBs.add(actualClientI);
		}
		
		logger.info(logHeader + "Successfully generated broker and client processes.");
		return true;
	}
	
	/**
	 * Launches all of the Processes generated in generateBrokerAndClientProcesses()
	 * ... by which I mean call the function that actually starts the Processes
	 * and wait a certain amount of time to make sure they start
	 * @see generateBrokerAndClientProcesses
	 * @see startAndStoreAllGivenProcesses
	 * 
	 * @return false if there's an error; true otherwise
	 */
	public boolean startProcesses()
	{
		if(brokerPBs.isEmpty())
		{
			logger.error(logHeader + "startRun() needs broker processes to be created first.\n" +
							"Please run generateBrokerAndClientProcesses() first.");
			return false;
		}
		
		if(clientPBs.isEmpty())
		{
			logger.error(logHeader + "startRun() needs client processes to be created first.\n" +
							"Please run generateBrokerAndClientProcesses() first.");
			return false;
		}
		
		boolean functionCheck = startAndStoreAllGivenProcesses(brokerPBs, activeBrokers, "brokers");
		
		if(!functionCheck)
		{
			killAllProcesses();
			return false;
		}
		
		Long waitTime = MILLI_SEC_NEEDED_TO_START_BROKER * brokerPBs.size();
		
		try 
		{				
			logger.trace("Pausing for brokers to start");
			Thread.sleep(waitTime);
		} 
		catch (InterruptedException e) 
		{
			logger.error("Error pausing for brokers", e);
			return false;
		}
		
		functionCheck = startAndStoreAllGivenProcesses(clientPBs, activeClients, "clients");
		
		if(!functionCheck)
		{
			killAllProcesses();
			return false;
		}
		
		waitTime = MILLI_SEC_NEEDED_TO_START_CLIENT * brokerPBs.size();
		
		try 
		{				
			logger.trace("Pausing for clients to start");
			Thread.sleep(waitTime);
		} 
		catch (InterruptedException e) 
		{
			logger.error("Error pausing for clients", e);
			return false;
		}
		
		logger.info(logHeader + "Everything launched");
		return true;
	}
	
	/**
	 * Loops through a given ProcessBuilder collection,
	 * starts all of its processes,
	 * and stores them
	 * 
	 * @param givenProcBuilds - the ProcessBuilder collection
	 * @param storage - the place where the finished Processes are stored
	 * @param processType - the type of process - Broker or Client
	 * @return false if there's an error; true otherwise
	 */
	private boolean startAndStoreAllGivenProcesses(ArrayList<ProcessBuilder> givenProcBuilds, ArrayList<Process> storage, String processType)
	{
		for(int i = 0 ; i < givenProcBuilds.size(); i++)
		{
			Process processI = null;
			try 
			{
				processI = givenProcBuilds.get(i).start();
			} 
			catch (IOException e) 
			{
				logger.error(logHeader + "error launching " + processType + " process " + i, e);
				return false;
			}
			storage.add(processI);
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
		for(int i = 0 ; i < activeBrokers.size() ; i++)
		{
			int terminationValue = INIT_TERMINATION_VALUE;
			try
			{
				terminationValue = activeBrokers.get(i).exitValue();
			}
		    catch(IllegalThreadStateException e)
		    {
		    	logger.trace("Broker " + i + " is still running");
		    }
			
			if(terminationValue != 0 && terminationValue != INIT_TERMINATION_VALUE)
			{
				logger.error(logHeader + "broker " + i + " terminated with error " + terminationValue);
				return ActiveProcessRetVal.Error;
			}
			else if(terminationValue == 0)
			{
				activeBrokers.remove(i);
			}
		}
		
		for(int i = 0 ; i < activeClients.size() ; i++)
		{
			int terminationValue = INIT_TERMINATION_VALUE;
			try
			{
				terminationValue = activeClients.get(i).exitValue();
			}
		    catch(IllegalThreadStateException e)
		    {
		    	logger.trace("Client " + i + " is still running");
		    }
			
			if(terminationValue != 0 && terminationValue != INIT_TERMINATION_VALUE)
			{
				logger.error(logHeader + "client " + i + " terminated with error " + terminationValue);
				return ActiveProcessRetVal.Error;
			}
			else if(terminationValue == 0)
			{
				activeClients.remove(i);
			}
		}
		
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
	
	/**
	 * Gets a certain serialized diary object 
	 * and adds it to the "bookshelf" 
	 * - a collection of ClientDiaries
	 * 
	 * @param diaryName - the name associated the diary object
	 * @see PSClientPADRES
	 * @return an ArrayList of diaries if everything works properly; null otherwise
	 */
	public HashMap<String, ClientDiary> collectDiaries()
	{		
		HashMap<String, ClientDiary> retVal = new HashMap<String, ClientDiary>();
		InputStream in = null;
		
		Iterator<String> clients = clientObjects.keySet().iterator(); 
		
		for( ; clients.hasNext() ; )
		{
			String clientI = clients.next();
			String clientIDiaryName = clientObjects.get(clientI).generateDiaryName();
			
			if(!distributed)
			{
				try 
				{
					in = new FileInputStream("/tmp/" + clientIDiaryName + ".dia");
				} 
				catch (FileNotFoundException e) 
				{
					logger.error(logHeader + "Error creating new file input stream to read diary: ", e);
					try 
					{
						in.close();
					} 
					catch (IOException e1) 
					{
						logger.error(logHeader + "Error closing FileInputStream: ", e1);
					}
					return null;
				}
			}
			else
			{
				Socket clientISocket = nodeSocket.get(clientI);
				try 
				{
					in = clientISocket.getInputStream();
				} 
				catch (IOException e) 
				{
					logger.error(logHeader + "Error creating new socket input stream to read diary: ", e);
					try 
					{
						in.close();
					} 
					catch (IOException e1) 
					{
						logger.error(logHeader + "Error closing socket InputStream: ", e1);
					}
					return null;
				}
			}
			
			ClientDiary tiedDiary = readDiaryObject(in);
			if(tiedDiary != null)
			{
				retVal.put(clientIDiaryName, tiedDiary);
			}
			else
			{
				logger.error(logHeader + "Error getting diary " + clientIDiaryName + "!");
				return null;
			}
		}
		
		return retVal;
	}
	
	/**
	 * Deserializes a ClientDiary object
	 * 
	 * @param diaryName - the name of this diary
	 * @return null on failure; the requested diary otherwise
	 */
	public ClientDiary readDiaryObject(InputStream givenIS)
	{
		ClientDiary diaryI = null;
		try
		{
			ObjectInputStream oISIn = new ObjectInputStream(givenIS);
			diaryI = (ClientDiary) oISIn.readObject();
			oISIn.close();
		}
		catch (IOException e)
		{
			logger.error(logHeader + "error accessing ObjectInputStream ", e);
			return null;
		}
		catch(ClassNotFoundException e)
		{
			logger.error(logHeader + "can't find class ", e);
			return null;
		}
		
		return diaryI;
	}
	
	/**
	 * Kills all existing processes
	 */
	public void killAllProcesses()
	{
		for(int i = 0; i < activeClients.size() ; i++)
		{
			activeClients.get(i).destroy();
		}
		
		for(int i = 0 ; i < activeBrokers.size() ; i++)
		{
			activeBrokers.get(i).destroy();
		}
		
		activeClients.clear();
		activeBrokers.clear();
	}
	
	/**
	 * Clears the process builders
	 */
	public void clearProcessBuilders()
	{
		brokerPBs.clear();
		clientPBs.clear();
	}
}
