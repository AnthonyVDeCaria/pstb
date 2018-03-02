package pstb.benchmark.object.client.padres;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.logging.log4j.ThreadContext;

import ca.utoronto.msrg.padres.client.BrokerState;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import pstb.analysis.diary.DiaryEntry;
import pstb.benchmark.object.client.PSClient;
import pstb.startup.workload.PSActionType;
import pstb.util.PSTBUtil;

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
public class PSClientPADRES extends PSClient
{
	// Constants
	private static final long serialVersionUID = 1L;
	private final String standardAttribute = "[class,eq,\"oneITS\"],[Date,isPresent,'some_date'],[ID,isPresent,0],[Name,isPresent,'some_name'],[Address,isPresent,'some_addr'],[Latitude,isPresent,0.0],[Longitude,isPresent,0.0],[LaneIndex,isPresent,0],[LoopOccupancy,isPresent,'some_occupancy'],[AvgSpeed,isPresent,0],[Vehicles/Interval,isPresent,0],[VdsDeviceID,isPresent,0],[RegionName,isPresent,'some_region']";
	private final String stdPubAttribute = "[class,\"oneITS\"],[Date,'2012-06-25 00:00:00'],[ID,200],[Name,'ds0040dsa Allen05'],[Address,'SB Allen Road - 401'],[Latitude,43.7276177739601],[Longitude,-79.4490468206761],[LaneIndex,2],[LoopOccupancy,'null'],[AvgSpeed,73],[Vehicles/Interval,1],[VdsDeviceID,1271],[RegionName,'Allen Road']";
	
	// PADRES Client Variables
	private PADRESClientExtension actualClient;
	private ArrayList<BrokerState> connectedBrokers;
	private ClientConfig cConfig;
	
	/**
	 * Empty Constructor
	 */
	public PSClientPADRES()
	{
		super();
		
		logHeader = "PClient: ";
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
		if(nodeName.isEmpty())
		{
			nodeLog.error(logHeader + "Attempted to initialize a client with no name");
			return false;
		}
		
		nodeLog.info(logHeader + "Attempting to initialize client " + nodeName);
		
		// Attempt to create a new config file
		try 
		{
			this.cConfig = new ClientConfig();
		} 
		catch (ClientException e) 
		{
			nodeLog.error(logHeader + "Error creating new Config for Client " + nodeName, e);
			return false;
		}
		// New Config file created
		
		this.cConfig.clientID = nodeName;
		
		if(connectAsWell)
		{
			// Check that there are brokerURIs to connect to
			if(brokersURIs.isEmpty())
			{
				nodeLog.error(logHeader + "Attempted to connect client " + nodeName + " that had no brokerURIs");
				return false;
			}
			// There are
			
			this.cConfig.connectBrokerList = (String[]) brokersURIs.toArray(new String[brokersURIs.size()]);
		}
		
		// Attempt to create the PADRES Client object
		try 
		{
			actualClient = new PADRESClientExtension(cConfig, this);
		}
		catch (ClientException e)
		{
			nodeLog.error(logHeader + "Cannot initialize new client " + nodeName, e);
			return false;
		}
		// Successful
		// If connecting was requested, that set would have also connected the client
		
		nodeLog.info(logHeader + "Initialized client " + nodeName);
		return true;
	}

	/**
	 * Attempts to shutdown the Client
	 * 
	 * @return false on error, true if successful
	 */
	public boolean shutdown() 
	{
		nodeLog.info(logHeader + "Attempting to shutdown client " + nodeName);
		
		try 
		{
			actualClient.shutdown();
		}
		catch (ClientException e) 
		{
			nodeLog.error(logHeader + "Cannot shutdown client " + nodeName, e);
			return false;
		}
		
		nodeLog.info(logHeader + "Client " + nodeName + " shutdown");
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
		nodeLog.info(logHeader + "Attempting to connect client " + nodeName + " to network.");
		
		for(int i = 0 ; i < brokersURIs.size() ; i++)
		{
			String iTHURI = brokersURIs.get(i);
			BrokerState brokerI = null;
			
			try
			{
				brokerI = actualClient.connect(iTHURI);
			}
			catch (ClientException e)
			{
				nodeLog.error(logHeader + "Cannot connect client " + nodeName + " to broker " + iTHURI + ": ", e);
				this.shutdown();
				return false;
			}
			
			connectedBrokers.add(brokerI);
		}
		
		nodeLog.info(logHeader + "Added client " + nodeName + " to network.");
		return true;
	}

	/**
	 * Disconnects the client from the network
	 */
	public void disconnect() 
	{
		nodeLog.info(logHeader + "Disconnecting client " + nodeName + " from network.");
		actualClient.disconnectAll();
	}
	
	/**
	 * Stores the given Message
	 * Assuming it's a Publication
	 * @param msg - the given Message
	 */
	public void storePublication(Message msg) 
	{
		ThreadContext.put("client", generateContext());
		
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
				diaryLock.lock();
				diary.addDiaryEntryToDiary(receivedMsg);
				receivedMessages++;
			}
			finally
			{
				diaryLock.unlock();
			}
			
			nodeLog.debug(logHeader + "new publication received " + pub.toString());
		}
	}
	
	@Override
	protected boolean advertise(String givenAttributes, DiaryEntry resultingEntry)
	{
		Message result = null;
		try 
		{
			result = actualClient.advertise(givenAttributes, brokersURIs.get(0));
		}
		catch(ClientException e)
		{
			nodeLog.error(logHeader + "PADRES client couldn't advertise " + givenAttributes + ": ", e);
			return false;
		}
		catch (ParseException e)
		{
			nodeLog.error(logHeader + "PADRES couldn't parse " + givenAttributes + " to advertise: ", e);
			return false;
		}
		
		if(result == null)
		{
			nodeLog.error(logHeader + "Couldn't advertise properly!");
			return false;
		}
		else
		{
			resultingEntry.addMessageID(result.getMessageID());
			return true;
		}
	}
	
	@Override
	protected boolean unadvertise(String givenAttributes, DiaryEntry resultingEntry)
	{
		DiaryEntry originalAd = diary.getDiaryEntryGivenActionTypeNAttributes(PSActionType.A, givenAttributes);
		if(originalAd == null)
		{
			nodeLog.error(logHeader + "Couldn't find original advertisement!");
			return false;
		}
		
		String originalAdID = originalAd.getMessageID();
		Message result = null;
		try
		{
			result = actualClient.unAdvertise(originalAdID);
		}
		catch(ClientException e)
		{
			nodeLog.error(logHeader + "PADRES client couldn't unadvertise " + givenAttributes + ": ", e);
			return false;
		}
		
		if(result == null)
		{
			nodeLog.error(logHeader + "Couldn't unadvertise properly!");
			return false;
		}
		else
		{
			resultingEntry.addMessageID(result.getMessageID());
			return true;
		}
	}
	
	@Override
	protected boolean subscribe(String givenAttributes, DiaryEntry resultingEntry)
	{
		Message result = null;
		try 
		{
			result = actualClient.subscribe(givenAttributes, brokersURIs.get(0));
		}
		catch(ClientException e)
		{
			nodeLog.error(logHeader + "PADRES client couldn't subscribe " + givenAttributes + ": ", e);
			return false;
		}
		catch (ParseException e)
		{
			nodeLog.error(logHeader + "PADRES couldn't parse " + givenAttributes + " to subscribe: ", e);
			return false;
		}
		
		if(result == null)
		{
			nodeLog.error(logHeader + "Couldn't subscribe properly!");
			return false;
		}
		else
		{
			resultingEntry.addMessageID(result.getMessageID());
			return true;
		}
	}
	
	@Override
	protected boolean unsubscribe(String givenAttributes, DiaryEntry resultingEntry)
	{
		DiaryEntry originalSub = diary.getDiaryEntryGivenActionTypeNAttributes(PSActionType.S, givenAttributes);
		if(originalSub == null)
		{
			nodeLog.error(logHeader + "Couldn't find original subscription!");
			return false;
		}
		
		String originalSubID = originalSub.getMessageID();
		Message result = null;
		try
		{
			result = actualClient.unSubscribe(originalSubID);
		}
		catch(ClientException e)
		{
			nodeLog.error(logHeader + "PADRES client couldn't unsubscribe " + givenAttributes + ": ", e);
			return false;
		}
		
		if(result == null)
		{
			nodeLog.error(logHeader + "Couldn't unsubscribe properly!");
			return false;
		}
		else
		{
			resultingEntry.addMessageID(result.getMessageID());
			return true;
		}
	}
	
	@Override
	protected boolean publish(String givenAttributes, DiaryEntry resultingEntry, Integer givenPayloadSize)
	{
		Message result = null;
		
		if(givenPayloadSize < 0)
		{
			nodeLog.error(logHeader + "Payload size is less than 0!");
			return false;
		}
		else if(givenPayloadSize == 0)
		{
			try 
			{
				result = actualClient.publish(givenAttributes, brokersURIs.get(0));
			} 
			catch (ClientException e) 
			{
				nodeLog.error(logHeader + "PADRES client couldn't publish " + givenAttributes + ": ", e);
				return false;
			}
		}
		else
		{
			Publication pubI = null;
			try 
			{
				pubI = MessageFactory.createPublicationFromString(givenAttributes);
			}
			catch (ParseException e)
			{
				nodeLog.error(logHeader + "couldn't convert " + givenAttributes + " into a publication: ", e);
				return false;
			}
			
			byte[] payload = new byte[givenPayloadSize];
			Arrays.fill( payload, (byte) 1 );
			pubI.setPayload(payload);
			
			try 
			{
				result = actualClient.publish(pubI, brokersURIs.get(0));
			} 
			catch (ClientException e) 
			{
				nodeLog.error(logHeader + "PADRES client couldn't publish " + givenAttributes + ": ", e);
				return false;
			}
		}
		
		if(result == null)
		{
			nodeLog.error(logHeader + "Couldn't publish properly!");
			return false;
		}
		else
		{
			resultingEntry.addMessageID(result.getMessageID());
			return true;
		}
	}

	@Override
	protected String generateThroughputAttributes(PSActionType givenPSAT, int messageNumber) {
		if(givenPSAT == null)
		{
			return null;
		}
		else if(givenPSAT.equals(PSActionType.P) || givenPSAT.equals(PSActionType.R))
		{
			Long currTime = System.currentTimeMillis();
			String currTimeString = PSTBUtil.DATE_FORMAT.format(currTime);
			return "[class,\"oneITS\"],[Name,'" + nodeName + "'],[Date,'" + currTimeString + "'],[Number," + messageNumber + "]";
		}
		else
		{
			return "[class,eq,\"oneITS\"],[Name,isPresent,'some_name'],[Date,isPresent,'some_date'],[Number,isPresent,0]";
		}
	}
}
