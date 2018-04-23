package pstb.analysis.analysisobjects.throughput;

import java.nio.file.Path;

import pstb.analysis.analysisobjects.PSTBAnalysisObject;

public abstract class PSTBThroughputAO extends PSTBAnalysisObject {
	
	public PSTBThroughputAO()
	{
		super();
	}
		
	/**
	 * Finishes the printing
	 * 
	 * @param givenFilePath - the location to print to
	 * @return false on failure; true otherwise
	 */
	public abstract boolean completeRecord(Path givenFilePath);
	
	public abstract void handleDataPoint(Double givenDataPoint);
	
	public abstract void handleDataPoints(Double x, Double y);
}
