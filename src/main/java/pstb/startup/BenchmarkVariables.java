/**
 * 
 */
package pstb.startup;

import java.util.ArrayList;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author padres-dev-4187
 *
 */
public class BenchmarkVariables {
	private static final Logger logger = LogManager.getRootLogger();
	
	String topologyFileName;
	Integer numRunsPerExperiment;
	ArrayList<Integer> idealMessageRates;
	
	public BenchmarkVariables()
	{
		topologyFileName = new String();
		numRunsPerExperiment = new Integer(0);
		idealMessageRates = new ArrayList<Integer>();
	}
	
	public void setBenchmarkVariable(Properties givenProperty)
	{
		setTopologyFileName(givenProperty.getProperty("pstb.topologyFileLocation"));
		setNumRunsPerExperiment(Integer.parseInt(givenProperty.getProperty("pstb.numRunsPerExperiment")));
		
//		ArrayList<Integer> iMR = givenProperty.getProperty("pstb.idealMessageRate");
//		setIdealMessageRate(iMR);
	}
	
	public void printAllFields()
	{
		logger.info("Properties: topologyFileName = " + topologyFileName);
		logger.info("Properties: numRunsPerExperiment = " + numRunsPerExperiment);
	}
	
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
		return anyFieldNull;
	}
	
	public String getTopologyFileName()
	{
		return this.topologyFileName;
	}
	
	public void setTopologyFileName(String tFN)
	{
		topologyFileName = tFN;
	}
	
	public Integer getNumRunsPerExperiment()
	{
		return this.numRunsPerExperiment;
	}
	
	public void setNumRunsPerExperiment(Integer nRPE)
	{
		numRunsPerExperiment = nRPE;
	}

//	public ArrayList<Integer> getIdealMessageRates()
//	{
//		return this.idealMessageRates;
//	}
//	
//	public void setIdealMessgaeRates(ArrayList<Integer> iMR)
//	{
//		idealMessageRates = iMR;
//	}
}
