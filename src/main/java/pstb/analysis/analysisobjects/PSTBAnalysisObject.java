package pstb.analysis.analysisobjects;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.startup.workload.PSActionType;

public abstract class PSTBAnalysisObject {
	protected String name;
	protected PSActionType type;
	
	// Logger
	protected String logHeader;
	protected final Logger log = LogManager.getRootLogger();
	
	public PSTBAnalysisObject()
	{
		name = null;
		type = null;
	}
	
	/**
	 * Sets the name
	 * 
	 * @param givenName - the new name
	 */
	public void setName(String givenName) {
		name = givenName;
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
	 * Gets the name
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the type
	 * 
	 * @return the type
	 */
	public PSActionType getType() {
		return type;
	}
	
	public boolean recordAO(Path givenFilePath)
	{
		if(type == null)
		{
			log.error(logHeader + "No PSActionType has been set yet!");
			return false;
		}
		
		try
		{
			Files.deleteIfExists(givenFilePath);
			Files.createFile(givenFilePath);
		}
		catch(Exception e)
		{
			log.error(logHeader + "Couldn't recreate file " + givenFilePath + ": ", e);
			return false;
		}
		
		boolean recordCheck = completeRecord(givenFilePath);
		if(!recordCheck)
		{
			log.error(logHeader + "Couldn't complete record!");
			return false;
		}
		
		return true;
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
