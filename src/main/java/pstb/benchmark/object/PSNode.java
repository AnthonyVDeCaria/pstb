package pstb.benchmark.object;

import org.apache.logging.log4j.Logger;

import pstb.analysis.diary.DistributedFlagValue;
import pstb.startup.config.BenchmarkMode;
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
	private static final int CONTEXT_COMPONENTS = 7;
	
	// Variables needed by user to produce output
	protected String benchmarkStartTime;
	protected String topologyFileString;
	protected Boolean distributed;
	protected NetworkProtocol protocol;
	protected BenchmarkMode mode;
	protected Long runLength;
	protected Integer runNumber;
//	protected Long initialDelay;
//	protected Integer initialPayload;
	protected Long periodLength;
	protected String nodeName;
	
	// Logger
	protected String logHeader;
	protected Logger nodeLog; 
	
	public PSNode()
	{
		benchmarkStartTime = null;
		topologyFileString = null;
		distributed = null;
		protocol = null;
		runLength = null;
		runNumber = null;
//		initialDelay = null;
//		initialPayload = null;
		periodLength = null;
		nodeName = null;
		
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
	 * Sets the mode
	 * 
	 * @param givenBM - the BenchmarkMode to set
	 */
	public void setMode(BenchmarkMode givenBM)
	{
		mode = givenBM;
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
	 * Sets the initialDelay value
	 * 
	 * @param givenID - the initialDelay value to be set
	 */
//	public void setInitialDelay(Long givenID)
//	{
//		initialDelay = givenID;
//	}
	
	/**
	 * Sets the initialPayload
	 * 
	 * @param givenIP - the given initialPayload
	 */
//	public void setInitialPayload(Integer givenIP)
//	{
//		initialPayload = givenIP;
//	}
	
	/**
	 * Sets the periodLength
	 * 
	 * @param givenPL - the given periodLength
	 */
	public void setPeriodLength(Long givenPL)
	{
		periodLength = givenPL;
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
		
		if(benchmarkStartTime == null)
		{
			nodeLog.error("No benchmark start time was given!");
			everythingPresent = false;
		}
		if(topologyFileString == null)
		{
			nodeLog.error("No topology file was given!");
			everythingPresent = false;
		}
		if(distributed == null)
		{
			nodeLog.error("No distributed information was given!");
			everythingPresent = false;
		}
		if(protocol == null)
		{
			nodeLog.error("No protocol was given!");
			everythingPresent = false;
		}
		if(mode == null)
		{
			nodeLog.error("No mode was given!");
			everythingPresent = false;
		}
		else if(mode.equals(BenchmarkMode.Normal))
		{
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
		}
		else if(mode.equals(BenchmarkMode.Throughput))
		{
			if(periodLength == null)
			{
				nodeLog.error("No periodLength was given!");
				everythingPresent = false;
			}
//			if(initialDelay == null)
//			{
//				nodeLog.error("No startingDelay was given!");
//				everythingPresent = false;
//			}
//			if(initialPayload == null)
//			{
//				nodeLog.error("No startingPayload was given!");
//				everythingPresent = false;
//			}
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
		
		// Convert the distributed value into a flag
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
		
		String[] contextArr = new String[CONTEXT_COMPONENTS];
		contextArr[0] = benchmarkStartTime;
		contextArr[1] = topologyFileString;
		contextArr[2] = distributedFlag.toString();
		contextArr[3] = protocol.toString();
		contextArr[6] = nodeName;
		
		if(mode.equals(BenchmarkMode.Normal))
		{
			// Convert the nanosecond runLength into milliseconds
			// WHY: neatness / it's what the user gave us->why confuse them?
			Long milliRunLength = (long) (runLength / PSTBUtil.MILLISEC_TO_NANOSEC.doubleValue());
			
			contextArr[4] = milliRunLength.toString();
			contextArr[5] = runNumber.toString();
		}
		else if(mode.equals(BenchmarkMode.Throughput))
		{
//			contextArr[4] = initialDelay.toString();
//			contextArr[5] = initialPayload.toString();
			
			// Convert the nanosecond runLength into milliseconds
			// WHY: neatness / it's what the user gave us->why confuse them?
			Long convertedPL = (long) (periodLength / PSTBUtil.MILLISEC_TO_NANOSEC.doubleValue());
			
			contextArr[4] = convertedPL.toString();
			contextArr[5] = "Throughput";
		}
		else
		{
			return null;
		}
		
		String retVal = String.join(PSTBUtil.DIARY_SEPARATOR, contextArr);
		return retVal;
	}
}
