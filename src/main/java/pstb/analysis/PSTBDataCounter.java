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
public class PSTBDataCounter extends PSTBAnalysisObject{
	// Variables
	private TreeMap<Long, Integer> frequency;
	private boolean recordByKey;
	
	/**
	 * Empty Constructor
	 */
	public PSTBDataCounter()
	{
		super();
		frequency = new TreeMap<Long, Integer>();
		recordByKey = true;
		logHeader = "PSTBDC: ";
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
	 * Changes the recordByKey variable to false
	 */
	public void recordByValues()
	{
		recordByKey = false;
	}
	
	/**
	 * Changes the recordByKey variable to true
	 */
	public void recordByKeys()
	{
		recordByKey = true;
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
	
	@Override
	public boolean completeRecord(Path givenFilePath)
	{
		if(frequency.isEmpty())
		{
			log.error(logHeader + "No data exists to be printed!");
			return false;
		}
		
		Long[] sortedTimes = null;
		if(!recordByKey)
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

	@Override
	public void handleDataPoint(Long givenDataPoint) {
		addOccurrence(givenDataPoint);
	}
}
