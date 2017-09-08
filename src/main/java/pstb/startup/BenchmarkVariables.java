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
	private static final Logger logger = LogManager.getRootLogger();
	
	String topologyFileName;
	Integer numRunsPerExperiment;
	ArrayList<Integer> idealMessageRates;
	ArrayList<ValidProtocol> protocols;
	
	/**
	 * Empty constructor
	 */
	public BenchmarkVariables()
	{
		topologyFileName = new String();
		numRunsPerExperiment = new Integer(0);
		idealMessageRates = new ArrayList<Integer>();
		protocols = new ArrayList<ValidProtocol>();
	}
	
	/**
	 * Sets the benchmark variables values
	 * @param givenProperty - the Properties object that contains all the benchmark values.
	 */
	public void setBenchmarkVariable(Properties givenProperty)
	{
		setTopologyFileName(givenProperty.getProperty("pstb.topologyFileLocation"));
		
		if(PSTBUtil.isInteger(givenProperty.getProperty("pstb.numRunsPerExperiment")))
		{
			setNumRunsPerExperiment(Integer.parseInt(givenProperty.getProperty("pstb.numRunsPerExperiment")));
		}
		
		String sIMR = givenProperty.getProperty("pstb.idealMessageRates");
		ArrayList<Integer> iMR = new ArrayList<Integer>();
		if(sIMR != null)
		{
			String[] splitIMR = sIMR.split(",");
			for(int i = 0 ; i < splitIMR.length ; i++)
			{
				if(!PSTBUtil.isInteger(splitIMR[i]))
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
			for(int i = 0 ; i < splitProtocols.length ; i++ )
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
		setProtocols(propProto);
		
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
		return anyFieldNull;
	}
	
	public String getTopologyFileName()
	{
		return this.topologyFileName;
	}
	
	public Integer getNumRunsPerExperiment()
	{
		return this.numRunsPerExperiment;
	}

	public ArrayList<Integer> getIdealMessageRates()
	{
		return this.idealMessageRates;
	}
	
	public ArrayList<ValidProtocol> getProtocols()
	{
		return this.protocols;
	}
	
	private void setTopologyFileName(String tFN)
	{
		topologyFileName = tFN;
	}
	
	private void setNumRunsPerExperiment(Integer nRPE)
	{
		numRunsPerExperiment = nRPE;
	}
	
	private void setIdealMessgaeRates(ArrayList<Integer> iMR)
	{
		idealMessageRates = iMR;
	}
	
	private void setProtocols(ArrayList<ValidProtocol> proto)
	{
		protocols = proto;
	}
}
