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

import pstb.startup.config.SupportedEngines.SupportedEngine;
import pstb.util.PSTBUtil;

public class BenchmarkConfig {
	private final String enginesString = "startup.engines";
	private final String tfsString = "startup.topologyFilesStrings";
	private final String distributedString = "startup.distributed";
	private final String dfsString = "startup.distributedFileString";
	private final String pProtocolsString = "startup.pProtocols";
	private final String sProtocolsString = "startup.sProtocols";
	private final String runLengthsString = "startup.runLengths";
	private final String nrpeString = "startup.numRunsPerExperiment";
	private final String wfsString = "startup.workloadFilesStrings";
	
	private ArrayList<SupportedEngine> engines;
	
	private ArrayList<String> topologyFilesStrings;
	
	private HashMap<String, DistributedState> distributed;
	private boolean wantDistributed;
	private String distributedFileString;
	
	private boolean wantPADRES;
	private ArrayList<PADRESNetworkProtocol> pProtocols;
	private boolean wantSIENA;
	private ArrayList<SIENANetworkProtocol> sProtocols;
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
		wantPADRES = false;
		pProtocols = new ArrayList<PADRESNetworkProtocol>();
		wantSIENA = false;
		sProtocols = new ArrayList<SIENANetworkProtocol>();
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
		String unsplitEngines = givenProperty.getProperty(enginesString);
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
					SupportedEngine eI = null;
					try
					{
						eI = SupportedEngine.valueOf(stringEI);
					}
					catch(IllegalArgumentException e)
					{
						everythingisProper = false;
						requestedEngines.clear();
						logger.error(logHeader + stringEI + " is not a Supported Engine: ", e);
						break;
					}
					if(eI.equals(SupportedEngine.PADRES))
					{
						wantPADRES = true;
					}
					else if(eI.equals(SupportedEngine.SIENA))
					{
						wantSIENA = true;
					}
					requestedEngines.add(eI);
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
		String unsplitTFS = givenProperty.getProperty(tfsString);
		String[] splitTFS = unsplitTFS.split(PSTBUtil.ITEM_SEPARATOR);
		setTopologyFilesStrings(PSTBUtil.turnStringArrayIntoArrayListString(splitTFS));
		
		// Distributed
		String unsplitDistributedStates = givenProperty.getProperty(distributedString);
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
			String requestedDFS = givenProperty.getProperty(dfsString);
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
		
		// PADRES Protocols
		if(wantPADRES)
		{
			String unsplitPProtocols = givenProperty.getProperty(pProtocolsString);
			ArrayList<PADRESNetworkProtocol> requestedPProtocols = new ArrayList<PADRESNetworkProtocol>();
			if(unsplitPProtocols != null)
			{
				String[] splitPProtocols = unsplitPProtocols.split(PSTBUtil.ITEM_SEPARATOR);
				int numPProtocols = splitPProtocols.length;
				if(numPProtocols <= PADRESNetworkProtocol.values().length)
				{
					for(int i = 0 ; i < numPProtocols ; i++ )
					{
						String stringPPI = splitPProtocols[i];
						try
						{
							PADRESNetworkProtocol ppI = PADRESNetworkProtocol.valueOf(stringPPI);
							requestedPProtocols.add(ppI);
						}
						catch(IllegalArgumentException e)
						{
							everythingisProper = false;
							requestedPProtocols.clear();
							logger.error(logHeader + stringPPI + " is not a valid Protocol: ", e);
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
			setPProtocols(requestedPProtocols);
		}
		
		
		// SIENA Protocols
		if(wantSIENA)
		{
			String unsplitSProtocols = givenProperty.getProperty(sProtocolsString);
			ArrayList<SIENANetworkProtocol> requestedSProtocols = new ArrayList<SIENANetworkProtocol>();
			if(unsplitSProtocols != null)
			{
				String[] splitSProtocols = unsplitSProtocols.split(PSTBUtil.ITEM_SEPARATOR);
				int numSProtocols = splitSProtocols.length;
				if(numSProtocols <= SIENANetworkProtocol.values().length)
				{
					for(int i = 0 ; i < numSProtocols ; i++ )
					{
						String stringSPI = splitSProtocols[i];
						try
						{
							SIENANetworkProtocol spI = SIENANetworkProtocol.valueOf(stringSPI);
							requestedSProtocols.add(spI);
						}
						catch(IllegalArgumentException e)
						{
							everythingisProper = false;
							requestedSProtocols.clear();
							logger.error(logHeader + stringSPI + " is not a valid Protocol: ", e);
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
			setSProtocols(requestedSProtocols);
		}
		
		// runLengths
		String unsplitRL = givenProperty.getProperty(runLengthsString);
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
		
		// numRunsPerExperiment
		String givenNRPE = givenProperty.getProperty(nrpeString);
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
		String unsplitWFS = givenProperty.getProperty(wfsString);
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
	 * Sets the PADRES protocols
	 * 
	 * @param nPProto - the new PADRES protocols
	 */
	private void setPProtocols(ArrayList<PADRESNetworkProtocol> nPProto)
	{
		pProtocols = nPProto;
	}
	
	/**
	 * Sets the SIENA protocols
	 * 
	 * @param nSProto - the new SIENA protocols
	 */
	private void setSProtocols(ArrayList<SIENANetworkProtocol> nSProto)
	{
		sProtocols = nSProto;
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
	 * Gets the wantPADRES value
	 * 
	 * @return wantPADRES - NOTE: this value is false if setBenchmarkConfig isn't run
	 */
	public boolean padresRequested() 
	{
		return wantPADRES;
	}
	
	/**
	 * Gets the PADRES protocols
	 * 
	 * @return protocols - the list of PADRES protocols to be use in different runs
	 */
	public ArrayList<PADRESNetworkProtocol> getPProtocols()
	{
		return pProtocols;
	}
	
	/**
	 * Gets the wantSIENA value
	 * 
	 * @return wantSIENA - NOTE: this value is false if setBenchmarkConfig isn't run
	 */
	public boolean sienaRequested() 
	{
		return wantSIENA;
	}
	
	/**
	 * Gets the SIENA protocols
	 * 
	 * @return protocols - the list of SIENA protocols to be use in different runs
	 */
	public ArrayList<SIENANetworkProtocol> getSProtocols()
	{
		return sProtocols;
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
	 * Prints all of the Benchmark variables
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
		logger.info(logHeader + "protocols = " + Arrays.toString(pProtocols.toArray()) + ".");
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
		if(wantPADRES && pProtocols.isEmpty())
		{
			logger.error(logHeader + "No Protocol(s) were given!");
			anyFieldNull = true;
		}
		if(wantSIENA && sProtocols.isEmpty())
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
