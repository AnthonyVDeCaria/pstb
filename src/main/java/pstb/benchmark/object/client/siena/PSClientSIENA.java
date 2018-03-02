/**
 * 
 */
package pstb.benchmark.object.client.siena;

import java.io.IOException;

import pstb.analysis.diary.DiaryEntry;
import pstb.benchmark.object.client.PSClient;
import pstb.startup.config.NetworkProtocol;
import pstb.startup.workload.PSActionType;
import pstb.util.PSTBUtil;
import siena.Filter;
import siena.Notification;
import siena.Op;
import siena.SienaException;
import siena.ThinClient;
import siena.comm.InvalidSenderException;
import siena.comm.KAPacketReceiver;
import siena.comm.PacketReceiver;
import siena.comm.TCPPacketReceiver;
import siena.comm.UDPPacketReceiver;

/**
 * @author padres-dev-4187
 * 
 * This class handles the functions associated with a SIENA Client
 * @see PSClient
 * @see PSClientPADRES
 */
public class PSClientSIENA extends PSClient
{
	// Constants
	private static final long serialVersionUID = 1L;
	private final String standardAttribute = "class,=,\"oneITS\"|Date,any,'some_date'|ID,any,0|Name,any,'some_name'|Address,any,'some_addr'|Latitude,any,0.0|Longitude,any,0.0|LaneIndex,any,0|LoopOccupancy,any,'some_occupancy'|AvgSpeed,any,0|Vehicles/Interval,any,0|VdsDeviceID,any,0|RegionName,any,'some_region'";
	private final String stdPubAttribute = "class,\"oneITS\"|Date,'2012-06-25 00:00:00'|ID,200|Name,'ds0040dsa Allen05'|Address,'SB Allen Road - 401'|Latitude,43.7276177739601|Longitude,-79.4490468206761|LaneIndex,2|LoopOccupancy,'null'|AvgSpeed,73|Vehicles/Interval,1|VdsDeviceID,1271|RegionName,'Allen Road'";
	
	// SIENA Client Variables
	private ThinClient actualClient;
	private SIENAListener actualSub;
	
	/**
	 * Empty Constructor
	 */
	public PSClientSIENA()
	{
		super();
		actualClient = null;
		actualSub = null;
		
		logHeader = "SClient: ";
	}
	
	/**
	 * Sets up the PSClientSIENA.
	 * 
	 * @return true if the startup completes successfully; false if there are any errors
	 */
	public boolean setupClient()
	{
		if(!variableCheck())
		{
			nodeLog.error(logHeader + "Not all variables have been set!");
			return false;
		}
		
		String brokerURI = brokersURIs.get(0);
		try 
		{
			actualClient = new ThinClient(brokerURI, nodeName);
		} 
		catch (InvalidSenderException e) 
		{
			nodeLog.error("Couldn't create the ThinClient Object: ", e);
			return false;
		}
		nodeLog.info(logHeader + "ThinClient created.");
		
		nodeLog.debug(logHeader + "Attempting to create and attach a receiver to the new ThinClient...");
		PacketReceiver neededReceiver = generateReceiver();
		if(neededReceiver == null)
		{
			nodeLog.info(logHeader + "Couldn't create a receiver!");
			return false;
		}
		actualClient.setReceiver(neededReceiver);
		nodeLog.info(logHeader + "Receiver attached.");
		
		nodeLog.debug(logHeader + "Creating a subscriber listener...");
		String context = generateContext();
		actualSub = new SIENAListener(diary, diaryLock, context);
		nodeLog.info(logHeader + "Listener created.");
		
		return true;
	}
	
	/**
	 * Shuts down the PSClientSIENA
	 * 
	 * @return false if there is an error; true otherwise
	 */
	public boolean shutdownClient()
	{
		if(actualClient == null)
		{
			nodeLog.error(logHeader + "No SIENA client exists!");
			return false;
		}
		
		actualClient.shutdown();
		return true;
	}
	
	@Override
	protected boolean advertise(String givenAttributes, DiaryEntry resultingEntry)
	{
		nodeLog.warn("SIENA is bullshit... so... this isn't working rn...");
		
//		Filter adI = generateFilterFromAttributes(givenAttributes);
//		if(adI == null)
//		{
//			nodeLog.error(logHeader + "Couldn't create ad filter!");
//			return false;
//		}
//		
//		try 
//		{
//			actualClient.advertise(adI, nodeName);
//		} 
//		catch (Exception e) 
//		{
//			nodeLog.error(logHeader + "Couldn't advertise " + givenAttributes + ": ", e);
//			return false;
//		}
//		
//		nodeLog.debug(logHeader + "Advertise successful.");
		return true;
	}
	
	@Override
	protected boolean unadvertise(String givenAttributes, DiaryEntry resultingEntry)
	{
		Filter unAdI = generateFilterFromAttributes(givenAttributes);
		if(unAdI == null)
		{
			nodeLog.error(logHeader + "Couldn't create unad filter!");
			return false;
		}
		
		try
		{
			actualClient.unadvertise(unAdI, nodeName);
		}
		catch (Exception e) 
		{
			nodeLog.error(logHeader + "Couldn't unadvertise " + givenAttributes + ": ", e);
			return false;
		}
		
		nodeLog.debug(logHeader + "Unadvertise successful.");
		return true;
	}
	
	@Override
	protected boolean subscribe(String givenAttributes, DiaryEntry resultingEntry)
	{
		Filter subI = generateFilterFromAttributes(givenAttributes);
		if(subI == null)
		{
			nodeLog.error(logHeader + "Couldn't create sub filter!");
			return false;
		}
		
		try
		{
			actualClient.subscribe(subI, actualSub);
		}
		catch (SienaException e) 
		{
			nodeLog.error(logHeader + "Couldn't subscribe to " + givenAttributes + ": ", e);
			return false;
		}
		
		nodeLog.debug(logHeader + "Subscription successful.");
		return true;
	}
	
	@Override
	protected boolean unsubscribe(String givenAttributes, DiaryEntry resultingEntry)
	{		
		Filter unSubI = generateFilterFromAttributes(givenAttributes);
		if(unSubI == null)
		{
			nodeLog.error(logHeader + "Couldn't create unsub filter!");
			return false;
		}
		
		try
		{
			actualClient.unsubscribe(unSubI, actualSub);
		}
		catch (Exception e) 
		{
			nodeLog.error(logHeader + "Couldn't unsubscribe from " + givenAttributes + ": ", e);
			return false;
		}
		
		nodeLog.debug(logHeader + "Unsubscription successful.");
		return true;
	}
	
	@Override
	protected boolean publish(String givenAttributes, DiaryEntry resultingEntry, Integer givenPayloadSize)
	{
		Notification pubI = generateNotificationFromAttributes(givenAttributes);
		if(pubI == null)
		{
			nodeLog.error(logHeader + "Couldn't create notification!");
			return false;
		}
		
		try
		{
			actualClient.publish(pubI);
		}
		catch (SienaException e) 
		{
			nodeLog.error(logHeader + "Couldn't unsubscribe from " + givenAttributes + ": ", e);
			return false;
		}
		
		nodeLog.debug(logHeader + "Publication successful.");
		return true;
	}
	
	/**
	 * Creates a SIENA Filter from a given attribute String.
	 * 
	 * @see siena.Filter
	 * @param attributes - the given attributes
	 * @return the resulting Filter
	 */
	private Filter generateFilterFromAttributes(String attributes)
	{
		Filter retVal = new Filter();
		
		String[] brokenAttributes = attributes.split("\\|");
		for(int i = 0 ; i < brokenAttributes.length ; i++)
		{
			String segmentI = brokenAttributes[i];
			
			String[] segmentIComponets = segmentI.split(",");
			String key = segmentIComponets[0];
			String operator = segmentIComponets[1];
			String value = segmentIComponets[2];
			
			short opValue = Op.op(operator);
			
			retVal.addConstraint(key, opValue, value);
		}
		
		return retVal;
	}
	
	/**
	 * Creates a SIENA Notification from a given attribute String.
	 * 
	 * @see siena.Notification
	 * @param attributes - the given attributes
	 * @return the resulting Notification
	 */
	private Notification generateNotificationFromAttributes(String attributes)
	{
		Notification retVal = new Notification();
		
		String[] brokenAttributes = attributes.split("\\|");
		for(int i = 0 ; i < brokenAttributes.length ; i++)
		{
			String segmentI = brokenAttributes[i];
			
			String[] segmentIComponets = segmentI.split(",");
			String key = segmentIComponets[0];
			String value = segmentIComponets[1];
			
			Double doubleCheck = PSTBUtil.checkIfDouble(value, false, null);
			if(doubleCheck == null)
			{
				retVal.putAttribute(key, value);
			}
			else
			{
				retVal.putAttribute(key, doubleCheck);
			}
		}
		
		return retVal;
	}
	
	/**
	 * Creates a SIENA PacketReceiver depending on the given protocol
	 * 
	 * @return the generated PacketReceiver
	 */
	private PacketReceiver generateReceiver()
	{
		try
		{
			if(protocol.equals(NetworkProtocol.ka))
			{
				return new KAPacketReceiver();
			}
			else if(protocol.equals(NetworkProtocol.tcp))
			{
				return new TCPPacketReceiver(0);
			}
			else if(protocol.equals(NetworkProtocol.udp))
			{
				return new UDPPacketReceiver(0);
			}
			else
			{
				nodeLog.error(logHeader + "Improper protocol!");
				return null;
			}
		}
		catch(IOException e)
		{
			nodeLog.error(logHeader + "Couldn't generate a new receiver: ", e);
			return null;
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
			return stdPubAttribute;
		}
		else
		{
			return standardAttribute;
		}
	}
}
