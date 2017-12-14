package pstb.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.Logger;

/**
 * @author padres-dev-4187
 * 
 * A tool that can record the number of times a specific event occurs.
 * For example, it can keep track of how many trees in a forest are 10 feet tall, or 11 feet tall, etc.
 */
public class Histogram 
{
	private HashMap<Double, Integer> freq;   // freq.get(i) = # occurrences of value i
	private final String logHeader = "Histogram: ";

	/**
	 * 	Empty Constructor 
	 */
	public Histogram() 
	{
		freq = new HashMap<Double, Integer>();
	}

	/**
	 * Add one occurrence of the value dataPoint.
	 * 
	 * @param dataPoint - the data point to add
	 */
	public void addOccurrence(Double dataPoint)
	{
		if(freq.containsKey(dataPoint))
		{
			Integer numOccurances = freq.get(dataPoint);
			numOccurances++;
			freq.put(dataPoint, numOccurances);
		}
		else
		{
			freq.put(dataPoint, 1);
		}
	}
	
	/**
	 * Gets the occurrences of a certain dataPoint
	 * 
	 * @param dataPoint - the data point requested
	 * @return the number of occurrences
	 */
	public Integer getOccurrences(Double dataPoint)
	{
		return freq.get(dataPoint);
	}
	
	/**
	 * Writes the histogram into a file
	 * 
	 * @param givenFilePath - the Path of the file we're writing to
	 * @param log - the Logger we record to if there's an error
	 * @return false if there's a failure; true otherwise
	 */
	public boolean recordHistogram(Path givenFilePath, Logger log)
	{
		if(!Files.exists(givenFilePath))
		{
			try 
			{
				Files.createFile(givenFilePath);
			} 
			catch (IOException e) 
			{
				log.error("Couldn't create a new file for this Histogram: ", e);
				return false;
			}
		}
		
		try
		{
			freq.forEach((dataPoint, numOccurances)->{
				String line = dataPoint.toString() + " occurred " + numOccurances.toString() + "\n";
				try
				{
					Files.write(givenFilePath, line.getBytes(), StandardOpenOption.APPEND);
				}
				catch(IOException e)
				{
					throw new IllegalArgumentException("Error writing dataPoint " + dataPoint.toString() 
															+ " with occurance " + numOccurances.toString());
				}
			});
		}
		catch(IllegalArgumentException e)
		{
			log.error(logHeader, e);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Does a certain action on each entry of the Histogram
	 * (A port of the HashMap forEach)
	 */
	public void forEach(BiConsumer<? super Double, ? super Integer> action)
	{
		freq.forEach(action);
	}
}
