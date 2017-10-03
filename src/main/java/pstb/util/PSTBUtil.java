/**
 * @author padres-dev-4187
 *
 * A collection of useful functions.
 */
package pstb.util;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PSTBUtil {
	private static final Logger logger = LogManager.getRootLogger();
	
	public static final Long INFINITY = (long)999999999;
	
	public static final String SPACE = " ";
	public static final String COMMA = ",";
	
	/**
	 * Sees if a given string is an Integer
	 * @param s - the string to look at
	 * @param logError - a boolean that determines if the error should be logged or not
	 * @return null if the string is an Integer; the value otherwise
	 */
	public static Integer checkIfInteger(String s, boolean logError) 
	{
		Integer num = null;
		try
		{
			num = Integer.parseInt(s);
			// s is a valid integer
		}
		catch (NumberFormatException ex)
		{
			// s is not an integer
			if(logError)
			{
				logger.error("isInt: " + s + " is not an integer", ex);
			}
		}
		return num;
	}
	
	/**
	 * Sees if a given string is a Long
	 * @param s - the string to look at
	 * @param logError - a boolean that determines if the error should be logged or not
	 * @return null if the string is a Long; the value otherwise
	 */
	public static Long checkIfLong(String s, boolean logError) 
	{
		Long num = null;
		try
		{
			num = Long.parseLong(s);
			// s is a valid integer
		}
		catch (NumberFormatException ex)
		{
			// s is not an integer
			if(logError)
			{
				logger.error("isInt: " + s + " is not an integer", ex);
			}
		}
		return num;
	}
	
	/**
	 * Turns a String[] into a ArrayList<String>
	 * @param input - the String[]
	 * @return the resulting ArrayList<String>
	 */
	public static ArrayList<String> turnStringArrayIntoArrayListString(String[] input)
	{
		return new ArrayList<String>(Arrays.asList(input));
	}
	
	/**
	 * Sees if an int is within a certain bound
	 * @param lowerBound - the lower bound
	 * @param upperBound - the upper bound
	 * @param testVariable - the variable being testing
	 * @return true if it is; false otherwise
	 */
	public static boolean isWithinInclusiveBound(int lowerBound, int upperBound, int testVariable)
	{
		return (testVariable >= lowerBound) && (testVariable <= lowerBound);
	}

}
