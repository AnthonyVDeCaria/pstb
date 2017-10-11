/**
 * 
 */
package pstb.util.diary;

import java.util.HashMap;

import pstb.util.ClientAction;
import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 *
 */
public class DiaryEntry {
	HashMap<DiaryHeader, String> page;
	
	/**
	 * The allowed headers for this Diary Entry
	 */
	private enum DiaryHeader {
		ClientAction, 
		TimeStartedAction, TimeBrokerAck, AckDelay, 
		Attributes, PayloadSize, 
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
	 * Client Action setter
	 * @param givenCA - the Client Action to add
	 */
	public void addClientAction(ClientAction givenCA)
	{
		page.put(DiaryHeader.ClientAction, givenCA.toString());
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
	
	public ClientAction getClientAction()
	{
		String storedClientAction = page.get(DiaryHeader.ClientAction);
		return ClientAction.valueOf(storedClientAction);
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
	
	public void printPage()
	{
		page.forEach((header, notes)->{
			System.out.println(header.toString() + ": " + notes);
		});
	}

}
