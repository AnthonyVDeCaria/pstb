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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;

import pstb.benchmark.PhysicalBroker;
import pstb.benchmark.PhysicalClient;

public class PSTBUtil {
	public static final Long MIN_TO_NANOSEC = new Long(60000000000L);
	public static final Long MILLISEC_TO_NANOSEC = new Long(1000000L);
	
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");    
	
	public static final String SPACE = " ";
	public static final String COMMA = ",";
	
	/**
	 * Sees if a given string is an Integer
	 * @param s - the string to look at
	 * @param log - a boolean that determines if we should log or not
	 * @return null if the string is an Integer; the value otherwise
	 */
	public static Integer checkIfInteger(String s, boolean log, Logger logger) 
	{
		Integer num = null;
		try
		{
			num = Integer.parseInt(s);
		}
		catch (NumberFormatException ex)
		{
			if(log)
			{
				logger.error("isInt: " + s + " is not an integer", ex);
			}
		}
		
		if(num != null && log)
		{
			logger.debug("isInt: " + s + " is an integer");
		}
		
		return num;
	}
	
	/**
	 * Sees if a given string is a Long
	 * @param s - the string to look at
	 * @param log - a boolean that determines if we should log or not
	 * @return null if the string is a Long; the value otherwise
	 */
	public static Long checkIfLong(String s, boolean log, Logger logger) 
	{
		Long num = null;
		try
		{
			num = Long.parseLong(s);
		}
		catch (NumberFormatException ex)
		{
			if(log)
			{
				logger.error("isLong: " + s + " is not a long", ex);
			}
		}
		
		if(num != null && log)
		{
			logger.debug("isLong: " + s + " is a long");
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
	public static boolean createObjectFile(Object givenObject, String givenObjectName, String fileExtension, 
												Logger logger, String logHeader)
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
	
	public enum TimeType{
		Nano
	}
	
	public static String createTimeString(Long givenTimeValue, TimeType givenTT)
	{
		String retVal = null;
		Long hours = new Long(0);
		Long minutes = new Long(0);
		Long seconds = new Long(0);
		Long milliseconds = new Long(0);
		Long microseconds = new Long(0);
		Long nanoseconds = new Long(0);
		
		if(givenTT.equals(TimeType.Nano))
		{
			hours = TimeUnit.NANOSECONDS.toHours(givenTimeValue);
			minutes = TimeUnit.NANOSECONDS.toMinutes(givenTimeValue);
			seconds = TimeUnit.NANOSECONDS.toSeconds(givenTimeValue);
			milliseconds = TimeUnit.NANOSECONDS.toMillis(givenTimeValue);
			microseconds = TimeUnit.NANOSECONDS.toMicros(givenTimeValue);
			nanoseconds = TimeUnit.NANOSECONDS.toNanos(givenTimeValue);
			
			if(hours.compareTo(0L) > 0)
			{
				Long hMinutes = minutes - TimeUnit.HOURS.toMinutes(hours);
				Long hSeconds = seconds - TimeUnit.HOURS.toSeconds(hours);
				Long hMillis = milliseconds - TimeUnit.HOURS.toMillis(hours);
				Long hMicros = microseconds - TimeUnit.HOURS.toMicros(hours);
				Long hNanos = nanoseconds - TimeUnit.HOURS.toNanos(hours);
				
				if(hMinutes.compareTo(0L) > 0)
				{
					retVal = String.format("%02d h, %02d m", hours, hMinutes);
				}
				else if(hSeconds.compareTo(0L) > 0)
				{
					retVal = String.format("%02d h, %02d s", hours, hSeconds);
				}
				else if(hMillis.compareTo(0L) > 0)
				{
					retVal = String.format("%02d h, %02d ms", hours, hMillis);
				}
				else if(hMicros.compareTo(0L) > 0)
				{
					retVal = String.format("%02d h, %02d us", hours, hMicros);
				}
				else if(hNanos.compareTo(0L) > 0)
				{
					retVal = String.format("%02d h, %02d ns", hours, hNanos);
				}
				else
				{
					retVal = String.format("%02d h", hours);
				}
			}
			else if(minutes.compareTo(0L) > 0)
			{
				Long mSeconds = seconds - TimeUnit.MINUTES.toSeconds(minutes);
				Long mMillis = milliseconds - TimeUnit.MINUTES.toMillis(minutes);
				Long mMicros = microseconds - TimeUnit.MINUTES.toMicros(minutes);
				Long mNanos = nanoseconds - TimeUnit.MINUTES.toNanos(minutes);
				
				if(mSeconds.compareTo(0L) > 0)
				{
					retVal = String.format("%02d m, %02d s", minutes, mSeconds);
				}
				else if(mMillis.compareTo(0L) > 0)
				{
					retVal = String.format("%02d m, %02d ms", minutes, mMillis);
				}
				else if(mMicros.compareTo(0L) > 0)
				{
					retVal = String.format("%02d m, %02d us", minutes, mMicros);
				}
				else if(mNanos.compareTo(0L) > 0)
				{
					retVal = String.format("%02d m, %02d ns", minutes, mNanos);
				}
				else
				{
					retVal = String.format("%02d m", minutes);
				}
			}
			else if(seconds.compareTo(0L) > 0)
			{
				Long sMillis = milliseconds - TimeUnit.SECONDS.toMillis(seconds);
				Long sMicros = microseconds - TimeUnit.SECONDS.toMicros(seconds);
				Long sNanos = nanoseconds - TimeUnit.SECONDS.toNanos(seconds);
				
				if(sMillis.compareTo(0L) > 0)
				{
					retVal = String.format("%02d s, %02d ms", seconds, sMillis);
				}
				else if(sMicros.compareTo(0L) > 0)
				{
					retVal = String.format("%02d s, %02d us", seconds, sMicros);
				}
				else if(sNanos.compareTo(0L) > 0)
				{
					retVal = String.format("%02d s, %02d ns", seconds, sNanos);
				}
				else
				{
					retVal = String.format("%02d s", seconds);
				}
			}
			else if(milliseconds.compareTo(0L) > 0)
			{
				Long miMicros = microseconds - TimeUnit.MILLISECONDS.toMicros(milliseconds);
				Long miNanos = nanoseconds - TimeUnit.MILLISECONDS.toNanos(milliseconds);
				
				if(miMicros.compareTo(0L) > 0)
				{
					retVal = String.format("%02d ms, %02d us", milliseconds, miMicros);
				}
				else if(miNanos.compareTo(0L) > 0)
				{
					retVal = String.format("%02d ms, %02d ns", milliseconds, miNanos);
				}
				else
				{
					retVal = String.format("%02d ms", milliseconds);
				}
			}
			else if(microseconds.compareTo(0L) > 0)
			{
				Long muNanos = nanoseconds - TimeUnit.MICROSECONDS.toNanos(microseconds);
				
				if(muNanos.compareTo(0L) > 0)
				{
					retVal = String.format("%02d us, %02d ns", microseconds, muNanos);
				}
				else
				{
					retVal = String.format("%02d us", microseconds);
				}
			}
			else
			{
				retVal = String.format("%02d ns", nanoseconds);
			}
		}
//		else if(givenTT.equals(TimeType.Milli))
//		{
//			hours = TimeUnit.MILLISECONDS.toHours(givenTimeValue);
//			minutes = TimeUnit.MILLISECONDS.toMinutes(givenTimeValue);
//			seconds = TimeUnit.MILLISECONDS.toSeconds(givenTimeValue);
//			milliseconds = TimeUnit.MILLISECONDS.toMillis(givenTimeValue);
//		}
		
		return retVal;
	}
}
