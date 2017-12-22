package pstb.benchmark.client.padres;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import ca.utoronto.msrg.padres.client.BrokerState;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;

import pstb.analysis.diary.ClientDiary;
import pstb.analysis.diary.DiaryEntry;
import pstb.analysis.diary.DistributedFlagValue;
import pstb.benchmark.client.PhysicalClient;
import pstb.util.PSTBUtil;
import pstb.startup.config.PADRESNetworkProtocol;
import pstb.startup.workload.PSAction;
import pstb.startup.workload.PSActionType;

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
public class PSTBClientPADRES implements java.io.Serializable 
{	
	private static final long serialVersionUID = 1L;
	private String clientName;
	private ArrayList<String> brokerURIs;
	
	private ArrayList<PSAction> workload;
	private Long runLength;
	private Integer runNumber;
	
	private ClientDiary diary;
	private String topologyFilePath;
	private PADRESNetworkProtocol protocol;
	private Boolean distributed;
	private String benchmarkStartTime;
	
	private PADRESClientExtension actualClient;
	private ArrayList<BrokerState> connectedBrokers;
	private ClientConfig cConfig;
	
	private final long DEFAULT_DELAY = 500;
	
	private final Long INIT_RUN_LENGTH = 0L;
	private final Integer INIT_RUN_NUMBER = -1;
	private final PADRESNetworkProtocol INIT_PROTOCOL = null;
	private final Boolean INIT_DISTRIBUTED = null;
	
	private final long MIN_RUNLENGTH = 1;
	
	ReentrantLock lock = new ReentrantLock();
	
	private final String logHeader = "Client: ";
	private final Logger clientLog = LogManager.getLogger(PhysicalClient.class);
	
	/**
	 * Empty Constructor
	 */
	public PSTBClientPADRES()
	{
		clientName = new String();
		brokerURIs = new ArrayList<String>();
		
		workload = new ArrayList<PSAction>();
		runLength = INIT_RUN_LENGTH;
		runNumber = INIT_RUN_NUMBER;
		
		diary = new ClientDiary();
		topologyFilePath = new String();
		protocol = INIT_PROTOCOL;
		distributed = INIT_DISTRIBUTED;
		benchmarkStartTime = new String();
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
	 * Adds a list of the Broker's this client is connected to
	 * (By which I mean their URIs)
	 * 
	 * @param givenConnectedBrokersURIs
	 */
	public void setConnectedBrokers(ArrayList<String> givenConnectedBrokersURIs)
	{
		brokerURIs = givenConnectedBrokersURIs;
	}
	
	/**
	 * Sets the Client workload
	 * 
	 * @param clientIWorkload - the given Workload
	 */
	public void setWorkload(ArrayList<PSAction> clientIWorkload)
	{
		workload = clientIWorkload;
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
	public void setTopologyFileString(String givenTFP) 
	{
		topologyFilePath = givenTFP;
	}
	
	/**
	 * Sets a protocol (that the diary will need)
	 * 
	 * @param givenNP - the NetworkProtocol to set
	 */
	public void setNetworkProtocol(PADRESNetworkProtocol givenNP)
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
	 * Sets the time the benchmark started (that the diary will need)
	 * 
	 * @param givenBST - a String version of the time the Benchmark started
	 */
	public void setBenchmarkStartTime(String givenBST)
	{
		benchmarkStartTime = givenBST;
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
	 * Gets the URIs of the Broker's this Client is connected to 
	 * 
	 * @return a list of the URIs
	 */
	public ArrayList<String> getBrokerURIs()
	{
		return brokerURIs;
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
	public PADRESNetworkProtocol getProtocol()
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
		if(benchmarkStartTime.isEmpty()
				|| topologyFilePath.isEmpty()
				|| protocol.equals(INIT_PROTOCOL)
				|| distributed.equals(INIT_DISTRIBUTED)
				|| runLength.equals(INIT_RUN_LENGTH) 
				|| runNumber.equals(INIT_RUN_NUMBER) 
				|| clientName.isEmpty()
			)
		{
			clientLog.error(logHeader + "Not all Diary values have been set");
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
			clientLog.error(logHeader + "error with distributed");
			return null;
		}
		// We can - it's now in a flag enum
		// WHY a flag enum: to collapse the space to two values
		
		// Convert the nanosecond runLength into milliseconds
		// WHY: neatness / it's what the user gave us->why confuse them?
		Long milliRunLength = (long) (runLength / PSTBUtil.MILLISEC_TO_NANOSEC.doubleValue());
		
//		return benchmarkStartTime + PSTBUtil.TYPE_SEPARATOR
//				+ topologyFilePath + PSTBUtil.TYPE_SEPARATOR
//				+ distributedFlag.toString() + PSTBUtil.TYPE_SEPARATOR
//				+ protocol.toString() + PSTBUtil.TYPE_SEPARATOR
//				+ milliRunLength.toString() + PSTBUtil.TYPE_SEPARATOR
//				+ runNumber.toString() + PSTBUtil.TYPE_SEPARATOR
//				+ clientName;
		
		return topologyFilePath + PSTBUtil.DIARY_SEPARATOR
				+ distributedFlag.toString() + PSTBUtil.DIARY_SEPARATOR
				+ protocol.toString() + PSTBUtil.DIARY_SEPARATOR
				+ milliRunLength.toString() + PSTBUtil.DIARY_SEPARATOR
				+ runNumber.toString() + PSTBUtil.DIARY_SEPARATOR
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
			clientLog.error(logHeader + "Attempted to initialize a client with no name");
			return false;
		}
		
		clientLog.info(logHeader + "Attempting to initialize client " + clientName);
		
		// Attempt to create a new config file
		try 
		{
			this.cConfig = new ClientConfig();
		} 
		catch (ClientException e) 
		{
			clientLog.error(logHeader + "Error creating new Config for Client " + clientName, e);
			return false;
		}
		// New Config file created
		
		this.cConfig.clientID = clientName;
		
		if(connectAsWell)
		{
			// Check that there are brokerURIs to connect to
			if(brokerURIs.isEmpty())
			{
				clientLog.error(logHeader + "Attempted to connect client " + clientName + " that had no brokerURIs");
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
			clientLog.error(logHeader + "Cannot initialize new client " + clientName, e);
			return false;
		}
		// Successful
		// If connecting was requested, that set would have also connected the client
		
		clientLog.info(logHeader + "Initialized client " + clientName);
		return true;
	}

	/**
	 * Attempts to shutdown the Client
	 * 
	 * @return false on error, true if successful
	 */
	public boolean shutdown() 
	{
		clientLog.info(logHeader + "Attempting to shutdown client " + clientName);
		
		try 
		{
			actualClient.shutdown();
		}
		catch (ClientException e) 
		{
			clientLog.error(logHeader + "Cannot shutdown client " + clientName, e);
			return false;
		}
		
		clientLog.info(logHeader + "Client " + clientName + " shutdown");
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
		clientLog.info(logHeader + "Attempting to connect client " + clientName + " to network.");
		
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
				clientLog.error(logHeader + "Cannot connect client " + clientName + 
							" to broker " + iTHURI, e);
				this.shutdown();
				return false;
			}
			
			connectedBrokers.add(brokerI);
		}
		
		clientLog.info(logHeader + "Added client " + clientName + " to network.");
		return true;
	}

	/**
	 * Disconnects the client from the network
	 */
	public void disconnect() 
	{
		clientLog.info(logHeader + "Disconnecting client " + clientName + " from network.");
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
			clientLog.error(logHeader + "missing runLength - Please run addRL() first");
			return false;
		}
		// We do
		
		ArrayList<PSAction> activeList = new ArrayList<PSAction>();
		
		PSTBUtil.synchronizeRun();
		
		clientLog.info(logHeader + "Starting run");
		int i = 0;
		Long runStart = System.nanoTime();
		Long currentTime = System.nanoTime();
		while( (currentTime - runStart) < runLength)
		{
			Long delayValue = new Long(DEFAULT_DELAY);
			
			updateActiveList(activeList);
			
			if(i < workload.size())
			{
				PSAction actionI = workload.get(i);
				PSActionType actionIsActionType = actionI.getActionType();
				
				clientLog.debug(logHeader + "Attempting to send " + actionIsActionType + " " + actionI.getAttributes() + ".");
				boolean checkPublication = launchAction(actionIsActionType, actionI);
				if(!checkPublication)
				{
					clientLog.error(logHeader + "launch failed!");
					cleanup(activeList);
					return false;
				}
				
				if(actionIsActionType.equals(PSActionType.A) || actionIsActionType.equals(PSActionType.S))
				{
					activeList.add(actionI);
				}
				
				i++;
			}
			
			// Sleep for some time
			try 
			{				
				clientLog.trace(logHeader + "pausing for " + delayValue.toString() + ".");
				Thread.sleep(delayValue);
			} 
			catch (InterruptedException e) 
			{
				clientLog.error(logHeader + "error sleeping in client " + clientName + ": ", e);
				return false;
			}
			
			currentTime = System.nanoTime();
		}

		boolean cleanupCheck = cleanup(activeList);
		if(!cleanupCheck)
		{
			clientLog.error(logHeader + "Couldn't end run cleanly!");
			return false;
		}
		else
		{
			clientLog.info(logHeader + "Run complete.");
			return true;
		}
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
			
			clientLog.debug(logHeader + "new publication received " + pub.toString());
		}
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
			clientLog.trace(logHeader + "Accessing entry " + aAIAttributes + ".");
			
			Long currentTime = System.nanoTime();
			
			DiaryEntry activeActionIEntry = diary.getDiaryEntryGivenActionTypeNAttributes(aAIActionType, aAIAttributes);
			if(activeActionIEntry == null)
			{
				clientLog.error(logHeader + "Couldn't find " + activeActionI.getActionType() + " " + activeActionI.getAttributes() 
										+ "'s diary entry!");
				return false;
			}
			
			Long activeActionIStartTime = activeActionIEntry.getStartedAction();
			
			clientLog.trace(logHeader + "Delay is = " + (currentTime - activeActionIStartTime) 
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
					clientLog.error(logHeader + "improper active list given");
					return false;
				}
				
				if(!check)
				{
					clientLog.error(logHeader + "Error ending " + activeActionI.getActionType() + " " + activeActionI.getAttributes());
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
			
			clientLog.debug(logHeader + "Removing " + inactionActionJ.getActionType().toString() + " " + inactionActionJ.getAttributes()
							+ " from ActiveList");
			givenActiveList.remove(j);
			clientLog.debug(logHeader + "Remove successful");
		}
		
		clientLog.trace(logHeader + "Update complete");
		return true;
	}
	
	/**
	 * Unadvertises and Ubsubscribes any active Ads or Subs.
	 * 
	 * @param activeList - the list of all remaining Ads and Subs
	 * @return false on failure; true otherwise
	 */
	private boolean cleanup(ArrayList<PSAction> activeList)
	{
		int sizeAL = activeList.size();
		clientLog.debug(logHeader + "Undoing " + sizeAL + " 'infinite' actions."); 
		for(int i = 0 ; i < sizeAL ; i++)
		{
			PSAction activeActionI = activeList.get(i);
			
			if(activeActionI.getActionType().equals(PSActionType.A))
			{
				boolean check = launchAction(PSActionType.V, activeActionI);
				if(!check)
				{
					clientLog.error(logHeader + "Couldn't unadvertize " + activeActionI.getAttributes() + "!");
					return false;
				}
			}
			else if(activeActionI.getActionType().equals(PSActionType.S))
			{
				boolean check = launchAction(PSActionType.U, activeActionI);
				if(!check)
				{
					clientLog.error(logHeader + "Couldn't unsubscribe " + activeActionI.getAttributes() + "!");
					return false;
				}
			}
			else
			{
				clientLog.error(logHeader + "ActiveList contains a improper PSAction!");
				return false;
			}
		}
		return true;
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
		clientLog.debug(logHeader + "Preparing to record " + selectedAction + " " + givenAction.getAttributes());
		
		/*
		 *  Make sure our inputs are proper
		 */
		// The only time the ActionTypes should differ is if we want to Unad / Unsub
		if(selectedAction != givenAction.getActionType())
		{
			if(selectedAction == PSActionType.V && givenAction.getActionType() == PSActionType.A)
			{
				clientLog.trace(logHeader + "Attempting to unadvertise");
			}
			else if(selectedAction == PSActionType.U && givenAction.getActionType() == PSActionType.S)
			{
				clientLog.trace(logHeader + "Attempting to unsubscribe");
			}
			else
			{
				clientLog.error(logHeader + "givenAction and selectedAction are different");
				return false;
			}
		}
		// This function should be used for the R(eceived) ActionType
		// That should be limited to storePublication
		if(selectedAction.equals(PSActionType.R))
		{
			clientLog.error(logHeader + "launchAction doesn't handle Received publications. Use storePublication()");
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
					clientLog.error(logHeader + "Couldn't find associated action to " + selectedAction + " " + attributes);
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
			
			clientLog.info(logHeader + selectedAction + " " + attributes + " recorded");
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
					clientLog.debug(logHeader + generalLog + "advertize " + givenAction.getAttributes());
					result = actualClient.advertise(givenAction.getAttributes(), brokerURIs.get(0));
					break;
				}
				case V:
				{
					String attri = givenAction.getAttributes();
					
					clientLog.debug(logHeader + generalLog + "unadvertize " + attri);
					
					DiaryEntry temp = diary.getDiaryEntryGivenActionTypeNAttributes(PSActionType.A, attri);
					String mID = temp.getMessageID();
					result = actualClient.unAdvertise(mID);
					break;
				}
				case P:
				{
					clientLog.debug(logHeader + generalLog + "publish " + givenAction.getAttributes());
					
					int payloadSize = givenAction.getPayloadSize();
					
					if(payloadSize < 0)
					{
						clientLog.error(logHeader + "Payload size is less than 0!");
						return null;
					}
					else if(payloadSize == 0)
					{
						result = actualClient.publish(givenAction.getAttributes(), brokerURIs.get(0));
					}
					else
					{
						Publication pubI = MessageFactory.createPublicationFromString(givenAction.getAttributes());
						
						byte[] payload = new byte[payloadSize];
						String payloadString = new String();
						try 
						{
							ByteArrayOutputStream bo = new ByteArrayOutputStream();
							ObjectOutputStream oo = new ObjectOutputStream(bo);
							oo.writeObject(payload);
							oo.flush();
							payloadString = Base64.getEncoder().encodeToString(bo.toByteArray());
						} 
						catch (IOException e) 
						{
							clientLog.error(logHeader + "Error creating payload: ", e);
							return null;
						}
						pubI.setPayload(payloadString);
						
						result = actualClient.publish(pubI, brokerURIs.get(0));
					}

					break;
				}
				case S:
				{
					clientLog.debug(logHeader + generalLog + "subscribe to " + givenAction.getAttributes());
					result = actualClient.subscribe(givenAction.getAttributes(), brokerURIs.get(0));
					break;
				}
				case U:
				{
					String attri = givenAction.getAttributes();
					
					clientLog.debug(logHeader + generalLog + "unsubscribe from " + attri);
					
					DiaryEntry temp = diary.getDiaryEntryGivenActionTypeNAttributes(PSActionType.S, attri);
					String mID = temp.getMessageID();
					result = actualClient.unSubscribe(mID);
					break;
				}
				default:
					break;
			}
		}
		catch(ClientException e)
		{
			clientLog.error(logHeader + "Client Error: ", e);
			return null;
		}
		catch (ParseException e)
		{
			clientLog.error(logHeader + "Error Parsing: ", e);
			return null;
		}
		
		clientLog.debug(logHeader + "Action successful.");
		return result;
	}
}
