/**
 * 
 */
package pstb.creation;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.startup.topology.LogicalTopology;
import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 *
 */
public class PhysicalTopology {
	// Constants
	protected final Integer MEM_CLIENT = 256;
	protected final Integer MEM_BROKER = 256;
	private final Integer LOCAL_START_PORT = 1100;
	private final int INIT_TERMINATION_VALUE = 9999;
	private final String BROKER_CLASS_NAME = "pstb.benchmark.broker.PhysicalBroker";
	private final String CLIENT_CLASS_NAME = "pstb.benchmark.client.PhysicalClient";
	
	// Variables that have a start value
	protected int freeHostI = -1;
	protected int givenMachineI = -1;
	protected Integer localPortNum = LOCAL_START_PORT;
	
	// Variables given by user on creation
	protected LogicalTopology startingTopology;
	protected String user;
	protected HashMap<String, ArrayList<Integer>> hostsAndPorts;
	
	// Variables set on creation
	protected String ipAddress;
	protected ArrayList<String> givenMachines;
	protected ArrayList<String> freeHosts;
	protected HashMap<String, Integer> hostIsPortJ;
	
	// Variables set on initial start up
	protected Boolean distributed;
	protected String topologyFileString;
	protected String benchmarkStartTime;
	
	// Variables set during Object creation
	protected HashMap<String, String> nodeMachine;
	
	// ProcessBuilders
	protected HashMap<String, ProcessBuilder> brokerProcesses;
	protected HashMap<String, ProcessBuilder> clientProcesses;
	
	// Processes
	private HashMap<String, Process> activeBrokers;
	private HashMap<String, Process> activeClients;
	
	// Logger
	private final String logHeader = "Physical Topology: ";
	private final Logger logger = LogManager.getRootLogger();
	
	public PhysicalTopology(LogicalTopology givenTopo, String givenUser, HashMap<String, ArrayList<Integer>> givenHostsAndPorts,
			String givenTFS, String givenBST) throws UnknownHostException
	{
		startingTopology = givenTopo;
		user = givenUser;
		hostsAndPorts = givenHostsAndPorts;
		
		InetAddress masterAddress = InetAddress.getLocalHost();
		ipAddress = masterAddress.getHostAddress();
		
		if(givenHostsAndPorts != null)
		{
			givenMachines = new ArrayList<String>(givenHostsAndPorts.keySet());
			
			freeHosts = new ArrayList<String>(givenHostsAndPorts.keySet());
			hostIsPortJ = new HashMap<String, Integer>();
			
			freeHosts.forEach((host)->{
				hostIsPortJ.put(host, 0);
			});
		}
		else
		{
			givenMachines = new ArrayList<String>();
			
			freeHosts = new ArrayList<String>();
			hostIsPortJ = new HashMap<String, Integer>();
		}
		
		distributed = null;
		topologyFileString = PSTBUtil.cleanTFS(givenTFS);
		benchmarkStartTime = givenBST;
		
		nodeMachine = new HashMap<String, String>();
		
		brokerProcesses = new HashMap<String, ProcessBuilder>();
		clientProcesses = new HashMap<String, ProcessBuilder>();
		
		activeBrokers = new HashMap<String, Process>();
		activeClients = new HashMap<String, Process>();
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
	 * Gets the Topology File Path
	 * 
	 * @return the TopologyFilePath
	 */
	public String getTopologyFilePath() 
	{
		return topologyFileString;
	}
	
	/**
	 * Gets the User
	 * 
	 * @return the User
	 */
	public String getUser() 
	{
		return user;
	}
	
	public boolean haveHostsAndPortsBeenSet()
	{
		return !hostsAndPorts.isEmpty();
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
	
	/**
	 * Retrieves a Process belonging to a particular node.
	 * 
	 * @param nodeName - the name of the node the Process is tied to.
	 * @return the Process requested if it exists; null otherwise
	 */
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
	 * Gets a URI Host for the Broker Object
	 * 
	 * @return the host's name
	 */
	protected String getBrokerHost()
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
	 * Gets a URI port for the Broker Object
	 * 
	 * @param givenHost - the name of the host this Broker will be tied to
	 * @return the port number 
	 */
	protected Integer getBrokerPort(String givenHost)
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
	 * Gets a machine that a Client can run on
	 * 
	 * @return the name of the machine
	 */
	protected String getClientMachine()
	{
		String machineName = PSTBUtil.LOCAL;
		
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
	
	/**
	 * Launches all of the Processes generated in generateBrokerAndClientProcesses()
	 * ... by which I mean call the function that actually starts the Processes
	 * @see generateBrokerAndClientProcesses
	 * @see startGivenNodeProcesses
	 * 
	 * @return false if there's an error; true otherwise
	 */
	public boolean startProcesses()
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
	
	/**
	 * Starts the Process for a given node.
	 * 
	 * @param nodeName - The name of said node
	 * @param nodeProcess - The ProcessBuilder associated with this node
	 * @param isBroker - is the node an Broker?
	 * @return false on error; true otherwise
	 */
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
				
		boolean retVal = startProcesses();
		return retVal;
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
			
			brokerCheck = isNodeIActive(true, brokerIName, brokerI);
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
			
			clientCheck = isNodeIActive(false, clientIName, clientI);
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
	
	/**
	 * Determines if a given node is still running.
	 * 
	 * @param isBroker - is the node a broker?
	 * @param nodeIName - the name of the node
	 * @param nodeI - the Process associated with the node
	 * @return null if there's a major error, false if the node isn't active, true if the node is active
	 */
	public Boolean isNodeIActive(boolean isBroker, String nodeIName, Process nodeI)
	{
		Integer checkProcessInt = null;
		String nodeType = new String();
		if(isBroker)
		{
			checkProcessInt = nodeIExitHandler(BROKER_CLASS_NAME, nodeIName, nodeI);
			nodeType = "Broker ";
		}
		else
		{
			checkProcessInt = nodeIExitHandler(CLIENT_CLASS_NAME, nodeIName, nodeI);
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
	
	/**
	 * Looks at a given node and returns its exit value
	 * 
	 * @param nodeIClass - the class of the node we're looking at
	 * @param nodeIName - the name of the node we're looking at
	 * @param nodeI - the Process of the node we're looking at
	 * @return the exit value, or a constant if it's still running.
	 * @see INIT_TERMINATION_VALUE
	 */
	private Integer nodeIExitHandler(String nodeIClass, String nodeIName, Process nodeI)
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
	public void destroyAllActiveNodes()
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
	 * Resets everything after a run is finished
	 */
	public void resetSystemAfterRun()
	{
		destroyAllActiveNodes();
		brokerProcesses.clear();
		clientProcesses.clear();
	}
}
