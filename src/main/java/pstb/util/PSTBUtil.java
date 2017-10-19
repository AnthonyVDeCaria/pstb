/**
 * @author padres-dev-4187
 *
 * A collection of useful functions.
 */
package pstb.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;

import pstb.benchmark.PhysicalBroker;
import pstb.benchmark.PhysicalClient;

public class PSTBUtil {
	public static final Long MIN_TO_NANOSEC = new Long(60000000000L);
	public static final Long MILLISEC_TO_NANOSEC = new Long(1000000L);
	
	public static final String SPACE = " ";
	public static final String COMMA = ",";
	
	/**
	 * Sees if a given string is an Integer
	 * @param s - the string to look at
	 * @param logError - a boolean that determines if the error should be logged or not
	 * @return null if the string is an Integer; the value otherwise
	 */
	public static Integer checkIfInteger(String s, boolean logError, Logger logger) 
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
	public static Long checkIfLong(String s, boolean logError, Logger logger) 
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
	
	/**
	 * Serializes the given Object and stores it in a file
	 * Allowing the processes after to access these objects and their functions
	 * @see PhysicalBroker
	 * @see PhysicalClient
	 * 
	 * @param givenObject - the Object to be stored in a file
	 * @param givenObjectName - the name of said Object
	 * @return false on error; true if successful
	 */
	public static boolean createObjectFile(Object givenObject, String givenObjectName, String fileExtension, Logger logger, String logHeader)
	{
		boolean check = checkFileExtension(fileExtension, logger, logHeader);
		
		if(!check)
		{
			return false;
		}
		
		try 
		{
			FileOutputStream fileOut = new FileOutputStream("/tmp/" + givenObjectName + fileExtension);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(givenObject);
			out.close();
			fileOut.close();
		} 
		catch (FileNotFoundException e) 
		{
			logger.error(logHeader + "couldn't generate serialized client file ", e);
			return false;
		} 
		catch (IOException e) 
		{
			logger.error(logHeader + "error with ObjectOutputStream ", e);
			return false;
		}
		
		return true;
	}
	
	public static boolean checkFileExtension(String fileExtension, Logger logger, String logHeader)
	{
		// Check File Ending
		Pattern fileEndingTest = Pattern.compile(".\\w+");
		if(!fileEndingTest.matcher(fileExtension).matches())
		{
			logger.error(logHeader + "Improper fileExtension");
			return false;
		}
		return true;
	}
}
