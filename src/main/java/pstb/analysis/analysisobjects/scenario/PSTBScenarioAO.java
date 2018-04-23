package pstb.analysis.analysisobjects.scenario;

import java.nio.file.Path;

import pstb.analysis.analysisobjects.PSTBAnalysisObject;
import pstb.startup.workload.PSActionType;

public abstract class PSTBScenarioAO extends PSTBAnalysisObject {
	protected PSActionType type;
	
	public PSTBScenarioAO()
	{
		type = null;
	}
	
	/**
	 * Sets the type
	 * 
	 * @param givenType - the new PSActionType
	 */
	public void setType(PSActionType givenType) {
		type = givenType;
	}
	
	/**
	 * Gets the type
	 * 
	 * @return the type
	 */
	public PSActionType getType() {
		return type;
	}
		
	/**
	 * Finishes the printing
	 * 
	 * @param givenFilePath - the location to print to
	 * @return false on failure; true otherwise
	 */
	public abstract boolean completeRecord(Path givenFilePath);
	
	public abstract void handleDataPoint(Long givenDataPoint);
}
