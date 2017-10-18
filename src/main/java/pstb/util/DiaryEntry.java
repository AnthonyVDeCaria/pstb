package pstb.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * @author padres-dev-4187
 *
 */
public class DiaryEntry {
	HashMap<DiaryHeader, String> page;
	
	/**
	 * The allowed headers for this Diary Entry
	 */
	public enum DiaryHeader {
		PSActionType, 
		TimeStartedAction, TimeBrokerAck, AckDelay, 
		MessageID, Attributes, PayloadSize, 
		TimeActiveStarted, TimeActiveAck,
		TimeCreated, TimeReceived, TimeDifference
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
	 * @param givenPSAT - the PSActionType to add
	 */
	public void addPSActionType(PSActionType givenPSAT)
	{
		page.put(DiaryHeader.PSActionType, givenPSAT.toString());
	}
	
	/**
	 * Time Started Action setter
	 * @param givenTSA - the Time Started Action to add
	 */
	public void addTimeStartedAction(Long givenTSA)
	{
		page.put(DiaryHeader.TimeStartedAction, givenTSA.toString());
	}
	
	/**
	 * Time Broker Ack(nowledged) setter 
	 * @param givenTBA - the Time Broker Acknowledged to add
	 */
	public void addTimeBrokerAck(Long givenTBA)
	{
		page.put(DiaryHeader.TimeBrokerAck, givenTBA.toString());
	}
	
	/**
	 * Ack(nowledge) Delay setter
	 * @param givenAD - the Acknowledge Delay to add
	 */
	public void addAckDelay(Long givenAD)
	{
		page.put(DiaryHeader.AckDelay, givenAD.toString());
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
		page.put(DiaryHeader.TimeActiveAck, givenTAA.toString());
	}
	
	public void addTimeCreated(Long givenTC)
	{
		page.put(DiaryHeader.TimeCreated, givenTC.toString());
	}
	
	public void addTimeReceived(Long givenTR)
	{
		page.put(DiaryHeader.TimeReceived, givenTR.toString());
	}
	
	public void addTimeDifference(Long givenTD)
	{
		page.put(DiaryHeader.TimeDifference, givenTD.toString());
	}
	
	public PSActionType getPSActionType()
	{
		String storedPSActionType = page.get(DiaryHeader.PSActionType);
		return PSActionType.valueOf(storedPSActionType);
	}
	
	public Long getTimeStartedAction()
	{
		String storedTimeStartedAction = page.get(DiaryHeader.TimeStartedAction);
		return PSTBUtil.checkIfLong(storedTimeStartedAction, false);
	}
	
	public Long getTimeBrokerAck()
	{
		String storedTimeBrokerAck = page.get(DiaryHeader.TimeBrokerAck);
		return PSTBUtil.checkIfLong(storedTimeBrokerAck, false);
	}
	
	public Long getAckDelay()
	{
		String storedTimeAckDelay = page.get(DiaryHeader.AckDelay);
		return PSTBUtil.checkIfLong(storedTimeAckDelay, false);
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
		return PSTBUtil.checkIfInteger(storedPayloadSize, false);
	}
	
	public Long getTimeActiveStarted()
	{
		String storedTimeActiveStarted = page.get(DiaryHeader.TimeActiveStarted);
		return PSTBUtil.checkIfLong(storedTimeActiveStarted, false);
	}
	
	public Long getTimeActiveAck()
	{
		String storedTimeActiveAck = page.get(DiaryHeader.TimeActiveAck);
		return PSTBUtil.checkIfLong(storedTimeActiveAck, false);
	}
	
	public Long getTimeCreated()
	{
		String storedTimeCreated = page.get(DiaryHeader.TimeCreated);
		return PSTBUtil.checkIfLong(storedTimeCreated, false);
	}
	
	public Long getTimeReceived()
	{
		String storedTimeReceived = page.get(DiaryHeader.TimeReceived);
		return PSTBUtil.checkIfLong(storedTimeReceived, false);
	}
	
	public Long getTimeDifference()
	{
		String storedTimeDifference = page.get(DiaryHeader.TimeDifference);
		return PSTBUtil.checkIfLong(storedTimeDifference, false);
	}
	
	public Long getDelay(DiaryHeader delayType) 
	{
		if(delayType == DiaryHeader.AckDelay)
		{
			return getAckDelay();
		}
		else if(delayType == DiaryHeader.TimeDifference)
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
	
	public boolean printPage(Path givenFilePath)
	{
		try
		{
			page.forEach((header, data)->{
				String line = header.toString() + ": " + data;
				try
				{
					Files.write(givenFilePath, line.getBytes());
				}
				catch(IOException e)
				{
					throw new IllegalArgumentException("IO failed");
				}
			});
		}
		catch(IllegalArgumentException e)
		{
			return false;
		}
		
		return true;
	}
}
