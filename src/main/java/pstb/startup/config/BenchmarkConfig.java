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
	private ArrayList<SupportedEngine> engines;
	
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
		
		engines = new ArrayList<SupportedEngine>();
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
		
		// Engines
		String unsplitEngines = givenProperty.getProperty("startup.engines");
		ArrayList<SupportedEngine> requestedEngines = new ArrayList<SupportedEngine>();
		if(unsplitEngines != null)
		{
			String[] splitEngines = unsplitEngines.split(PSTBUtil.ITEM_SEPARATOR);
			int numRE = splitEngines.length;
			if(numRE <= SupportedEngine.values().length)
			{
				for(int i = 0 ; i < numRE ; i++ )
				{
					String stringEI = splitEngines[i];
					try
					{
						SupportedEngine eI = SupportedEngine.valueOf(stringEI);
						requestedEngines.add(eI);
					}
					catch(IllegalArgumentException e)
					{
						everythingisProper = false;
						requestedEngines.clear();
						logger.error(logHeader + stringEI + " is not a Supported Engine: ", e);
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
		setEngines(requestedEngines);
		
		// TopologyFilesStrings
		String unsplitTFS = givenProperty.getProperty("startup.topologyFilesStrings");
		String[] splitTFS = unsplitTFS.split(PSTBUtil.ITEM_SEPARATOR);
		setTopologyFilesStrings(PSTBUtil.turnStringArrayIntoArrayListString(splitTFS));
		
		// Distributed
		String unsplitDistributedStates = givenProperty.getProperty("startup.distributed");
		HashMap<String, DistributedState> requestedDS = new HashMap<String, DistributedState>();
		if(unsplitDistributedStates != null)
		{
			String[] splitDS = unsplitDistributedStates.split(PSTBUtil.ITEM_SEPARATOR);
			int numRequestedDS = splitDS.length;
			if(numRequestedDS == splitTFS.length)
			{
				for(int i = 0 ; i < numRequestedDS ; i++ )
				{
					String stringDSI = splitDS[i];
					try
					{
						DistributedState dsI = DistributedState.valueOf(stringDSI);
						
						if(dsI.equals(DistributedState.Yes) || dsI.equals(DistributedState.Both))
						{
							wantDistributed = true;
						}
						
						requestedDS.put(topologyFilesStrings.get(i), dsI);
					}
					catch(IllegalArgumentException e)
					{
						everythingisProper = false;
						requestedDS.clear();
						logger.error(logHeader + stringDSI + " is not a valid Distributed value: ", e);
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
		setDistributed(requestedDS);
		
		// DistributedFileString
		if(wantDistributed)
		{
			String requestedDFS = givenProperty.getProperty("startup.distributedFileString");
			if(requestedDFS == null || requestedDFS.equals("null"))
			{
				logger.error(logHeader + "No valid Distributed File given!");
				everythingisProper = false;
			}
			else
			{
				setDistributedFileString(requestedDFS);
			}
		}
		
		// runLengths
		String unsplitRL = givenProperty.getProperty("startup.runLengths");
		ArrayList<Long> requestedRL = new ArrayList<Long>();
		if(unsplitRL != null)
		{
			String[] splitRL = unsplitRL.split(PSTBUtil.ITEM_SEPARATOR);
			for(int i = 0 ; i < splitRL.length ; i++)
			{
				String stringRLI = splitRL[i];
				try
				{
					Long rlI = Long.parseLong(stringRLI);
					requestedRL.add(rlI);
				}
				catch(IllegalArgumentException e)
				{
					everythingisProper = false;
					requestedRL.clear();
					logger.error(logHeader + stringRLI + " is not a valid Integer: ", e);
					break;
				}
			}
		}
		else
		{
			everythingisProper = false;
		}
		setRunLengths(requestedRL);
		
		// Protocols
		String unsplitProtocols = givenProperty.getProperty("startup.protocols");
		ArrayList<NetworkProtocol> requestedProtocols = new ArrayList<NetworkProtocol>();
		if(unsplitProtocols != null)
		{
			String[] splitProtocols = unsplitProtocols.split(PSTBUtil.ITEM_SEPARATOR);
			int numGivenProtocols = splitProtocols.length;
			if(numGivenProtocols <= NetworkProtocol.values().length)
			{
				for(int i = 0 ; i < numGivenProtocols ; i++ )
				{
					String stringPI = splitProtocols[i];
					try
					{
						NetworkProtocol pI = NetworkProtocol.valueOf(stringPI);
						requestedProtocols.add(pI);
					}
					catch(IllegalArgumentException e)
					{
						everythingisProper = false;
						requestedProtocols.clear();
						logger.error(logHeader + stringPI + " is not a valid Protocol: ", e);
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
		setProtocols(requestedProtocols);
		
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
		String unsplitWFS = givenProperty.getProperty("startup.workloadFilesStrings");
		ArrayList<String> requestedPWFS = new ArrayList<String>();
		if(unsplitWFS != null)
		{
			String[] splitPWFS = unsplitWFS.split(PSTBUtil.ITEM_SEPARATOR);
			requestedPWFS = PSTBUtil.turnStringArrayIntoArrayListString(splitPWFS);
		}
		else
		{
			everythingisProper = false;
		}
		setWorkloadFilesStrings(requestedPWFS);
		
		return everythingisProper;
	}
	
	/**
	 * NOTE: All the setter functions are private 
	 * as the only "setter" that should be accessed is setBenchmarkConfig
	 */
	
	/**
	 * Sets the engines
	 * 
	 * @param nE - the new engines
	 */
	private void setEngines(ArrayList<SupportedEngine> nE)
	{
		engines = nE;
	}
	
	
	/**
	 * Sets the topologyFilesStrings
	 * 
	 * @param nTFS - the new topologyFilesStrings
	 */
	private void setTopologyFilesStrings(ArrayList<String> nTFS)
	{
		topologyFilesStrings = nTFS;
	}
	
	/**
	 * Sets the distributed array
	 * 
	 * @param nDis - the new distributed
	 */
	private void setDistributed(HashMap<String, DistributedState> nDis)
	{
		distributed = nDis;
	}
	
	/**
	 * Sets the distributed array
	 * 
	 * @param nDFS - the new distributed File String
	 */
	private void setDistributedFileString(String nDFS) 
	{
		distributedFileString = nDFS;
	}
	
	/**
	 * Sets the protocols
	 * 
	 * @param nProto - the new protocols
	 */
	private void setProtocols(ArrayList<NetworkProtocol> nProto)
	{
		protocols = nProto;
	}
	
	/**
	 * Sets the runLength
	 * 
	 * @param proto - the new protocols
	 */
	private void setRunLengths(ArrayList<Long> nRL)
	{
		runLengths = nRL;
	}
	
	/**
	 * Sets the numRunsPerExperiment
	 * 
	 * @param nNRPE - the new numRunsPerExperiment
	 */
	private void setNumRunsPerExperiment(Integer nNRPE)
	{
		numRunsPerExperiment = nNRPE;
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
	 * Gets the engines
	 * 
	 * @return engines - the list of engines to be used
	 */
	public ArrayList<SupportedEngine> getEngines()
	{
		return engines;
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
		logger.info(logHeader + "engines = " + Arrays.toString(engines.toArray()) + ".");
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
		logger.info(logHeader + "protocols = " + Arrays.toString(protocols.toArray()) + ".");
		logger.info(logHeader + "runLength = " + Arrays.toString(runLengths.toArray()) + ".");
		logger.info(logHeader + "numRunsPerExperiment = " + numRunsPerExperiment + ".");
		logger.info(logHeader + "workloadFilesStrings = " + Arrays.toString(workloadFilesStrings.toArray()) + ".");
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
		
		if(engines.isEmpty())
		{
			logger.error(logHeader + "No Engine(s) were given!");
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
		if(protocols.isEmpty())
		{
			logger.error(logHeader + "No Protocol(s) were given!");
			anyFieldNull = true;
		}
		if(runLengths.isEmpty())
		{
			logger.error(logHeader + "No Run Length(s) were given!");
			anyFieldNull = true;
		}
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
		
		return anyFieldNull;
	}
}
