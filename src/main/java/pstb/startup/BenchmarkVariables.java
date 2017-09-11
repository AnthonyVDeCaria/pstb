/**
 * @author padres-dev-4187
 * 
 * A wrapper around the variables that set a certain benchmark.
 *
 */
package pstb.startup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.util.PSTBUtil;
import pstb.util.ValidDistributedValues;
import pstb.util.ValidProtocol;

public class BenchmarkVariables {
	Integer numRunsPerExperiment;
	
	ArrayList<Integer> runLengths;
	ArrayList<Integer> idealMessageRates;
	
	ArrayList<ValidProtocol> protocols;
	ArrayList<String> topologyFilesPaths;
	ArrayList<ValidDistributedValues> distributed;
	
	private static final Logger logger = LogManager.getRootLogger();
	
	/**
	 * Empty constructor
	 * 
	 * The Integers are set to 0, as these are not valid numbers
	 * (You can't have no runs per experiment, or run for 0 minutes) 
	 */
	public BenchmarkVariables()
	{
		numRunsPerExperiment = new Integer(0);
		runLengths = new ArrayList<Integer>();
		idealMessageRates = new ArrayList<Integer>();
		protocols = new ArrayList<ValidProtocol>();
		topologyFilesPaths = new ArrayList<String>();
		distributed = new ArrayList<ValidDistributedValues>();
	}
	
	/**
	 * Sets all the benchmark variables' values after doing some quick parsing
	 * (Seeing as some of these fields have to go from String to other types)
	 * If there is any errors, these fields will either not be set (the Integers),
	 * or be made empty (ArrayLists).
	 * The tested fields are numRunsPerExperiment, idealMessageRates,
	 * protocols, runLength.
	 * 
	 * @param givenProperty - the Properties object that contains the desired values.
	 * @return true if everything sets successfully; false otherwise
	 */
	public boolean setBenchmarkVariable(Properties givenProperty)
	{
		boolean everythingisProper = true;
		
		/*
		 * numRunsPerExperiment
		 */
		String sNRPE = givenProperty.getProperty("pstb.numRunsPerExperiment");
		if(PSTBUtil.isInteger(sNRPE, true))
		{
			setNumRunsPerExperiment(Integer.parseInt(sNRPE));
		}
		else
		{
			everythingisProper = false;			
		}
		
		/*
		 * runLengths
		 */
		String sRL = givenProperty.getProperty("pstb.runLengths");
		ArrayList<Integer> rL = new ArrayList<Integer>();
		if(sRL != null)
		{
			String[] splitRL = sRL.split(",");
			for(int i = 0 ; i < splitRL.length ; i++)
			{
				if(!PSTBUtil.isInteger(splitRL[i], true))
				{
					rL.clear();
					break;
				}
				else
				{
					rL.add(Integer.parseInt(splitRL[i]));
				}
			}
		}
		else
		{
			everythingisProper = false;
		}
		setRunLengths(rL);
		
		/*
		 * idealMessageRates
		 */
		String unsplitIMR = givenProperty.getProperty("pstb.idealMessageRates");
		ArrayList<Integer> propIMR = new ArrayList<Integer>();
		if(unsplitIMR != null)
		{
			String[] splitIMR = unsplitIMR.split(",");
			for(int i = 0 ; i < splitIMR.length ; i++)
			{
				if(!PSTBUtil.isInteger(splitIMR[i], true))
				{
					propIMR.clear();
					break;
				}
				else
				{
					propIMR.add(Integer.parseInt(splitIMR[i]));
				}
			}
		}
		else
		{
			everythingisProper = false;
		}
		setIdealMessgaeRates(propIMR);
		
		/*
		 * Protocols
		 */
		String unsplitProtocols = givenProperty.getProperty("pstb.protocols");
		ArrayList<ValidProtocol> propProto = new ArrayList<ValidProtocol>();
		if(unsplitProtocols != null)
		{
			String[] splitProtocols = unsplitProtocols.split(",");
			int numGivenProtocols = splitProtocols.length;
			if(numGivenProtocols <= ValidProtocol.values().length)
			{
				for(int i = 0 ; i < numGivenProtocols ; i++ )
				{
					try
					{
						ValidProtocol sPI = ValidProtocol.valueOf(splitProtocols[i]);
						propProto.add(sPI);
					}
					catch(IllegalArgumentException e)
					{
						propProto.clear();
						logger.error("Properties: " + splitProtocols[i] + " is not a valid protocol.", e);
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
		String unsplitTFP = givenProperty.getProperty("pstb.topologyFilesPaths");
		String[] splitTFP = unsplitTFP.split(",");
		setTopologyFilesPaths(PSTBUtil.turnStringArrayIntoArrayListString(splitTFP));
		
		/*
		 * distributed
		 */
		String unsplitDis = givenProperty.getProperty("pstb.distributed");
		ArrayList<ValidDistributedValues> propDis = new ArrayList<ValidDistributedValues>();
		if(unsplitDis != null)
		{
			String[] splitDis = unsplitDis.split(",");
			int numGivenDis = splitDis.length;
			if(numGivenDis == splitTFP.length)
			{
				for(int i = 0 ; i < numGivenDis ; i++ )
				{
					try
					{
						ValidDistributedValues sDI = ValidDistributedValues.valueOf(splitDis[i]);
						propDis.add(sDI);
					}
					catch(IllegalArgumentException e)
					{
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
		logger.info("Properties: runLength = " + Arrays.toString(runLengths.toArray()));
		logger.info("Properties: idealMessageRates = " + Arrays.toString(idealMessageRates.toArray()));
		logger.info("Properties: protocols = " + Arrays.toString(protocols.toArray()));
		logger.info("Properties: topologyFilesPaths = " + Arrays.toString(topologyFilesPaths.toArray()));
		logger.info("Properties: distributed = " + Arrays.toString(distributed.toArray()));
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
		if(runLengths.isEmpty())
		{
			logger.error("Properties: No Run Length(s) were given!");
			anyFieldNull = true;
		}
		if(idealMessageRates.isEmpty())
		{
			logger.error("Properties: No Ideal Message Rate(s) were given!");
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
	public ArrayList<Integer> getRunLengths()
	{
		return this.runLengths;
	}

	/**
	 * Gets the idealMessageRates
	 * @return idealMessageRates - the list of message rates the network should send each run
	 */
	public ArrayList<Integer> getIdealMessageRates()
	{
		return this.idealMessageRates;
	}
	
	/**
	 * Gets the protocols
	 * @return protocols - the list of protocols to be use in different runs
	 */
	public ArrayList<ValidProtocol> getProtocols()
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
	public ArrayList<ValidDistributedValues> getDistributed()
	{
		return this.distributed;
	}
	
	/**
	 * NOTE: All the setter functions are private 
	 * as the only "setter" that should be accessed is setBenchmarkVariable
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
	 * Sets the runLength
	 * @param proto - the new protocols
	 */
	private void setRunLengths(ArrayList<Integer> rL)
	{
		runLengths = rL;
	}
	
	/**
	 * Sets the idealMessageRates
	 * @param iMR - the new idealMessageRates
	 */
	private void setIdealMessgaeRates(ArrayList<Integer> iMR)
	{
		idealMessageRates = iMR;
	}
	
	/**
	 * Sets the protocols
	 * @param proto - the new protocols
	 */
	private void setProtocols(ArrayList<ValidProtocol> proto)
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
	 * Sets the protocols
	 * @param dis - the new distributed
	 */
	private void setDistributed(ArrayList<ValidDistributedValues> dis)
	{
		distributed = dis;
	}
}
