package pstb.benchmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

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
import pstb.util.ClientDiary;
import pstb.util.DiaryEntry;
import pstb.util.NodeRole;
import pstb.util.PSAction;
import pstb.util.Workload;

/**
 * The Client Object
 * 
 * Handles all of the client actions: 
 * initializing it, shutting it down, 
 * connecting and disconnecting it to a broker,
 * starting it (i.e. handle advertisements, publications and subscriptions)
 * and message processing.
 * 
 * @author padres-dev-4187
 */
public class PSClientPADRES implements java.io.Serializable 
{	
	private static final long serialVersionUID = 1L;
	private String clientName;
	private ArrayList<NodeRole> clientRoles;
	private ArrayList<String> brokerURIs;
	
	private Workload clientWorkload;
	private Long runLength;
	
	private ClientDiary diary;
	
	private PADRESClientExtension actualClient;
	private ArrayList<BrokerState> connectedBrokers;
	private ClientConfig cConfig;
	
	private final long DEFAULT_DELAY = 500;
	
	private final long MIN_RUNLENGTH = 1;
	
	private final int SEED_AD = 23;
	
	private final String logHeader = "Client: ";
	private Logger logger;
	
	/**
	 * Empty Constructor
	 */
	public PSClientPADRES()
	{
		clientName = new String ();
		clientRoles = new ArrayList<NodeRole>();
		brokerURIs = new ArrayList<String>();
		
		clientWorkload = new Workload();
		runLength = new Long(0);
		
		diary = new ClientDiary();
	}
	
	/**
	 * Adds a new NodeRole to clientRoles
	 * Only if it's not a B(roker) Role
	 * And it's not already there
	 * @param newNodeRole - the new Role
	 * @return false on failure; true otherwise
	 */
	public boolean addNewClientRole(NodeRole newNodeRole)
	{
		boolean successfulAdd = false;
		if(!newNodeRole.equals(NodeRole.B) && !clientRoles.contains(newNodeRole))
		{
			clientRoles.add(newNodeRole);
			successfulAdd = true;
		}
		return successfulAdd;
	}
	
	/**
	 * Sets the runLength value
	 * @param givenRL - the rL value to be set
	 */
	public void addRL(Long givenRL)
	{
		runLength = givenRL;
	}
	
	/**
	 * Sets the Client workload
	 * @param givenW - the given Workload
	 */
	public void addWorkload(Workload givenW)
	{
		clientWorkload = givenW;
	}
	
	/**
	 * Sets the Client's name
	 * @param givenName - the new name
	 */
	public void addClientName(String givenName)
	{
		clientName = givenName;
	}
	
	/**
	 * Adds a list of the Broker's this client is connected to
	 * (By which I mean their URIs)
	 * @param givenConnectedBrokersURIs
	 */
	public void addConnectedBrokers(ArrayList<String> givenConnectedBrokersURIs)
	{
		brokerURIs = givenConnectedBrokersURIs;
	}
	
	/**
	 * Adds the Logger this Client must use
	 * @param givenLogger - the Logger
	 */
	public void addLogger(Logger givenLogger)
	{
		logger = givenLogger;
	}
	/**
	 * Gets this Client's roles
	 * @return a list of its roles
	 */
	public ArrayList<NodeRole> getClientRoles()
	{
		return this.clientRoles;
	}
	
	/**
	 * Get the runLength
	 * @return the runLength
	 */
	public Long getRunLength()
	{
		return this.runLength;
	}
	
	/**
	 * Gets this Client's workload 
	 * @return the stored Workload
	 */
	public Workload getWorkload()
	{
		return this.clientWorkload;
	}
	
	/**
	 * Get's this Client's name
	 * @return this Client's name
	 */
	public String getClientName()
	{
		return this.clientName;
	}
	
	/**
	 * Gets the URIs of the Broker's this Client is connected to 
	 * @return a list of the URIs
	 */
	public ArrayList<String> getBrokerURIs()
	{
		return this.brokerURIs;
	}
	
	/**
	 * Get this Client's diary
	 * AKA the notes its made of all PSActions
	 * @return its Diary
	 */
	public ClientDiary getDiary()
	{
		return this.diary;
	}

	/**
	 * Sets some of the variables and creates a new Client
	 * (The idea being you would initialize a general Client first before giving it it's tasks
	 * @param givenName - the name of the client
	 * @param givenURIs - the BrokerURIs this client will connect to
	 * @param givenWorkload - the set of actions this client will have to do
	 * @return false if there's a failure; true otherwise
	 */
	public boolean initialize() 
	{
		logger.info(logHeader + "Attempting to initialize client " + clientName);
		
		try 
		{
			this.cConfig = new ClientConfig();
		} 
		catch (ClientException e) 
		{
			logger.error(logHeader + "Error creating new Config for Client " + clientName, e);
			return false;
		}
		
		this.cConfig.clientID = clientName;
		this.cConfig.connectBrokerList = (String[]) brokerURIs.toArray(new String[brokerURIs.size()]);
		
		try 
		{
			actualClient = new PADRESClientExtension(cConfig, this);
		}
		catch (ClientException e)
		{
			logger.error(logHeader + "Cannot initialize new client " + clientName, e);
			return false;
		}
		
		logger.info(logHeader + "Initialized client " + clientName);
		return true;
	}

	/**
	 * Attempts to shutdown the Client
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
		
		logger.info(logHeader + "Shutdown client " + clientName);
		return true;
	}

	/**
	 * Connects this client to the network 
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
		if( runLength < MIN_RUNLENGTH )
		{
			logger.error(logHeader + "missing runLength\n" + "Please run addRL() first");
			return false;
		}
		
		logger.info(logHeader + "Starting run");
				
		Long runStart = System.nanoTime();
		
		ArrayList<PSAction> activeSubsList = new ArrayList<PSAction>();
		ArrayList<PSAction> givenSubWorkload = clientWorkload.getSubscriberWorkload(); // Optimization: why get it multiple times?
		
		ArrayList<PSAction> activeAdsList = new ArrayList<PSAction>();
		ArrayList<PSAction> givenAdWorkload = clientWorkload.getAdvertiserWorkload(); // Optimization: why get it multiple times?
		HashMap<PSAction, Integer> activeAdsPublicationJ = new HashMap<PSAction, Integer>();
		
		int numAdsToSend = 0;
		int numSubsToSend = 0;
		int numAdsSent = 0;
		int numSubsSent = 0;
		
		Random activeAdIGenerator = new Random(SEED_AD);
		
		for(int i = 0; i < clientRoles.size() ; i++)
		{
			if(clientRoles.get(i) == NodeRole.P)
			{
				numAdsToSend = givenAdWorkload.size();
			}
			else if(clientRoles.get(i) == NodeRole.S)
			{
				numSubsToSend = givenSubWorkload.size();
			}
			else
			{
				logger.error(logHeader + "illegal NodeRole added to client " + clientName);
				return false;
			}
		}
		
		Long currentTime = System.nanoTime();
		
		while( (currentTime - runStart) < runLength)
		{
			Long delayValue = new Long(DEFAULT_DELAY);
			
			if(numSubsSent < numSubsToSend)
			{
				AddToActiveListsRetVal check = new AddToActiveListsRetVal();
				check = increaseActiveList(PSActionType.S, activeSubsList, givenSubWorkload, activeAdsPublicationJ);
				if(check.hasError())
				{
					return false;
				}
				else
				{
					numSubsSent++;
					delayValue = check.getDelayValue();
				}
			}
			else
			{
				if(numAdsSent < numAdsToSend)
				{
					AddToActiveListsRetVal check = new AddToActiveListsRetVal();
					check = increaseActiveList(PSActionType.A, activeAdsList, givenAdWorkload, activeAdsPublicationJ);
					if(check.hasError())
					{
						return false;
					}
					else
					{
						numAdsSent++;
						delayValue = check.getDelayValue();
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
					
					if(clientRoles.contains(NodeRole.P))
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
							logger.debug(logHeader + "Attempting to send a new publication");
							Integer i = activeAdIGenerator.nextInt(numActiveAds);
							PSAction activeAdI = activeAdsList.get(i);
							
							ArrayList<PSAction> activeAdIsPublications = clientWorkload.getPublicationWorkloadForAd(activeAdI);
							
							if(activeAdIsPublications == null)
							{
								logger.error(logHeader + "Couldn't find Publications for Ad " + activeAdI.getAttributes());
								return false;
							}
							
							Integer j = activeAdsPublicationJ.get(activeAdI);
							
							PSAction publicationJOfAdI = activeAdIsPublications.get(j);
							
							logger.debug(logHeader + "Attempting to send publication " + j);
							boolean checkPublication = launchAction(PSActionType.P, publicationJOfAdI);
							if(!checkPublication)
							{
								logger.error(logHeader + " Error sending Publication " + j);
								return false;
							}
							
							logger.info(logHeader + "Sent publication " + publicationJOfAdI.getAttributes());
							
							delayValue = publicationJOfAdI.getActionDelay();
							
							j++;
							
							if(j > activeAdIsPublications.size())
							{
								logger.debug(logHeader + "Advertisement " + activeAdI.getAttributes() + " has no more publications -> "
													+ "Unadvertising");
								boolean checkUnad = launchAction(PSActionType.V, activeAdI);
								if(!checkUnad)
								{
									logger.error(logHeader + "Error unadvertising " + activeAdI.getAttributes());
									return false;
								}
							}
							else
							{
								activeAdsPublicationJ.put(activeAdI, j);
							}
						}
					}
				}
			}
			
			/*
			 * Wait
			 */
			try {				
				logger.trace(logHeader + "pausing");
				Thread.sleep(delayValue);
			} 
			catch (InterruptedException e) 
			{
				logger.error(logHeader + "error sleeping in client " + clientName, e);
				return false;
			}
			
			currentTime = System.nanoTime();
		}
		
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
		ThreadContext.put("client", clientName);
		
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
			
			logger.debug(logHeader + "new publication received " + pub.toString());
		}
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
		
		DiaryEntry thisEntry = new DiaryEntry();
		thisEntry.addPSActionType(selectedAction);
		boolean actionSuccessful = false;
		
		Long startAction = System.nanoTime();
		Message result = executeAction(selectedAction, givenAction);
		Long actionAcked = System.nanoTime();
		
		if(result != null)
		{
			actionSuccessful = true;
			Long timeDiff = actionAcked - startAction;
			
			String attributes = givenAction.getAttributes();
			
			thisEntry.addTimeStartedAction(startAction);
			thisEntry.addTimeBrokerAck(actionAcked);
			thisEntry.addAckDelay(timeDiff);
			thisEntry.addMessageID(result.getMessageID());
			thisEntry.addAttributes(attributes);
			
			if(selectedAction.equals(PSActionType.U) || selectedAction.equals(PSActionType.V))
			{
				DiaryEntry ascAction = null;
				
				if(selectedAction.equals(PSActionType.U))
				{
					ascAction = diary.getDiaryEntryGivenActionTypeNAttributes(PSActionType.S, attributes);
				}
				else
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
					Long aAStarted = ascAction.getTimeStartedAction();
					Long aAAcked = ascAction.getTimeBrokerAck();
					
					Long startedDif = startAction - aAStarted;
					Long ackedDif = actionAcked - aAAcked;
					
					thisEntry.addTimeActiveStarted(startedDif);
					thisEntry.addTimeActiveAck(ackedDif);
				}
			}
			
			diary.addDiaryEntryToDiary(thisEntry);
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
	
	/**
	 * The return values of the addToActiveList function
	 */
	private class AddToActiveListsRetVal
	{
		private boolean error;
		private Long delayValue;
		
		public boolean hasError() {
			return error;
		}

		public void setError(boolean error) {
			this.error = error;
		}

		public Long getDelayValue() {
			return delayValue;
		}

		public void setDelayValue(Long delayValue) {
			this.delayValue = delayValue;
		}
		
		public AddToActiveListsRetVal()
		{
			error = true;
			delayValue = new Long(-1);
		}
	}
	
	/**
	 * Add a given action to a given active list
	 * I.e. a sub to a sub or a ad to an ad
	 * 
	 * @param requestedAction - the 
	 * @param activeList
	 * @param givenWorkload
	 * @param activeAdsPublicationJ
	 * @return
	 */
	private AddToActiveListsRetVal increaseActiveList(PSActionType requestedAction, ArrayList<PSAction> activeList, 
														ArrayList<PSAction> givenWorkload,
														HashMap<PSAction, Integer> activeAdsPublicationJ)
	{
		AddToActiveListsRetVal retVal = new AddToActiveListsRetVal();
		
		int nextI = activeList.size();
		PSAction nextAction = givenWorkload.get(nextI);
		
		if(givenWorkload.isEmpty())
		{
			logger.error(logHeader + "Workload has no actions");
			return retVal;
		}
		if(requestedAction != givenWorkload.get(0).getActionType())
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

		boolean check = launchAction(requestedAction, nextAction);
		
		if(!check)
		{
			logger.error(logHeader + "Error launching " + requestedAction.toString() + " " + nextI);
			return retVal;
		}
		
		if(requestedAction == PSActionType.A)
		{
			check = addNewAdToActiveAdsPublicationJ(nextAction, activeAdsPublicationJ);
			if(!check)
			{
				return retVal;
			}
		}
		
		activeList.add(nextAction);
		
		retVal.setDelayValue(nextAction.getActionDelay());
		retVal.setError(false);
		
		return retVal;
	}
	
	private boolean addNewAdToActiveAdsPublicationJ(PSAction newAction, HashMap<PSAction, Integer> activeAdsPublicationJ)
	{
		if(newAction.getActionType() != PSActionType.A)
		{
			logger.error(logHeader + "newAction is not an Ad");
			return false;
		}
		
		ArrayList<PSAction> newActionsPublications = clientWorkload.getPublicationWorkloadForAd(newAction);
		
		if(newActionsPublications == null)
		{
			logger.error(logHeader + "Could not find any Publications for Ad " + newAction.getAttributes());
			return false;
		}
		
		if(activeAdsPublicationJ.containsKey(newAction))
		{
			logger.error(logHeader + "Ad " + newAction.getAttributes() + " already exists in activeAdsPublicationJ");
			return false;
		}
		
		activeAdsPublicationJ.put(newAction, 0);
		logger.debug(logHeader + "added Ad " + newAction.getAttributes() + " to activeAdsPublicationJ");
		return true;
	}
	
	private boolean updateActiveList(ArrayList<PSAction> givenActiveList)
	{	
		int numActiveActions = givenActiveList.size();
		ArrayList<Integer> nodesToRemove = new ArrayList<Integer>();
		
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
			
			Long activeActionIStartTime = activeActionIEntry.getTimeStartedAction();
			
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
}
