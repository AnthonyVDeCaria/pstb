/**
 * @author padres-dev-4187
 * 
 * A wrapper around the variables that set a certain benchmark.
 *
 */
package pstb.startup.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import pstb.util.PSTBUtil;

public class BenchmarkConfig {
	private ArrayList<String> topologyFilesStrings;
	
	private HashMap<String, DistributedState> distributed;
	private boolean wantDistributed;
	private String distributedFileString;
	
	private ArrayList<NetworkProtocol> protocols;
	private ArrayList<Long> runLengths; // Milliseconds
	private Integer numRunsPerExperiment;
	
	private ArrayList<String> workloadFilesStrings;
	
	private Logger logger = null;
	private final String logHeader = "Properties: ";
	
	/**
	 * Constructor
	 * 
	 * The Integers are set to 0, as these are not valid numbers
	 * (You can't have 0 runs per experiment) 
	 */
	public BenchmarkConfig(Logger log)
	{
		logger = log;
		
		topologyFilesStrings = new ArrayList<String>();
		distributed = new HashMap<String, DistributedState>();
		wantDistributed = false;
		distributedFileString = new String();
		protocols = new ArrayList<NetworkProtocol>();
		runLengths = new ArrayList<Long>();
		numRunsPerExperiment = new Integer(0);
		workloadFilesStrings = new ArrayList<String>();
	}
	
	/**
	 * Sets all the benchmark variables' values after doing some quick parsing
	 * (seeing as some of these fields have to go from String to other types).
	 * If there is any errors, these fields will either not be set (the Integers),
	 * or be made empty (ArrayLists).
	 * The tested fields are numRunsPerExperiment, runLengths, idealMessageRates,
	 * protocols, and distributed.
	 * 
	 * @param givenProperty - the Properties object that contains the desired values.
	 * @return true if everything sets successfully; false otherwise
	 */
	public boolean setBenchmarkConfig(Properties givenProperty)
	{
		boolean everythingisProper = true;
		
		// TopologyFilesStrings
		String unsplitTFS = givenProperty.getProperty("startup.topologyFilesStrings");
		String[] splitTFS = unsplitTFS.split(PSTBUtil.ITEM_SEPARATOR);
		setTopologyFilesStrings(PSTBUtil.turnStringArrayIntoArrayListString(splitTFS));
		
		// Distributed
		String unsplitDis = givenProperty.getProperty("startup.distributed");
		HashMap<String, DistributedState> propDis = new HashMap<String, DistributedState>();
		if(unsplitDis != null)
		{
			String[] splitDis = unsplitDis.split(PSTBUtil.ITEM_SEPARATOR);
			int numGivenDis = splitDis.length;
			if(numGivenDis == splitTFS.length)
			{
				for(int i = 0 ; i < numGivenDis ; i++ )
				{
					try
					{
						DistributedState sDI = DistributedState.valueOf(splitDis[i]);
						
						if(sDI.equals(DistributedState.Yes) || sDI.equals(DistributedState.Both))
						{
							wantDistributed = true;
						}
						
						propDis.put(topologyFilesStrings.get(i), sDI);
					}
					catch(IllegalArgumentException e)
					{
						everythingisProper = false;
						propDis.clear();
						logger.error(logHeader + splitDis[i] + " is not a valid Distributed value: ", e);
					}
				}
			}
			else
			{
				everythingisProper = false;
			}
		}
		else
		{
			everythingisProper = false;
		}
		setDistributed(propDis);
		
		// DistributedFileString
		if(wantDistributed)
		{
			String propDFS = givenProperty.getProperty("startup.distributedFileString");
			if(propDFS == null || propDFS.equals("null"))
			{
				logger.error(logHeader + "No valid Distributed File given!");
				everythingisProper = false;
			}
			else
			{
				setDistributedFileString(propDFS);
			}
		}
		
		// runLengths
		String unsplitRL = givenProperty.getProperty("startup.runLengths");
		ArrayList<Long> propRL = new ArrayList<Long>();
		if(unsplitRL != null)
		{
			String[] splitRL = unsplitRL.split(PSTBUtil.ITEM_SEPARATOR);
			for(int i = 0 ; i < splitRL.length ; i++)
			{
				try
				{
					Long sRLI = Long.parseLong(splitRL[i]);
					propRL.add(sRLI);
				}
				catch(IllegalArgumentException e)
				{
					everythingisProper = false;
					propRL.clear();
					logger.error(logHeader + splitRL[i] + " is not a valid Integer: ", e);
					break;
				}
			}
		}
		else
		{
			everythingisProper = false;
		}
		setRunLengths(propRL);
		
		// Protocols
		String unsplitProtocols = givenProperty.getProperty("startup.protocols");
		ArrayList<NetworkProtocol> propProto = new ArrayList<NetworkProtocol>();
		if(unsplitProtocols != null)
		{
			String[] splitProtocols = unsplitProtocols.split(PSTBUtil.ITEM_SEPARATOR);
			int numGivenProtocols = splitProtocols.length;
			if(numGivenProtocols <= NetworkProtocol.values().length)
			{
				for(int i = 0 ; i < numGivenProtocols ; i++ )
				{
					try
					{
						NetworkProtocol sPI = NetworkProtocol.valueOf(splitProtocols[i]);
						propProto.add(sPI);
					}
					catch(IllegalArgumentException e)
					{
						everythingisProper = false;
						propProto.clear();
						logger.error(logHeader + splitProtocols[i] + " is not a valid Protocol: ", e);
						break;
					}
				}
			}
			else
			{
				everythingisProper = false;
			}
		}
		else
		{
			everythingisProper = false;
		}
		setProtocols(propProto);
		
		// numRunsPerExperiment
		String givenNRPE = givenProperty.getProperty("startup.numRunsPerExperiment");
		try
		{
			Integer intNRPE = Integer.parseInt(givenNRPE);
			setNumRunsPerExperiment(intNRPE);
		}
		catch(IllegalArgumentException e)
		{
			logger.error(logHeader + givenNRPE + " is not a valid Integer: ", e);
			everythingisProper = false;
		}
		
		// workloadFilesStrings
		String unsplitPWFS = givenProperty.getProperty("startup.workloadFilesStrings");
		ArrayList<String> propPWFS = new ArrayList<String>();
		if(unsplitPWFS != null)
		{
			String[] splitPWFS = unsplitPWFS.split(PSTBUtil.ITEM_SEPARATOR);
			propPWFS = PSTBUtil.turnStringArrayIntoArrayListString(splitPWFS);
		}
		setWorkloadFilesStrings(propPWFS);
		
		return everythingisProper;
	}
	
	/**
	 * NOTE: All the setter functions are private 
	 * as the only "setter" that should be accessed is setBenchmarkConfig
	 */
	
	/**
	 * Sets the topologyFilesStrings
	 * 
	 * @param tFS - the new topologyFilesStrings
	 */
	private void setTopologyFilesStrings(ArrayList<String> tFS)
	{
		topologyFilesStrings = tFS;
	}
	
	/**
	 * Sets the distributed array
	 * 
	 * @param dis - the new distributed
	 */
	private void setDistributed(HashMap<String, DistributedState> dis)
	{
		distributed = dis;
	}
	
	/**
	 * Sets the distributed array
	 * 
	 * @param dFS - the new distributed File String
	 */
	private void setDistributedFileString(String dFS) 
	{
		distributedFileString = dFS;
	}
	
	/**
	 * Sets the protocols
	 * 
	 * @param proto - the new protocols
	 */
	private void setProtocols(ArrayList<NetworkProtocol> proto)
	{
		protocols = proto;
	}
	
	/**
	 * Sets the runLength
	 * 
	 * @param proto - the new protocols
	 */
	private void setRunLengths(ArrayList<Long> rL)
	{
		runLengths = rL;
	}
	
	/**
	 * Sets the numRunsPerExperiment
	 * 
	 * @param nRPE - the new numRunsPerExperiment
	 */
	private void setNumRunsPerExperiment(Integer nRPE)
	{
		numRunsPerExperiment = nRPE;
	}
	
	/**
	 * Sets the workloadFilesStrings
	 * 
	 * @param nWFS - the new workloadFilesStrings
	 */
	private void setWorkloadFilesStrings(ArrayList<String> nWFS)
	{
		workloadFilesStrings = nWFS;
	}
	
	/**
	 * Gets the topologyFilesStrings
	 * 
	 * @return topologyFilesStrings - the paths to all the Topology Files
	 */
	public ArrayList<String> getTopologyFilesStrings()
	{
		return topologyFilesStrings;
	}
	
	/**
	 * Gets the distributed
	 * 
	 * @return distributed - the list of wither each topology is distributed or not
	 */
	public HashMap<String, DistributedState> getDistributed()
	{
		return distributed;
	}
	
	/**
	 * Gets the wantDistributed value
	 * 
	 * @return wantDistributed - NOTE: this value is false if setBenchmarkConfig isn't run
	 */
	public boolean distributedRequested()
	{
		return wantDistributed;
	}
	
	/**
	 * Gets the distributedFileString
	 * 
	 * @return the distributedFileString
	 */
	public String getDistributedFileString()
	{
		return distributedFileString;
	}
	
	/**
	 * Gets the protocols
	 * 
	 * @return protocols - the list of protocols to be use in different runs
	 */
	public ArrayList<NetworkProtocol> getProtocols()
	{
		return protocols;
	}
	
	/**
	 * Gets the runLength
	 * 
	 * @return  runLength - the list of minutes each experiment's run's will take
	 */
	public ArrayList<Long> getRunLengths()
	{
		return runLengths;
	}
	
	/**
	 * Gets the numRunsPerExperiment
	 * 
	 * @return numRunsPerExperiment - the number of runs an experiment has to complete
	 */
	public Integer getNumRunsPerExperiment()
	{
		return numRunsPerExperiment;
	}
	
	/**
	 * Gets the WorkloadFilesStrings
	 * 
	 * @return the WorkloadFilesStrings 
	 */
	public ArrayList<String> getWorkloadFilesStrings()
	{
		return workloadFilesStrings;
	}
	
	/**
	 * Prints all of the Benchmark Variables
	 */
	public void printAllFields()
	{
		logger.info(logHeader + "numRunsPerExperiment = " + numRunsPerExperiment + ".");
		logger.info(logHeader + "workloadFilesStrings = " + Arrays.toString(workloadFilesStrings.toArray()) + ".");
		logger.info(logHeader + "runLength = " + Arrays.toString(runLengths.toArray()) + ".");
		logger.info(logHeader + "protocols = " + Arrays.toString(protocols.toArray()) + ".");
		logger.info(logHeader + "topologyFilesStrings = " + Arrays.toString(topologyFilesStrings.toArray()) + ".");
		logger.info(logHeader + "distributed = " + distributed.toString() + ".");
		if(wantDistributed)
		{
			logger.info(logHeader + "Distributed systems requested.");
			logger.info(logHeader + "distributedFileString = " + distributedFileString + ".");
		}
		else
		{
			logger.info(logHeader + "Distributed systems not requested.");
		}
	}
	
	/**
	 * Checks if any of the fields are "null"
	 * I.e. Not been set
	 * 
	 * @return false if no field is "null", true if one is
	 */
	public boolean checkForNullFields()
	{
		boolean anyFieldNull = false;
		
		if(numRunsPerExperiment.equals(0))
		{
			logger.error(logHeader + "No Number of Experiment Runs was given!");
			anyFieldNull = true;
		}
		if(workloadFilesStrings.isEmpty())
		{
			logger.error(logHeader + "No Workload File String(s) were given!");
			anyFieldNull = true;
		}
		if(runLengths.isEmpty())
		{
			logger.error(logHeader + "No Run Length(s) were given!");
			anyFieldNull = true;
		}
		if(protocols.isEmpty())
		{
			logger.error(logHeader + "No Protocol(s) were given!");
			anyFieldNull = true;
		}
		if(topologyFilesStrings.isEmpty())
		{
			logger.error(logHeader + "No Topology File String(s) were given!");
			anyFieldNull = true;
		}
		if(distributed.isEmpty())
		{
			logger.error(logHeader + "No Distributed information was given!");
			anyFieldNull = true;
		}
		if(wantDistributed && distributedFileString.isEmpty())
		{
			logger.error(logHeader + " No Distributed File String was given!");
			anyFieldNull = true;
		}
		
		return anyFieldNull;
	}
}
