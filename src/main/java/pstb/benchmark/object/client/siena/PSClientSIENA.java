/**
 * 
 */
package pstb.benchmark.object.client.siena;

import pstb.analysis.diary.DiaryEntry;
import pstb.benchmark.object.client.PSClient;
import pstb.startup.workload.PSActionType;
import siena.Filter;
import siena.Notification;
import siena.Op;
import siena.SienaException;
import siena.ThinClient;
import siena.comm.InvalidSenderException;

/**
 * @author padres-dev-4187
 *
 */
public class PSClientSIENA extends PSClient
{
	// Constants
	private static final long serialVersionUID = 1L;
	
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
		
		String context = generateContext();
		actualSub = new SIENAListener(diary, diaryLock, context);
		
		return true;
	}
	
	public boolean startClient()
	{
		if(actualClient == null)
		{
			nodeLog.error(logHeader + "No SIENA client exists!");
			return false;
		}
		
		actualClient.run();
		return true;
	}
	
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
		Filter adI = generateFilterFromAttributes(givenAttributes);
		if(adI == null)
		{
			nodeLog.error(logHeader + "Couldn't create ad filter!");
			return false;
		}
		
		try 
		{
			actualClient.advertise(adI, nodeName);
		} 
		catch (Exception e) 
		{
			nodeLog.error(logHeader + "Couldn't advertise " + givenAttributes + ": ", e);
			return false;
		}
		
		nodeLog.debug(logHeader + "Advertise successful.");
		return true;
	}
	
	@Override
	protected boolean unadvertise(String givenAttributes, DiaryEntry resultingEntry)
	{
		DiaryEntry associatedAd = diary.getDiaryEntryGivenActionTypeNAttributes(PSActionType.A, givenAttributes);
		String associatedAdsAttributes = associatedAd.getAttributes();
		Filter unAdI = generateFilterFromAttributes(associatedAdsAttributes);
		if(unAdI == null)
		{
			nodeLog.error(logHeader + "Couldn't create unad filter!");
			return false;
		}
		
		try
		{
			actualClient.unadvertise(unAdI, nodeName);
		}
		catch (SienaException e) 
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
		DiaryEntry associatedAd = diary.getDiaryEntryGivenActionTypeNAttributes(PSActionType.A, givenAttributes);
		String associatedAdsAttributes = associatedAd.getAttributes();
		Filter unSubI = generateFilterFromAttributes(associatedAdsAttributes);
		if(unSubI == null)
		{
			nodeLog.error(logHeader + "Couldn't create unsub filter!");
			return false;
		}
		
		try
		{
			actualClient.unsubscribe(unSubI, actualSub);
		}
		catch (SienaException e) 
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
			
			if(opValue == Op.ANY)
			{
				value = null;
			}
			
			retVal.addConstraint(key, opValue, value);
		}
		
		return retVal;
	}
	
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
			
			retVal.putAttribute(key, value);
		}
		
		return retVal;
	}
}
