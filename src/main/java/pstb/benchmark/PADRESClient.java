package pstb.benchmark;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.utoronto.msrg.padres.client.BrokerState;
import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.comm.MessageQueue;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import pstb.util.ClientAction;
import pstb.util.PSAction;
import pstb.util.Workload;

public class PADRESClient{	
	private Client actualClient;
	private ArrayList<BrokerState> connectedBrokers;
	
	private String clientName;
	private ArrayList<String> brokerURIs;
	private Workload clientWorkload;
	private ArrayList<HashMap<DiaryHeader, String>> diary;
	
	private Integer idealMessageRate;
	private Integer runLength;
	
	private final String logHeader = "Client: ";
	private static final Logger clientLogger = LogManager.getRootLogger();
	
	public PADRESClient()
	{
		clientName = new String();
		brokerURIs = new ArrayList<String>();
		diary = new ArrayList<HashMap<DiaryHeader, String>>();
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
			Workload givenWorkload, Integer givenIMR, Integer givenRL) 
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
		idealMessageRate = givenIMR;
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

	public void startRun() 
	{
		if(!clientWorkload.getWorkloadS().isEmpty())
		{
			// Subscriber
		}
		else
		{
			// Publisher
			ArrayList<PSAction> advertisement = clientWorkload.getWorkloadA();
			int i = 0;
			int pubsSent = 0;
			
//			boolean checkAction = exectueAction(ClientAction.A, advertisement.getAttributes());
//			
//			if(!checkAction)
//			{
//				clientLogger.error(logHeader + "Error sending advertizement " + advertisement.getAttributes() + 
//						" from client " + clientName);
//				return;
//			}
//			else
//			{
//				Long timeActive = advertisement.getTimeActive();
//				
//				while(timeActive > 0)
//				{
//					Long start = System.currentTimeMillis();
//					String iTHAttri = publications.get(i).getAttributes();
//					checkAction = exectueAction(ClientAction.P, iTHAttri);
//					
//					if(!checkAction)
//					{
//						clientLogger.error(logHeader + "Error sending publication " + pubsSent + 
//								" " + iTHAttri + " from client " + clientName);
//						return;
//					}
//					else
//					{
//						pubsSent++;
//						i++;
//						if(i >= publications.size())
//						{
//							i = 0;
//						}
//						Long end = System.currentTimeMillis();
//						timeActive -= (end - start);
//					}
//				}
//			}
		}
	}
	
	private boolean exectueAction(ClientAction givenAction, String attributes)
	{
		boolean actionSuccessful = false;
		createDiaryEntry(givenAction, attributes); 
		Long startAction = System.nanoTime();
		actionSuccessful = handleAction(givenAction, attributes);
		Long actionAcked = System.nanoTime();
		
		if(actionSuccessful)
		{
			Long timeDiff = actionAcked - startAction;
			
			HashMap<DiaryHeader, String> thisEntry = getDiaryEntry(givenAction.toString(), attributes);
			
			updateDiaryEntry(DiaryHeader.TimeStartedAction, startAction.toString(), thisEntry);
			updateDiaryEntry(DiaryHeader.TimeBrokerAck, actionAcked.toString(), thisEntry);
			updateDiaryEntry(DiaryHeader.AckDelay, timeDiff.toString(), thisEntry);
			
			if(givenAction.equals(ClientAction.U) || givenAction.equals(ClientAction.V))
			{
				HashMap<DiaryHeader, String> ascAction = null;
				
				if(givenAction.equals(ClientAction.U))
				{
					ascAction = getDiaryEntry(ClientAction.S.toString(), attributes);
				}
				else
				{
					ascAction = getDiaryEntry(ClientAction.A.toString(), attributes);
				}
				
				if (ascAction == null)
				{
					clientLogger.error(logHeader + "Counldn't find asscociated action to " + givenAction 
							+ " " + attributes);
					return false;
				}
				else
				{
					Long aAStarted = Long.parseLong(ascAction.get(DiaryHeader.TimeStartedAction));
					Long aAAcked = Long.parseLong(ascAction.get(DiaryHeader.TimeBrokerAck));
					
					Long startedDif = startAction - aAStarted;
					Long ackedDif = actionAcked - aAAcked;
					
					updateDiaryEntry(DiaryHeader.TimeActiveStarted, startedDif.toString(), thisEntry);
					updateDiaryEntry(DiaryHeader.TimeActiveAck, ackedDif.toString(), thisEntry);
				}
			}
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

	/**
	 * Creates a new diary entry
	 * @param givenAction - the action the client is accomplishing; be it Advertise, Publish, etc
	 * @param attributes - the Attributes associated with this action
	 */
	private void createDiaryEntry(ClientAction givenAction, String attributes) 
	{
		HashMap<DiaryHeader, String> newEntry = new HashMap<DiaryHeader, String>();
		
		newEntry.put(DiaryHeader.ClientAction, givenAction.toString());
		newEntry.put(DiaryHeader.Attributes, attributes);
		
		diary.add(newEntry);
	}
	
	/**
	 * Gets a diary entry given it's associated client action and attributes
	 * @param clientAction - the associated client action 
	 * @param attributes - the associated attributes
	 * @return Either the given diary entry, or null
	 */
	private HashMap<DiaryHeader, String> getDiaryEntry(String clientAction, String attributes)
	{
		HashMap<DiaryHeader, String> appropriateDiary = null;
		for(int i = 0; i < diary.size() ; i++)
		{
			HashMap<DiaryHeader, String> iTHEntry = diary.get(i);
			if(iTHEntry.containsValue(clientAction) && iTHEntry.containsValue(attributes))
			{
				appropriateDiary = iTHEntry;
				break;
			}
		}
		return appropriateDiary;
	}
	
	/**
	 * Given an ClientAction and attributes, update the diary entry with new data 
	 * @param newHeader - the new header
	 * @param newData - the new data to add
	 * @param givenEntry - the entry to be updated
	 */
	private void updateDiaryEntry(DiaryHeader newHeader, String newData, HashMap<DiaryHeader, String> givenEntry) 
	{
		givenEntry.put(newHeader, newData);
	}
	
}
