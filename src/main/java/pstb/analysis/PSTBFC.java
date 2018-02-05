/**
 * 
 */
package pstb.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.Logger;

import pstb.startup.workload.PSActionType;
import pstb.util.FrequencyCounter;

/**
 * @author padres-dev-4187
 * 
 * Extends the Frequency Counter class to allow increased functionality crucial to PSTB
 * Such as what PSActionType we are is recording, 
 * as well as identifying units when recording into a file.
 * @see FrequencyCounter
 */
public class PSTBFC extends FrequencyCounter {
	private String name;
	private PSActionType type;
	
	private final String logHeader = "PSTBFC: ";
	
	/**
	 * Empty Constructor
	 */
	public PSTBFC()
	{
		super();
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
	
	/**
	 * Writes this PSTB Frequency Counter into a file - including providing unit help.
	 * 
	 * @param givenFilePath - the Path we are to write to
	 * @param log - the Logger file we should use if there are errors
	 * @return false on failure; true otherwise
	 */
	public boolean recordPSTBHistogram(Path givenFilePath, Logger log)
	{
		String line = null;
		
		if(type == null)
		{
			log.error(logHeader + "No type has been set!");
			return false;
		}
		else if(type.equals(PSActionType.R))
		{
			line = "Units are in milliseconds\n\n";
		}
		else
		{
			line = "Units are in nanoseconds\n\n";
		}
		
		try
		{
			Files.deleteIfExists(givenFilePath);
			Files.write(givenFilePath, line.getBytes());
		}
		catch(IOException e)
		{
			log.error(logHeader + "Error writing to file:", e);
			return false;
		}
		
		boolean check = record(givenFilePath, log);
		if(!check)
		{
			log.error(logHeader + "Error writing the rest of the frequency counter to a file!");
			return false;
		}
		
		return true;
	}

}
