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
	private final String modesString = "startup.modes";
	private final String tfsString = "startup.topologyFilesStrings";
	private final String distributedString = "startup.distributed";
	private final String dfsString = "startup.distributedFileString";
	private final String protocolsString = "startup.protocols";
	private final String runLengthsString = "startup.runLengths";
	private final String nrpeString = "startup.numRunsPerExperiment";
//	private final String initialDelayString = "startup.initialDelay";
//	private final String initialPayloadString = "startup.initialPayload";
	private final String periodLengthString = "startup.periodLength";
	private final String wfsString = "startup.workloadFilesStrings";
	
	private ArrayList<PSEngine> engines;
	private ArrayList<BenchmarkMode> modes;
	
	private ArrayList<String> topologyFilesStrings;
	
	private HashMap<String, DistributedState> distributed;
	private boolean wantDistributed;
	private String distributedFileString;
	
	private ArrayList<NetworkProtocol> protocols;
	private boolean wantPADRES;
	private boolean wantSIENA;
	
	private boolean wantNormal;
	private ArrayList<Long> runLengths; // Milliseconds
	private Integer numRunsPerExperiment;
	
	private boolean wantThroughput;
	private Long periodLength;
//	private Long initialDelay;
//	private Integer initialPayload;
	
	private ArrayList<String> workloadFilesStrings;
	
	private Logger logger = null;
	private final String logHeader = "Benchmark Config: ";
	
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
		modes = new ArrayList<BenchmarkMode>();
		
		topologyFilesStrings = new ArrayList<String>();
		
		distributed = new HashMap<String, DistributedState>();
		wantDistributed = false;
		distributedFileString = null;
		
		protocols = new ArrayList<NetworkProtocol>();
		wantPADRES = false;
		wantSIENA = false;
		
		wantNormal = false;
		runLengths = new ArrayList<Long>();
		numRunsPerExperiment = null;
		
		wantThroughput = false;
		periodLength = null;
//		initialDelay = null;
//		initialPayload = null;
		
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
						engines.clear();
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
					engines.add(eI);
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
		
		String unsplitModes = givenProperty.getProperty(modesString);
		if(unsplitModes != null)
		{
			String[] splitModes = unsplitModes.split(PSTBUtil.ITEM_SEPARATOR);
			int numModes = splitModes.length;
			if(numModes <= PSEngine.values().length)
			{
				for(int i = 0 ; i < numModes ; i++ )
				{
					String stringMI = splitModes[i];
					BenchmarkMode mI = null;
					try
					{
						mI = BenchmarkMode.valueOf(stringMI);
					}
					catch(IllegalArgumentException e)
					{
						everythingisProper = false;
						modes.clear();
						logger.error(logHeader + stringMI + " is not a supported BenchmarkMode: ", e);
						break;
					}
					
					if(mI.equals(BenchmarkMode.Normal))
					{
						wantNormal = true;
					}
					else if(mI.equals(BenchmarkMode.Throughput))
					{
						wantThroughput = true;
					}
					
					modes.add(mI);
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
		
		// TopologyFilesStrings
		String unsplitTFS = givenProperty.getProperty(tfsString);
		String[] splitTFS = unsplitTFS.split(PSTBUtil.ITEM_SEPARATOR);
		topologyFilesStrings = PSTBUtil.turnStringArrayIntoArrayListString(splitTFS);
		
		// Distributed
		String unsplitDistributedStates = givenProperty.getProperty(distributedString);
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
						
						distributed.put(topologyFilesStrings.get(i), dsI);
					}
					catch(IllegalArgumentException e)
					{
						everythingisProper = false;
						distributed.clear();
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
		
		// DistributedFileString
		if(wantDistributed)
		{
			distributedFileString = givenProperty.getProperty(dfsString);
			if(distributedFileString == null || distributedFileString.equals("null"))
			{
				logger.error(logHeader + "No valid Distributed File given!");
				everythingisProper = false;
			}
		}
		
		// Protocols
		String unsplitProtocols = givenProperty.getProperty(protocolsString);
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
						protocols.add(pI);
					}
					else
					{
						everythingisProper = false;
						protocols.clear();
						logger.error(logHeader + stringPI + " is not a valid Protocol!");
						break;
					}
				}
				
				if((wantPADRES && givenPADRESProtocols == 0) || (wantSIENA && givenSIENAProtocols == 0))
				{
					everythingisProper = false;
					protocols.clear();
					logger.error(logHeader + "not enough protocols were given for the requested engines!");
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
		
		if(wantNormal)
		{
			// runLengths
			String unsplitRL = givenProperty.getProperty(runLengthsString);
			if(unsplitRL != null)
			{
				String[] splitRL = unsplitRL.split(PSTBUtil.ITEM_SEPARATOR);
				for(int i = 0 ; i < splitRL.length ; i++)
				{
					String stringRLI = splitRL[i];
					try
					{
						Long rlI = Long.parseLong(stringRLI);
						runLengths.add(rlI);
					}
					catch(IllegalArgumentException e)
					{
						everythingisProper = false;
						runLengths.clear();
						logger.error(logHeader + stringRLI + " is not a valid Integer: ", e);
						break;
					}
				}
			}
			else
			{
				everythingisProper = false;
			}
			
			// numRunsPerExperiment
			String givenNRPE = givenProperty.getProperty(nrpeString);
			try
			{
				numRunsPerExperiment = Integer.parseInt(givenNRPE);
			}
			catch(IllegalArgumentException e)
			{
				logger.error(logHeader + givenNRPE + " is not a valid Integer: ", e);
				everythingisProper = false;
			}
		}
		
		if(wantThroughput)
		{
			// PeriodLength
			String givenPLS = givenProperty.getProperty(periodLengthString);
			Long temp = null;
			try
			{
				temp = Long.parseLong(givenPLS);
			}
			catch(IllegalArgumentException e)
			{
				logger.error(logHeader + givenPLS + " is not a valid Long: ", e);
				everythingisProper = false;
			}
			periodLength = temp * PSTBUtil.MILLISEC_TO_NANOSEC;
			
//			// InitialDelay
//			String givenIDS = givenProperty.getProperty(initialDelayString);
//			try
//			{
//				initialDelay = Long.parseLong(givenIDS);
//			}
//			catch(IllegalArgumentException e)
//			{
//				logger.error(logHeader + givenIDS + " is not a valid Long: ", e);
//				everythingisProper = false;
//			}
//			
//			// InitialPayload
//			String givenIPS = givenProperty.getProperty(initialPayloadString);
//			try
//			{
//				initialPayload = Integer.parseInt(givenIPS);
//			}
//			catch(IllegalArgumentException e)
//			{
//				logger.error(logHeader + givenIPS + " is not a valid Integer: ", e);
//				everythingisProper = false;
//			}
		}
		
		// workloadFilesStrings
		String unsplitWFS = givenProperty.getProperty(wfsString);
		if(unsplitWFS != null)
		{
			String[] splitWFS = unsplitWFS.split(PSTBUtil.ITEM_SEPARATOR);
			workloadFilesStrings = PSTBUtil.turnStringArrayIntoArrayListString(splitWFS);
		}
		else
		{
			everythingisProper = false;
		}
		
		return everythingisProper;
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
	 * Gets the modes
	 * 
	 * @return modes - the list of modes to be used
	 */
	public ArrayList<BenchmarkMode> getModes()
	{
		return modes;
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
	 * Gets the initialDelay
	 * 
	 * @return initialDelay - the initial delay value to be used in a throughput experiment
	 */
//	public Long getInitialDelay()
//	{
//		return initialDelay;
//	}
	
	/**
	 * Gets the initialPayload
	 * 
	 * @return initialPayload - the initial payload value to be used in a throughput experiment
	 */
//	public Integer getInitialPayload()
//	{
//		return initialPayload;
//	}
	
	/**
	 * Gets the periodLength
	 * 
	 * @return periodLength - the length of a period in a throughput experiment
	 */
	public Long getPeriodLength()
	{
		return periodLength;
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
		logger.info(logHeader + "modes = " + Arrays.toString(modes.toArray()) + ".");
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
		if(wantNormal)
		{
			logger.info(logHeader + "runLength = " + Arrays.toString(runLengths.toArray()) + ".");
			logger.info(logHeader + "numRunsPerExperiment = " + numRunsPerExperiment + ".");
		}
		if(wantThroughput)
		{
//			logger.info(logHeader + "initialDelay = " + initialDelay + ".");
//			logger.info(logHeader + "initialPayload = " + initialPayload + ".");
			logger.info(logHeader + "periodLength = " + periodLength + ".");
		}
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
		if(modes.isEmpty())
		{
			logger.error(logHeader + "No Modes(s) were given!");
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
		if(wantDistributed && (distributedFileString == null))
		{
			logger.error(logHeader + " No Distributed File String was given!");
			anyFieldNull = true;
		}
		if((wantPADRES || wantSIENA) && protocols.isEmpty())
		{
			logger.error(logHeader + "No Protocol(s) were given!");
			anyFieldNull = true;
		}
		if(wantNormal && runLengths.isEmpty())
		{
			logger.error(logHeader + "No Run Length(s) were given!");
			anyFieldNull = true;
		}
		if(wantNormal && numRunsPerExperiment == null)
		{
			logger.error(logHeader + "No Number of Experiment Runs was given!");
			anyFieldNull = true;
		}
		if(wantThroughput && periodLength == null)
		{
			logger.error(logHeader + "No Period Length was given!");
			anyFieldNull = true;
		}
//		if(wantThroughput && initialDelay == null)
//		{
//			logger.error(logHeader + "No Initial Delay was given!");
//			anyFieldNull = true;
//		}
//		if(wantThroughput && initialPayload == null)
//		{
//			logger.error(logHeader + "No Initial Payload was given!");
//			anyFieldNull = true;
//		}
		if(workloadFilesStrings.isEmpty())
		{
			logger.error(logHeader + "No Workload File String(s) were given!");
			anyFieldNull = true;
		}
		
		return anyFieldNull;
	}
}
