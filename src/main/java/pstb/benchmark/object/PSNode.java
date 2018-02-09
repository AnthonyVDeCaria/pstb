/**
 * 
 */
package pstb.benchmark.object;

import org.apache.logging.log4j.Logger;

import pstb.analysis.diary.DistributedFlagValue;
import pstb.startup.config.NetworkProtocol;
import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 * 
 * The base class of the node Objects.
 */
public abstract class PSNode implements java.io.Serializable
{
	// Constants
	protected static final long serialVersionUID = 1L;
	
	// Variables needed by user to produce output
	protected String benchmarkStartTime;
	protected String topologyFileString;
	protected Boolean distributed;
	protected NetworkProtocol protocol;
	protected Long runLength;
	protected Integer runNumber;
	protected String nodeName;
	
	// Logger
	protected String logHeader;
	protected Logger nodeLog; 
	
	public PSNode()
	{
		benchmarkStartTime = new String();
		topologyFileString = new String();
		distributed = null;
		protocol = null;
		runLength = null;
		runNumber = null;
		nodeName = new String();
		
		logHeader = null;
		nodeLog = null;
	}
	
	/**
	 * Sets the time the benchmark started (that the diary will need)
	 * 
	 * @param givenBST - a String version of the time the Benchmark started
	 */
	public void setBenchmarkStartTime(String givenBST)
	{
		benchmarkStartTime = givenBST;
	}
	
	/**
	 * Sets a String of the current Topology File (that the diary will need)
	 * 
	 * @param givenTFS - the topologyFileString to set
	 */
	public void setTopologyFileString(String givenTFS) 
	{
		topologyFileString = givenTFS;
	}
	
	/**
	 * Sets a distributed boolean (that the diary will need)
	 * 
	 * @param givenDis - the Distributed value to set
	 */
	public void setDistributed(Boolean givenDis)
	{
		distributed = givenDis;
	}
	
	/**
	 * Sets a protocol (that the diary will need)
	 * 
	 * @param givenNP - the NetworkProtocol to set
	 */
	public void setNetworkProtocol(NetworkProtocol givenNP)
	{
		protocol = givenNP;
	}
	
	/**
	 * Sets the runLength value
	 * 
	 * @param givenRL - the rL value to be set
	 */
	public void setRunLength(Long givenRL)
	{
		runLength = givenRL;
	}
	
	/**
	 * Sets the run number (that the diary will need)
	 * 
	 * @param givenRN - the given run number
	 */
	public void setRunNumber(Integer givenRN)
	{
		runNumber = givenRN;
	}
	
	/**
	 * Sets the Client's name
	 * 
	 * @param givenName - the new name
	 */
	public void setNodeName(String givenName)
	{
		nodeName = givenName;
	}
	
	/**
	 * Get's this Client's name
	 * 
	 * @return this Client's name
	 */
	public String getNodeName()
	{
		return nodeName;
	}
	
	/**
	 * Get the runLength
	 * 
	 * @return the runLength
	 */
	public Long getRunLength()
	{
		return runLength;
	}
	
	/**
	 * Sees if all the context variables have been set
	 * 
	 * @return false on error; true otherwise
	 */
	protected boolean contextVariableCheck()
	{
		boolean everythingPresent = true;
		
		if(benchmarkStartTime.isEmpty())
		{
			nodeLog.error("No benchmark start time was given!");
			everythingPresent = false;
		}
		if(topologyFileString.isEmpty())
		{
			nodeLog.error("No topology file was given!");
			everythingPresent = false;
		}
		if(protocol == null)
		{
			nodeLog.error("No protocol was given!");
			everythingPresent = false;
		}
		if(distributed == null)
		{
			nodeLog.error("No distributed information was given!");
			everythingPresent = false;
		}
		if(runLength == null)
		{
			nodeLog.error("No run length was given!");
			everythingPresent = false;
		}
		if(runNumber == null)
		{
			nodeLog.error("No run number was given!");
			everythingPresent = false;
		}
		if(nodeName.isEmpty())
		{
			nodeLog.error("No client name was given!");
			everythingPresent = false;
		}
		
		return everythingPresent;
	}
	
	/**
	 * Generates the context this node is operating in
	 * 
	 * @return the generated context
	 */
	public String generateContext()
	{
		//Check that we have everything
		if(!contextVariableCheck())
		{
			nodeLog.error(logHeader + "Not all variables have been set!");
			return null;
		}
		// We do

		// Check that we can access the distributed
		DistributedFlagValue distributedFlag = null;
		if(distributed.booleanValue() == true)
		{
			distributedFlag = DistributedFlagValue.D;
		}
		else if(distributed.booleanValue() == false)
		{
			distributedFlag = DistributedFlagValue.L;
		}
		else
		{
			nodeLog.error(logHeader + "error with distributed");
			return null;
		}
		// We can - it's now in a flag enum
		// WHY a flag enum: to collapse the space to two values
		
		// Convert the nanosecond runLength into milliseconds
		// WHY: neatness / it's what the user gave us->why confuse them?
		Long milliRunLength = (long) (runLength / PSTBUtil.MILLISEC_TO_NANOSEC.doubleValue());
		
		return benchmarkStartTime + PSTBUtil.DIARY_SEPARATOR
				+ topologyFileString + PSTBUtil.DIARY_SEPARATOR
				+ distributedFlag.toString() + PSTBUtil.DIARY_SEPARATOR
				+ protocol.toString() + PSTBUtil.DIARY_SEPARATOR
				+ milliRunLength.toString() + PSTBUtil.DIARY_SEPARATOR
				+ runNumber.toString() + PSTBUtil.DIARY_SEPARATOR
				+ nodeName;
	}
}
