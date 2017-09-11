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
	
	public static ArrayList<String> turnStringArrayIntoArrayListString(String[] input)
	{
		return new ArrayList<String>(Arrays.asList(input));
	}

}
