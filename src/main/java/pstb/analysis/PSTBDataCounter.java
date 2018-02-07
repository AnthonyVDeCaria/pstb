/**
 * 
 */
package pstb.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.Logger;

import pstb.startup.workload.PSActionType;
import pstb.util.PSTBUtil;
import pstb.util.PSTBUtil.TimeType;

/**
 * @author padres-dev-4187
 * 
 * Extends the Frequency Counter class to allow increased functionality crucial to PSTB
 * Such as what PSActionType we are is recording, 
 * as well as identifying units when recording into a file.
 * @see FrequencyCounter
 */
public class PSTBDataCounter {
	private String name;
	private PSActionType type;
	private TreeMap<Long, Integer> frequency;
	
	private final String logHeader = "PSTBDC: ";
	
	/**
	 * Empty Constructor
	 */
	public PSTBDataCounter()
	{
		frequency = new TreeMap<Long, Integer>();
		name = null;
		type = null;
	}
	
	/**
	 * Add one occurrence of the value dataPoint.
	 * 
	 * @param dataPoint - the data point to add
	 */
	public void addOccurrence(Long dataPoint)
	{
		if(frequency.containsKey(dataPoint))
		{
			Integer numOccurances = frequency.get(dataPoint);
			numOccurances++;
			frequency.put(dataPoint, numOccurances);
		}
		else
		{
			frequency.put(dataPoint, 1);
		}
	}
	
	/**
	 * Gets the occurrences of a certain dataPoint
	 * 
	 * @param dataPoint - the data point requested
	 * @return the number of occurrences
	 */
	public Integer getOccurrences(Long dataPoint)
	{
		return frequency.get(dataPoint);
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
	 * Writes this PSTBDataCounter into a file.
	 * 
	 * @param givenFilePath - the Path we are to write to
	 * @param log - the Logger file we should use if there are errors
	 * @param byValue - do we want this data written by value as opposed to by key?
	 * @return false on failure; true otherwise
	 */
	public boolean recordPSTBDC(Path givenFilePath, Logger log, boolean byValue)
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
		catch(IOException e)
		{
			log.error(logHeader + "Couldn't recreate file " + givenFilePath + ": ", e);
			return false;
		}
		
		Long[] sortedTimes = null;
		if(byValue)
		{
			Map<Long, Integer> sortedFrequency = PSTBUtil.sortGivenMapByValue(frequency);
			sortedTimes = sortedFrequency.keySet().toArray(new Long[sortedFrequency.size()]);
		}
		else
		{
			sortedTimes = frequency.keySet().toArray(new Long[frequency.size()]);
		}
		
		for(int i = 0 ; i < sortedTimes.length ; i++)
		{
			Long timeI = sortedTimes[i];
			Integer frequencyI = frequency.get(timeI);	
			
			String convertedTimeI = null;
			if(type.equals(PSActionType.R))
			{
				convertedTimeI = PSTBUtil.createTimeString(timeI, TimeType.Milli);
			}
			else
			{
				convertedTimeI = PSTBUtil.createTimeString(timeI, TimeType.Nano);
			}
			
			String lineI = convertedTimeI + "	" + "(" + timeI + ")" + "	" + "occurred" + " " + frequencyI + "\n";
			try
			{
				Files.write(givenFilePath, lineI.getBytes(), StandardOpenOption.APPEND);
			}
			catch(IOException e)
			{
				log.error(logHeader + "Error writing dataPoint " + timeI + " with occurance " + frequencyI + ": ", e);
				return false;
			}
		}
		
		return true;
	}

}
