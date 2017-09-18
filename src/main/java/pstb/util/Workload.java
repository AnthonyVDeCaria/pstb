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
	private HashMap<String, ArrayList<PSAction>> workloadP;
	
	public Workload()
	{
		workloadA = new ArrayList<PSAction>();
		workloadS = new ArrayList<PSAction>();
		workloadP = new HashMap<String, ArrayList<PSAction>>();
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
		workloadP.put(newAd.getAttributes(), new ArrayList<PSAction>());
	}
	
	public boolean updateWorkloadP(String attributes, PSAction newPublication)
	{
		boolean foundProperAd = false;
		
		if(workloadP.containsKey(attributes))
		{
			ArrayList<PSAction> temp = workloadP.get(attributes);
			temp.add(newPublication);
			workloadP.put(attributes, temp);
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
	
	public HashMap<String, ArrayList<PSAction>> getWorkloadP()
	{
		return workloadP;
	}
	
	public void printWorkload()
	{
		
	}
}
