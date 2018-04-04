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
	private final String periodLengthString = "startup.periodLength";
	private final String msString = "startup.messageSize";
	private final String naString = "startup.numAttribute";
	private final String arString = "startup.attributeRatio";
	private final String wfsString = "startup.workloadFilesStrings";
	
	private ArrayList<PSEngine> engines;
	private ArrayList<ExperimentType> modes;
	
	private ArrayList<String> topologyFilesStrings;
	
	private HashMap<String, DistributedState> distributed;
	private boolean wantDistributed;
	private String distributedFileString;
	
	private ArrayList<NetworkProtocol> protocols;
	private boolean wantPADRES;
	private boolean wantSIENA;
	
	private boolean wantNormal;
	private ArrayList<Long> runLengths; // Nanoseconds
	private Integer numRunsPerExperiment;
	
	private boolean wantThroughput;
	private Long periodLength;
	private ArrayList<MessageSize> messageSizes;
	private ArrayList<NumAttribute> numAttributes;
	private ArrayList<AttributeRatio> attributeRatios;
	
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
		modes = new ArrayList<ExperimentType>();
		
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
		messageSizes = new ArrayList<MessageSize>();
		numAttributes = new ArrayList<NumAttribute>();
		attributeRatios = new ArrayList<AttributeRatio>();
		
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
		
		// ExperimentTypes
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
					ExperimentType mI = null;
					try
					{
						mI = ExperimentType.valueOf(stringMI);
					}
					catch(IllegalArgumentException e)
					{
						everythingisProper = false;
						modes.clear();
						logger.error(logHeader + stringMI + " is not a supported BenchmarkMode: ", e);
						break;
					}
					
					if(mI.equals(ExperimentType.Scenario))
					{
						wantNormal = true;
					}
					else if(mI.equals(ExperimentType.Throughput))
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
			
			// MessageSize
			String unsplitMessageSizes = givenProperty.getProperty(msString);
			if(unsplitMessageSizes != null)
			{
				String[] splitMS = unsplitMessageSizes.split(PSTBUtil.ITEM_SEPARATOR);
				int numRequestedMS = splitMS.length;
				if(numRequestedMS <= MessageSize.values().length)
				{
					for(int i = 0 ; i < numRequestedMS ; i++ )
					{
						String stringMSI = splitMS[i];
						try
						{
							MessageSize msI = MessageSize.valueOf(stringMSI);
							
							messageSizes.add(msI);
						}
						catch(IllegalArgumentException e)
						{
							everythingisProper = false;
							messageSizes.clear();
							logger.error(logHeader + stringMSI + " is not a valid MessageSize: ", e);
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
			
			// NumAttributes
			String unsplitNumAttributes = givenProperty.getProperty(naString);
			if(unsplitNumAttributes != null)
			{
				String[] splitNA = unsplitNumAttributes.split(PSTBUtil.ITEM_SEPARATOR);
				int numRequestedNA = splitNA.length;
				if(numRequestedNA <= NumAttribute.values().length)
				{
					for(int i = 0 ; i < numRequestedNA ; i++ )
					{
						String stringNAI = splitNA[i];
						try
						{
							NumAttribute naI = NumAttribute.valueOf(stringNAI);
							
							numAttributes.add(naI);
						}
						catch(IllegalArgumentException e)
						{
							everythingisProper = false;
							numAttributes.clear();
							logger.error(logHeader + stringNAI + " is not a valid NumAttributes: ", e);
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
			
			// AttributeRatios
			String unsplitAttributeRatios = givenProperty.getProperty(arString);
			if(unsplitAttributeRatios != null)
			{
				String[] splitAR = unsplitAttributeRatios.split(PSTBUtil.ITEM_SEPARATOR);
				int numRequestedAR = splitAR.length;
				if(numRequestedAR <= AttributeRatio.values().length)
				{
					for(int i = 0 ; i < numRequestedAR ; i++ )
					{
						String stringARI = splitAR[i];
						try
						{
							AttributeRatio arI = AttributeRatio.valueOf(stringARI);
							
							attributeRatios.add(arI);
						}
						catch(IllegalArgumentException e)
						{
							everythingisProper = false;
							attributeRatios.clear();
							logger.error(logHeader + stringARI + " is not a valid AttributeRatio: ", e);
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
	public ArrayList<ExperimentType> getModes()
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
	 * Gets the wantSIENA value
	 * 
	 * @return wantSIENA - NOTE: this value is false if setBenchmarkConfig isn't run
	 */
	public boolean sienaRequested() 
	{
		return wantSIENA;
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
	 * @return numRunsPerExperiment - the number of runs an Scenario experiment has to complete
	 */
	public Integer getNumRunsPerExperiment()
	{
		return numRunsPerExperiment;
	}
	
	/**
	 * Gets the periodLength
	 * 
	 * @return periodLength - the length of a period in a Throughput experiment
	 */
	public Long getPeriodLength()
	{
		return periodLength;
	}
	
	/**
	 * Gets the messageSizes
	 * 
	 * @return messageSizes - the payload sizes requested in a Throughput experiment
	 */
	public ArrayList<MessageSize> getMessageSizes()
	{
		return messageSizes;
	}
	
	/**
	 * Gets the numAttributes
	 * 
	 * @return numAttributes - the number of attributes requested in a Throughput experiment
	 */
	public ArrayList<NumAttribute> getNumAttributes()
	{
		return numAttributes;
	}
	
	/**
	 * Gets the attributeRatios
	 * 
	 * @return attributeRatios - the attribute ratios requested in a Throughput experiment
	 */
	public ArrayList<AttributeRatio> getAttributeRatios()
	{
		return attributeRatios;
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
			logger.info(logHeader + "periodLength = " + periodLength + ".");
			logger.info(logHeader + "messageSizes = " + Arrays.toString(messageSizes.toArray()) + ".");
			logger.info(logHeader + "numAttributes = " + Arrays.toString(numAttributes.toArray()) + ".");
			logger.info(logHeader + "attributeRatios = " + Arrays.toString(attributeRatios.toArray()) + ".");
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
		if(wantThroughput && messageSizes.isEmpty())
		{
			logger.error(logHeader + "No Message Size(s) were given!");
			anyFieldNull = true;
		}
		if(wantThroughput && numAttributes.isEmpty())
		{
			logger.error(logHeader + "No Number of Attribute(s) were given!");
			anyFieldNull = true;
		}
		if(wantThroughput && attributeRatios.isEmpty())
		{
			logger.error(logHeader + "No AttributeRatio(s) were given!");
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
