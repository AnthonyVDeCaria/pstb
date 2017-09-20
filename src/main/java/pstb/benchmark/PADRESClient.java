package pstb.benchmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.utoronto.msrg.padres.client.BrokerState;
import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import pstb.util.ClientAction;
import pstb.util.PSAction;
import pstb.util.Workload;
import pstb.util.diary.ClientDiary;
import pstb.util.diary.DiaryEntry;

public class PADRESClient 
{	
	private Client actualClient;
	private ArrayList<BrokerState> connectedBrokers;
	
	private String clientName;
	private ArrayList<String> brokerURIs;
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
	public PADRESClient()
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
			Workload givenWorkload, Integer givenIMP, Integer givenRL) 
	{
		clientLogger.info(logHeader + "Attempting to initialize client " + givenName);
		
		try 
		{
			actualClient = new Client(givenName);
		} 
		catch (ClientException e)
		{
			clientLogger.error(logHeader + "Cannot initialize new client " + givenName, e);
			return false;
		}
		
		clientName = givenName;
		brokerURIs = givenURIs;
		clientWorkload = givenWorkload;
		idealMessagePeriod = givenIMP;
		runLength = givenRL;
		
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
		
		if(!clientWorkload.getWorkloadS().isEmpty())
		{
			// Subscriber
			Long currentTime = System.nanoTime();
			
			ArrayList<PSAction> subsList = clientWorkload.getWorkloadS();
			
			for(int i = 0 ; i < subsList.size() ; i++)
			{
				PSAction subI = subsList.get(i);
				
				boolean checkSub = exectueAction(ClientAction.S, subI);
				if(!checkSub)
				{
					clientLogger.error(logHeader + " Error launching subscriptions");
					return false;
				}
			}
			
			while( (currentTime - runStart) < runLength)
			{
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
		}
		else
		{
			// Publisher
			ArrayList<PSAction> activeAdsList = clientWorkload.getWorkloadA();
			HashMap<PSAction, Integer> activeAdsPublicationI = new HashMap<PSAction, Integer>();
			Random activeAdIGenerator = new Random(adSeed);
			
			/*
			 * Advertise
			 * 	- create the Publication i Map
			 * 	- actually advertise
			 */
			for(int i = 0 ; i < activeAdsList.size() ; i++)
			{
				PSAction adI = activeAdsList.get(i);
				
				activeAdsPublicationI.put(adI, 0);
				
				boolean checkAd = exectueAction(ClientAction.A, adI);
				if(!checkAd)
				{
					clientLogger.error(logHeader + " Error launching advertisements");
					return false;
				}
			}
			
			Long currentTime = System.nanoTime();
			
			while( (currentTime - runStart) < runLength)
			{
				Integer i = activeAdIGenerator.nextInt(activeAdsList.size());
				PSAction activeAdI = null; 
				
				/*
				 * Get a pseudo-random advertisement
				 */
				while(activeAdI == null && activeAdsList.size() > 0)
				{
					activeAdI = activeAdsList.get(i);
					
					currentTime = System.nanoTime(); // This isn't really needed, but it looks proper
					
					Long activeAdIStartTime = diary.
							getDiaryEntryGivenCAA(ClientAction.A.toString(), activeAdI.getAttributes())
							.getTimeStartedAction(); // there is a possibility for null here
					
					if( (currentTime - activeAdIStartTime) >= activeAdI.getTimeActive() )
					{
						activeAdsPublicationI.remove(activeAdI);
						activeAdsList.remove(activeAdI);
						boolean checkUnad = exectueAction(ClientAction.V, activeAdI);
						if(!checkUnad)
						{
							clientLogger.error(logHeader + " Error unadvertising " + activeAdI.getAttributes());
						}
						activeAdI = null;
					}
				}
				
				if(activeAdsList.size() > 0)
				{
					/*
					 * Get the Ith/Jth publication and publish it
					 */
					ArrayList<PSAction> activeAdIsPublications = clientWorkload.getWorkloadP().get(activeAdI);
					Integer j = activeAdsPublicationI.get(activeAdI);
					
					boolean checkPublication = exectueAction(ClientAction.P, activeAdIsPublications.get(j));
					if(!checkPublication)
					{
						clientLogger.error(logHeader + " Error launching Publication " + activeAdIsPublications.get(j).getAttributes());
						return false;
					}
					
					// Update (/reset) the I/J variable
					j++;
					if(j >= activeAdIsPublications.size())
					{
						j = 0;
					}
					activeAdsPublicationI.put(activeAdI, j);
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
	
}
