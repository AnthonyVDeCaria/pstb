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
	
	/**
	 * Sees if a given string is an Integer
	 * @param s - the string to look at
	 * @param logError - a boolean that determines if the error should be logged or not
	 * @return true if the string is an Integer; false otherwise
	 */
	public static boolean isInteger(String s, boolean logError) 
	{
		boolean isValidInteger = false;
		try
		{
			Integer.parseInt(s);
			// s is a valid integer
			isValidInteger = true;
		}
		catch (NumberFormatException ex)
		{
			// s is not an integer
			if(logError)
			{
				logger.error("isInt: " + s + " is not an integer", ex);
			}
		}
		return isValidInteger;
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

}
