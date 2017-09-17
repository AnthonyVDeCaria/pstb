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
import pstb.util.Workload;

public class PADRESClient{
	private static final Logger clientLogger = LogManager.getRootLogger();
	
	private Client actualClient;
	private ArrayList<BrokerState> connectedBrokers;
	private Thread listens;
	private MessageQueue receivedQueue;
	
	private String clientName;
	private ArrayList<String> brokerURIs;
	private HashMap<ClientAction, ArrayList<Workload>> actions;
	private ArrayList<HashMap<DiaryHeader, String>> diary;
	
	public PADRESClient()
	{
		clientName = new String();
		brokerURIs = new ArrayList<String>();
		diary = new ArrayList<HashMap<DiaryHeader, String>>();
		receivedQueue = new MessageQueue();
		actions = new HashMap<ClientAction, ArrayList<Workload>>();
	}

	/**
	 * Sets some of the variables and creates a new Client
	 * (The idea being you would initialize a general Client first before giving it it's tasks
	 * @param givenName - the name of the client
	 * @param givenURIs - the BrokerURIs this client will connect to
	 * @param givenActionSet - the set of actions this client will have to do
	 * @return false if there's a failure; true otherwise
	 */
	public boolean initialize(String givenName, ArrayList<String> givenURIs, 
			HashMap<ClientAction, ArrayList<Workload>> givenActionSet) 
	{
		clientLogger.info("Client: Attempting to initialize client " + givenName);
		
		try 
		{
			actualClient = new Client(givenName);
		} 
		catch (ClientException e)
		{
			clientLogger.error("Client: Cannot initialize new client " + givenName, e);
			return false;
		}
		
		clientName = givenName;
		brokerURIs = givenURIs;
		actions = givenActionSet;
		clientLogger.info("Client:  Initialized client " + givenName);
		return true;
	}

	/**
	 * Attempts to shutdown the Client
	 * @return false on error, true if successful
	 */
	public boolean shutdown() 
	{
		clientLogger.info("Client: Attempting to shutdown client " + clientName);
		
		try 
		{
			actualClient.shutdown();
		}
		catch (ClientException e) 
		{
			clientLogger.error("Client: Cannot shutdown client " + clientName, e);
			return false;
		}
		
		clientLogger.info("Client: Shutdown client " + clientName);
		return true;
	}

	/**
	 * Connects this client to the network 
	 * @return false on error; true if successful
	 */
	public boolean connect() 
	{
		clientLogger.info("Client: Attempting to connect client " + clientName + " to network.");
		
		for(int i = 0 ; i < brokerURIs.size() ; i++)
		{
			String iTHURI = brokerURIs.get(i);
			try
			{
				connectedBrokers.add(actualClient.connect(iTHURI));
			}
			catch (ClientException e)
			{
				clientLogger.error("Client: Cannot connect client " + clientName + 
							" to broker " + iTHURI, e);
				disconnect();
				return false;
			}
		}
		
		clientLogger.info("Client: Added client " + clientName + " to network.");
		return true;
	}

	/**
	 * Disconnects the client from the network
	 */
	public void disconnect() 
	{
		clientLogger.info("Client: Disconnecting client " + clientName + " from network.");
		actualClient.disconnectAll();
	}

	public void startRun() 
	{
				
	}
	
	/**
	 * Starts the listening thread
	 */
	public void listen() 
	{
		actualClient.addMessageQueue(receivedQueue);
		listens = new Thread (new PADRESListen());
		listens.start();
	}
	
	private boolean exectueAction(ClientAction givenAction, String attributes)
	{
		boolean actionSuccessful = false;
		createDiaryEntry(givenAction, attributes); 
		Long startTime = System.nanoTime();
		actionSuccessful = handleAction(givenAction, attributes);
		Long endTime = System.nanoTime();
		
		if(actionSuccessful)
		{
			Long timeDiff = endTime - startTime;
			
			updateDiaryEntry(DiaryHeader.TimeStartedAction, startTime.toString(), givenAction.toString(), attributes);
			updateDiaryEntry(DiaryHeader.TimeBrokerAck, endTime.toString(), givenAction.toString(), attributes);
			updateDiaryEntry(DiaryHeader.AckDelay, timeDiff.toString(), givenAction.toString(), attributes);
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
					clientLogger.info(generalLog + "advertize " + attributes);
					actualClient.advertise(attributes, brokerURIs.get(0));
					break;
				}
				case V:
				{
					clientLogger.info(generalLog + "unadvertize " + attributes);
					actualClient.unAdvertiseAll(); // For now
					break;
				}
				case P:
				{
					clientLogger.info(generalLog + "publish " + attributes);
					actualClient.publish(attributes, brokerURIs.get(0));
					break;
				}
				case S:
				{
					clientLogger.info(generalLog + "subscribe to " + attributes);
					actualClient.subscribe(attributes, brokerURIs.get(0));
					break;
				}
				case U:
				{
					clientLogger.info(generalLog + "unsubscribe from " + attributes);
					actualClient.unsubscribeAll(); // For now
					break;
				}
				default:
					break;
			}
		}
		catch(ClientException e)
		{
			clientLogger.error("Client: Client Error", e);
			return false;
		}
		catch (ParseException e)
		{
			clientLogger.error("Client: Error Parsing", e);
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
	 * Given an ClientAction and attributes, update the diary entry with new data 
	 * @param newHeader - the new header
	 * @param newData - the new data to add
	 * @param attributes - the given attributes
	 * @param clientAction - the client action associated with this transaction
	 * @return false on error; true if successful
	 */
	private boolean updateDiaryEntry(DiaryHeader newHeader, String newData, String clientAction, String attributes) 
	{
		boolean successfulUpdate = false;
		
		for(int i = 0; i < diary.size() ; i++)
		{
			HashMap<DiaryHeader, String> iTHEntry = diary.get(i);
			if(iTHEntry.containsValue(clientAction) && iTHEntry.containsValue(attributes))
			{
				iTHEntry.put(newHeader, newData);
				successfulUpdate = true;
				break;
			}
		}
		
		return successfulUpdate;
	}
	
	/**
	 * Listens for incoming publications.
	 * Thread waits for notify() signals, which are sent when a new publication is received.
	 * Once it receives a new publication - it creates a new diary entry
	 */
	private class PADRESListen implements Runnable
	{
		public void run() 
		{	
			while(true)
			{
				try 
				{
					synchronized(receivedQueue)
					{
						receivedQueue.wait();
					}
				} 
				catch (InterruptedException e) 
				{
					clientLogger.error("Client: Listening Error", e);
				}
				
				// Pop publication from Message Queue
				Message newMsg = receivedQueue.removeFirst();
				if(newMsg != null)
				{
					//TODO: creating a new thread to handle the recieved message
					//		i.e. adding it to the diary
					System.out.println(newMsg.toString());
				}
			}
		}
	}

}
