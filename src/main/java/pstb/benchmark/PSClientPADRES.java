package pstb.benchmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.utoronto.msrg.padres.client.BrokerState;
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

public class PSClientPADRES 
{	
	private PADRESClientExtension actualClient;
	private ArrayList<BrokerState> connectedBrokers;
	
	private ClientDiary diary;
	private ArrayList<NodeRole> clientRoles;
	
	private String clientName;
	private ArrayList<String> brokerURIs;
	private Workload clientWorkload;
	private Long runLength;
	
	private final int DEFAULT_DELAY = 500;
	
	private final int SEED_AD = 23;
	
	private final int SEED_SELECTION = 23;
	private final int DOSUB = 0;
	private final int DOPUB = 1;
	
	private final String logHeader = "Client: ";
	private static final Logger clientLogger = LogManager.getRootLogger();
	
	/**
	 * Empty Constructor
	 */
	public PSClientPADRES()
	{
		diary = new ClientDiary();
		clientRoles = new ArrayList<NodeRole>();
	}

	/**
	 * Sets some of the variables and creates a new Client
	 * (The idea being you would initialize a general Client first before giving it it's tasks
	 * @param givenName - the name of the client
	 * @param givenURIs - the BrokerURIs this client will connect to
	 * @param givenWorkload - the set of actions this client will have to do
	 * @return false if there's a failure; true otherwise
	 */
	public boolean initialize(String givenName, ArrayList<String> givenURIs, Workload givenWorkload) 
	{
		clientLogger.info(logHeader + "Attempting to initialize client " + givenName);
		
		try 
		{
			actualClient = new PADRESClientExtension(givenName, this);
		} 
		catch (ClientException e)
		{
			clientLogger.error(logHeader + "Cannot initialize new client " + givenName, e);
			return false;
		}
		
		clientName = givenName;
		brokerURIs = givenURIs;
		clientWorkload = givenWorkload;
		
		clientLogger.info(logHeader + "Initialized client " + givenName);
		return true;
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
			try
			{
				connectedBrokers.add(actualClient.connect(iTHURI));
			}
			catch (ClientException e)
			{
				clientLogger.error(logHeader + "Cannot connect client " + clientName + 
							" to broker " + iTHURI, e);
				disconnect();
				return false;
			}
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
		if( runLength.equals((long)0) )
		{
			clientLogger.error(logHeader + "missing runLength\n" + "Please run either addRL() first");
			return false;
		}
		
		Long runStart = System.nanoTime();
		
		ArrayList<PSAction> activeSubsList = new ArrayList<PSAction>();
		ArrayList<PSAction> givenSubWorkload = clientWorkload.getSubscriberWorkload();
		boolean allSubsOnceActive = false;
		
		ArrayList<PSAction> activeAdsList = new ArrayList<PSAction>();
		ArrayList<PSAction> givenAdWorkload = clientWorkload.getAdvertiserWorkload();
		HashMap<PSAction, Integer> activeAdsPublicationJ = new HashMap<PSAction, Integer>();
		boolean allAdsOnceActive = false;
		
		Random activeAdIGenerator = new Random(SEED_AD);
		Random selectionGenerator = new Random(SEED_SELECTION);
		
		Long currentTime = System.nanoTime();
		
		while( (currentTime - runStart) < runLength)
		{
			Long delayValue = new Long(DEFAULT_DELAY);
			
			int nextAction = selectionGenerator.nextInt(clientRoles.size());
			if(clientRoles.size() == 1 && clientRoles.get(0) == NodeRole.P)
			{
				nextAction = DOPUB;
			}
			
			if(nextAction == DOSUB)
			{	
				if(!allSubsOnceActive)
				{
					UpdateActiveListsRetVal check = updateActiveLists(ClientAction.S, activeSubsList, givenSubWorkload);
					
					if(check.isError())
					{
						return false;
					}
					
					delayValue = check.getDelayValue();
					
					allSubsOnceActive = check.areListsEqualSize();
				}
				else
				{
					Integer numActiveSubs = activeSubsList.size();
					if(numActiveSubs > 0)
					{
						for(int i = 0 ; i < numActiveSubs ; i++)
						{
							PSAction activeSubI = activeSubsList.get(i);
							
							currentTime = System.nanoTime();
							
							Long activeSubIStartTime = diary.
									getDiaryEntryGivenCAA(ClientAction.S.toString(), activeSubI.getAttributes())
									.getTimeStartedAction();
							
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
			}
			else
			{
				if(!allAdsOnceActive)
				{
					UpdateActiveListsRetVal check = updateActiveLists(ClientAction.A, activeAdsList, givenAdWorkload);
					
					if(check.isError())
					{
						return false;
					}
					
					delayValue = check.getDelayValue();
					
					allAdsOnceActive = check.areListsEqualSize();
				}
				else
				{
					PSAction activeAdI = null;
					Integer numActiveAds = activeAdsList.size();
					
					while(activeAdI == null && numActiveAds > 0)
					{
						Integer i = activeAdIGenerator.nextInt(numActiveAds);
						
						activeAdI = activeAdsList.get(i);
						
						currentTime = System.nanoTime(); // This isn't really needed, but it looks proper
						
						Long activeAdIStartTime = diary.
								getDiaryEntryGivenCAA(ClientAction.A.toString(), activeAdI.getAttributes())
								.getTimeStartedAction(); // there is a possibility for null here
						
						if( (currentTime - activeAdIStartTime) >= activeAdI.getTimeActive() )
						{
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
						ArrayList<PSAction> activeAdIsPublications = clientWorkload.getPublicationWorkloadForAd(activeAdI);
						Integer j = activeAdsPublicationJ.get(activeAdI);
						
						if(j > activeAdIsPublications.size())
						{
							clientLogger.info(logHeader + "Advertisement " + activeAdI.getAttributes() + " has no more publications\n"
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
		
		return true;
	}
	
	private boolean executeAction(ClientAction givenActionType, PSAction givenAction)
	{
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
					clientLogger.info(logHeader + generalLog + "advertize " + givenAction);
					actualClient.advertise(givenAction.getAttributes(), brokerURIs.get(0));
					break;
				}
				case V:
				{
					clientLogger.info(logHeader + generalLog + "unadvertize " + givenAction);
					actualClient.unAdvertiseAll(); // For now
					break;
				}
				case P:
				{
					clientLogger.info(logHeader + generalLog + "publish " + givenAction);
					actualClient.publish(givenAction.getAttributes(), brokerURIs.get(0));
					break;
				}
				case S:
				{
					clientLogger.info(logHeader + generalLog + "subscribe to " + givenAction);
					actualClient.subscribe(givenAction.getAttributes(), brokerURIs.get(0));
					break;
				}
				case U:
				{
					clientLogger.info(logHeader + generalLog + "unsubscribe from " + givenAction);
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
		}
		
		return storedMessage;
	}
	
	private class UpdateActiveListsRetVal
	{
		private boolean error;
		private boolean listsEqualSize;
		private Long delayValue;
		
		public boolean isError() {
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
		
		public UpdateActiveListsRetVal()
		{
			error = false;
			listsEqualSize = false;
			delayValue = new Long(-1);
		}
		
	}
	
	private UpdateActiveListsRetVal updateActiveLists(ClientAction givenAction, 
														ArrayList<PSAction> activeList, ArrayList<PSAction> givenWorkload)
	{
		int nextI = activeList.size();
		PSAction nextAction = givenWorkload.get(nextI);
		
		UpdateActiveListsRetVal retVal = new UpdateActiveListsRetVal();
		
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
			retVal.setError(true);
			return retVal;
		}
		
		if(!check)
		{
			clientLogger.error(logHeader + "Error launching " + givenAction.toString() + " " + nextI);
			retVal.setError(true);
			return retVal;
		}
		
		activeList.add(nextAction);
		retVal.setDelayValue(nextAction.getActionDelay());
		
		if(activeList.size() == givenWorkload.size())
		{
			retVal.setListsEqualSize(true);
		}
		return retVal;
	}
}
