package pstb.benchmark;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.utoronto.msrg.padres.client.BrokerState;
import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientException;
import pstb.util.ClientAction;
import pstb.util.NodeRole;
//import pstb.util.ValidProtocol;

public class PADRESClient{
	private static final Logger clientLogger = LogManager.getRootLogger();
	
	private Client actualClient;
	private ArrayList<BrokerState> connectedBrokerStates;
	
	private DateFormat dateFormat;
	private String clientName;
	private ArrayList<NodeRole> roles;
	private ArrayList<String> brokerURIs;
	//TODO: Publisher Workload
	//TODO: Subscriber Workload
	private ArrayList<HashMap<DiaryHeader, String>> diary;
	
	public PADRESClient()
	{
		dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		clientName = new String();
		roles = new ArrayList<NodeRole>();
		brokerURIs = new ArrayList<String>();
		diary = new ArrayList<HashMap<DiaryHeader, String>>();
	}

	/**
	 * Sets all the variables and creates a new Client
	 * @param givenName - the name of the client
	 * @param givenRoles - the roles of this client
	 * @param givenURIs - the BrokerURIs this client will connect to
	 * @return false if there's a failure; true otherwise
	 */
	public boolean initialize(String givenName, ArrayList<NodeRole> givenRoles, ArrayList<String> givenURIs) 
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
		roles = givenRoles;
		brokerURIs = givenURIs;
		clientLogger.info("Client:  Initialized client " + givenName);
		return true;
	}

	public boolean shutdown() 
	{
		return false;
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
				connectedBrokerStates.add(actualClient.connect(iTHURI));
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
		actualClient.disconnectAll();
	}

	public void startRun() 
	{

	}

	public boolean handleAction(ClientAction givenAction, String predicates) 
	{
		return false;
	}


	public void listen() 
	{
		
	}

	/**
	 * 
	 * @param givenAction - the action the client is accomplishing; be it Advertise, Publish, etc
	 * @param attributes - the Attributes associated with this action
	 */
	public void createDiaryEntry(ClientAction givenAction, String attributes) 
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
	 * @param ClientAction - the client action associated with this transaction
	 * @return false on error; true if successful
	 */
	public boolean updateDiaryEntry(DiaryHeader newHeader, String newData, String ClientAction, String attributes) 
	{
		boolean successfulUpdate = false;
		
		for(int i = 0; i < diary.size() ; i++)
		{
			HashMap<DiaryHeader, String> iTHEntry = diary.get(i);
			if(iTHEntry.containsValue(ClientAction) && iTHEntry.containsValue(attributes))
			{
				iTHEntry.put(newHeader, newData);
				successfulUpdate = true;
				break;
			}
		}
		
		return successfulUpdate;
	}

}
