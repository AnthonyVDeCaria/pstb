/**
 * 
 */
package pstb.startup;

import java.util.ArrayList;

/**
 * @author padres-dev-4187
 *
 */
public class BenchmarkVariables {
	String topologyFileName;
	ArrayList<Integer> idealMessageRates;
	
	public BenchmarkVariables()
	{
		topologyFileName = new String();
		idealMessageRates = new ArrayList<Integer>();
	}
	
	public String getTopologyFileName()
	{
		return this.topologyFileName;
	}
	
	public void setTopologyFileName(String tFN)
	{
		topologyFileName = tFN;
	}
	
	public boolean checkForNullFields()
	{
		return (topologyFileName.isEmpty() | idealMessageRates.isEmpty());
	}

}
