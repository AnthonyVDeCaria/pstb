/**
 * 
 */
package pstb.creation.topology;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.analysis.diary.DistributedFlagValue;
import pstb.benchmark.object.PSNode;
import pstb.benchmark.object.broker.PSBroker;
import pstb.benchmark.object.client.PSClient;
import pstb.benchmark.object.client.PSClientMode;
import pstb.benchmark.throughput.TPMaster;
import pstb.creation.server.ObjectServer;
import pstb.startup.config.AttributeRatio;
import pstb.startup.config.ExperimentType;
import pstb.startup.config.MessageSize;
import pstb.startup.config.NetworkProtocol;
import pstb.startup.config.NumAttribute;
import pstb.startup.config.SupportedEngines.PSEngine;
import pstb.startup.distributed.Machine;
import pstb.startup.topology.ClientNotes;
import pstb.startup.topology.LogicalTopology;
import pstb.startup.topology.NodeRole;
import pstb.startup.workload.PSAction;
import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 * 
 * This class takes a logical topology and develops the  
 */
public abstract class PhysicalTopology {
    // Constants
    private final int AVAILABLE_MACHINE_MEM = 8192;
    private final double BROKER_WEIGHT = 0.75;
    private final int STARTING_BROKER_MEM = (int) (AVAILABLE_MACHINE_MEM * BROKER_WEIGHT);
    private final int STARTING_CLIENT_MEM = (int) (AVAILABLE_MACHINE_MEM * (1 - BROKER_WEIGHT));
    private final int INIT_TERMINATION_VALUE = 9999;
    
    // Variables that have a start value
    protected int givenMachineI = 0;
    
    // Variables given by user on creation
    protected ExperimentType mode;
    protected LogicalTopology startingTopology;
    protected NetworkProtocol protocol;
    protected String user;
    protected ArrayList<Machine> givenMachines;
    protected HashMap<String, ArrayList<PSAction>> masterWorkload;
    protected String benchmarkNumber;
    protected String topologyFileString;
    
    // Variables set on creation
    protected String ipAddress;
    protected String BROKER_PROCESS_CLASS_NAME;
    protected String CLIENT_PROCESS_CLASS_NAME;
    
    // Variables set on initial start up
    protected boolean distributed;
    
    // Variables set during Object creation
    private HashMap<String, String> nodeMachine;
    private HashMap<String, HashMap<NodeRole, Integer>> numNodesMachine;
        
    // Objects
    protected HashMap<String, PSNode> brokerObjects;
    protected HashMap<String, PSNode> clientObjects;
    private int numThroughputPubs;
    
    // Server
    protected ServerSocket everythingSocket;
    protected ObjectServer brokerServer;
    protected ObjectServer clientServer;
    protected TPMaster throughputMaster;
    protected CountDownLatch brokerServerComplete;
    protected CountDownLatch clientServerComplete;
    
    // ProcessBuilders
    protected HashMap<String, ProcessBuilder> brokerProcesses;
    protected HashMap<String, ProcessBuilder> clientProcesses;
    
    // Processes
    protected HashMap<String, Process> activeBrokers;
    protected HashMap<String, Process> activeClients;
    
    // Logger
    protected String logHeader;
    protected final Logger logger = LogManager.getRootLogger();
    
    /**
     * Filled Constructor
     * 
     * @param givenExp - the type of experiment we're conducting
     * @param givenTopo - the LogicalTopology, i.e. what the network looks like
     * @param givenProtocol - the protocol the 
     * @param givenUser
     * @param givenHostsAndPorts
     * @param givenWorkload
     * @param givenBST
     * @param givenTFS
     * @throws UnknownHostException
     */
    public PhysicalTopology(ExperimentType givenExp, LogicalTopology givenTopo,  
            NetworkProtocol givenProtocol, String givenUser, ArrayList<Machine> givenHostsAndPorts, 
            HashMap<String, ArrayList<PSAction>> givenWorkload, 
            String givenBST, String givenTFS,
            ServerSocket givenSS) throws Exception
    {
        mode = givenExp;
        
        startingTopology = givenTopo;
        
        protocol = givenProtocol;
        
        user = givenUser;
        givenMachines = givenHostsAndPorts;
        
        masterWorkload = givenWorkload;
        
        benchmarkNumber = givenBST;
        topologyFileString = PSTBUtil.cleanTFS(givenTFS);
        
        InetAddress masterAddress = InetAddress.getLocalHost();
        ipAddress = masterAddress.getHostAddress();
        
        distributed = false;
        
        nodeMachine = new HashMap<String, String>();
        numNodesMachine = new HashMap<String, HashMap<NodeRole, Integer>>();
        
        brokerObjects = new HashMap<String, PSNode>();
        clientObjects = new HashMap<String, PSNode>();
        numThroughputPubs = 0;
        
        everythingSocket = givenSS;
        brokerServer = null;
        clientServer = null;
        throughputMaster = null;
        brokerServerComplete = null;
        clientServerComplete = null;
        
        brokerProcesses = new HashMap<String, ProcessBuilder>();
        clientProcesses = new HashMap<String, ProcessBuilder>();
        
        activeBrokers = new HashMap<String, Process>();
        activeClients = new HashMap<String, Process>();
        
        logHeader = null;
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
     * Gets the User
     * 
     * @return the User
     */
    public String getUser() 
    {
        return user;
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
     * Gets the Distributed Boolean
     * 
     * @return the Distributed Boolean
     */
    public boolean getDistributed()
    {
        return distributed;
    }
    
    public String getName()
    {
        DistributedFlagValue temp = DistributedFlagValue.L;
        
        if(distributed)
        {
            temp = DistributedFlagValue.D;
        }
        
        return topologyFileString + PSTBUtil.CONTEXT_SEPARATOR + temp + PSTBUtil.CONTEXT_SEPARATOR + protocol;
    }
    
    /**
     * Retrieves a Broker Object.
     * (Basically an extension of the HashMap's get() function)
     * 
     * @param givenName - the name
     * @return the broker object - which could be null if none exists
     */
    public PSBroker getParticularBroker(String givenName)
    {
        return (PSBroker) brokerObjects.get(givenName);
    }
    
    /**
     * Retrieves a Client Object.
     * (Basically an extension of the HashMap's get() function)
     * 
     * @param givenName - the name
     * @return the client object - which could be null if none exists
     */
    public PSClient getParticularClient(String givenName)
    {
        return (PSClient) clientObjects.get(givenName);
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
     * Determines if the Broker and Client ProcessBuilders have been created
     * 
     * @return true is they are; false if they aren't
     */
    public boolean doProcessBuildersExist()
    {
        return !brokerProcesses.isEmpty() && !clientProcesses.isEmpty();
    }
    
    /**
     * This code develops the Physical Topology
     * I.e. creates all the Broker and Client Objects
     * using information from the BenchmarkProperties file
     * 
     * @param givenDistributed - the distributed flag
     * @return false if an error occurs, true otherwise
     */
    public boolean developTopologyObjects(boolean givenDistributed)
    {
        distributed = givenDistributed;
        
        if(givenMachines == null || givenMachines.isEmpty())
        {
            logger.error(logHeader + "No Hosts and Ports have been submitted to handle a distributed topology!");
            return false;
        }
        
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
        
        if(mode.equals(ExperimentType.Throughput))
        {
            if(numThroughputPubs == 0)
            {
                logger.error(logHeader + "No throughput publishers created!");
                return false;
            }
            else
            {
                clientObjects.forEach((clientName, client)->{
                    PSClient actualClient = (PSClient) client;
                    actualClient.setNumPubs(numThroughputPubs);
                });
            }
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
        logger.debug(logHeader + "Attempting to develop PSBroker Objects...");
        TreeMap<String, ArrayList<String>> brokerList = startingTopology.getBrokers();
        
        // Loop through the brokerList -> that way we can create a bunch of unconnected brokerObjects
        Iterator<String> iteratorBL = brokerList.keySet().iterator();
        for( ; iteratorBL.hasNext() ; )
        {
            String brokerIName = iteratorBL.next();
            
            Machine host = getMachine(true);
            if(host == null)
            {
                logger.error(logHeader + "no machine exists for broker " + brokerIName + "!");
            }
            String hostName = host.getMachineName();
            Integer port = host.getActivePort();
            
            PSBroker actBrokerI = createPSBrokerObject(brokerIName, hostName, port);
            
            brokerObjects.put(brokerIName, actBrokerI);
            nodeMachine.put(brokerIName, hostName);
            updateNNM(hostName, NodeRole.B);
            
            // Make sure the port gets updated - i.e. removed from selection
            host.updateActivePort();
            int hostIndex = givenMachines.indexOf(host);
            givenMachines.set(hostIndex, host);
        }
        
        // Now loop through the brokerObject, accessing the brokerList to find a given broker's connections
        Iterator<String> iteratorBO = brokerObjects.keySet().iterator();
        for( ; iteratorBO.hasNext() ; )
        {
            String brokerIName = iteratorBO.next();
            PSBroker brokerI = (PSBroker) brokerObjects.get(brokerIName);
            ArrayList<String> bIConnectedNodes = brokerList.get(brokerIName);
            if(bIConnectedNodes == null)
            {
                int numBrokers = startingTopology.numBrokers();
                if(numBrokers > 1)
                {
                    logger.error(logHeader + brokerIName + " isn't connected to anything, but other nodes exist!");
                    return false;
                }
                String[] nURIs = new String[0];
                brokerI.setNeighbourURIs(nURIs);
            }
            else
            {
                ArrayList<String> neededURIs = new ArrayList<String>();
                for(int j = 0 ; j < bIConnectedNodes.size() ; j++)
                {
                    String brokerJName = bIConnectedNodes.get(j);
                    PSBroker actBrokerJ = (PSBroker) brokerObjects.get(brokerJName);
                    if(actBrokerJ == null)
                    {
                        logger.error(logHeader + "Couldn't find " + brokerJName + " that " + brokerIName + " is connected to!");
                        return false;
                    }
                    neededURIs.add(actBrokerJ.getBrokerURI());
                }
                
                String[] nURIs = (String[]) neededURIs.toArray(new String[neededURIs.size()]);
                handleAdjacentBrokers(brokerI, bIConnectedNodes, nURIs);
            }
            
            brokerI.setBenchmarkNumber(benchmarkNumber);
            brokerI.setTopologyFileString(topologyFileString);
            brokerI.setDistributed(distributed);
            brokerI.setMode(mode);
            
            brokerObjects.put(brokerIName, brokerI);
        }
        
        logger.debug(logHeader + "All broker objects developed.");
        return true;
    }
    
    /**
     * Gets a machine that a Client can run on
     * 
     * @return the name of the machine
     */
    private Machine getMachine(boolean isBroker)
    {    
        Machine retVal = null;
        if(!distributed)
        {
            retVal = givenMachines.get(givenMachineI);
        }
        else
        {
            int wrongMachines = 0;
            boolean keepIterating = true;
            while(keepIterating)
            {
                Machine currentMachine = givenMachines.get(givenMachineI);
                if(isBroker && (currentMachine.getActivePort() == null))
                {
                    wrongMachines++;
                    
                    if(wrongMachines > givenMachines.size())
                    {
                        return null;
                    }
                } 
                else 
                {
                    keepIterating = false;
                    retVal = currentMachine;
                }
                updateGMI();
            }
        }
        
        return retVal;
    }
    
    private void updateGMI()
    {
        givenMachineI = (givenMachineI + 1) % givenMachines.size();
    }
    
    /**
     * Allows a child class to create their version of a PSBroker Object and pass it to developBrokers()
     * 
     * @see developBrokers()
     * @param givenName - the name / id of this PSBroker Object
     * @param givenHost - the host of this PSBroker Object
     * @param givenPort - the port of this PSBroker Object
     * @return the resulting PSBroker
     */
    public abstract PSBroker createPSBrokerObject(String givenName, String givenHost, Integer givenPort);
    
    public abstract void handleAdjacentBrokers(PSBroker givenBroker, ArrayList<String> connectedBrokersNames, 
            String[] connectedBrokersURIs);
    
    /**
     * "Develops" (creates) the Client objects
     * 
     * @return false if there's an error, true otherwise 
     */
    private boolean developClients()
    {
        logger.debug(logHeader + "Attempting to develop PSClient Objects...");
        boolean isThroughputPub = true;
        
        TreeMap<String, ClientNotes> clientList = startingTopology.getClients();
        Iterator<String> cliIterator = clientList.keySet().iterator();
        for( ; cliIterator.hasNext() ; )
        {
            PSClient clientI = createPSClientObject();
            
            String clientIName = cliIterator.next();
            ClientNotes clientINotes = clientList.get(clientIName);
            
            String workloadFileString = clientINotes.getRequestedWorkload();
            ArrayList<PSAction> clientIWorkload = null;
            if(mode.equals(ExperimentType.Scenario))
            {
                clientIWorkload = masterWorkload.get(workloadFileString);
                if(clientIWorkload == null)
                {
                    logger.error(logHeader + "Client " + clientIName + " is requesting a non-existant workload!");
                    return false;
                }
                
                clientI.setClientMode(PSClientMode.Scenario);
            }
            else if(mode.equals(ExperimentType.Throughput))
            {
                if(workloadFileString.contains("pub") || workloadFileString.contains("Pub"))
                {
                    clientI.setClientMode(PSClientMode.TPPub);
                    numThroughputPubs++;
                }
                else if(workloadFileString.contains("sub") || workloadFileString.contains("Sub"))
                {
                    clientI.setClientMode(PSClientMode.TPSub);
                }
                else
                {
                    if(isThroughputPub)
                    {
                        clientI.setClientMode(PSClientMode.TPPub);
                        numThroughputPubs++;
                    }
                    else
                    {
                        clientI.setClientMode(PSClientMode.TPSub);
                    }
                    isThroughputPub = !isThroughputPub;
                }
            }
            else
            {
                return false;
            }
            
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
                
                PSBroker brokerJ = (PSBroker) brokerObjects.get(brokerJName);
                String brokerJURI = brokerJ.getBrokerURI();
                clientIBrokerURIs.add(brokerJURI);
            }
            
            clientI.setNodeName(clientIName);
            clientI.setBrokersURIs(clientIBrokerURIs);
            clientI.setDistributed(distributed);
            clientI.setNetworkProtocol(protocol);
            clientI.setTopologyFileString(topologyFileString);
            clientI.setBenchmarkNumber(benchmarkNumber);
            clientI.setWorkload(clientIWorkload);
            clientI.setMode(mode);
            
            clientObjects.put(clientIName, clientI);
            
            Machine clientIMachine = getMachine(false);
            if(clientIMachine == null)
            {
                logger.error(logHeader + "no machine exists for broker " + clientIName + "!");
            }
            String clientIMachineName = clientIMachine.getMachineName();
            nodeMachine.put(clientIName, clientIMachineName);
            updateNNM(clientIMachineName, NodeRole.C);
        }
        
        logger.debug(logHeader + "All client objects developed.");
        return true;
    }
    
    /**
     * Allows a child class to create their version of a PSClient Object and pass it to developClients()
     * 
     * @see developClients()
     * @return the resulting PSClient
     */
    public abstract PSClient createPSClientObject();
    
    private void updateNNM(String givenMachine, NodeRole givenNR)
    {
        HashMap<NodeRole, Integer> numNodesMachineI = numNodesMachine.get(givenMachine);
        if(numNodesMachineI == null)
        {
            numNodesMachineI = new HashMap<NodeRole, Integer>();
            numNodesMachineI.put(givenNR, 1);
        }
        else
        {
            Integer numGivenNodeMachineI = numNodesMachineI.get(givenNR);
            
            if(numGivenNodeMachineI == null)
            {
                numNodesMachineI.put(givenNR, 1);
            }
            else
            {
                numGivenNodeMachineI++;
                numNodesMachineI.put(givenNR, numGivenNodeMachineI);
            }
        }
        
        numNodesMachine.put(givenMachine, numNodesMachineI);
    }
    
    /**
     * Prepares the topology for a run
     * 
     * @param startSignal - the signal that will be used to let key threads know the run has started
     * @param givenRunLength - the amount of time this run will last
     * @param givenRunNumber - the current run we're on
     * @return false if there is an issue; true otherwise
     */
    public boolean prepareScenarioExperiment(Long givenRunLength, Integer givenRunNumber)
    {
        if(brokerObjects.isEmpty())
        {
            logger.error(logHeader + "prepareRun() needs brokers objects! Please run developTopologyObjects() first!");
            return false;
        }
        
        if(clientObjects.isEmpty())
        {
            logger.error(logHeader + "prepareRun() needs client objects! Please run developTopologyObjects() first!");
            return false;
        }
        
        clientObjects.forEach((clientName, actualClient)->{
            actualClient.setRunLength(givenRunLength);
            actualClient.setRunNumber(givenRunNumber);
        });
        
        brokerObjects.forEach((brokerName, actualBroker)->{
            actualBroker.setRunLength(givenRunLength);
            actualBroker.setRunNumber(givenRunNumber);
        });
        
        String contextB = PSTBUtil.generateContext(distributed, benchmarkNumber, topologyFileString, protocol, mode, givenRunLength, 
                givenRunNumber, null, null, null, null, "BrokerServer", logger, logHeader);
        
        String contextC = PSTBUtil.generateContext(distributed, benchmarkNumber, topologyFileString, protocol, mode, givenRunLength, 
                givenRunNumber, null, null, null, null, "ClientServer", logger, logHeader);
        
        setupServers(contextB, contextC);
        
        boolean retVal = generateBrokerAndClientProcesses(true);
        return retVal;
    }
    
    /**
     * Prepares the topology for a run
     * 
     * @param startSignal - the signal that will be used to let key threads know the run has started
     * @param msI 
     * @param naI 
     * @param arI 
     * @return false if there is an issue; true otherwise
     */
    public boolean prepareThroughputRun(Long plI, MessageSize msI, NumAttribute naI, AttributeRatio arI, boolean sendDiaries)
    {
        if(brokerObjects.isEmpty())
        {
            logger.error(logHeader + "prepareRun() needs brokers objects! Please run developTopologyObjects() first!");
            return false;
        }
        
        if(clientObjects.isEmpty())
        {
            logger.error(logHeader + "prepareRun() needs client objects! Please run developTopologyObjects() first!");
            return false;
        }
        
        clientObjects.forEach((clientName, actualClient)->{
            actualClient.setPeriodLength(plI);
            actualClient.setAR(arI);
            actualClient.setNA(naI);
            actualClient.setMS(msI);
        });
        
        brokerObjects.forEach((brokerName, actualBroker)->{
            actualBroker.setPeriodLength(plI);
            actualBroker.setAR(arI);
            actualBroker.setNA(naI);
            actualBroker.setMS(msI);
        });
        
        String contextB = PSTBUtil.generateContext(distributed, benchmarkNumber, topologyFileString, protocol, mode, null, 
                null, plI, arI, naI, msI, "BrokerServer", logger, logHeader);
        
        String contextC = PSTBUtil.generateContext(distributed, benchmarkNumber, topologyFileString, protocol, mode, null, 
                null, plI, arI, naI, msI, "ClientServer", logger, logHeader);
        
        String mContext = PSTBUtil.generateContext(distributed, benchmarkNumber, topologyFileString, protocol, mode, null, 
                null, plI, arI, naI, msI, "TPMaster", logger, logHeader);
        
        setupServers(contextB, contextC);
        setUpTPMaster(plI, mContext);
        
        boolean retVal = generateBrokerAndClientProcesses(sendDiaries);
        return retVal;
    }
    
    /**
     * Prepares the PSTBServer for the run
     * 
     * @param startSignal - the signal that will be used to let key threads know the run has started
     */
    private void setupServers(String contextBS, String contextCS)
    {
        brokerServerComplete = new CountDownLatch(1);
        clientServerComplete = new CountDownLatch(1);
        
        brokerServer = new ObjectServer(brokerObjects, false, contextBS, brokerServerComplete, everythingSocket);
        clientServer = new ObjectServer(clientObjects, true, contextCS, clientServerComplete, everythingSocket);
    }
    
    private void setUpTPMaster(Long givenPL, String context)
    {
        logger.debug(logHeader + "Starting Throughput Master...");
        
        HashMap<String, PSClientMode> tmDatabase = new HashMap<String, PSClientMode>();
        clientObjects.forEach((clientIsName, clientIShell)->{
            PSClient clientI = (PSClient) clientIShell;
            PSClientMode clientIsMode = clientI.getClientMode();
            
            tmDatabase.put(clientIsName, clientIsMode);
        });
        
        logger.info(logHeader + "Throughput Master started.");
        
        throughputMaster = new TPMaster(tmDatabase, givenPL, context, everythingSocket);
    }
    
    /**
     * Generates processes using the Broker and Client objects created in developPhysicalTopology()
     * ... by which I mean call a function to generate ProcessBuilder.
     * 
     * @return false on error; true otherwise
     */
    private boolean generateBrokerAndClientProcesses(boolean sendDiaries)
    {
        boolean brokerCheck = generateMultipleNodeProcesses(brokerObjects, brokerProcesses, true, false);
        if(!brokerCheck)
        {
            logger.error(logHeader + "Issue generating broker processes!");
            return false;
        }
        
        boolean clientCheck = generateMultipleNodeProcesses(clientObjects, clientProcesses, false, sendDiaries);
        if(!clientCheck)
        {
            logger.error(logHeader + "Issue generating client processes!");
            return false;
        }
        
        logger.info(logHeader + "Successfully generated broker and client processes.");
        return true;
    }
    
    /**
     * Generates a ProcessBuilder for each object passed
     * 
     * @param givenNodeObjects - the collection of PSNode objects to be turned into processes  
     * @param processShelf - the collection of processes
     * @param isBroker - is this node a broker?
     * @return false on failure; true otherwise
     */
    private boolean generateMultipleNodeProcesses(HashMap<String, PSNode> givenNodeObjects, 
            HashMap<String, ProcessBuilder> processShelf,
            boolean isBroker, boolean sendDiaries)
    {
        Iterator<String> iteratorNO = givenNodeObjects.keySet().iterator();
        for( ; iteratorNO.hasNext() ; )
        {
            String nodeIName = iteratorNO.next();
            String nodeIContext = givenNodeObjects.get(nodeIName).generateNodeContext();
            
            ProcessBuilder brokerIProcess = generateNodeProcess(nodeIName, nodeIContext, isBroker, sendDiaries);
            if(brokerIProcess == null)
            {
                logger.error(logHeader + "Couldn't create node process for " + nodeIName + "!");
                return false;
            }
            else
            {
                processShelf.put(nodeIName, brokerIProcess);
            }
        }
        
        return true;
    }
    
    /**
     * Generates the ProcessBuilder for the requested node
     * 
     * @param nodeName - the name of the node
     * @param isBroker - is this node a Broker?
     * @return null on error; the requesting ProcessBuilder otherwise
     */
    private ProcessBuilder generateNodeProcess(String nodeName, String nodeContext, boolean isBroker, boolean sendDiaries)
    {
        if(brokerServer == null)
        {
            logger.error(logHeader + "No server exists!");
            return null;
        }
        if(nodeMachine.isEmpty())
        {
            logger.error(logHeader + "No machines have been given!");
            return null;
        }
        
        Integer port = PSTBUtil.PORT;
        
        String memory = null;
        String nodetype = null;
        String sendDiary = null;
        if(isBroker)
        {
            memory = calculateMemoryForNode(nodeName, NodeRole.B).toString();
            nodetype = NodeRole.B.toString();
            sendDiary = "false";
        }
        else
        {
            memory = calculateMemoryForNode(nodeName, NodeRole.C).toString();
            nodetype = NodeRole.C.toString();
            sendDiary = String.valueOf(sendDiaries);
        }
        PSEngine engine = getEngine();
        
        ArrayList<String> command = new ArrayList<String>();
        
        String machine = "localhost";
        if(!distributed)
        {
            command.add("./startNode.sh");
        }
        else
        {
            command.add("./startRemoteNode.sh");
            
            if(user == null)
            {
                return null;
            }
            command.add(user);
            
            machine = nodeMachine.get(nodeName);
            command.add(machine);
        }
        
        command.add(memory);
        command.add(nodeName);
        command.add(nodeContext);
        command.add(ipAddress);
        command.add(port.toString());
        command.add(engine.toString());
        command.add(nodetype);
        command.add(sendDiary);
        
        logger.info(nodeName + "'s memory is " + memory + " on machine " + machine + ".");
        
        String[] finalCommand = command.toArray(new String[0]);
        ProcessBuilder createdProcessShell = new ProcessBuilder(finalCommand);
        createdProcessShell.redirectErrorStream(true);
        
        return createdProcessShell;
    }
    
    private Integer calculateMemoryForNode(String nodeName, NodeRole givenNT)
    {
        if(givenNT == null)
        {
            return null;
        }
        
        String machineName = nodeMachine.get(nodeName);
        HashMap<NodeRole, Integer> numNodesMachineI = numNodesMachine.get(machineName);
        Integer numNodeNTMachineI = numNodesMachineI.get(givenNT);
        Integer memory = AVAILABLE_MACHINE_MEM;
        
        if(numNodesMachineI.size() != 1)
        {
            if(givenNT.equals(NodeRole.B))
            {
                memory = STARTING_BROKER_MEM;
            }
            else if(givenNT.equals(NodeRole.C))
            {
                memory = STARTING_CLIENT_MEM;
            }
            else
            {
                return null;
            }
        }
        
        return memory / numNodeNTMachineI;
    }
    
    protected abstract PSEngine getEngine();
    
    /**
     * Starts the run. 
     * I.e. Starts the PSTBServer and all of the ProcessBuilders.
     * 
     * @return false on failure; true otherwise
     */
    public boolean startRun()
    {
        boolean processesExist = doProcessBuildersExist();
        if(!processesExist)
        {
            logger.error(logHeader + "startRun() needs processes to be created first! Please run prepareRun()!");
            return false;
        }
        
        AtomicBoolean noBrokerErrors = new AtomicBoolean(true);
        Thread.UncaughtExceptionHandler brokerServerExceptionNet = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                logger.error(logHeader + "Error in the serverExceptionNet thread: ", e);
                noBrokerErrors.set(false);
            }
        };
        brokerServer.setUncaughtExceptionHandler(brokerServerExceptionNet);
        brokerServer.start();
        
        int numBrokers = brokerProcesses.size();
        boolean startBrokersCheck = startMultipleNodeProcesses(brokerProcesses, true);
        if(!startBrokersCheck)
        {
            logger.error(logHeader + "Failed to start all brokers!");
            return false;
        }
        
        try
        {
            brokerServerComplete.await();
        }
        catch(InterruptedException e) 
        {
            logger.error(logHeader + "Couldn't wait for brokers to receive their objects: ", e);
            return false;
        }
        logger.debug(logHeader + "Broker Server done.");
        
        PSTBUtil.waitAPeriod(numBrokers * PSTBUtil.SEC_TO_NANOSEC, logger, logHeader);
        
        if(!noBrokerErrors.get())
        {
            return false;
        }
        
        boolean connectBrokersCheck = connectAllBrokers();
        if(!connectBrokersCheck)
        {
            logger.error(logHeader + "Failed to connect all brokers!");
            return false;
        }
        
        PSTBUtil.waitAPeriod(numBrokers * PSTBUtil.SEC_TO_NANOSEC, logger, logHeader);
        
        if(!noBrokerErrors.get())
        {
            return false;
        }
        
        AtomicBoolean noClientErrors = new AtomicBoolean(true);
        Thread.UncaughtExceptionHandler clientServerExceptionNet = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                logger.error(logHeader + "Error in the serverExceptionNet thread: ", e);
                noClientErrors.set(false);
            }
        };
        clientServer.setUncaughtExceptionHandler(clientServerExceptionNet);
        clientServer.start();
        
        boolean startClientsCheck = startMultipleNodeProcesses(clientProcesses, false);
        if(!startClientsCheck)
        {
            logger.error(logHeader + "Failed to start all clients!");
            return false;
        }
        
        try
        {
            clientServerComplete.await();
        }
        catch(InterruptedException e) 
        {
            logger.error(logHeader + "Couldn't wait for brokers to receive their objects: ", e);
            return false;
        }
        logger.debug(logHeader + "Client Server done.");
        
        if(!noClientErrors.get())
        {
            return false;
        }
        
        if(mode.equals(ExperimentType.Throughput))
        {
            Thread.UncaughtExceptionHandler nodeHandlerExceptionNet = new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    logger.error(logHeader + "Error in the thread " + t.getName() + ": ", e);
                }
            };
            throughputMaster.setUncaughtExceptionHandler(nodeHandlerExceptionNet);        
            throughputMaster.start();
        }
        
        logger.debug(logHeader + "Everything launched.");
        return true;
    }
    
    /**
     * Starts all of processes requested
     * 
     * @param givenProcesses - a collection of processes
     * @param isBroker - is this collection of processes brokers or clients?
     * @return false on failure; true otherwise
     */
    private boolean startMultipleNodeProcesses(HashMap<String, ProcessBuilder> givenProcesses, boolean isBroker)
    {
        Iterator<String> nodeProcessIT = givenProcesses.keySet().iterator();
        for( ; nodeProcessIT.hasNext() ; )
        {
            String nodeIName = nodeProcessIT.next();
            ProcessBuilder nodeIProcess = givenProcesses.get(nodeIName);
            
            boolean handleCheck = startGivenNodeProcess(nodeIName, nodeIProcess, isBroker);
            if(!handleCheck)
            {
                logger.error(logHeader + "Couldn't start node " + nodeIName + "!");
                return false;
            }
        }
        
        logger.info(logHeader + "All given processes started.");
        return true;
    }
    
    /**
     * Starts the Process for a given node.
     * 
     * @param nodeName - The name of said node
     * @param nodeProcess - The ProcessBuilder associated with this node
     * @param isBroker - is the node a Broker?
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
    
    protected abstract boolean connectAllBrokers();
    
    /**
     * The values checkActiveProcesses can return
     */
    public enum ActiveProcessRetVal
    {
        Error, StillRunning, AllOff, FloatingBrokers
    }
    
    private enum NodeState{
        ScriptFailed, NodeFailed, NodeComplete, NodeRunning
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
        ArrayList<String> completedBrokers = checkBrokers();
        ArrayList<String> completedClients = checkClients();
        
        if(completedBrokers == null || completedClients == null)
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
            activeClients.forEach((clientName, clientProcess)->{
                logger.debug(logHeader + "Waiting on client " + clientName);
            });
            
            return ActiveProcessRetVal.StillRunning;
        }
    }
    
    public ArrayList<String> checkBrokers()
    {
        ArrayList<String> completedBrokers = new ArrayList<String>();
        
        Iterator<String> iteratorAB = activeBrokers.keySet().iterator();
        for( ; iteratorAB.hasNext() ; )
        {
            String brokerIName = iteratorAB.next();
            Process brokerI = activeBrokers.get(brokerIName);
            
            NodeState brokerCheck = nodeIsState(true, brokerIName, brokerI);
            if(brokerCheck.equals(NodeState.ScriptFailed))
            {
                completedBrokers = null;
                break;
            }
            else if(brokerCheck.equals(NodeState.NodeFailed))
            {
                completedBrokers = null;
            }
            else if(brokerCheck.equals(NodeState.NodeComplete))
            {
                completedBrokers.add(brokerIName);
            }
        }
        
        return completedBrokers;
    }
    
    public ArrayList<String> checkClients()
    {
        ArrayList<String> completedClients = new ArrayList<String>();
        
        Iterator<String> iteratorAC = activeClients.keySet().iterator();
        for( ; iteratorAC.hasNext() ; )
        {
            String clientIName = iteratorAC.next();
            Process clientI = activeClients.get(clientIName);
            
            NodeState clientCheck = nodeIsState(true, clientIName, clientI);
            if(clientCheck.equals(NodeState.ScriptFailed))
            {
                completedClients = null;
                break;
            }
            else if(clientCheck.equals(NodeState.NodeFailed))
            {
                completedClients = null;
            }
            else if(clientCheck.equals(NodeState.NodeComplete))
            {
                completedClients.add(clientIName);
            }
        }
        
        return completedClients;
    }
    
    /**
     * Determines if a given node is still running.
     * 
     * @param isBroker - is the node a broker?
     * @param nodeIName - the name of the node
     * @param nodeI - the Process associated with the node
     * @return null if there's a major error, false if the node isn't active, true if the node is active
     */
    public NodeState nodeIsState(boolean isBroker, String nodeIName, Process nodeI)
    {
        Integer checkProcessInt = null;
        if(isBroker)
        {
            checkProcessInt = nodeIExitHandler(BROKER_PROCESS_CLASS_NAME, nodeIName, nodeI);
        }
        else
        {
            checkProcessInt = nodeIExitHandler(CLIENT_PROCESS_CLASS_NAME, nodeIName, nodeI);
        }
        
        if(checkProcessInt == null)
        {
            logger.error(logHeader + "Error running checkProcessI!");
            return NodeState.ScriptFailed;
        }
        else if(checkProcessInt != 0 && checkProcessInt != INIT_TERMINATION_VALUE)
        {
            logger.error(logHeader + "Node " + nodeIName + " terminated with an error!");
            return NodeState.NodeFailed;
        }
        else if(checkProcessInt == 0)
        {
            return NodeState.NodeComplete;
        }
        
        return NodeState.NodeRunning;
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
            command.add(nodeIClass + " " + nodeIName);
        }
        else
        {
            command.add("scripts/checkMachine.sh"); 
            command.add(user);
            command.add(nodeMachine.get(nodeIName));
            command.add("\"" + nodeIClass + " " + nodeIName + "\"");
        }
        
        String[] finalCommand = command.toArray(new String [0]);
        Boolean isNodeIRunning = PSTBUtil.createANewProcess(finalCommand, logger, false, false,
            logHeader + "Couldn't create a new process to see if " + nodeIName + " was still running: ",
            logHeader + "Process " + nodeIName + " is still running.", 
            logHeader + "Process " + nodeIName + " is no longer running.");
        
        if(isNodeIRunning == null)
        {
            logger.error(logHeader + "Error running initial checking function!");
            return null;
        }
        else if(isNodeIRunning.booleanValue())
        {
            return retVal;
        }
        
        try
        {
            retVal = nodeI.exitValue();
        }
        catch(IllegalThreadStateException e)
        {
            logger.warn(logHeader + "Process doesn't exist, but we're being told to wait for a Process!");
            return retVal;
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
    public void destroyAllNodes()
    {
        ArrayList<String> command = new ArrayList<String>();
        
        if(distributed)
        {
            command.add("scripts/killAllNodes.sh");
            command.add(user);
        }
        else
        {
            command.add("scripts/killAllNodesOnThisMachine.sh");
        }
        
        String[] kill = command.toArray(new String[0]);
        
        ActiveProcessRetVal temp = ActiveProcessRetVal.StillRunning;
        while(!temp.equals(ActiveProcessRetVal.AllOff) && !temp.equals(ActiveProcessRetVal.Error))
        {
            PSTBUtil.createANewProcess(kill, logger, false, false, "Couldn't run kill process :", "", "");
            temp = checkActiveProcesses();
        }
        
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
        destroyAllNodes();
        brokerProcesses.clear();
        clientProcesses.clear();
        brokerServer = null;
    }
}
