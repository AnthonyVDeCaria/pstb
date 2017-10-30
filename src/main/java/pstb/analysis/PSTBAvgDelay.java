/**
 * 
 */
package pstb.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.Logger;

import pstb.util.PSActionType;
import pstb.util.PSTBUtil;
import pstb.util.PSTBUtil.TimeType;

/**
 * @author padres-dev-4187
 * 
 * A file that allows us to store an average delay
 * and a name associated with it 
 * @see Analyzer
 */
public class PSTBAvgDelay {
	private String name;
	private Long value;
	private PSActionType delayType;
	
	private final String logHeader = "PSTBAvgDelay: ";

	/**
	 * Constructor
	 */
	public PSTBAvgDelay() {
		setName(new String());
		setValue(new Long(0));
	}

	/**
	 * Sets the name
	 * 
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Sets the value
	 * 
	 * @param value the value to set
	 */
	public void setValue(Long value) {
		this.value = value;
	}
	
	/**
	 * @param delayType the delayType to set
	 */
	public void setDelayType(PSActionType delayType) {
		this.delayType = delayType;
	}
	
	/**
	 * Gets the name of this Delay
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the value
	 * 
	 * @return the value
	 */
	public Long getValue() {
		return value;
	}
	
	/**
	 * @return the delayType
	 */
	public PSActionType getDelayType() {
		return delayType;
	}
	
	/**
	 * Prints the delay value
	 * 
	 * @param givenFilePath - the location to print to
	 * @return false on failure; true otherwise
	 */
	public boolean recordAvgDelay(Path givenFilePath, Logger log)
	{
		String line = null;
		
		if(delayType.equals(null))
		{
			log.error(logHeader + "No delayType was set");
		}
		else if(delayType.equals(PSActionType.R))
		{
			line = PSTBUtil.createTimeString(value, TimeType.Milli);
		}
		else
		{
			line = PSTBUtil.createTimeString(value, TimeType.Nano);
		}
		
		try
		{
			Files.deleteIfExists(givenFilePath);
			Files.write(givenFilePath, line.getBytes());
		}
		catch(IOException e)
		{
			log.error(logHeader + " error writing average delay to the requested file: ", e);
			return false;
		}
		
		return true;
	}

}
