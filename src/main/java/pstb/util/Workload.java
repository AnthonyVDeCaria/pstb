/**
 * @author padres-dev-4187
 * 
 * Contains the Workload information for all clients
 * I.e. what advertisements exist, what subscribers, etc.
 * 
 * Note that the Publication workload is dependant on advertisers
 * That is why it's a HashMap - the idea being you give a certain Advertisement
 * and you'll get a bunch of Publications that go with that Advertisement
 */
package pstb.util;

import java.util.ArrayList;
import java.util.HashMap;

public class Workload {
	private ArrayList<PSAction> allAds;
	private ArrayList<PSAction> allSubs;
	private HashMap<PSAction, ArrayList<PSAction>> allPubs;
	
	/**
	 * Empty constructor
	 */
	public Workload()
	{
		allAds = new ArrayList<PSAction>();
		allSubs = new ArrayList<PSAction>();
		allPubs = new HashMap<PSAction, ArrayList<PSAction>>();
	}
	
	/**
	 * Updates the Subscriber Workload
	 * @param newSub - the new Subscription
	 */
	public void updateSubscriptionWorkload(PSAction newSub)
	{
		allSubs.add(newSub);
	}
	
	/**
	 * Updates the Advertiser Workload
	 * (and thus the Publisher Workload)
	 * @param newAd - the new Advertisement
	 */
	public void updateAdvertisementWorkload(PSAction newAd)
	{
		allAds.add(newAd);
		allPubs.put(newAd, new ArrayList<PSAction>());
	}
	
	/**
	 * Updates the publication workload for a given Advertisement
	 * @param givenAd - the Ad we're updating
	 * @param newPublication - the new Publication to insert
	 * @return true if we found the given Ad/inserted the new publication; false otherwise
	 */
	public boolean updatePublicationWorkload(PSAction givenAd, PSAction newPublication)
	{
		boolean foundProperAd = false;
		
		if(allPubs.containsKey(givenAd))
		{
			ArrayList<PSAction> temp = allPubs.get(givenAd);
			temp.add(newPublication);
			allPubs.put(givenAd, temp);
			foundProperAd = true;
		}
		
		return foundProperAd;
	}
	
	/**
	 * Gets the Advertiser Workload
	 * @return the Advertiser Workload
	 */
	public ArrayList<PSAction> getAdvertiserWorkload()
	{
		return allAds;
	}
	
	/**
	 * Gets the Subscription Workload
	 * @return the Subscription Workload
	 */
	public ArrayList<PSAction> getSubscriberWorkload()
	{
		return allSubs;
	}
	
	/**
	 * Gets the total Publication Workload
	 * @return the Publication Workload for all Ads
	 */
	public HashMap<PSAction, ArrayList<PSAction>> getAllPublicationWorkloads()
	{
		return allPubs;
	}
	
	/**
	 * Gets the Publication Workload for a given advertisement
	 * @param givenAd - the Advertisement that these Publications are tied to
	 * @return null if this Advertisement doesn't exist; the publications otherwise
	 */
	public ArrayList<PSAction> getPublicationWorkloadForAd(PSAction givenAd)
	{
		ArrayList<PSAction> pubsGivenAd = null;
		
		if(allPubs.containsKey(givenAd))
		{
			pubsGivenAd = allPubs.get(givenAd);
		}
		
		return pubsGivenAd;
	}
}
