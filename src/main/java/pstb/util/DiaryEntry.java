package pstb.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;

import pstb.util.PSTBUtil.TimeType;

/**
 * @author padres-dev-4187
 *
 */
public class DiaryEntry  implements java.io.Serializable
{
	private static final long serialVersionUID = 1L;
	HashMap<DiaryHeader, String> page;
	
	private final String logHeader = "DiaryEntry: ";
	
	/**
	 * The allowed headers for this Diary Entry
	 */
	public enum DiaryHeader {
		PSActionType,
		TimeActionStarted, TimeBrokerFinished,
		StartedAction, EndedAction, ActionDelay,
		MessageID, Attributes, PayloadSize, 
		TimeActiveStarted, TimeActiveEnded,
		TimeMessageCreated, TimeMessageReceived, MessageDelay
	}
	
	/**
	 * Empty Constructor
	 */
	public DiaryEntry()
	{
		page = new HashMap<DiaryHeader, String>();
	}
	
	/**
	 * PSActionType setter
	 * 
	 * @param givenPSAT - the PSActionType to add
	 */
	public void addPSActionType(PSActionType givenPSAT)
	{
		page.put(DiaryHeader.PSActionType, givenPSAT.toString());
	}
	
	public void addTimeActionStarted(Long givenTAS)
	{
		page.put(DiaryHeader.TimeActionStarted, givenTAS.toString());
	}
	
	public void addTimeBrokerFinished(Long givenTBF)
	{
		page.put(DiaryHeader.TimeBrokerFinished, givenTBF.toString());
	}
	
	/**
	 * Started Action setter
	 * 
	 * @param givenSA - the Time Started Action to add
	 */
	public void addStartedAction(Long givenSA)
	{
		page.put(DiaryHeader.StartedAction, givenSA.toString());
	}
	
	/**
	 * Action Ended setter 
	 * 
	 * @param givenEA - the Action Ended to add
	 */
	public void addEndedAction(Long givenEA)
	{
		page.put(DiaryHeader.EndedAction, givenEA.toString());
	}
	
	/**
	 * Action Delay setter
	 * @param givenAD - the Action Delay to add
	 */
	public void addActionDelay(Long givenAD)
	{
		page.put(DiaryHeader.ActionDelay, givenAD.toString());
	}
	
	public void addMessageID(String givenMID)
	{
		page.put(DiaryHeader.MessageID, givenMID);
	}
	
	public void addAttributes(String givenA)
	{
		page.put(DiaryHeader.Attributes, givenA);
	}
	
	public void addPayloadSize(Integer givenPS)
	{
		page.put(DiaryHeader.PayloadSize, givenPS.toString());
	}
	
	public void addTimeActiveStarted(Long givenTAS)
	{
		page.put(DiaryHeader.TimeActiveStarted, givenTAS.toString());
	}
	
	public void addTimeActiveAck(Long givenTAA)
	{
		page.put(DiaryHeader.TimeActiveEnded, givenTAA.toString());
	}
	
	public void addTimeCreated(Long givenTC)
	{
		page.put(DiaryHeader.TimeMessageCreated, givenTC.toString());
	}
	
	public void addTimeReceived(Long givenTR)
	{
		page.put(DiaryHeader.TimeMessageReceived, givenTR.toString());
	}
	
	public void addTimeDifference(Long givenTD)
	{
		page.put(DiaryHeader.MessageDelay, givenTD.toString());
	}
	
	public PSActionType getPSActionType()
	{
		String storedPSActionType = page.get(DiaryHeader.PSActionType);
		return PSActionType.valueOf(storedPSActionType);
	}
	
	public Long getStartedAction()
	{
		String storedTimeStartedAction = page.get(DiaryHeader.StartedAction);
		return PSTBUtil.checkIfLong(storedTimeStartedAction, false, null);
	}
	
	public Long getEndedAction()
	{
		String storedTimeBrokerAck = page.get(DiaryHeader.EndedAction);
		return PSTBUtil.checkIfLong(storedTimeBrokerAck, false, null);
	}
	
	public Long getActionDelay()
	{
		String storedActionDelay = page.get(DiaryHeader.ActionDelay);
		return PSTBUtil.checkIfLong(storedActionDelay, false, null);
	}
	
	public String getMessageID()
	{
		return page.get(DiaryHeader.MessageID);
	}
	
	public String getAttributes()
	{
		return page.get(DiaryHeader.Attributes);
	}
	
	public Integer getPayloadSize()
	{
		String storedPayloadSize = page.get(DiaryHeader.PayloadSize);
		return PSTBUtil.checkIfInteger(storedPayloadSize, false, null);
	}
	
	public Long getTimeActiveStarted()
	{
		String storedTimeActiveStarted = page.get(DiaryHeader.TimeActiveStarted);
		return PSTBUtil.checkIfLong(storedTimeActiveStarted, false, null);
	}
	
	public Long getTimeActiveEnded()
	{
		String storedTimeActiveEnded = page.get(DiaryHeader.TimeActiveEnded);
		return PSTBUtil.checkIfLong(storedTimeActiveEnded, false, null);
	}
	
	public Long getTimeCreated()
	{
		String storedTimeCreated = page.get(DiaryHeader.TimeMessageCreated);
		return PSTBUtil.checkIfLong(storedTimeCreated, false, null);
	}
	
	public Long getTimeReceived()
	{
		String storedTimeReceived = page.get(DiaryHeader.TimeMessageReceived);
		return PSTBUtil.checkIfLong(storedTimeReceived, false, null);
	}
	
	public Long getTimeDifference()
	{
		String storedTimeDifference = page.get(DiaryHeader.MessageDelay);
		return PSTBUtil.checkIfLong(storedTimeDifference, false, null);
	}
	
	public Long getDelay(DiaryHeader delayType) 
	{
		if(delayType == DiaryHeader.ActionDelay)
		{
			return getActionDelay();
		}
		else if(delayType == DiaryHeader.MessageDelay)
		{
			return getTimeDifference();
		}
		else
		{
			return null;
		}
	}
	
	public boolean containsKey(Object value) 
	{
		return page.containsKey(value);
	}
	
	/**
	 * Combs the Entry looking for a value
	 * (Basically an extension of the HashMap<> containsValue()
	 * @param value
	 * @return true if it's in the Entry; false otherwise
	 */
	public boolean containsValue(Object value) 
	{
		return page.containsValue(value);
	}
	
	public boolean recordPage(Path givenFilePath, Logger log)
	{
		if(!Files.exists(givenFilePath))
		{
			try 
			{
				Files.createFile(givenFilePath);
			} 
			catch (IOException e) 
			{
				log.error(logHeader + "Couldn't create a new file for this DiaryEntry: ", e);
				return false;
			}
		}
		
		try
		{
			page.forEach((header, data)->{
				/*
				 *  If we're looking at TimeActionStarted or TimeBrokerFinished, don't record the number value
				 *  Instead, convert that number into a date
				 */
				if(header.equals(DiaryHeader.TimeActionStarted) || header.equals(DiaryHeader.TimeBrokerFinished))
				{
					Long convertedData = PSTBUtil.checkIfLong(data, false, null);
					if(convertedData != null)
					{
						data = PSTBUtil.DATE_FORMAT.format(convertedData);
					}
					else
					{
						throw new IllegalArgumentException("Converted data null for header" + header.toString() 
																+ " and data " + data);
					}
				}
				
				String line = header.toString() + ": " + data;
				try
				{
					Files.write(givenFilePath, line.getBytes(), StandardOpenOption.APPEND);
				}
				catch(IOException e)
				{
					throw new IllegalArgumentException("IO failed at line " + line);
				}
				
				/*
				 * If we're looking at a delay value or a time started/ended value,
				 * Then let's convert it from a long number into something meaningful.
				 * Except for TimeStartedAction or TimeEndedAction - these values don't mean anything.
				 * 
				 * Why have both the original value and the converted value?
				 * That way a user can double check that the values are accurate, if they wish.
				 */
				if(header.equals(DiaryHeader.ActionDelay) 
					|| header.equals(DiaryHeader.MessageDelay)
					|| header.equals(DiaryHeader.TimeActiveStarted)
					|| header.equals(DiaryHeader.TimeActiveEnded)
					|| header.equals(DiaryHeader.TimeMessageCreated) 
					|| header.equals(DiaryHeader.TimeMessageReceived)
					)
				{
					Long convertedData = PSTBUtil.checkIfLong(data, false, null);
					if(convertedData != null)
					{
						String formatted = null;
						
						// All of these are time stamps in nanoseconds
						if(header.equals(DiaryHeader.ActionDelay) 
								|| header.equals(DiaryHeader.TimeActiveStarted)
								|| header.equals(DiaryHeader.TimeActiveEnded)
								)
						{
							formatted = PSTBUtil.createTimeString(convertedData, TimeType.Nano);
						}
						// This time stamp is in milliseconds
						else if(header.equals(DiaryHeader.MessageDelay))
						{
							formatted = PSTBUtil.createTimeString(convertedData, TimeType.Milli);
						}
						// The other 2 (for now) are dates 
						else
						{
							formatted = PSTBUtil.DATE_FORMAT.format(convertedData);
						}
						
						line = " -> " + formatted;
						try
						{
							Files.write(givenFilePath, line.getBytes(), StandardOpenOption.APPEND);
						}
						catch(IOException e)
						{
							throw new IllegalArgumentException("IO failed to add time data at line " + line);
						}
					}
					else
					{
						throw new IllegalArgumentException("Converted data null at line " + line);
					}
				}
				
				line = "\n";
				try
				{
					Files.write(givenFilePath, line.getBytes(), StandardOpenOption.APPEND);
				}
				catch(IOException e)
				{
					throw new IllegalArgumentException("IO failed to add a newline at line " + line);
				}
			});
		}
		catch(IllegalArgumentException e)
		{
			log.error(logHeader + "Error writing to file: ", e);
			return false;
		}
		
		return true;
	}
}
