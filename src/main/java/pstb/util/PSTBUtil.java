/**
 * @author padres-dev-4187
 *
 * A collection of useful functions.
 */
package pstb.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import pstb.analysis.diary.ClientDiary;
import pstb.analysis.diary.DiaryHeader;
import pstb.analysis.diary.DistributedFlagValue;
import pstb.benchmark.process.broker.PSTBBrokerProcess;
import pstb.benchmark.process.client.PSTBClientProcess;
import pstb.startup.config.AttributeRatio;
import pstb.startup.config.ExperimentType;
import pstb.startup.config.MessageSize;
import pstb.startup.config.NetworkProtocol;
import pstb.startup.config.NumAttribute;

public class PSTBUtil {
	public static final Long MIN_TO_NANOSEC = 60000000000L;
	public static final Long SEC_TO_NANOSEC = 1000000000L;
	public static final Long SEC_TO_MILLISEC = 1000L;
	public static final Long MILLISEC_TO_NANOSEC = 1000000L;
	
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyy-MM-dd-HH:mm:ss.SSS");
	public static final String DATE_REGEX = "\\d{4}-\\d{2}-\\d{2}-\\d{2}:\\d{2}:\\d{2}.\\d{3}";
	public static final String BENCHMARK_NUMBER_REGEX = "[a-zA-Z0-9+-]{5}";
	
	public static final char[] ALPHABET = { '0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'A', 'B', 'C', 'D', 'E', 'F',
			'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
			'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
			'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd',
			'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
			'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
			'u', 'v', 'w', 'x', 'y', 'z', '+', '-'};
    public static final int ALPHABET_LEN = ALPHABET.length;
	
	public static final String COLUMN_SEPARATOR = "	";
	public static final String ITEM_SEPARATOR = ",";
	public static final String CONTEXT_SEPARATOR = "_";
	public static final String FOLDER_REPLACEMENT = "-";
	
	public static final String LOCAL = "localhost";
	public static final String MASTER = "TPMaster";
	
	public static final Integer PORT = 4444;
	
	public static final String INIT = "On your command.";
	public static final String START = "start";
	public static final String LINK = "connect";
	
	public static final String ERROR = "ERROR!";
	public static final String STOP = "STOP!";
	
	
	/**
	 * Sees if a given String is an Integer
	 * 
	 * @param s - the String to look at
	 * @param log - a boolean that determines if we should log or not
	 * @return null if the String is an Integer; the value otherwise
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
				logger.error("isInt: " + s + " is not an integer: ", ex);
			}
		}
		
		if(num != null && log)
		{
			logger.debug("isInt: " + s + " is an integer.");
		}
		
		return num;
	}
	
	/**
	 * Sees if a given String is a Long
	 * 
	 * @param s - the string to look at
	 * @param log - a boolean that determines if we should log or not
	 * @return null if the String is a Long; the value otherwise
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
				logger.error("isLong: " + s + " is not a long: ", ex);
			}
		}
		
		if(num != null && log)
		{
			logger.debug("isLong: " + s + " is a long.");
		}
		
		return num;
	}
	
	/**
	 * Sees if a given String is a Double
	 * 
	 * @param s - the String to look at
	 * @param log - a boolean that determines if we should log or not
	 * @return null if the String is a Long; the value otherwise
	 */
	public static Double checkIfDouble(String s, boolean log, Logger logger) 
	{
		Double num = null;
		try
		{
			num = Double.parseDouble(s);
		}
		catch (NumberFormatException ex)
		{
			if(log)
			{
				logger.error("isDouble: " + s + " is not a double: ", ex);
			}
		}
		
		if(num != null && log)
		{
			logger.debug("isLong: " + s + " is a double.");
		}
		
		return num;
	}
	
	/**
	 * Turns a String[] into a ArrayList<String>
	 * 
	 * @param input - the String[]
	 * @return the resulting ArrayList<String>
	 */
	public static ArrayList<String> turnStringArrayIntoArrayListString(String[] input)
	{
		return new ArrayList<String>(Arrays.asList(input));
	}
	
	/**
	 * Sees if an int is within a certain range
	 * @param testVariable - the integer being tested
	 * @param lowerBound - the lower bound
	 * @param upperBound - the upper bound
	 * @return true if it is; false otherwise
	 */
	public static boolean isWithinRangeInclusive(int testVariable, int lowerBound, int upperBound)
	{
		return (testVariable >= lowerBound) && (testVariable <= upperBound);
	}
	
	/**
	 * Gets the file extension of a given File String.
	 * 
	 * @param givenFileString - the File String to get the extension from
	 * @param receiveDot - returns the dot with the extension
	 * @return the extension
	 */
	public static String getFileExtension(String givenFileString, boolean receiveDot)
	{
		int dot = givenFileString.lastIndexOf(".");
		if(receiveDot)
		{
		    return givenFileString.substring(dot);
		}
		else
	    {
		    return givenFileString.substring(dot + 1);
	    }
	}
	
	/**
	 * Serializes the given Object and stores it in a file
	 * Allowing the processes after to access these objects and their functions
	 * @see PSTBBrokerProcess
	 * @see PSTBClientProcess
	 * 
	 * @param givenObject - the Object to be stored in a file
	 * @param out - the OutputStream to write the object to
	 * @param logger - the Logger to record errors
	 * @param logHeader - the header to put when logging
	 * @return false on error; true if successful
	 */
	public static boolean sendObject(Object givenObject, OutputStream out, Logger logger, String logHeader)
	{
		try 
		{
			ObjectOutputStream oOut = new ObjectOutputStream(out);
			oOut.writeObject(givenObject);
		}
		catch (Exception e) 
		{
			logger.error(logHeader + "error with ObjectOutputStream ", e);
			return false;
		}
		
		return true;
	}
	
	public static String readConnection(Socket connection, Logger log, String logHeader)
	{
		String inputLine = new String();
		try 
		{
			BufferedReader bufferedIn = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			
			if ((inputLine = bufferedIn.readLine()) != null) 
			{
				return inputLine;
            }
		}
		catch (IOException e) 
		{
			log.error(logHeader + "Couldn't get a BufferedReader/InputStreamReader onto the Socket: ", e);
		}
		
		return null; 
	}
	
	/**
	 * Deserializes a ClientDiary object
	 * 
	 * @param diaryName - the name of this diary
	 * @return null on failure; the requested diary otherwise
	 */
	public static ClientDiary readDiaryObject(InputStream givenIS, Logger log, String logHeader)
	{
		ClientDiary diaryI = null;
		try
		{
			ObjectInputStream oISIn = new ObjectInputStream(givenIS);
			diaryI = (ClientDiary) oISIn.readObject();
			oISIn.close();
		}
		catch (IOException e)
		{
			log.error(logHeader + "error accessing ObjectInputStream ", e);
			return null;
		}
		catch(ClassNotFoundException e)
		{
			log.error(logHeader + "can't find class ", e);
			return null;
		}
		
		return diaryI;
	}
	
	public static boolean sendStringAcrossSocket(OutputStream givenOutputStream, String givenString)
	{
		PrintWriter out = new PrintWriter(givenOutputStream, true);
		out.println(givenString);
		
		return true;
	}
	
	/**
	 * Determines if the given line can be ignored
	 * - i.e. its blank, or starts with a #
	 * 
	 * @param fileline - the line from the file
	 * @return true if it can be ignored; false if it can't
	 */
	public static boolean checkIfLineIgnorable(String fileline)
	{
		boolean isLineIgnorable = false;
		if(fileline.length() == 0 || fileline.startsWith("#"))
		{
			isLineIgnorable = true;
		}
		return isLineIgnorable;
	}
	
	/**
	 * @author padres-dev-4187
	 * 
	 * Acceptable createTimeString units
	 */
	public enum TimeType{
		Nano, Milli
	}
	
	/**
	 * Given a time value and what the original units are 
	 * generates an associated string.
	 * Example 1000000000 Nano -> 1 sec
	 * 
	 * @param givenTimeValue - the time value to convert
	 * @param givenTT - the units of this time value
	 * @return the associated converted string
	 */
	public static String createTimeStringOLD(Long givenTimeValue, TimeType givenTT)
	{
		String retVal = null;
		Long hours = 0L;
		Long minutes = 0L;
		Long seconds = 0L;
		Long milliseconds = 0L;
		Long microseconds = 0L;
		Long nanoseconds = 0L;
		
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
					retVal = String.format("%02d h, %02d min", hours, hMinutes);
				}
				else if(hSeconds.compareTo(0L) > 0)
				{
					retVal = String.format("%02d h, %02d sec", hours, hSeconds);
				}
				else if(hMillis.compareTo(0L) > 0)
				{
					retVal = String.format("%02d h, %02d msec", hours, hMillis);
				}
				else if(hMicros.compareTo(0L) > 0)
				{
					retVal = String.format("%02d h, %02d usec", hours, hMicros);
				}
				else if(hNanos.compareTo(0L) > 0)
				{
					retVal = String.format("%02d h, %02d nsec", hours, hNanos);
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
					retVal = String.format("%02d min, %02d sec", minutes, mSeconds);
				}
				else if(mMillis.compareTo(0L) > 0)
				{
					retVal = String.format("%02d min, %02d msec", minutes, mMillis);
				}
				else if(mMicros.compareTo(0L) > 0)
				{
					retVal = String.format("%02d min, %02d usec", minutes, mMicros);
				}
				else if(mNanos.compareTo(0L) > 0)
				{
					retVal = String.format("%02d min, %02d nsec", minutes, mNanos);
				}
				else
				{
					retVal = String.format("%02d min", minutes);
				}
			}
			else if(seconds.compareTo(0L) > 0)
			{
				Long sMillis = milliseconds - TimeUnit.SECONDS.toMillis(seconds);
				Long sMicros = microseconds - TimeUnit.SECONDS.toMicros(seconds);
				Long sNanos = nanoseconds - TimeUnit.SECONDS.toNanos(seconds);
				
				if(sMillis.compareTo(0L) > 0)
				{
					retVal = String.format("%02d sec, %02d msec", seconds, sMillis);
				}
				else if(sMicros.compareTo(0L) > 0)
				{
					retVal = String.format("%02d sec, %02d usec", seconds, sMicros);
				}
				else if(sNanos.compareTo(0L) > 0)
				{
					retVal = String.format("%02d sec, %02d nsec", seconds, sNanos);
				}
				else
				{
					retVal = String.format("%02d sec", seconds);
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
		else if(givenTT.equals(TimeType.Milli))
		{
			hours = TimeUnit.MILLISECONDS.toHours(givenTimeValue);
			minutes = TimeUnit.MILLISECONDS.toMinutes(givenTimeValue);
			seconds = TimeUnit.MILLISECONDS.toSeconds(givenTimeValue);
			milliseconds = TimeUnit.MILLISECONDS.toMillis(givenTimeValue);
			
			if(hours.compareTo(0L) > 0)
			{
				Long hMinutes = minutes - TimeUnit.HOURS.toMinutes(hours);
				Long hSeconds = seconds - TimeUnit.HOURS.toSeconds(hours);
				Long hMillis = milliseconds - TimeUnit.HOURS.toMillis(hours);
				
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
				else
				{
					retVal = String.format("%02d h", hours);
				}
			}
			else if(minutes.compareTo(0L) > 0)
			{
				Long mSeconds = seconds - TimeUnit.MINUTES.toSeconds(minutes);
				Long mMillis = milliseconds - TimeUnit.MINUTES.toMillis(minutes);
				
				if(mSeconds.compareTo(0L) > 0)
				{
					retVal = String.format("%02d m, %02d s", minutes, mSeconds);
				}
				else if(mMillis.compareTo(0L) > 0)
				{
					retVal = String.format("%02d m, %02d ms", minutes, mMillis);
				}
				else
				{
					retVal = String.format("%02d m", minutes);
				}
			}
			else if(seconds.compareTo(0L) > 0)
			{
				Long sMillis = milliseconds - TimeUnit.SECONDS.toMillis(seconds);
				
				if(sMillis.compareTo(0L) > 0)
				{
					retVal = String.format("%02d s, %02d ms", seconds, sMillis);
				}
				else
				{
					retVal = String.format("%02d s", seconds);
				}
			}
			else
			{
				retVal = String.format("%02d ms", milliseconds);
			}
		}
		
		return retVal;
	}
	
	public static String createTimeString(Long givenTimeValue, TimeType givenTT, TimeUnit units)
	{
		String retVal = null;
		Long seconds = 0L;
		Long milliseconds = 0L;
		Long microseconds = 0L;
		Long nanoseconds = 0L;
		
		if(units.equals(TimeUnit.SECONDS) || units.equals(TimeUnit.MILLISECONDS) 
				|| units.equals(TimeUnit.MICROSECONDS) || units.equals(TimeUnit.NANOSECONDS))
		{
			if(givenTT.equals(TimeType.Nano))
			{
				seconds = TimeUnit.NANOSECONDS.toSeconds(givenTimeValue);
				milliseconds = TimeUnit.NANOSECONDS.toMillis(givenTimeValue);
				microseconds = TimeUnit.NANOSECONDS.toMicros(givenTimeValue);
				nanoseconds = TimeUnit.NANOSECONDS.toNanos(givenTimeValue);
				
				if(units.equals(TimeUnit.SECONDS))
				{
					Long sMillis = milliseconds - TimeUnit.SECONDS.toMillis(seconds);
					
					if(sMillis.compareTo(0L) > 0)
					{
						retVal = String.format("%02d.%03d sec", seconds, sMillis);
					}
					else
					{
						retVal = String.format("%02d sec", seconds);
					}
				}
				else if(units.equals(TimeUnit.MILLISECONDS))
				{
					Long miMicros = microseconds - TimeUnit.MILLISECONDS.toMicros(milliseconds);
					
					if(miMicros.compareTo(0L) > 0)
					{
						retVal = String.format("%02d.%03d msec", milliseconds, miMicros);
					}
					else
					{
						retVal = String.format("%02d msec", milliseconds);
					}
				}
				else if(units.equals(TimeUnit.MICROSECONDS) )
				{
					Long muNanos = nanoseconds - TimeUnit.MICROSECONDS.toNanos(microseconds);
					
					if(muNanos.compareTo(0L) > 0)
					{
						retVal = String.format("%02d.%03d usec", microseconds, muNanos);
					}
					else
					{
						retVal = String.format("%02d usec", microseconds);
					}
				}
				else
				{
					retVal = String.format("%02d nsec", nanoseconds);
				}
			}
			else if(givenTT.equals(TimeType.Milli))
			{
				seconds = TimeUnit.MILLISECONDS.toSeconds(givenTimeValue);
				milliseconds = TimeUnit.MILLISECONDS.toMillis(givenTimeValue);
				
				if(units.equals(TimeUnit.SECONDS) )
				{
					Long sMillis = milliseconds - TimeUnit.SECONDS.toMillis(seconds);
					
					if(sMillis.compareTo(0L) > 0)
					{
						retVal = String.format("%02d.%03d sec", seconds, sMillis);
					}
					else
					{
						retVal = String.format("%02d sec", seconds);
					}
				}
				else
				{
					retVal = String.format("%02d msec", milliseconds);
				}
			}
		}
		
		return retVal;
	}
	
	/**
	 * Cleans a given TopologyFileString
	 * by replacing / and \ with _.
	 * 
	 * @param givenTFS - the Topology File String to clean
	 * @return the cleaned topology file string
	 */
	public static String cleanTFS(String givenTFS)
	{
		return givenTFS.replace("/", FOLDER_REPLACEMENT).replace("\\", FOLDER_REPLACEMENT).replaceAll("\\.\\w+", "");
	}
	
	/**
	 * Breaks a space filled string into multiple components and adds it to an ArrayList<String>
	 * 
	 * @param input - the string to break up
	 * @param finalArray - the ArrayList<String> to add everything to
	 * @param startLocation - the location in the array to add this broker string to 
	 * If it's longer than the ArrayList, the broken strings will be added to the end of the ArrayList
	 * @return the array location ended on
	 */
	public static int breakStringAndAddToArrayList(String input, ArrayList<String> finalArray, int startLocation)
	{
		int i = 0;
		String[] temp = input.split("\\s+");
		
		if(startLocation >= finalArray.size())
		{
			for( ; i < temp.length ; i++)
			{
				finalArray.add(temp[i]);
			}
		}
		else
		{
			for( ; i < temp.length ; i++)
			{
				finalArray.add(startLocation + i, temp[i]);
			}
		}
		
		return i;
	}
	
	public static Boolean createANewProcess(String[] command, Logger log, boolean seeProcess, boolean seeMessages, 
			String newProcessException, String processSuccessful, String processFailure)
	{
		ProcessBuilder newProcess = new ProcessBuilder(command);
		
		newProcess.redirectErrorStream(true);
		
		Process pNewProcess = null;
		try 
		{
			pNewProcess = newProcess.start();
		} 
		catch (IOException e) 
		{
			log.error(newProcessException, e);
			return null;
		}
		
		if(seeProcess)
		{
			BufferedReader pNewProcessReader = new BufferedReader(new InputStreamReader(pNewProcess.getInputStream()));
			String line = null;
			try
			{
				while( (line = pNewProcessReader.readLine()) != null)
				{
					log.info(line);
				}
			}
			catch (IOException e)
			{
				log.error("Couldn't read output from new Process: ", e);
				return null;
			}
		}
		
		try 
		{
			pNewProcess.waitFor();
		} 
		catch (InterruptedException e)
		{
			log.error("Couldn't end new process: ", e);
			return null;
		}
		
		int exitValue = pNewProcess.exitValue();
		
		if(exitValue != 0)
		{
			if(seeMessages)
			{
				log.error(processFailure + " | Error = " + exitValue);
			}
			return false;
		}
		else
		{
			if(seeMessages)
			{
				log.info(processSuccessful);
			}
			return true;
		}
	}
	
	public static void synchronizeRun()
	{
		Calendar cal = Calendar.getInstance();
		int seconds = -1;
		
		while(seconds != 00 && seconds != 15 && seconds != 30 && seconds != 45)
		{
			cal.setTimeInMillis(System.currentTimeMillis());
			seconds = cal.get(Calendar.SECOND);
		}
	}
	
	public static <K, V extends Comparable<? super V>> Map<K, V> sortGivenMapByValue(Map<K, V> map) {
		return map.entrySet()
	    		.stream()
	    		.sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
	    		.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}
	
	public static boolean createFolder(Path givenFolderPath, Logger givenLogger, String givenLogHeader)
	{
		if(Files.notExists(givenFolderPath))
		{
			try 
			{
				Files.createDirectories(givenFolderPath);
			} 
			catch (IOException e) 
			{
				givenLogger.error(givenLogHeader + "Couldn't create requested folder: ", e);
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Returns a random String from a String[] from the Broker ("B") group
	 * 
	 * @param givenStrings - the given strings
	 * @returns the selected string
	 */
	public static String randomlySelectString(String[] givenStrings)
	{
		Random generator = new Random();
		int numStrings = givenStrings.length;
		int i = generator.nextInt(numStrings);
		return givenStrings[i];
	}
	
	public static void waitAPeriod(long periodToWait, Logger log, String logHeader)
	{
		String convertedPeriod = createTimeString(periodToWait, TimeType.Nano, TimeUnit.SECONDS);
		
		log.debug(logHeader + "Pausing for " + convertedPeriod + "...");
		long endTime = System.nanoTime() + periodToWait;
		while(endTime > System.nanoTime())
		{
			// Nothing
		}
		log.debug(logHeader + "Pause complete.");
	}
	
	public static String generateContext(Boolean distributed, String benchmarkStartTime, String topologyFileString, 
			NetworkProtocol protocol, ExperimentType mode, Long runLength, Integer runNumber, Long periodLength,
			AttributeRatio ar, NumAttribute na, MessageSize ms, String name,
			Logger nodeLog, String logHeader)
	{
		// Convert the distributed value into a flag
		DistributedFlagValue distributedFlag = null;
		if(distributed.booleanValue() == true)
		{
			distributedFlag = DistributedFlagValue.D;
		}
		else if(distributed.booleanValue() == false)
		{
			distributedFlag = DistributedFlagValue.L;
		}
		else
		{
			nodeLog.error(logHeader + "error with distributed");
			return null;
		}
		
		ArrayList<String> context = new ArrayList<String>();
		context.add(benchmarkStartTime);
		context.add(topologyFileString);
		context.add(distributedFlag.toString());
		context.add(protocol.toString());
		
		if(mode.equals(ExperimentType.Scenario))
		{
			// Convert the nanosecond runLength into milliseconds
			// WHY: neatness / it's what the user gave us->why confuse them?
			Long milliRunLength = (long) (runLength / PSTBUtil.MILLISEC_TO_NANOSEC.doubleValue());
			
			context.add(milliRunLength.toString());
			context.add(runNumber.toString());
		}
		else if(mode.equals(ExperimentType.Throughput))
		{
			// Convert the nanosecond runLength into milliseconds
			// WHY: neatness / it's what the user gave us->why confuse them?
			Long convertedPL = (long) (periodLength / PSTBUtil.MILLISEC_TO_NANOSEC.doubleValue());
			
			context.add(convertedPL.toString());
			context.add(ms.toString());
			context.add(na.toString());
			context.add(ar.toString());
		}
		else
		{
			return null;
		}
		
		context.add(name);
		
		String retVal = String.join(PSTBUtil.CONTEXT_SEPARATOR, context);
		return retVal;
	}

	public static boolean isDHThroughput(DiaryHeader givenDH) {
		return givenDH.equals(DiaryHeader.CurrentThroughput) 
				|| givenDH.equals(DiaryHeader.AverageThroughput)
				|| givenDH.equals(DiaryHeader.RoundLatency)
				|| givenDH.equals(DiaryHeader.Secant)
				|| givenDH.equals(DiaryHeader.CurrentRatio)
				|| givenDH.equals(DiaryHeader.FinalThroughput);
	}
	
	public static boolean isDHThroughputGraphable(DiaryHeader givenDH) {
		return givenDH.equals(DiaryHeader.CurrentThroughput) 
				|| givenDH.equals(DiaryHeader.AverageThroughput)
				|| givenDH.equals(DiaryHeader.RoundLatency)
				|| givenDH.equals(DiaryHeader.Secant)
				|| givenDH.equals(DiaryHeader.CurrentRatio);
	}

	public static String encode(int victim)
	{
		final List<Character> list = new ArrayList<>();
	
		do 
		{
			list.add(ALPHABET[victim % ALPHABET_LEN]);
			victim /= ALPHABET_LEN;
		}
		while (victim > 0);
		
		Collections.reverse(list);
		String retVal = list.stream()
				.map(k -> String.valueOf(k))
				.collect(Collectors.joining("", "", ""));
		return retVal;
	}
}
