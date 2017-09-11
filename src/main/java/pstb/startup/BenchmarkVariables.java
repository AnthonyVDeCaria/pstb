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
import pstb.util.ValidProtocol;

public class BenchmarkVariables {
	String topologyFileName;
	Integer numRunsPerExperiment;
	ArrayList<Integer> idealMessageRates;
	ArrayList<ValidProtocol> protocols;
	Integer runLength;
	
	private static final Logger logger = LogManager.getRootLogger();
	
	/**
	 * Empty constructor
	 */
	public BenchmarkVariables()
	{
		topologyFileName = new String();
		numRunsPerExperiment = new Integer(0);
		idealMessageRates = new ArrayList<Integer>();
		protocols = new ArrayList<ValidProtocol>();
		runLength = new Integer(0);
	}
	
	/**
	 * Sets all the benchmark variables' values 
	 * @param givenProperty - the Properties object that contains all the benchmark values.
	 */
	public void setBenchmarkVariable(Properties givenProperty)
	{
		setTopologyFileName(givenProperty.getProperty("pstb.topologyFileLocation"));
		
		String sNRPE = givenProperty.getProperty("pstb.numRunsPerExperiment");
		if(PSTBUtil.isInteger(sNRPE, false))
		{
			setNumRunsPerExperiment(Integer.parseInt(sNRPE));
		}
		
		String sIMR = givenProperty.getProperty("pstb.idealMessageRates");
		ArrayList<Integer> iMR = new ArrayList<Integer>();
		if(sIMR != null)
		{
			String[] splitIMR = sIMR.split(",");
			for(int i = 0 ; i < splitIMR.length ; i++)
			{
				if(!PSTBUtil.isInteger(splitIMR[i], false))
				{
					iMR.clear();
					break;
				}
				else
				{
					iMR.add(Integer.parseInt(splitIMR[i]));
				}
			}
		}
		setIdealMessgaeRates(iMR);
		
		String unbrokenProtocols = givenProperty.getProperty("pstb.protocols");
		ArrayList<ValidProtocol> propProto = new ArrayList<ValidProtocol>();
		if(unbrokenProtocols != null)
		{
			String[] splitProtocols = unbrokenProtocols.split(",");
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
		}
		setProtocols(propProto);
		
		String sRL = givenProperty.getProperty("pstb.runLength");
		if(PSTBUtil.isInteger(sRL, false))
		{
			setRunLength(Integer.parseInt(sRL));
		}
		
	}
	
	/**
	 * Prints all of the Benchmark Variables
	 */
	public void printAllFields()
	{
		logger.info("Properties: topologyFileName = " + topologyFileName);
		logger.info("Properties: numRunsPerExperiment = " + numRunsPerExperiment);
		logger.info("Properties: idealMessageRates = " + Arrays.toString(idealMessageRates.toArray()));
	}
	
	/**
	 * Checks if any of the fields are "null" - not been set
	 * @return false if no field is "null", true if one is
	 */
	public boolean checkForNullFields()
	{
		boolean anyFieldNull = false;
		
		if(topologyFileName.isEmpty())
		{
			logger.error("Properties: No Topology File was given!");
			anyFieldNull = true;
		}
		else if(numRunsPerExperiment.equals(0))
		{
			logger.error("Properties: No Number of Experiment Runs was given!");
			anyFieldNull = true;
		}
		else if(idealMessageRates.isEmpty())
		{
			logger.error("Properties: No Ideal Message Rate(s) was given!");
			anyFieldNull = true;
		}
		else if(protocols.isEmpty())
		{
			logger.error("Properties: No Protocol(s) was given!");
			anyFieldNull = true;
		}
		else if(runLength.equals(0))
		{
			logger.error("Properties: No Run Length was given!");
			anyFieldNull = true;
		}
		return anyFieldNull;
	}
	
	/**
	 * Gets the topologyFileNameProtocols
	 * @return topologyFileName - the name of the Topology File
	 */
	public String getTopologyFileName()
	{
		return this.topologyFileName;
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
	 * Gets the runLength
	 * @return  runLength - the length in minutes of a single run
	 */
	public Integer getRunLength()
	{
		return this.runLength;
	}
	
	/**
	 * NOTE: All the setter functions are private 
	 * as the only "setter" that should be accessed is setBenchmarkVariable
	 */
	
	/**
	 * Sets the topologyFileName
	 * @param tFN - the new topologyFileName
	 */
	private void setTopologyFileName(String tFN)
	{
		topologyFileName = tFN;
	}
	
	/**
	 * Sets the numRunsPerExperiment
	 * @param nRPE - the new numRunsPerExperiment
	 */
	private void setNumRunsPerExperiment(Integer nRPE)
	{
		numRunsPerExperiment = nRPE;
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
	 * Sets the runLength
	 * @param proto - the new protocols
	 */
	private void setRunLength(Integer rL)
	{
		runLength = rL;
	}
}
