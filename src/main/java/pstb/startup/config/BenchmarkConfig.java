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

import pstb.creation.topology.PADRESTopology;
import pstb.creation.topology.SIENATopology;
import pstb.startup.config.SupportedEngines.PSEngine;
import pstb.util.PSTBUtil;

public class BenchmarkConfig {
	private final String enginesString = "startup.engines";
	private final String tfsString = "startup.topologyFilesStrings";
	private final String distributedString = "startup.distributed";
	private final String dfsString = "startup.distributedFileString";
	private final String protocolsString = "startup.protocols";
	private final String runLengthsString = "startup.runLengths";
	private final String nrpeString = "startup.numRunsPerExperiment";
	private final String wfsString = "startup.workloadFilesStrings";
	
	private ArrayList<PSEngine> engines;
	
	private ArrayList<String> topologyFilesStrings;
	
	private HashMap<String, DistributedState> distributed;
	private boolean wantDistributed;
	private String distributedFileString;
	
	private boolean wantPADRES;
	private boolean wantSIENA;
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
		
		engines = new ArrayList<PSEngine>();
		topologyFilesStrings = new ArrayList<String>();
		distributed = new HashMap<String, DistributedState>();
		wantDistributed = false;
		distributedFileString = new String();
		protocols = new ArrayList<NetworkProtocol>();
		wantPADRES = false;
		wantSIENA = false;
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
		ArrayList<PSEngine> requestedEngines = new ArrayList<PSEngine>();
		if(unsplitEngines != null)
		{
			String[] splitEngines = unsplitEngines.split(PSTBUtil.ITEM_SEPARATOR);
			int numRE = splitEngines.length;
			if(numRE <= PSEngine.values().length)
			{
				for(int i = 0 ; i < numRE ; i++ )
				{
					String stringEI = splitEngines[i];
					PSEngine eI = null;
					try
					{
						eI = PSEngine.valueOf(stringEI);
					}
					catch(IllegalArgumentException e)
					{
						everythingisProper = false;
						requestedEngines.clear();
						logger.error(logHeader + stringEI + " is not a Supported Engine: ", e);
						break;
					}
					if(eI.equals(PSEngine.PADRES))
					{
						wantPADRES = true;
					}
					else if(eI.equals(PSEngine.SIENA))
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
		
		// Protocols
		String unsplitProtocols = givenProperty.getProperty(protocolsString);
		ArrayList<NetworkProtocol> requestedProtocols = new ArrayList<NetworkProtocol>();
		if(unsplitProtocols != null)
		{
			String[] splitProtocols = unsplitProtocols.split(PSTBUtil.ITEM_SEPARATOR);
			int numProtocols = splitProtocols.length;
			if(numProtocols <= PADRESTopology.SUPPORTED_PROTOCOLS.size() + SIENATopology.SUPPORTED_PROTOCOLS.size())
			{
				int givenPADRESProtocols = 0;
				int givenSIENAProtocols = 0;
				
				for(int i = 0 ; i < numProtocols ; i++ )
				{
					String stringPI = splitProtocols[i];
					NetworkProtocol pI = NetworkProtocol.valueOf(stringPI);
					
					boolean validPADRES = wantPADRES && PADRESTopology.SUPPORTED_PROTOCOLS.contains(pI);
					boolean validSIENA = wantSIENA && SIENATopology.SUPPORTED_PROTOCOLS.contains(pI);
					
					if(validPADRES)
					{
						givenPADRESProtocols++;
					}
					if(validSIENA)
					{
						givenSIENAProtocols++;
					}
					
					if(validPADRES || validSIENA)
					{
						requestedProtocols.add(pI);
					}
					else
					{
						everythingisProper = false;
						requestedProtocols.clear();
						logger.error(logHeader + stringPI + " is not a valid Protocol!");
						break;
					}
				}
				
				if((wantPADRES && givenPADRESProtocols == 0) || (wantSIENA && givenSIENAProtocols == 0))
				{
					everythingisProper = false;
					requestedProtocols.clear();
					logger.error(logHeader + " not enough protocols were given for the requested engines!");
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
	private void setEngines(ArrayList<PSEngine> nE)
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
	public ArrayList<PSEngine> getEngines()
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
	 * Gets the protocols
	 * 
	 * @return protocols - the list of protocols to be used in different runs
	 */
	public ArrayList<NetworkProtocol> getProtocols()
	{
		return protocols;
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
		if((wantPADRES || wantSIENA) && protocols.isEmpty())
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
