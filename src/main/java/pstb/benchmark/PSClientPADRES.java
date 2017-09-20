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
	
	private String clientName;
	private ArrayList<String> brokerURIs;
	private NodeRole clientJob;
	private Workload clientWorkload;
	private ClientDiary diary;
	private Integer idealMessagePeriod;
	private Integer runLength;
	
	private final int adSeed = 23;
	
	private final String logHeader = "Client: ";
	private static final Logger clientLogger = LogManager.getRootLogger();
	
	/**
	 * Empty Constructor
	 */
	public PSClientPADRES()
	{
		clientName = new String();
		brokerURIs = new ArrayList<String>();
		diary = new ClientDiary();
		clientWorkload = new Workload();
	}

	/**
	 * Sets some of the variables and creates a new Client
	 * (The idea being you would initialize a general Client first before giving it it's tasks
	 * @param givenName - the name of the client
	 * @param givenURIs - the BrokerURIs this client will connect to
	 * @param givenWorkload - the set of actions this client will have to do
	 * @return false if there's a failure; true otherwise
	 */
	public boolean initialize(String givenName, ArrayList<String> givenURIs, 
			Workload givenWorkload, Integer givenIMP, Integer givenRL, NodeRole givenRole) 
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
		
		if(givenRole.equals(NodeRole.B))
		{
			clientLogger.error(logHeader + "a client is not a broker");
			return false;
		}
		
		clientName = givenName;
		brokerURIs = givenURIs;
		clientWorkload = givenWorkload;
		idealMessagePeriod = givenIMP;
		runLength = givenRL;
		clientJob = givenRole;
		
		clientLogger.info(logHeader + "Initialized client " + givenName);
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
		Long runStart = System.nanoTime();
		
		ArrayList<PSAction> activeSubsList = clientWorkload.getSubscriberWorkload();
		
		ArrayList<PSAction> activeAdsList = clientWorkload.getAdvertiserWorkload();
		HashMap<PSAction, Integer> activeAdsPublicationJ = new HashMap<PSAction, Integer>();
		Random activeAdIGenerator = new Random(adSeed);
		
		boolean isSub = clientJob.equals(NodeRole.S);
		
		if(isSub)
		{		
			for(int i = 0 ; i < activeSubsList.size() ; i++)
			{
				PSAction subI = activeSubsList.get(i);
				
				boolean checkSub = exectueAction(ClientAction.S, subI);
				if(!checkSub)
				{
					clientLogger.error(logHeader + " Error launching subscriptions");
					return false;
				}
			}
		}
		else
		{
			for(int i = 0 ; i < activeAdsList.size() ; i++)
			{
				PSAction adI = activeAdsList.get(i);
				
				activeAdsPublicationJ.put(adI, 0);
				
				boolean checkAd = exectueAction(ClientAction.A, adI);
				if(!checkAd)
				{
					clientLogger.error(logHeader + " Error launching advertisements");
					return false;
				}
			}
		}
			
		Long currentTime = System.nanoTime();
		
		while( (currentTime - runStart) < runLength)
		{
			if(isSub)
			{
				/*
				 * Make sure our subs are active
				 * If not, stop them
				 */
				Integer numActiveSubs = activeSubsList.size();
				if(numActiveSubs > 0)
				{
					for(int i = 0 ; i < numActiveSubs ; i++)
					{
						PSAction activeSubI = activeSubsList.get(i);
						
						currentTime = System.nanoTime(); // This isn't really needed, but it looks proper
						
						Long activeSubIStartTime = diary.
								getDiaryEntryGivenCAA(ClientAction.S.toString(), activeSubI.getAttributes())
								.getTimeStartedAction();
						
						if((currentTime - activeSubIStartTime) >= activeSubI.getTimeActive())
						{
							boolean checkSub = exectueAction(ClientAction.U, activeSubI);
							if(!checkSub)
							{
								clientLogger.error(logHeader + " Error unsubscribing " + activeSubI);
								return false;
							}
						}
					}
				}
			}
			else 
			{
				PSAction activeAdI = null;
				Integer numActiveAds = activeAdsList.size();
				
				/*
				 * Get a pseudo-random advertisement
				 * If this advertisement should be no longer active
				 * 	- unadvertise it
				 * 	- look for a new one
				 * 	- ... unless they are gone
				 */
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
						
						boolean checkUnad = exectueAction(ClientAction.V, activeAdI);
						if(!checkUnad)
						{
							clientLogger.error(logHeader + " Error unadvertising " + activeAdI.getAttributes());
							return false;
						}
						activeAdI = null;
					}
				}
				
				/*
				 * If we have an Ad
				 * 	- Get the Jth publication of this ad 
				 * 	- and publish it
				 */
				if(activeAdI != null)
				{
					ArrayList<PSAction> activeAdIsPublications = clientWorkload.getPublicationWorkloadForAd(activeAdI);
					Integer j = activeAdsPublicationJ.get(activeAdI);
					
					PSAction publicationJOfAdI = activeAdIsPublications.get(j);
					
					boolean checkPublication = exectueAction(ClientAction.P, publicationJOfAdI);
					if(!checkPublication)
					{
						clientLogger.error(logHeader + " Error launching Publication " + publicationJOfAdI.getAttributes());
						return false;
					}
					
					// Update (/reset) the J variable
					j++;
					if(j >= activeAdIsPublications.size())
					{
						j = 0;
					}
					activeAdsPublicationJ.put(activeAdI, j);
				}
			}
			
			/*
			 * Wait for idealMessagePeriod
			 */
			try {				
				clientLogger.trace(logHeader + "pausing for IMR");
				Thread.sleep(idealMessagePeriod);
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
	
	private boolean exectueAction(ClientAction givenActionType, PSAction givenAction)
	{
		boolean actionSuccessful = false;
		DiaryEntry thisEntry = new DiaryEntry();
		thisEntry.addClientAction(givenActionType);
		
		Long startAction = System.nanoTime();
		actionSuccessful = handleAction(givenActionType, givenAction.getAttributes());
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
	
	private boolean handleAction(ClientAction givenAction, String attributes) 
	{
		String generalLog = "Attempting to ";
		
		try
		{
			switch(givenAction)
			{
				case A:
				{
					clientLogger.info(logHeader + generalLog + "advertize " + attributes);
					actualClient.advertise(attributes, brokerURIs.get(0));
					break;
				}
				case V:
				{
					clientLogger.info(logHeader + generalLog + "unadvertize " + attributes);
					actualClient.unAdvertiseAll(); // For now
					break;
				}
				case P:
				{
					clientLogger.info(logHeader + generalLog + "publish " + attributes);
					actualClient.publish(attributes, brokerURIs.get(0));
					break;
				}
				case S:
				{
					clientLogger.info(logHeader + generalLog + "subscribe to " + attributes);
					actualClient.subscribe(attributes, brokerURIs.get(0));
					break;
				}
				case U:
				{
					clientLogger.info(logHeader + generalLog + "unsubscribe from " + attributes);
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
	
}
