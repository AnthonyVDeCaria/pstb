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
	private ArrayList<String> pubWorkloadFilesPaths;
	private String subWorkloadFilePath;
	
	private ArrayList<Long> runLengths; // Milliseconds
	
	private ArrayList<NetworkProtocol> protocols;
	private ArrayList<String> topologyFilesPaths;
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
		pubWorkloadFilesPaths = new ArrayList<String>();
		subWorkloadFilePath = new String();
		runLengths = new ArrayList<Long>();
		protocols = new ArrayList<NetworkProtocol>();
		topologyFilesPaths = new ArrayList<String>();
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
		
		/*
		 * numRunsPerExperiment
		 */
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
		
		/*
		 * pubWorkloadFilesPaths
		 */
		String unsplitPWFP = givenProperty.getProperty("startup.pubWorkloadFilesPaths");
		ArrayList<String> propPWFP = new ArrayList<String>();
		if(unsplitPWFP != null)
		{
			String[] splitPWFP = unsplitPWFP.split(PSTBUtil.COMMA);
			propPWFP = PSTBUtil.turnStringArrayIntoArrayListString(splitPWFP);
		}
		setPubWorkloadFilesPaths(propPWFP);
		
		/*
		 * subWorkloadFilePath
		 */
		String propSWFP = givenProperty.getProperty("startup.subWorkloadFilePath");
		setSubWorkloadFilePath(propSWFP);
		
		/*
		 * runLengths
		 */
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
		
		/*
		 * Protocols
		 */
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
		
		/*
		 * topologyFilesPaths
		 */
		String unsplitTFP = givenProperty.getProperty("startup.topologyFilesPaths");
		String[] splitTFP = unsplitTFP.split(PSTBUtil.COMMA);
		setTopologyFilesPaths(PSTBUtil.turnStringArrayIntoArrayListString(splitTFP));
		
		/*
		 * distributed
		 */
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
						propDis.put(topologyFilesPaths.get(i), sDI);
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
		
		/*
		 * return
		 */
		return everythingisProper;
	}
	
	/**
	 * Prints all of the Benchmark Variables
	 */
	public void printAllFields()
	{
		logger.info("Properties: numRunsPerExperiment = " + numRunsPerExperiment);
		logger.info("Properties: pubWorkloadFilePath = " + Arrays.toString(pubWorkloadFilesPaths.toArray()));
		logger.info("Properties: subWorkloadFilePath = " + subWorkloadFilePath);
		logger.info("Properties: runLength = " + Arrays.toString(runLengths.toArray()));
		logger.info("Properties: protocols = " + Arrays.toString(protocols.toArray()));
		logger.info("Properties: topologyFilesPaths = " + Arrays.toString(topologyFilesPaths.toArray()));
		logger.info("Properties: distributed = " + distributed.toString());
	}
	
	/**
	 * Checks if any of the fields are "null"
	 * I.e. Not been set
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
		if(pubWorkloadFilesPaths.isEmpty())
		{
			logger.error("Properties: No Publisher Workload Files were given!");
			anyFieldNull = true;
		}
		if(subWorkloadFilePath.isEmpty())
		{
			logger.error("Properties: No Subscriber Workload File was given!");
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
		if(topologyFilesPaths.isEmpty())
		{
			logger.error("Properties: No Topology File(s) were given!");
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
	 * Gets the numRunsPerExperiment
	 * @return numRunsPerExperiment - the number of runs an experiment has to complete
	 */
	public Integer getNumRunsPerExperiment()
	{
		return this.numRunsPerExperiment;
	}
	
	/**
	 * Gets the runLength
	 * @return  runLength - the list of minutes each experiment's run's will take
	 */
	public ArrayList<Long> getRunLengths()
	{
		return this.runLengths;
	}
	
	/**
	 * Gets the protocols
	 * @return protocols - the list of protocols to be use in different runs
	 */
	public ArrayList<NetworkProtocol> getProtocols()
	{
		return this.protocols;
	}
	
	/**
	 * Gets the topologyFilesPaths
	 * @return topologyFilesPaths - the paths to all the Topology Files
	 */
	public ArrayList<String> getTopologyFilesPaths()
	{
		return this.topologyFilesPaths;
	}
	
	/**
	 * Gets the distributed
	 * @return distributed - the list of wither each topology is distributed or not
	 */
	public HashMap<String, DistributedState> getDistributed()
	{
		return this.distributed;
	}
	
	/**
	 * Gets the pubWorkloadFilesPaths
	 * @return the pubWorkloadFilesPaths
	 */
	public ArrayList<String> getPubWorkloadFilesPaths()
	{
		return this.pubWorkloadFilesPaths;
	}
	
	/**
	 * Gets the subWorkloadFilePath
	 * @return the subWorkloadFilePath
	 */
	public String getSubWorkloadFilePath()
	{
		return this.subWorkloadFilePath;
	}
	
	/**
	 * NOTE: All the setter functions are private 
	 * as the only "setter" that should be accessed is setBenchmarkConfig
	 */
	
	/**
	 * Sets the numRunsPerExperiment
	 * @param nRPE - the new numRunsPerExperiment
	 */
	private void setNumRunsPerExperiment(Integer nRPE)
	{
		numRunsPerExperiment = nRPE;
	}
	
	/**
	 * Sets the pubWorkloadFilesPaths
	 * @param nPWFP - the new pubWorkloadFilesPaths
	 */
	private void setPubWorkloadFilesPaths(ArrayList<String> nPWFP)
	{
		pubWorkloadFilesPaths = nPWFP;
	}
	
	/**
	 * Sets the subWorkloadFilePath
	 * @param nSWFP - the new subWorkloadFilePath
	 */
	private void setSubWorkloadFilePath(String nSWFP)
	{
		subWorkloadFilePath = nSWFP;
	}
	
	/**
	 * Sets the runLength
	 * @param proto - the new protocols
	 */
	private void setRunLengths(ArrayList<Long> rL)
	{
		runLengths = rL;
	}
	
	/**
	 * Sets the protocols
	 * @param proto - the new protocols
	 */
	private void setProtocols(ArrayList<NetworkProtocol> proto)
	{
		protocols = proto;
	}
	
	/**
	 * Sets the topologyFilesPaths
	 * @param tFP - the new topologyFilesPaths
	 */
	private void setTopologyFilesPaths(ArrayList<String> tFP)
	{
		topologyFilesPaths = tFP;
	}
	
	/**
	 * Sets the distributed array
	 * @param dis - the new distributed
	 */
	private void setDistributed(HashMap<String, DistributedState> dis)
	{
		distributed = dis;
	}
}
