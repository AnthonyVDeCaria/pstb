package pstb.benchmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.apache.logging.log4j.Logger;

import ca.utoronto.msrg.padres.client.BrokerState;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import pstb.util.ClientAction;
import pstb.util.NodeRole;
import pstb.util.PSAction;
import pstb.util.Workload;
import pstb.util.diary.ClientDiary;
import pstb.util.diary.DiaryEntry;

/**
 * 
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
	private final int SEED_SELECTION = 23;
	
	private final int DOSUB = 0;
	private final int DOPUB = 1;
	
	private final String logHeader = "Client: ";
	private Logger clientLogger;
	
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
	
	public void addWorkload(Workload givenW)
	{
		clientWorkload = givenW;
	}
	
	public void addClientName(String givenName)
	{
		clientName = givenName;
	}
	
	public void addConnectedBrokers(ArrayList<String> givenConnectedBrokersURIs)
	{
		brokerURIs = givenConnectedBrokersURIs;
	}
	
	public void addLogger(Logger givenLogger)
	{
		clientLogger = givenLogger;
	}
	
	public ArrayList<NodeRole> getClientRoles()
	{
		return this.clientRoles;
	}
	
	public Long getRunLength()
	{
		return this.runLength;
	}
	
	public Workload getWorkload()
	{
		return this.clientWorkload;
	}
	
	public String getClientName()
	{
		return this.clientName;
	}
	
	public ArrayList<String> getBrokerURIs()
	{
		return this.brokerURIs;
	}
	
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
		clientLogger.info(logHeader + "Attempting to initialize client " + clientName);
		
		try 
		{
			this.cConfig = new ClientConfig();
		} 
		catch (ClientException e) 
		{
			clientLogger.error(logHeader + "Error creating new Config for Client " + clientName, e);
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
			clientLogger.error(logHeader + "Cannot initialize new client " + clientName, e);
			return false;
		}
		
		clientLogger.info(logHeader + "Initialized client " + clientName);
		return true;
	}

	/**
	 * Attempts to shutdown the Client
	 * @return false on error, true if successful
	 */
	public boolean shutdown() 
	{
		clientLogger.info(logHeader + "Attempting to shutdown client " + clientName);
		
		try 
		{
			actualClient.shutdown();
		}
		catch (ClientException e) 
		{
			clientLogger.error(logHeader + "Cannot shutdown client " + clientName, e);
			return false;
		}
		
		clientLogger.info(logHeader + "Shutdown client " + clientName);
		return true;
	}

	/**
	 * Connects this client to the network 
	 * @return false on error; true if successful
	 */
	public boolean connect() 
	{
		clientLogger.info(logHeader + "Attempting to connect client " + clientName + " to network.");
		
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
				clientLogger.error(logHeader + "Cannot connect client " + clientName + 
							" to broker " + iTHURI, e);
				this.shutdown();
				return false;
			}
			
			connectedBrokers.add(brokerI);
		}
		
		clientLogger.info(logHeader + "Added client " + clientName + " to network.");
		return true;
	}

	/**
	 * Disconnects the client from the network
	 */
	public void disconnect() 
	{
		clientLogger.info(logHeader + "Disconnecting client " + clientName + " from network.");
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
			clientLogger.error(logHeader + "missing runLength\n" + "Please run either addRL() first");
			return false;
		}
		
		clientLogger.info(logHeader + "Starting run");
				
		Long runStart = System.nanoTime();
		
		ArrayList<PSAction> activeSubsList = new ArrayList<PSAction>();
		ArrayList<PSAction> givenSubWorkload = clientWorkload.getSubscriberWorkload(); // Optimization: why get it multiple times?
		boolean allSubsOnceActive = false;
		
		ArrayList<PSAction> activeAdsList = new ArrayList<PSAction>();
		ArrayList<PSAction> givenAdWorkload = clientWorkload.getAdvertiserWorkload(); // Optimization: why get it multiple times?
		HashMap<PSAction, Integer> activeAdsPublicationJ = new HashMap<PSAction, Integer>();
		boolean allAdsOnceActive = false;
		
		Random activeAdIGenerator = new Random(SEED_AD);
		Random selectionGenerator = new Random(SEED_SELECTION);
		
		Long currentTime = System.nanoTime();
		
		while( (currentTime - runStart) < runLength)
		{
			Long delayValue = new Long(DEFAULT_DELAY);
			
			/*
			 * Determine our next action
			 * Are we doing subscriber work or publisher work?
			 */
			int nextAction = -1; 
			if(clientRoles.size() == 1)
			{
				if(clientRoles.get(0) == NodeRole.P)
				{
					nextAction = DOPUB;
					allSubsOnceActive = true;
				}
				else if(clientRoles.get(0) == NodeRole.S)
				{
					nextAction = DOSUB;
					allAdsOnceActive = true;
				}
				else
				{
					clientLogger.error(logHeader + "illegal NodeRole added to client " + clientName);
					return false;
				}
			}
			else
			{
				nextAction = selectionGenerator.nextInt(clientRoles.size());
			}
			
			/*
			 * First, we make sure we have all of our Ads/Subs were active at least once
			 */
			if(!allSubsOnceActive || !allAdsOnceActive)
			{
				AddToActiveListsRetVal check = new AddToActiveListsRetVal();
				
				if(nextAction == DOSUB)
				{
					check = addToActiveList(ClientAction.S, activeSubsList, givenSubWorkload, activeAdsPublicationJ);
					allSubsOnceActive = check.areListsEqualSize();
				}
				else if(nextAction == DOPUB)
				{
					check = addToActiveList(ClientAction.A, activeAdsList, givenAdWorkload, activeAdsPublicationJ);
					
					allAdsOnceActive = check.areListsEqualSize();
				}
				else
				{
					clientLogger.error(logHeader + "nextAction error in client " + clientName
										+ "when attempting to send all subs/ads.");
					return false;
				}
				
				if(check.hasError())
				{
					return false;
				}
				
				delayValue = check.getDelayValue();
			}
			else
			{
				/*
				 * If they were, then we handle them being active
				 * For subscribers, this means making sure that our subscriptions are active
				 * For advertisers, this means sending any publications that exist for that advertisement
				 * 					along with making sure the ads themselves are active
				 */
				if(nextAction == DOSUB)
				{
					Integer numActiveSubs = activeSubsList.size();
					if(numActiveSubs > 0)
					{
						for(int i = 0 ; i < numActiveSubs ; i++)
						{
							PSAction activeSubI = activeSubsList.get(i);
							
							currentTime = System.nanoTime();
							
							DiaryEntry subIEntry = diary.getDiaryEntryGivenCAA(ClientAction.S.toString(), activeSubI.getAttributes());
							if(subIEntry == null)
							{
								clientLogger.error(logHeader + "Couldn't find subscription " + activeSubI.getAttributes() 
														+ "'s diary entry!");
								return false;
							}
							
							Long activeSubIStartTime = subIEntry.getTimeStartedAction();
							
							if((currentTime - activeSubIStartTime) >= activeSubI.getTimeActive())
							{
								boolean checkSub = executeAction(ClientAction.U, activeSubI);
								if(!checkSub)
								{
									clientLogger.error(logHeader + "Error unsubscribing " + activeSubI);
									return false;
								}
							}
						}
					}
				}
				else if(nextAction == DOPUB)
				{
					clientLogger.debug(logHeader + "Attempting to launching new publication");
					PSAction activeAdI = null;
					Integer numActiveAds = activeAdsList.size();
					
					while(activeAdI == null && numActiveAds > 0)
					{
						clientLogger.debug(logHeader + "Attempting to get a random ad");
						Integer i = activeAdIGenerator.nextInt(numActiveAds);
						
						activeAdI = activeAdsList.get(i);
						
						currentTime = System.nanoTime();
						
						DiaryEntry adIEntry = diary.getDiaryEntryGivenCAA(ClientAction.A.toString(), activeAdI.getAttributes());
						if(adIEntry == null)
						{
							clientLogger.error(logHeader + "Couldn't find advertisement " + activeAdI.getAttributes() 
													+ "'s diary entry!");
							return false;
						}
						
						Long activeAdIStartTime = adIEntry.getTimeStartedAction();
												
						if( (currentTime - activeAdIStartTime) >= activeAdI.getTimeActive() )
						{
							clientLogger.debug(logHeader + "removing ad " + activeAdI.getAttributes());
							activeAdsPublicationJ.remove(activeAdI);
							
							activeAdsList.remove(activeAdI);
							
							boolean checkUnad = executeAction(ClientAction.V, activeAdI);
							if(!checkUnad)
							{
								clientLogger.error(logHeader + "Error unadvertising " + activeAdI.getAttributes());
								return false;
							}
							activeAdI = null;
						}
					}
					
					if(activeAdI != null)
					{
						clientLogger.debug(logHeader + "Random ad found");
						clientLogger.debug(logHeader + "Attempting to launch a publication");
						ArrayList<PSAction> activeAdIsPublications = clientWorkload.getPublicationWorkloadForAd(activeAdI);
						Integer j = activeAdsPublicationJ.get(activeAdI);
						
						clientLogger.debug("J = " + j + " | Size = " + activeAdIsPublications.size());
						
						if(j > activeAdIsPublications.size())
						{
							clientLogger.debug(logHeader + "Advertisement " + activeAdI.getAttributes() + " has no more publications\n"
												+ "Unadvertising");
							boolean checkUnad = executeAction(ClientAction.V, activeAdI);
							if(!checkUnad)
							{
								clientLogger.error(logHeader + "Error unadvertising " + activeAdI.getAttributes());
								return false;
							}
						}
						else
						{
							PSAction publicationJOfAdI = activeAdIsPublications.get(j);
							
							clientLogger.debug(logHeader + "Attempting to publish " + publicationJOfAdI.getAttributes());
							boolean checkPublication = executeAction(ClientAction.P, publicationJOfAdI);
							if(!checkPublication)
							{
								clientLogger.error(logHeader + " Error launching Publication " + publicationJOfAdI.getAttributes());
								return false;
							}
							
							delayValue = publicationJOfAdI.getActionDelay();
							
							j++;
							activeAdsPublicationJ.put(activeAdI, j);
						}
					}
				}
				else
				{
					clientLogger.error(logHeader + "nextAction error in client " + clientName + "\n" +
										"attemping to handle active subs and ads.");
					return false;
				}
			}
			
			/*
			 * Wait
			 */
			try {				
				clientLogger.trace(logHeader + "pausing");
				Thread.sleep(delayValue);
			} 
			catch (InterruptedException e) 
			{
				clientLogger.error(logHeader + "error sleeping in client " + clientName, e);
				return false;
			}
			
			currentTime = System.nanoTime();
		}
		
		clientLogger.info(logHeader + "Run Complete");
		return true;
	}

	public boolean storePublication(Message msg) 
	{
		boolean storedMessage = false;
		
		if(msg instanceof PublicationMessage)
		{
			Long currentTime = System.currentTimeMillis();
			DiaryEntry receivedMsg = new DiaryEntry();
			
			Publication pub = ((PublicationMessage) msg).getPublication();
			
			Long timePubCreated = pub.getTimeStamp().getTime();
			
			receivedMsg.addClientAction(ClientAction.R);
			receivedMsg.addAttributes(pub.toString());
			receivedMsg.addTimeCreated(timePubCreated);
			receivedMsg.addTimeReceived(currentTime);
			receivedMsg.addTimeDifference(currentTime - timePubCreated);
			
			storedMessage = true;
			clientLogger.debug(logHeader + "new publication received");
		}
		
		return storedMessage;
	}
	
	private boolean executeAction(ClientAction givenActionType, PSAction givenAction)
	{
		clientLogger.debug(logHeader + "Preparing to record action");
		boolean actionSuccessful = false;
		DiaryEntry thisEntry = new DiaryEntry();
		thisEntry.addClientAction(givenActionType);
		
		Long startAction = System.nanoTime();
		actionSuccessful = handleAction(givenActionType, givenAction);
		Long actionAcked = System.nanoTime();
		
		if(actionSuccessful)
		{
			Long timeDiff = actionAcked - startAction;
			
			thisEntry.addTimeStartedAction(startAction);
			thisEntry.addTimeBrokerAck(actionAcked);
			thisEntry.addAckDelay(timeDiff);
			thisEntry.addAttributes(givenAction.getAttributes());
			
			if(givenActionType.equals(ClientAction.U) || givenActionType.equals(ClientAction.V))
			{
				DiaryEntry ascAction = null;
				
				if(givenActionType.equals(ClientAction.U))
				{
					ascAction = diary.getDiaryEntryGivenCAA(ClientAction.S.toString(), givenAction.getAttributes());
				}
				else
				{
					ascAction = diary.getDiaryEntryGivenCAA(ClientAction.A.toString(), givenAction.getAttributes());
				}
				
				if (ascAction == null)
				{
					clientLogger.error(logHeader + "Counldn't find asscociated action to " + givenActionType 
							+ " " + givenAction);
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
		}
		
		clientLogger.debug(logHeader + "Action recorded");
		return actionSuccessful;
	}
	
	private boolean handleAction(ClientAction givenActionType, PSAction givenAction) 
	{
		String generalLog = "Attempting to ";
		
		try
		{
			switch(givenActionType)
			{
				case A:
				{
					clientLogger.info(logHeader + generalLog + "advertize " + givenAction.getAttributes());
					actualClient.advertise(givenAction.getAttributes(), brokerURIs.get(0));
					break;
				}
				case V:
				{
					clientLogger.info(logHeader + generalLog + "unadvertize " + givenAction.getAttributes());
					actualClient.unAdvertiseAll(); // For now
					break;
				}
				case P:
				{
					clientLogger.info(logHeader + generalLog + "publish " + givenAction.getAttributes());
					actualClient.publish(givenAction.getAttributes(), brokerURIs.get(0));
					break;
				}
				case S:
				{
					clientLogger.info(logHeader + generalLog + "subscribe to " + givenAction.getAttributes());
					actualClient.subscribe(givenAction.getAttributes(), brokerURIs.get(0));
					break;
				}
				case U:
				{
					clientLogger.info(logHeader + generalLog + "unsubscribe from " + givenAction.getAttributes());
					actualClient.unsubscribeAll(); // For now
					break;
				}
				default:
					break;
			}
		}
		catch(ClientException e)
		{
			clientLogger.error(logHeader + "Client Error", e);
			return false;
		}
		catch (ParseException e)
		{
			clientLogger.error(logHeader + "Error Parsing", e);
			return false;
		}
		
		clientLogger.info(logHeader + "Action successful");
		return true;
	}
	
	private class AddToActiveListsRetVal
	{
		private boolean error;
		private boolean listsEqualSize;
		private Long delayValue;
		
		public boolean hasError() {
			return error;
		}

		public void setError(boolean error) {
			this.error = error;
		}

		public boolean areListsEqualSize() {
			return listsEqualSize;
		}

		public void setListsEqualSize(boolean listsEqualSize) {
			this.listsEqualSize = listsEqualSize;
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
			listsEqualSize = false;
			delayValue = new Long(-1);
		}
	}
	
	private AddToActiveListsRetVal addToActiveList(ClientAction givenAction, 
														ArrayList<PSAction> activeList, ArrayList<PSAction> givenWorkload,
														HashMap<PSAction, Integer> activeAdsPublicationJ)
	{
		AddToActiveListsRetVal retVal = new AddToActiveListsRetVal();
		
		int nextI = activeList.size();
		PSAction nextAction = givenWorkload.get(nextI);
		
		boolean check = true; 
		
		if(givenAction == ClientAction.S)
		{
			check = executeAction(ClientAction.S, nextAction);
		}
		else if(givenAction == ClientAction.A)
		{
			check = executeAction(ClientAction.A, nextAction);
		}
		else
		{
			clientLogger.error(logHeader + "Error - told to handle unexpected action " + nextI);
			return retVal;
		}
		
		if(!check)
		{
			clientLogger.error(logHeader + "Error launching " + givenAction.toString() + " " + nextI);
			return retVal;
		}
		
		activeList.add(nextAction);
		
		if(givenAction == ClientAction.A)
		{
			check = addNewAdToActiveAdsPublicationJ(nextAction, activeAdsPublicationJ);
			if(!check)
			{
				return retVal;
			}
		}
		
		if(activeList.size() == givenWorkload.size())
		{
			retVal.setListsEqualSize(true);
		}
		
		retVal.setDelayValue(nextAction.getActionDelay());
		retVal.setError(false);
		
		return retVal;
	}
	
	private boolean addNewAdToActiveAdsPublicationJ(PSAction newAction, HashMap<PSAction, Integer> activeAdsPublicationJ)
	{
		ArrayList<PSAction> newActionsPublications = clientWorkload.getPublicationWorkloadForAd(newAction);
		
		if(newActionsPublications == null)
		{
			clientLogger.error(logHeader + "Could not find any Publications for Ad " + newAction.getAttributes());
			return false;
		}
		
		if(activeAdsPublicationJ.containsKey(newAction))
		{
			clientLogger.error(logHeader + "Ad " + newAction.getAttributes() + " already exists in activeAdsPublicationJ");
			return false;
		}
		
		activeAdsPublicationJ.put(newAction, 0);
		clientLogger.debug(logHeader + "added " + newAction.getAttributes() + "'s number of publications to activeAdsPublicationJ");
		return true;
	}
}
