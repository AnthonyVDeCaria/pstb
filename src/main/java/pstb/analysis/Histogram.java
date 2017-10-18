package pstb.analysis;

import java.util.HashMap;
import java.util.function.BiConsumer;

/**
 * @author padres-dev-4187
 * 
 * Handles a histogram - a tool that can record the number of times a specific event occurs.
 * For example, how many trees in a forest are 10 feet tall.
 */
public class Histogram 
{
	private HashMap<Double, Integer> freq;   // freq.get(i) = # occurrences of value i

	/**
	 * 	Empty Constructor 
	 */
	public Histogram() 
	{
		freq = new HashMap<Double, Integer>();
	}

	/**
	 * Add one occurrence of the value dataPoint. 
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
	 * @param dataPoint - the data point requested
	 * @return the number of occurrences
	 */
	public Integer getOccurrences(Double dataPoint)
	{
		return freq.get(dataPoint);
	}
	
	/**
	 * Prints the histogram on the screen
	 */
	public void printHistogram()
	{
		freq.forEach((dataPoint, numOccurances)->{
			System.out.println(dataPoint.toString() + " occured " + numOccurances.toString());
		});
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
