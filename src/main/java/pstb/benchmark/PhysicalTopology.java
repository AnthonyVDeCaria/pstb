package pstb.benchmark;

import pstb.util.LogicalTopology;
import pstb.util.NetworkProtocol;
import pstb.util.NodeRole;
import pstb.util.PubSubGroup;
import pstb.util.Workload;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

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
	private HashMap<String, PSBrokerPADRES> devBrokers;
	private HashMap<String, PSClientPADRES> devClients;
	private ArrayList<ProcessBuilder> phyBrokers;
	private ArrayList<ProcessBuilder> phyClients;
	private ArrayList<Process> activeBrokers;
	private ArrayList<Process> activeClients;
	
	private int MEMORY = 512;
	private String CLIENT_INT = "java -Xmx" + MEMORY + "M -Xverify:none "
								+ "-cp target/pstb-0.0.1-SNAPSHOT-jar-with-dependencies.jar "
								+ "pstb.benchmark.PhysicalClient ";
	private String BROKER_INT = //"screen -dmS broker java -Xmx1024M -Djava.rmi.server.codebase=file:${PADRES_HOME}/build/ "
									"java -Xmx" + MEMORY + "M "
									+ "-cp target/pstb-0.0.1-SNAPSHOT-jar-with-dependencies.jar -Djava.awt.headless=true "
//									+ "-Djava.security.policy=${PADRES_HOME}/etc/java.policy " 
									+ "pstb.benchmark.PhysicalBroker ";
	
	private NetworkProtocol protocol;
	
	private PubSubGroup brokerList; 
	private PubSubGroup publisherList;
	private PubSubGroup subscriberList;
	
	private static final Integer PORTSTART = 1100;
	private Integer portNum = PORTSTART;
	
	private final String logHeader = "Physical Topology: ";
	private static final Logger logger = LogManager.getRootLogger();
	
	private static final int INIT_TERMINATION_VALUE = 99999999;
	
	private static final Long NANO_SEC_NEEDED_TO_START_BROKER = new Long(2000000000L);
	private static final Long NANO_SEC_NEEDED_TO_START_CLIENT = new Long(8000000000L); 
	
	/**
	 * Empty Constructor
	 */
	public PhysicalTopology() 
	{
		devBrokers = new HashMap<String, PSBrokerPADRES>();
		devClients = new HashMap<String, PSClientPADRES>();
		phyBrokers = new ArrayList<ProcessBuilder>();
		phyClients = new ArrayList<ProcessBuilder>();
		activeBrokers = new ArrayList<Process>();
		activeClients = new ArrayList<Process>();
		
		brokerList = new PubSubGroup();
		publisherList = new PubSubGroup();
		subscriberList = new PubSubGroup();
	}
	
	/**
	 * Determines if the Broker and Client objects have been created
	 * 
	 * @return true is they are; false if they aren't
	 */
	public boolean doObjectsExist()
	{
		return devBrokers.isEmpty() && devClients.isEmpty();
	}
	
	/**
	 * Determines if the Broker and Client ProcessBuilders have been created
	 * 
	 * @return true is they are; false if they aren't
	 */
	public boolean doProcessBuildersExist()
	{
		return phyBrokers.isEmpty() && phyClients.isEmpty();
	}
	
	/**
	 * This code develops the Physical Topology
	 * I.e. creates all the Broker and Client Objects
	 * using information from the BenchmarkProperties file
	 * 
	 * @param distributed - the distributed flag
	 * @param givenTopo - the LogicalTopology that we must make physical
	 * @param givenProtocol - the messaging protocol
	 * @return false if an error occurs, true otherwise
	 */
	public boolean developPhysicalTopology(boolean distributed, LogicalTopology givenTopo, NetworkProtocol givenProtocol)
	{
		brokerList = givenTopo.getGroup(NodeRole.B);
		publisherList = givenTopo.getGroup(NodeRole.P);
		subscriberList = givenTopo.getGroup(NodeRole.S);
		
		protocol = givenProtocol;
		
		boolean checkGB = developBrokers(distributed);
		if(!checkGB)
		{
			logger.error(logHeader + "Error generating physical Brokers");
			return false;
		}
		
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
	 * @param givenDis - the distributed flag
	 * @return false if there's an error, true otherwise
	 */
	private boolean developBrokers(boolean givenDis)
	{
		logger.debug(logHeader + "Attempting to develop broker objects");
		
		Set<String> setBL = brokerList.keySet();
		Iterator<String> iteratorBL = setBL.iterator();
		
		for( ; iteratorBL.hasNext() ; )
		{
			String brokerI = iteratorBL.next();
			String hostName = getHost(givenDis);
			Integer port = getPort();
			
			PSBrokerPADRES actBrokerI = new PSBrokerPADRES(hostName, port, protocol, brokerI);
			
			devBrokers.put(brokerI, actBrokerI);
		}
		
		Set<String> setGB = devBrokers.keySet();
		Iterator<String> iteratorGB = setGB.iterator();
		for( ; iteratorGB.hasNext(); )
		{
			String brokerIName = iteratorGB.next();
			PSBrokerPADRES brokerI = devBrokers.get(brokerIName);
			
			ArrayList<String> neededURIs = new ArrayList<String>();
			
			ArrayList<String> bIConnectedNodes = brokerList.getNodeConnections(brokerIName);
			for(int j = 0 ; j < bIConnectedNodes.size() ; j++)
			{
				String brokerJName = bIConnectedNodes.get(j);
				PSBrokerPADRES actBrokerJ = devBrokers.get(brokerJName);
				if(actBrokerJ == null)
				{
					logger.error(logHeader + "couldn't find " + brokerJName + " in genBrokers that " + brokerIName 
									+ " is connected to.");
					return false;
				}
				neededURIs.add(actBrokerJ.getBrokerURI());
			}
			
			String[] nURIs = (String[]) neededURIs.toArray(new String[neededURIs.size()]);
			
			brokerI.setNeighbourURIs(nURIs);
			
			devBrokers.put(brokerIName, brokerI);
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
	private String getHost(boolean distributed)
	{
		String hostName = "localhost";
		
		if(distributed)
		{
			// TODO: handle distributed systems
		}
		
		return hostName;
	}
	
	/**
	 * Gets a port for broker development 
	 * 
	 * @return the port number 
	 */
	private Integer getPort()
	{
		// TODO: handle distributed systems
		
		Integer retVal = portNum;
		portNum++;
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
		Set<String> pubSet = publisherList.keySet();
		Iterator<String> pubIterator = pubSet.iterator();
		
		Set<String> subSet = subscriberList.keySet();
		Iterator<String> subIterator = subSet.iterator();
		
		for( ; pubIterator.hasNext() ; )
		{
			String publisherNameI = pubIterator.next();
			boolean addPubCheck = developAndStoreNewClientObject(publisherNameI, publisherList, NodeRole.P);
			if(!addPubCheck)
			{
				return false;
			}
		}
		
		for( ; subIterator.hasNext() ; )
		{
			String subscriberNameI = subIterator.next();
			
			if(devClients.containsKey(subscriberNameI))
			{
				devClients.get(subscriberNameI).addNewClientRole(NodeRole.S);
			}
			else
			{
				boolean addSubCheck = developAndStoreNewClientObject(subscriberNameI, subscriberList, NodeRole.S);
				if(!addSubCheck)
				{
					return false;
				}
			}
		}
		
		logger.debug(logHeader + "All clients developed");
		return true;
	}
	
	/**
	 * Creates a new Client object and adds it to devClients 
	 * 
	 * @param clientIName - the name of the Client
	 * @param clientList - the PubSubGroup this Client belongs to
	 * @param givenNR - the role this client has to perform
	 * @return false on error, true otherwise
	 */
	private boolean developAndStoreNewClientObject(String clientIName, PubSubGroup clientList, NodeRole givenNR)
	{
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
			
			boolean doesBrokerJExist = devBrokers.containsKey(brokerJName);
			
			if(!doesBrokerJExist)
			{
				logger.error(logHeader + "Client " + clientIName + " references a broker " + brokerJName + " that doesn't exist");
				return false;
			}
			
			String brokerJURI = devBrokers.get(brokerJName).getBrokerURI();
			clientIBrokerURIs.add(brokerJURI);
		}
		
		clientI.addClientName(clientIName);
		clientI.addConnectedBrokers(clientIBrokerURIs);
		clientI.addNewClientRole(givenNR);
		
		devClients.put(clientIName, clientI);
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
		if(devClients.isEmpty())
		{
			logger.error(logHeader + "addRunLengthToAllClients() needs clients to be created first.\n" +
							"Please run developPhysicalTopology().");
			return false;
		}
		
		devClients.forEach((clientName, actualClient)->{
			actualClient.addRL(givenRL);
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
		if(devClients.isEmpty())
		{
			logger.error(logHeader + "addRunLengthToAllClients() needs clients to be created first.\n" +
							"Please run developPhysicalTopology().");
			return false;
		}
		
		devClients.forEach((clientName, actualClient)->{
			actualClient.addWorkload(givenWorkload);
		});
		
		return true;
	}
	
	/**
	 * Generates processes using the Broker and Client objects 
	 * created in developPhysicalTopology()
	 * 
	 * @return false on error; true otherwise
	 */
	public boolean generateBrokerAndClientProcesses()
	{
		if(devBrokers.isEmpty())
		{
			logger.error(logHeader + "generateBrokerAndClientProcesses() needs brokers to be created first.\n" +
							"Please run developPhysicalTopology() first.");
			return false;
		}
		
		if(devClients.isEmpty())
		{
			logger.error(logHeader + "generateBrokerAndClientProcesses() needs clients to be created first.\n" +
							"Please run developPhysicalTopology().");
			return false;
		}
		
		Set<String> setGB = devBrokers.keySet();
		Iterator<String> iteratorGB = setGB.iterator();
		for( ; iteratorGB.hasNext() ; )
		{
			String brokerIName = iteratorGB.next();
			PSBrokerPADRES brokerI = devBrokers.get(brokerIName);
			
			String brokerCommand = BROKER_INT + " -n " + brokerIName;
			
			boolean objectFileCheck = createObjectFile(brokerI, brokerIName);
			
			if(!objectFileCheck)
			{
				return false;
			}
			
			ProcessBuilder actualBrokerI = new ProcessBuilder(brokerCommand.split("\\s+")).inheritIO();
			phyBrokers.add(actualBrokerI);
		}
		
		Set<String> setGC = devClients.keySet();
		Iterator<String> iteratorGC = setGC.iterator();
		for( ; iteratorGC.hasNext() ; )
		{
			String clientIName = iteratorGC.next();
			PSClientPADRES clientI = devClients.get(clientIName);
			
			String clientCommand = CLIENT_INT + " -n " + clientIName;
			
			boolean objectFileCheck = createObjectFile(clientI, clientIName);
			
			if(!objectFileCheck)
			{
				return false;
			}
			
			ProcessBuilder actualClientI = new ProcessBuilder(clientCommand.split("\\s+"));
			phyClients.add(actualClientI);
		}
		
		logger.info(logHeader + "Successfully generated broker and client processes.");
		return true;
	}
	
	/**
	 * Serializes the given Object and stores it in a file
	 * Allowing the processes after to access these objects and their functions
	 * @see PhysicalBroker
	 * @see PhysicalClient
	 * 
	 * @param givenObject - the Object to be stored in a file
	 * @param givenObjectName - the name of said Object
	 * @return false on error; true if successful
	 */
	private boolean createObjectFile(Object givenObject, String givenObjectName)
	{
		try 
		{
			FileOutputStream fileOut = new FileOutputStream("/tmp/" + givenObjectName + ".ser");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(givenObject);
			out.close();
			fileOut.close();
		} 
		catch (FileNotFoundException e) 
		{
			logger.error(logHeader + "couldn't generate serialized client file ", e);
			return false;
		} 
		catch (IOException e) 
		{
			logger.error(logHeader + "error with ObjectOutputStream ", e);
			return false;
		}
		
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
	public boolean launchProcesses()
	{
		if(phyBrokers.isEmpty())
		{
			logger.error(logHeader + "startRun() needs broker processes to be created first.\n" +
							"Please run generateBrokerAndClientProcesses() first.");
			return false;
		}
		
		if(phyClients.isEmpty())
		{
			logger.error(logHeader + "startRun() needs client processes to be created first.\n" +
							"Please run generateBrokerAndClientProcesses() first.");
			return false;
		}
		
		boolean functionCheck = startAndStoreAllGivenProcesses(phyBrokers, activeBrokers, "brokers");
		
		if(!functionCheck)
		{
			killAllProcesses();
			return false;
		}
		
		Long startTime = System.nanoTime();
		Long currentTime = System.nanoTime();
		Long waitTime = NANO_SEC_NEEDED_TO_START_BROKER * phyBrokers.size();
		
		while( (currentTime - startTime) < waitTime )
		{
			currentTime = System.nanoTime();
		}
		
		functionCheck = startAndStoreAllGivenProcesses(phyClients, activeClients, "clients");
		
		if(!functionCheck)
		{
			killAllProcesses();
			return false;
		}
		
		startTime = System.nanoTime();
		currentTime = System.nanoTime();
		waitTime = NANO_SEC_NEEDED_TO_START_CLIENT * phyClients.size();
		while( (currentTime - startTime) < waitTime )
		{
			currentTime = System.nanoTime();
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
				logger.error(logHeader + "a broker terminated with error " + terminationValue);
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
				logger.error(logHeader + "a client terminated with error " + terminationValue);
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
	 * Kills all existing processes
	 */
	public void killAllProcesses()
	{
		for(int i = 0 ; i < activeBrokers.size() ; i++)
		{
			activeBrokers.get(i).destroy();
		}
		for(int i = 0; i < activeClients.size() ; i++)
		{
			activeClients.get(i).destroy();
		}
	}
}
