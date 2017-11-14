package pstb.benchmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import ca.utoronto.msrg.padres.client.BrokerState;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import pstb.util.PSActionType;
import pstb.util.PSTBUtil;
import pstb.util.ClientDiary;
import pstb.util.ClientRole;
import pstb.util.DiaryEntry;
import pstb.util.DistributedFlagValue;
import pstb.util.NetworkProtocol;
import pstb.util.PSAction;
import pstb.util.Workload;

/**
 * @author padres-dev-4187
 * 
 * The Client Object
 * 
 * Handles all of the client actions: 
 * initializing it, shutting it down, 
 * connecting and disconnecting it to a broker,
 * starting it (i.e. handle advertisements, publications and subscriptions)
 * and message processing.
 */
public class PSClientPADRES implements java.io.Serializable 
{	
	private static final long serialVersionUID = 1L;
	private String clientName;
	private ArrayList<ClientRole> clientRoles;
	private ArrayList<String> brokerURIs;
	
	private Workload clientWorkload;
	private Long runLength;
	private Integer runNumber;
	
	private ClientDiary diary;
	private String topologyFilePath;
	private NetworkProtocol protocol;
	private Boolean distributed;
	
	private PADRESClientExtension actualClient;
	private ArrayList<BrokerState> connectedBrokers;
	private ClientConfig cConfig;
	
	private final long DEFAULT_DELAY = 500;
	
	private final Long INIT_RUN_LENGTH = new Long(0);
	private final Integer INIT_RUN_NUMBER = new Integer(-1);
	private final NetworkProtocol INIT_PROTOCOL = null;
	private final Boolean INIT_DISTRIBUTED = null;
	
	private final long MIN_RUNLENGTH = 1;
	
	private final int SEED_AD = 23;
	
	private final int INIT_J = 0;
	
	ReentrantLock lock = new ReentrantLock();
	
	private final String logHeader = "Client: ";
	private Logger logger;
	
	/**
	 * Empty Constructor
	 */
	public PSClientPADRES()
	{
		clientName = new String();
		clientRoles = new ArrayList<ClientRole>();
		brokerURIs = new ArrayList<String>();
		
		clientWorkload = new Workload();
		runLength = INIT_RUN_LENGTH;
		runNumber = INIT_RUN_NUMBER;
		
		diary = new ClientDiary();
		topologyFilePath = new String();
		protocol = INIT_PROTOCOL;
		distributed = INIT_DISTRIBUTED;
	}
	
	/**
	 * Sets the Client's name
	 * 
	 * @param givenName - the new name
	 */
	public void setClientName(String givenName)
	{
		clientName = givenName;
	}
	
	/**
	 * Sets the ClientRoles
	 * 
	 * @param roles - the new ClientRoles
	 */
	public void setClientRoles(ArrayList<ClientRole> roles)
	{
		clientRoles = roles;
	}
	
	/**
	 * Adds a list of the Broker's this client is connected to
	 * (By which I mean their URIs)
	 * 
	 * @param givenConnectedBrokersURIs
	 */
	public void addConnectedBrokers(ArrayList<String> givenConnectedBrokersURIs)
	{
		brokerURIs = givenConnectedBrokersURIs;
	}
	
	/**
	 * Sets the Client workload
	 * 
	 * @param givenW - the given Workload
	 */
	public void addWorkload(Workload givenW)
	{
		clientWorkload = givenW;
	}
	
	/**
	 * Sets the runLength value
	 * 
	 * @param givenRL - the rL value to be set
	 */
	public void setRunLength(Long givenRL)
	{
		runLength = givenRL;
	}
	
	/**
	 * Adds the run number
	 * 
	 * @param givenRN - the given run number
	 */
	public void setRunNumber(Integer givenRN)
	{
		runNumber = givenRN;
	}
	
	/**
	 * Sets a Topology file path (that the diary will need)
	 * 
	 * @param givenTFP - the topologyFilePath to set
	 */
	public void setTopologyFilePath(String givenTFP) 
	{
		topologyFilePath = givenTFP;
	}
	
	/**
	 * Sets a protocol (that the diary will need)
	 * 
	 * @param givenNP - the NetworkProtocol to set
	 */
	public void setNetworkProtocol(NetworkProtocol givenNP)
	{
		protocol = givenNP;
	}
	
	/**
	 * Sets a distributed boolean (that the diary will need)
	 * 
	 * @param givenDis - the Distributed value to set
	 */
	public void setDistributed(Boolean givenDis)
	{
		distributed = givenDis;
	}
	
	/**
	 * Adds the Logger this Client must use
	 * 
	 * @param givenLogger - the Logger
	 */
	public void addLogger(Logger givenLogger)
	{
		logger = givenLogger;
	}
	
	/**
	 * Get's this Client's name
	 * 
	 * @return this Client's name
	 */
	public String getClientName()
	{
		return clientName;
	}
	
	/**
	 * Gets this Client's roles
	 * 
	 * @return a list of its roles
	 */
	public ArrayList<ClientRole> getClientRoles()
	{
		return clientRoles;
	}
	
	/**
	 * Gets the URIs of the Broker's this Client is connected to 
	 * 
	 * @return a list of the URIs
	 */
	public ArrayList<String> getBrokerURIs()
	{
		return brokerURIs;
	}
	
	/**
	 * Gets this Client's workload 
	 * 
	 * @return the stored Workload
	 */
	public Workload getWorkload()
	{
		return clientWorkload;
	}
	
	/**
	 * Get the runLength
	 * 
	 * @return the runLength
	 */
	public Long getRunLength()
	{
		return runLength;
	}
	
	/**
	 * Gets this Client's run number
	 * 
	 * @return the stored run number
	 */
	public Integer getRunNumber()
	{
		return runNumber;
	}
	
	/**
	 * Get this Client's diary
	 * AKA the notes its made of all PSActions
	 * 
	 * @return its Diary
	 */
	public ClientDiary getDiary()
	{
		return diary;
	}
	
	/**
	 * Gets this Client's topologyFilePath
	 * 
	 * @return the topologyFilePath
	 */
	public String getTopologyFilePath()
	{
		return topologyFilePath;
	}
	
	/**
	 * Gets this Client's protocol
	 * 
	 * @return the protocol
	 */
	public NetworkProtocol getProtocol()
	{
		return protocol;
	}
	
	/**
	 * Gets this Client's distributed Boolean
	 * 
	 * @return the distributed boolean
	 */
	public Boolean getDistributed()
	{
		return distributed;
	}
	
	public String generateDiaryName()
	{
		//Check that we have everything
		if(runLength.equals(INIT_RUN_LENGTH) 
				|| runNumber.equals(INIT_RUN_NUMBER) 
				|| topologyFilePath.isEmpty()
				|| protocol.equals(INIT_PROTOCOL)
				|| distributed.equals(INIT_DISTRIBUTED)
				|| clientName.isEmpty()
			)
		{
			logger.error(logHeader + "Not all Diary values have been set");
			return null;
		}
		// We do

		// Check that we can access the distributed
		DistributedFlagValue distributedFlag = null;
		if(distributed.booleanValue() == true)
		{
			distributedFlag = DistributedFlagValue.D;
		}
		else if(distributed.booleanValue() == false)
		{
			distributedFlag = DistributedFlagValue.L;
		}
		else
		{
			logger.error(logHeader + "error with distributed");
			return null;
		}
		// We can - it's now in a flag enum
		// WHY a flag enum: to collapse the space to two values
		
		// Convert the nanosecond runLength into milliseconds
		// WHY: neatness / it's what the user gave us->why confuse them?
		Long milliRunLength = (long) (runLength / PSTBUtil.MILLISEC_TO_NANOSEC.doubleValue());
		
		return topologyFilePath + "-"
				+ distributedFlag.toString() + "-"
				+ protocol.toString() + "-"
				+ milliRunLength.toString() + "-"
				+ runNumber.toString() + "-"
				+ clientName;
	}

	/**
	 * Sets some of the variables and creates a new Client
	 * (The idea being you would initialize a general Client first before giving it it's tasks
	 * 
	 * @param connectAsWell - connects this client to the network
	 * @return false if there's a failure; true otherwise
	 */
	public boolean initialize(boolean connectAsWell) 
	{
		// Check that the name exists
		if(clientName.isEmpty())
		{
			logger.error(logHeader + "Attempted to initialize a client with no name");
			return false;
		}
		
		logger.info(logHeader + "Attempting to initialize client " + clientName);
		
		// Attempt to create a new config file
		try 
		{
			this.cConfig = new ClientConfig();
		} 
		catch (ClientException e) 
		{
			logger.error(logHeader + "Error creating new Config for Client " + clientName, e);
			return false;
		}
		// New Config file created
		
		this.cConfig.clientID = clientName;
		
		if(connectAsWell)
		{
			// Check that there are brokerURIs to connect to
			if(brokerURIs.isEmpty())
			{
				logger.error(logHeader + "Attempted to connect client " + clientName + " that had no brokerURIs");
				return false;
			}
			// There are
			
			this.cConfig.connectBrokerList = (String[]) brokerURIs.toArray(new String[brokerURIs.size()]);
		}
		
		// Attempt to create the PADRES Client object
		try 
		{
			actualClient = new PADRESClientExtension(cConfig, this);
		}
		catch (ClientException e)
		{
			logger.error(logHeader + "Cannot initialize new client " + clientName, e);
			return false;
		}
		// Successful
		// If connecting was requested, that set would have also connected the client
		
		logger.info(logHeader + "Initialized client " + clientName);
		return true;
	}

	/**
	 * Attempts to shutdown the Client
	 * 
	 * @return false on error, true if successful
	 */
	public boolean shutdown() 
	{
		logger.info(logHeader + "Attempting to shutdown client " + clientName);
		
		try 
		{
			actualClient.shutdown();
		}
		catch (ClientException e) 
		{
			logger.error(logHeader + "Cannot shutdown client " + clientName, e);
			return false;
		}
		
		logger.info(logHeader + "Client " + clientName + " shutdown");
		return true;
	}

	/**
	 * Connects this client to the network
	 * NOTE: NOT CURRENTLY WORKING! 
	 * Please use initialize(true)
	 * 
	 * @return false on error; true if successful
	 */
	public boolean connect() 
	{
		logger.info(logHeader + "Attempting to connect client " + clientName + " to network.");
		
		for(int i = 0 ; i < brokerURIs.size() ; i++)
		{
			String iTHURI = brokerURIs.get(i);
			BrokerState brokerI = null;
			
			try
			{
				brokerI = actualClient.connect(iTHURI);
			}
			catch (ClientException e)
			{
				logger.error(logHeader + "Cannot connect client " + clientName + 
							" to broker " + iTHURI, e);
				this.shutdown();
				return false;
			}
			
			connectedBrokers.add(brokerI);
		}
		
		logger.info(logHeader + "Added client " + clientName + " to network.");
		return true;
	}

	/**
	 * Disconnects the client from the network
	 */
	public void disconnect() 
	{
		logger.info(logHeader + "Disconnecting client " + clientName + " from network.");
		actualClient.disconnectAll();
	}

	/**
	 * Begins the run for the client
	 * @return false if there are any errors; true otherwise
	 */
	public boolean startRun()
	{
		// Check if we have a runLength
		if( runLength < MIN_RUNLENGTH )
		{
			logger.error(logHeader + "missing runLength - Please run addRL() first");
			return false;
		}
		// We do
		
		logger.info(logHeader + "Starting run");
		
		// Get the start time
		Long runStart = System.nanoTime();
		
		/*
		 * Variable setting
		 */
		// Workloads / active Actions
		ArrayList<PSAction> activeSubsList = new ArrayList<PSAction>();
		ArrayList<PSAction> givenSubWorkload = clientWorkload.getSubscriberWorkload(); // Optimization: why get it multiple times?
		
		ArrayList<PSAction> activeAdsList = new ArrayList<PSAction>();
		ArrayList<PSAction> givenAdWorkload = clientWorkload.getAdvertiserWorkload(); // Optimization: why get it multiple times?
		HashMap<PSAction, Integer> activeAdsPublicationJ = new HashMap<PSAction, Integer>();
		
		// Ints
		int numAdsToSend = 0;
		int numSubsToSend = 0;
		int numAdsSent = 0;
		int numSubsSent = 0;
		
		// Random
		Random activeAdIGenerator = new Random(SEED_AD);
		
		// Determine how many Ads and Subs to send
		if(clientRoles.contains(ClientRole.P))
		{
			numAdsToSend = givenAdWorkload.size();
		}
		if(clientRoles.contains(ClientRole.S))
		{
			numSubsToSend = givenSubWorkload.size();
		}
		
		Long currentTime = System.nanoTime();
		
		/*
		 * Run Pseudocode
		 * 
		 * While we're still allowed to run
		 * 	If we haven't sent all our subs
		 * 		Send a new sub
		 * 		Get its delay
		 * 	If we have sent all our subs
		 * 		But haven't sent all our ads
		 * 			Send an ad
		 * 			Get its delay
		 * 		And sent all our ads
		 * 			Update our subs - i.e. remove subs that should be finished
		 * 			If we're also a Publisher
		 * 				Update our ads
		 * 				If we have any ads active
		 * 					Send a publication
		 * 					Get its delay
		 * 	Sleep for a bit using our delay - default or from the action
		 * 
		 * Note that if a Client is only a sub, or only a pub, the number of ads/subs it has to send is 0
		 */
		while( (currentTime - runStart) < runLength)
		{
			Long delayValue = new Long(DEFAULT_DELAY);
			
			if(numSubsSent < numSubsToSend)
			{
				Long check = increaseActiveList(PSActionType.S, activeSubsList, givenSubWorkload, activeAdsPublicationJ);
				if(check == null)
				{
					return false;
				}
				else
				{
					numSubsSent++;
					delayValue = check;
				}
			}
			else
			{
				if(numAdsSent < numAdsToSend)
				{
					Long check = increaseActiveList(PSActionType.A, activeAdsList, givenAdWorkload, activeAdsPublicationJ);
					if(check == null)
					{
						return false;
					}
					else
					{
						numAdsSent++;
						delayValue = check;
					}
				}
				else
				{
					boolean checkUpdate = updateActiveList(activeSubsList);
					if(!checkUpdate)
					{
						logger.error(logHeader + "Error updating activeSubsList");
						return false;
					}
					
					if(clientRoles.contains(ClientRole.P))
					{
						checkUpdate = updateActiveList(activeAdsList);
						if(!checkUpdate)
						{
							logger.error(logHeader + "Error updating activeAdsList");
							return false;
						}
						
						Integer numActiveAds = activeAdsList.size();
						
						if(numActiveAds > 0)
						{
							Long sendPubCheck = sendPublication(numActiveAds, activeAdsList, activeAdIGenerator,
																	activeAdsPublicationJ);
							
							if(sendPubCheck == null)
							{
								return false;
							}
							else
							{
								delayValue = sendPubCheck;
							}
						}
					}
				}
			}
			
			// Sleep for some time
			try {				
				logger.trace(logHeader + "pausing for " + delayValue.toString());
				Thread.sleep(delayValue);
			} 
			catch (InterruptedException e) 
			{
				logger.error(logHeader + "error sleeping in client " + clientName, e);
				return false;
			}
			
			currentTime = System.nanoTime();
		}
		
		/*
		 * Clean up
		 */
		// Ads
		logger.debug(logHeader + "Unadvertizing any 'infinite' ads"); 
		for(int i = 0 ; i < activeAdsList.size() ; i++)
		{
			PSAction stillActiveAdI = activeAdsList.get(i);
			boolean check = launchAction(PSActionType.V, stillActiveAdI);
			if(!check)
			{
				logger.error(logHeader + "Error unadvertizing " + stillActiveAdI.getAttributes());
				return false;
			}
		}
		
		// Subs
		logger.debug(logHeader + "Unsubscribing any 'infinite' subs"); 
		for(int i = 0 ; i < activeSubsList.size() ; i++)
		{
			PSAction stillActiveSubI = activeSubsList.get(i);
			boolean check = launchAction(PSActionType.U, stillActiveSubI);
			if(!check)
			{
				logger.error(logHeader + "Error unsubscribing " + stillActiveSubI.getAttributes());
				return false;
			}
		}
				
		logger.info(logHeader + "Run Complete");
		return true;
	}
	
	/**
	 * Stores the given Message
	 * Assuming it's a Publication
	 * @param msg - the given Message
	 */
	public void storePublication(Message msg) 
	{
		ThreadContext.put("client", generateDiaryName());
		
		if(msg instanceof PublicationMessage)
		{
			Long currentTime = System.currentTimeMillis();
			DiaryEntry receivedMsg = new DiaryEntry();
			
			Publication pub = ((PublicationMessage) msg).getPublication();
			
			Long timePubCreated = pub.getTimeStamp().getTime();
			
			receivedMsg.addPSActionType(PSActionType.R);
			receivedMsg.addMessageID(pub.getPubID());
			receivedMsg.addTimeCreated(timePubCreated);
			receivedMsg.addTimeReceived(currentTime);
			receivedMsg.addTimeDifference(currentTime - timePubCreated);
			receivedMsg.addAttributes(pub.toString());
			
			try
			{
				lock.lock();
				diary.addDiaryEntryToDiary(receivedMsg);
			}
			finally
			{
				lock.unlock();
			}
			
			logger.debug(logHeader + "new publication received " + pub.toString());
		}
	}
	
	/**
	 * Launch the next unlaunched action 
	 * and add it to its given active list
	 * 
	 * @param requestedAction - the type of Actions that are in this activeList and givenWorkload
	 * @param activeList - the given activeList
	 * @param givenWorkload - the master list of PSActions 
	 * @param activeAdsPublicationJ - In the case of an Ad, the HashMap of Ads and their jTH value
	 * 
	 * @return null on failure, the delayValue of the next
	 */
	private Long increaseActiveList(PSActionType requestedAction, ArrayList<PSAction> activeList, ArrayList<PSAction> givenWorkload,
										HashMap<PSAction, Integer> activeAdsPublicationJ)
	{
		Long retVal = null;
		int nextI = activeList.size();
		int sizeGW = givenWorkload.size();
		
		/*
		 * Checks
		 * 
		 * 0) If the requestedAction is an ad or a sub
		 * 1) If activeList.size() is < givenWorkload.size()
		 * 2) If the requestedAction is the same as the givenWorkload's
		 * 3) If the requestedAction is the same as the activeList's - assuming activeList has Actions  
		 */
		if(!requestedAction.equals(PSActionType.A) && !requestedAction.equals(PSActionType.S))
		{
			logger.error(logHeader + "Illegal action requested");
			return retVal;
		}
		if(nextI >= sizeGW)
		{
			logger.error(logHeader + "No more Actions to add: " + nextI + " = " + sizeGW);
			return retVal;
		}
		if(requestedAction != givenWorkload.get(0).getActionType()) // This assumes that the Workload is proper. Can't check everything.
		{
			logger.error(logHeader + "Type mismatch - workload");
			return retVal;
		}
		if(nextI > 0)
		{
			if(requestedAction != activeList.get(nextI-1).getActionType())
			{
				logger.error(logHeader + "Type mismatch - activeList");
				return retVal;
			}
		}
		
		/*
		 * Get the next action Pseudocode
		 * 
		 * Since the activeList is smaller than the givenWorkload
		 * And the activeList is:
		 * 	- empty 
		 * 	- already has some of the givenWorkload's actions
		 * Get the next action
		 * Since it will always be different than the ones already in the list
		 * 
		 * ... as you can imagine, this is not a bulletproof idea...
		 */
		PSAction nextAction = givenWorkload.get(nextI);
		
		boolean check = launchAction(requestedAction, nextAction);
		if(!check)
		{
			logger.error(logHeader + "Error launching " + requestedAction.toString() + " " + nextI + " " + nextAction.getAttributes());
			return retVal;
		}
		
		// If we're dealing with Advertisements, we also have to add them to the Publication J HashMap
		// That way when we we know which publication we've sent / are on
		if(requestedAction == PSActionType.A)
		{
			ArrayList<PSAction> newActionsPublications = clientWorkload.getPublicationWorkloadForAd(nextAction);
			
			if(newActionsPublications == null)
			{
				logger.error(logHeader + "Could not find any Publications for Ad " + nextAction.getAttributes());
				
				check = launchAction(PSActionType.V, nextAction);
				if(!check)
				{
					logger.error(logHeader + "Error unadvertizing problem ad " + nextAction.getAttributes());
					return retVal;
				}
				
				return retVal;
			}
			
			activeAdsPublicationJ.put(nextAction, INIT_J);
		}
		
		activeList.add(nextAction);
		
		retVal = nextAction.getActionDelay();
		
		return retVal;
	}
	
	/**
	 * Updates the givenActiveList
	 * I.e. checks every node and sees if it's still active or not
	 * by looking at its start and exist time
	 * 
	 * @param givenActiveList - the ActiveList to look over
	 * @return false on any error; true if successful
	 */
	private boolean updateActiveList(ArrayList<PSAction> givenActiveList)
	{	
		int numActiveActions = givenActiveList.size();
		ArrayList<Integer> nodesToRemove = new ArrayList<Integer>();
		
		/*
		 * updateActiveList Pseudocode
		 * 
		 * For each member of the givenActiveList
		 * 	Get the iTH one - itself, its PSActionType and its Attributes
		 * 	Get the currentTime using System.nanoTime()
		 * 	Get the Diary Entry associated with this Action - the one that was created when it was launched
		 * 		Can't find it?
		 * 			Error
		 * 		Found it?
		 * 			Get the System.nanoTime() value associated with starting it (Currently that's getStartedAction())
		 * 			If the Difference between the currentTime and the "start Time" is >= the time it should have been active
		 * 				Launch the associated undo action - U or V (assuming that the offending Action is a S or A)
		 * 					Undo Successful?
		 * 						Add i to an ArrayList<Integer>
		 * 					Undo Unsuccessful?
		 * 						Error
		 * 
		 * After this is complete
		 * 	Loop through the new ArrayList<Integer>
		 * 		Remove the jTH Action from givenActiveList
		 */
		for(int i = 0 ; i < numActiveActions ; i++)
		{
			PSAction activeActionI = givenActiveList.get(i);
			PSActionType aAIActionType = activeActionI.getActionType();
			String aAIAttributes = activeActionI.getAttributes();
			logger.trace(logHeader + "Accessing entry " + aAIAttributes);
			
			Long currentTime = System.nanoTime();
			
			DiaryEntry activeActionIEntry = diary.getDiaryEntryGivenActionTypeNAttributes(aAIActionType, aAIAttributes);
			if(activeActionIEntry == null)
			{
				logger.error(logHeader + "Couldn't find " + activeActionI.getActionType() + " " + activeActionI.getAttributes() 
										+ "'s diary entry!");
				return false;
			}
			
			Long activeActionIStartTime = activeActionIEntry.getStartedAction();
			
			logger.trace(logHeader + "Delay is = " + (currentTime - activeActionIStartTime) 
							+ " TA is = " + activeActionI.getTimeActive());
			
			if((currentTime - activeActionIStartTime) >= activeActionI.getTimeActive())
			{
				boolean check = true;
				if(activeActionI.getActionType() == PSActionType.S)
				{
					check = launchAction(PSActionType.U, activeActionI);
				}
				else if(activeActionI.getActionType() == PSActionType.A)
				{
					check = launchAction(PSActionType.V, activeActionI);
				}
				else
				{
					logger.error(logHeader + "improper active list given");
					return false;
				}
				
				if(!check)
				{
					logger.error(logHeader + "Error ending " + activeActionI.getActionType() + " " + activeActionI.getAttributes());
					return false;
				}
				else
				{
					nodesToRemove.add(i);
				}
			}
		}
		
		for(int i = 0 ; i < nodesToRemove.size() ; i++)
		{
			int j = nodesToRemove.get(i);
			PSAction inactionActionJ = givenActiveList.get(j);
			
			logger.debug(logHeader + "Removing " + inactionActionJ.getActionType().toString() + " " + inactionActionJ.getAttributes()
							+ " from ActiveList");
			givenActiveList.remove(j);
			logger.debug(logHeader + "Remove successful");
		}
		
		logger.trace(logHeader + "Update complete");
		return true;
	}
	
	/**
	 * Sends a publication
	 * 
	 * @param numActiveAds - the number of Active Ads 
	 * @param activeAdsList - the list of Active Ads
	 * @param activeAdIGenerator - a RNG 
	 * @param activeAdsPublicationJ - The HashMap of Ads to the number of Publications they've sent 
	 * @return the DelayValue associated with the launched publication; or null if there's an error
	 */
	private Long sendPublication(Integer numActiveAds, ArrayList<PSAction> activeAdsList, Random activeAdIGenerator,
										HashMap<PSAction, Integer> activeAdsPublicationJ)
	{
		Long retVal = null;
		
		logger.debug(logHeader + "Attempting to send a new publication");
		Integer i = activeAdIGenerator.nextInt(numActiveAds);
		PSAction activeAdI = activeAdsList.get(i);
		
		ArrayList<PSAction> activeAdIsPublications = clientWorkload.getPublicationWorkloadForAd(activeAdI);
		if(activeAdIsPublications == null)
		{
			logger.error(logHeader + "Couldn't find Publications for Ad " + activeAdI.getAttributes());
			return retVal;
		}
		
		Integer j = activeAdsPublicationJ.get(activeAdI);
		
		PSAction publicationJOfAdI = activeAdIsPublications.get(j);
		
		logger.debug(logHeader + "Attempting to send publication " + j);
		boolean checkPublication = launchAction(PSActionType.P, publicationJOfAdI);
		if(!checkPublication)
		{
			logger.error(logHeader + " Error sending Publication " + j);
			return retVal;
		}
		
		logger.info(logHeader + "Sent publication " + publicationJOfAdI.getAttributes());
		
		j++;
		
		if(j < activeAdIsPublications.size())
		{
			activeAdsPublicationJ.put(activeAdI, j);
		}
		else
		{
			// Unadvertise a finished ad
			
			logger.debug(logHeader + "Advertisement " + activeAdI.getAttributes() + " has no more publications -> Unadvertising");
			
			boolean checkUnad = launchAction(PSActionType.V, activeAdI);
			if(!checkUnad)
			{
				logger.error(logHeader + "Error unadvertising " + activeAdI.getAttributes());
				return retVal;
			}
			
			activeAdsPublicationJ.remove(activeAdI);
			activeAdsList.remove(activeAdI);
		}
		
		retVal = publicationJOfAdI.getActionDelay();
		return retVal;
	}
	
	/**
	 * Create a new diary entry and record all the information associated with it
	 * e.g. TimeStartedAction, AckDelay, TimeActiveAck, ...
	 * assuming executeAction runs successfully.
	 * @see executeAction
	 * 
	 * @param selectedAction - the type of action we're doing
	 * @param givenAction - the given Action
	 * @return true if the action was recorded; false on error
	 */
	private boolean launchAction(PSActionType selectedAction, PSAction givenAction)
	{
		logger.debug(logHeader + "Preparing to record " + selectedAction + " " + givenAction.getAttributes());
		
		/*
		 *  Make sure our inputs are proper
		 */
		// The only time the ActionTypes should differ is if we want to Unad / Unsub
		if(selectedAction != givenAction.getActionType())
		{
			if(selectedAction == PSActionType.V && givenAction.getActionType() == PSActionType.A)
			{
				logger.trace(logHeader + "Attempting to unadvertise");
			}
			else if(selectedAction == PSActionType.U && givenAction.getActionType() == PSActionType.S)
			{
				logger.trace(logHeader + "Attempting to unsubscribe");
			}
			else
			{
				logger.error(logHeader + "givenAction and selectedAction are different");
				return false;
			}
		}
		// This function should be used for the R(eceived) ActionType
		// That should be limited to storePublication
		if(selectedAction.equals(PSActionType.R))
		{
			logger.error(logHeader + "launchAction doesn't handle Received publications. Use storePublication()");
			return false;
		}
		
		// Variable initialization - DiaryEntry and boolean (that will be returned)
		// This is a pessimistic function - assumes that the action will fail
		DiaryEntry thisEntry = new DiaryEntry();
		boolean actionSuccessful = false;
		
		// Execute the action... after getting some number's first
		Long startTime = System.currentTimeMillis();
		Long startAction = System.nanoTime();
		Message result = executeAction(selectedAction, givenAction);
		Long endAction = System.nanoTime();
		Long brokerFinished = System.currentTimeMillis();
		
		if(result != null)
		{
			// It worked
			actionSuccessful = true;
			
			// Get a few missing recordings
			Long timeDiff = endAction - startAction;
			String attributes = givenAction.getAttributes();
			
			// Add all recordings to DiaryEntry
			thisEntry.addPSActionType(selectedAction);
			thisEntry.addStartedAction(startAction);
			thisEntry.addEndedAction(endAction);
			thisEntry.addActionDelay(timeDiff);
			thisEntry.addTimeActionStarted(startTime);
			thisEntry.addTimeBrokerFinished(brokerFinished);
			thisEntry.addMessageID(result.getMessageID());
			thisEntry.addAttributes(attributes);
			
			
			if(selectedAction.equals(PSActionType.U) || selectedAction.equals(PSActionType.V))
			{
				// If we're unsubscribing / unadvertising, we need to determine how long they were active for
				// So we need to find the associated subscribe / advertise
				DiaryEntry ascAction = null;
				
				if(selectedAction.equals(PSActionType.U))
				{
					ascAction = diary.getDiaryEntryGivenActionTypeNAttributes(PSActionType.S, attributes);
				}
				else // I can do this cause I limited the options above
				{
					ascAction = diary.getDiaryEntryGivenActionTypeNAttributes(PSActionType.A, attributes);
				}
				
				if (ascAction == null)
				{
					logger.error(logHeader + "Couldn't find associated action to " + selectedAction + " " + attributes);
					return false;
				}
				else
				{
					Long aAStarted = ascAction.getStartedAction();
					Long aAAcked = ascAction.getEndedAction();
					
					Long startedDif = startAction - aAStarted;
					Long ackedDif = endAction - aAAcked;
					
					thisEntry.addTimeActiveStarted(startedDif);
					thisEntry.addTimeActiveAck(ackedDif);
				}
			}
			
			// Add the entry to the diary
			// However, since the diary is being used by both the sending thread and the listening thread, we need a lock
			try
			{
				lock.lock();
				diary.addDiaryEntryToDiary(thisEntry);
			}
			finally
			{
				lock.unlock();
			}
			
			logger.info(logHeader + selectedAction + " " + attributes + " recorded");
		}
		
		return actionSuccessful;
	}
	
	/**
	 * Handles calling the particular PADRES function associated with a given action.
	 * I.e. if the action is an advertisement, it would call the advertise function
	 * 
	 * @param givenAction - the Action itself
	 * @return the proper message on success; null on failure
	 */
	private Message executeAction(PSActionType selectedAction, PSAction givenAction) 
	{
		String generalLog = "Attempting to ";
		Message result = null;
		
		try
		{
			switch(selectedAction)
			{
				case A:
				{
					logger.debug(logHeader + generalLog + "advertize " + givenAction.getAttributes());
					result = actualClient.advertise(givenAction.getAttributes(), brokerURIs.get(0));
					break;
				}
				case V:
				{
					String attri = givenAction.getAttributes();
					
					logger.debug(logHeader + generalLog + "unadvertize " + attri);
					
					DiaryEntry temp = diary.getDiaryEntryGivenActionTypeNAttributes(PSActionType.A, attri);
					String mID = temp.getMessageID();
					result = actualClient.unAdvertise(mID); // For now
					break;
				}
				case P:
				{
					logger.debug(logHeader + generalLog + "publish " + givenAction.getAttributes());
					result = actualClient.publish(givenAction.getAttributes(), brokerURIs.get(0));
					break;
				}
				case S:
				{
					logger.debug(logHeader + generalLog + "subscribe to " + givenAction.getAttributes());
					result = actualClient.subscribe(givenAction.getAttributes(), brokerURIs.get(0));
					break;
				}
				case U:
				{
					String attri = givenAction.getAttributes();
					
					logger.debug(logHeader + generalLog + "unsubscribe from " + attri);
					
					DiaryEntry temp = diary.getDiaryEntryGivenActionTypeNAttributes(PSActionType.S, attri);
					String mID = temp.getMessageID();
					result = actualClient.unSubscribe(mID); // For now
					break;
				}
				default:
					break;
			}
		}
		catch(ClientException e)
		{
			logger.error(logHeader + "Client Error", e);
			return null;
		}
		catch (ParseException e)
		{
			logger.error(logHeader + "Error Parsing", e);
			return null;
		}
		
		logger.debug(logHeader + "Action successful");
		return result;
	}
}
