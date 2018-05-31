package pstb.analysis.analysisobjects.throughput;

import java.nio.file.Path;

import pstb.analysis.analysisobjects.PSTBAnalysisObject;
import pstb.analysis.diary.DiaryHeader;

public abstract class PSTBThroughputAO extends PSTBAnalysisObject {
	protected DiaryHeader associated;
	
	public PSTBThroughputAO(DiaryHeader givenDH)
	{
		super();
		associated = givenDH;
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
	
	public DiaryHeader getAssociatedDH()
	{
		return associated;
	}
}
