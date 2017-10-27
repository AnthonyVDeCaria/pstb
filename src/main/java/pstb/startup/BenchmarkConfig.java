/**
 * @author padres-dev-4187
 * 
 * A wrapper around the variables that set a certain benchmark.
 *
 */
package pstb.startup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import pstb.util.PSTBUtil;
import pstb.util.DistributedState;
import pstb.util.NetworkProtocol;

public class BenchmarkConfig {
	private Integer numRunsPerExperiment;
	private ArrayList<String> pubWorkloadFilesStrings;
	private String subWorkloadFileString;
	
	private ArrayList<Long> runLengths; // Milliseconds
	
	private ArrayList<NetworkProtocol> protocols;
	private ArrayList<String> topologyFilesStrings;
	private HashMap<String, DistributedState> distributed;
	
	private Logger logger = null;
	
	/**
	 * Constructor
	 * 
	 * The Integers are set to 0, as these are not valid numbers
	 * (You can't have 0 runs per experiment) 
	 */
	public BenchmarkConfig(Logger log)
	{
		logger = log;
		
		numRunsPerExperiment = new Integer(0);
		pubWorkloadFilesStrings = new ArrayList<String>();
		subWorkloadFileString = new String();
		runLengths = new ArrayList<Long>();
		protocols = new ArrayList<NetworkProtocol>();
		topologyFilesStrings = new ArrayList<String>();
		distributed = new HashMap<String, DistributedState>();
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
		
		// numRunsPerExperiment
		String givenNRPE = givenProperty.getProperty("startup.numRunsPerExperiment");
		try
		{
			Integer intNRPE = Integer.parseInt(givenNRPE);
			setNumRunsPerExperiment(intNRPE);
		}
		catch(IllegalArgumentException e)
		{
			logger.error("Properties: " + givenNRPE + " is not a valid Integer.", e);
			everythingisProper = false;
		}
		
		// pubWorkloadFilesStrings
		String unsplitPWFP = givenProperty.getProperty("startup.pubWorkloadFilesStrings");
		ArrayList<String> propPWFP = new ArrayList<String>();
		if(unsplitPWFP != null)
		{
			String[] splitPWFP = unsplitPWFP.split(PSTBUtil.COMMA);
			propPWFP = PSTBUtil.turnStringArrayIntoArrayListString(splitPWFP);
		}
		setPubWorkloadFilesStrings(propPWFP);
		
		// subWorkloadFileString
		String propSWFP = givenProperty.getProperty("startup.subWorkloadFileString");
		setSubWorkloadFileString(propSWFP);
		
		// runLengths
		String unsplitRL = givenProperty.getProperty("startup.runLengths");
		ArrayList<Long> propRL = new ArrayList<Long>();
		if(unsplitRL != null)
		{
			String[] splitRL = unsplitRL.split(PSTBUtil.COMMA);
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
					logger.error("Properties: " + splitRL[i] + " is not a valid Integer.", e);
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
			String[] splitProtocols = unsplitProtocols.split(PSTBUtil.COMMA);
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
						logger.error("Properties: " + splitProtocols[i] + " is not a valid Protocol.", e);
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
		
		// topologyFilesStrings
		String unsplitTFP = givenProperty.getProperty("startup.topologyFilesStrings");
		String[] splitTFP = unsplitTFP.split(PSTBUtil.COMMA);
		setTopologyFilesStrings(PSTBUtil.turnStringArrayIntoArrayListString(splitTFP));
		
		// distributed
		String unsplitDis = givenProperty.getProperty("startup.distributed");
		HashMap<String, DistributedState> propDis = new HashMap<String, DistributedState>();
		if(unsplitDis != null)
		{
			String[] splitDis = unsplitDis.split(PSTBUtil.COMMA);
			int numGivenDis = splitDis.length;
			if(numGivenDis == splitTFP.length)
			{
				for(int i = 0 ; i < numGivenDis ; i++ )
				{
					try
					{
						DistributedState sDI = DistributedState.valueOf(splitDis[i]);
						propDis.put(topologyFilesStrings.get(i), sDI);
					}
					catch(IllegalArgumentException e)
					{
						everythingisProper = false;
						propDis.clear();
						logger.error("Properties: " + splitDis[i] + " is not a valid Distributed value.", e);
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
		
		return everythingisProper;
	}
	
	/**
	 * Prints all of the Benchmark Variables
	 */
	public void printAllFields()
	{
		logger.info("Properties: numRunsPerExperiment = " + numRunsPerExperiment);
		logger.info("Properties: pubWorkloadFilesStrings = " + Arrays.toString(pubWorkloadFilesStrings.toArray()));
		logger.info("Properties: subWorkloadFileString = " + subWorkloadFileString);
		logger.info("Properties: runLength = " + Arrays.toString(runLengths.toArray()));
		logger.info("Properties: protocols = " + Arrays.toString(protocols.toArray()));
		logger.info("Properties: topologyFilesStrings = " + Arrays.toString(topologyFilesStrings.toArray()));
		logger.info("Properties: distributed = " + distributed.toString());
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
			logger.error("Properties: No Number of Experiment Runs was given!");
			anyFieldNull = true;
		}
		if(pubWorkloadFilesStrings.isEmpty())
		{
			logger.error("Properties: No Publisher Workload Strings were given!");
			anyFieldNull = true;
		}
		if(subWorkloadFileString.isEmpty())
		{
			logger.error("Properties: No Subscriber Workload String was given!");
			anyFieldNull = true;
		}
		if(runLengths.isEmpty())
		{
			logger.error("Properties: No Run Length(s) were given!");
			anyFieldNull = true;
		}
		if(protocols.isEmpty())
		{
			logger.error("Properties: No Protocol(s) were given!");
			anyFieldNull = true;
		}
		if(topologyFilesStrings.isEmpty())
		{
			logger.error("Properties: No Topology File String(s) were given!");
			anyFieldNull = true;
		}
		if(distributed.isEmpty())
		{
			logger.error("Properties: No Distributed information was given!");
			anyFieldNull = true;
		}
		
		return anyFieldNull;
	}
	
	/**
	 * NOTE: All the setter functions are private 
	 * as the only "setter" that should be accessed is setBenchmarkConfig
	 */
	
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
	 * Sets the pubWorkloadFilesStrings
	 * 
	 * @param nPWFS - the new pubWorkloadFilesStrings
	 */
	private void setPubWorkloadFilesStrings(ArrayList<String> nPWFS)
	{
		pubWorkloadFilesStrings = nPWFS;
	}
	
	/**
	 * Sets the subWorkloadFileString
	 * 
	 * @param nSWFS - the new subWorkloadFileString
	 */
	private void setSubWorkloadFileString(String nSWFS)
	{
		subWorkloadFileString = nSWFS;
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
	 * Sets the protocols
	 * 
	 * @param proto - the new protocols
	 */
	private void setProtocols(ArrayList<NetworkProtocol> proto)
	{
		protocols = proto;
	}
	
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
	 * Gets the numRunsPerExperiment
	 * 
	 * @return numRunsPerExperiment - the number of runs an experiment has to complete
	 */
	public Integer getNumRunsPerExperiment()
	{
		return this.numRunsPerExperiment;
	}
	
	/**
	 * Gets the runLength
	 * 
	 * @return  runLength - the list of minutes each experiment's run's will take
	 */
	public ArrayList<Long> getRunLengths()
	{
		return this.runLengths;
	}
	
	/**
	 * Gets the protocols
	 * 
	 * @return protocols - the list of protocols to be use in different runs
	 */
	public ArrayList<NetworkProtocol> getProtocols()
	{
		return this.protocols;
	}
	
	/**
	 * Gets the topologyFilesStrings
	 * 
	 * @return topologyFilesStrings - the paths to all the Topology Files
	 */
	public ArrayList<String> getTopologyFilesStrings()
	{
		return this.topologyFilesStrings;
	}
	
	/**
	 * Gets the distributed
	 * 
	 * @return distributed - the list of wither each topology is distributed or not
	 */
	public HashMap<String, DistributedState> getDistributed()
	{
		return this.distributed;
	}
	
	/**
	 * Gets the pubWorkloadFilesStrings
	 * 
	 * @return the pubWorkloadFilesStrings 
	 */
	public ArrayList<String> getPubWorkloadFilesStrings()
	{
		return this.pubWorkloadFilesStrings;
	}
	
	/**
	 * Gets the subWorkloadFileString
	 * 
	 * @return the subWorkloadFileString
	 */
	public String getSubWorkloadFileString()
	{
		return this.subWorkloadFileString;
	}
}
