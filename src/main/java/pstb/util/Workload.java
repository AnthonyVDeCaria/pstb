/**
 * @author padres-dev-4187
 *
 */
package pstb.util;

import java.util.ArrayList;
import java.util.HashMap;

public class Workload {
	private ArrayList<PSAction> workloadA;
	private ArrayList<PSAction> workloadS;
	private HashMap<PSAction, ArrayList<PSAction>> workloadP;
	
	/**
	 * Empty constructor
	 */
	public Workload()
	{
		workloadA = new ArrayList<PSAction>();
		workloadS = new ArrayList<PSAction>();
		workloadP = new HashMap<PSAction, ArrayList<PSAction>>();
	}
	
	public void updateWorkloadS(PSAction newSub)
	{
		workloadS.add(newSub);
	}
	
	/**
	 * Updates the advertiser
	 * @param givenAction - the action being updated
	 */
	public void updateWorkloadA(PSAction newAd)
	{
		workloadA.add(newAd);
		workloadP.put(newAd, new ArrayList<PSAction>());
	}
	
	public boolean updateWorkloadP(PSAction givenAd, PSAction newPublication)
	{
		boolean foundProperAd = false;
		
		if(workloadP.containsKey(givenAd))
		{
			ArrayList<PSAction> temp = workloadP.get(givenAd);
			temp.add(newPublication);
			workloadP.put(givenAd, temp);
			foundProperAd = true;
		}
		
		return foundProperAd;
	}
	
	public ArrayList<PSAction> getWorkloadA()
	{
		return workloadA;
	}
	
	public ArrayList<PSAction> getWorkloadS()
	{
		return workloadS;
	}
	
	public HashMap<PSAction, ArrayList<PSAction>> getWorkloadP()
	{
		return workloadP;
	}
	
	public void printWorkload()
	{
		
	}
}
