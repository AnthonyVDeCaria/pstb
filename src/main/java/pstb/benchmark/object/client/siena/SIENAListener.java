/**
 * 
 */
package pstb.benchmark.object.client.siena;

import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import pstb.analysis.diary.ClientDiary;
import pstb.analysis.diary.DiaryEntry;
import pstb.benchmark.process.client.PSTBClientProcess;
import pstb.startup.workload.PSActionType;
import siena.Notifiable;
import siena.Notification;
import siena.SienaException;

/**
 * @author padres-dev-4187
 *
 */
public class SIENAListener implements Notifiable
{
	// Needed variables
	private ClientDiary diary;
	private ReentrantLock diaryLock;
	private String diaryName;
	
	// Logger
	private final String logHeader = "SListener: ";
	private final Logger clientLog = LogManager.getLogger(PSTBClientProcess.class);
	
	public SIENAListener(ClientDiary givenDiary, ReentrantLock givenLock, String givenDiaryName)
	{
		diary = givenDiary;
		diaryLock = givenLock;
		diaryName = givenDiaryName;
	}

	@Override
	public void notify(Notification arg0) throws SienaException {
		ThreadContext.put("client", diaryName);
		
		clientLog.debug("Received a new message...");
		
		Long currentTime = System.currentTimeMillis();
		DiaryEntry receivedMsg = new DiaryEntry();
		
		String attributes = arg0.toString();
		
		receivedMsg.addPSActionType(PSActionType.R);
		receivedMsg.addTimeReceived(currentTime);
		receivedMsg.addAttributes(attributes);
		
		try
		{
			diaryLock.lock();
			diary.addDiaryEntryToDiary(receivedMsg);
		}
		finally
		{
			diaryLock.unlock();
		}
		
		clientLog.debug(logHeader + "New publication received " + attributes + ".");
		System.out.println(attributes);
	}

	@Override
	public void notify(Notification[] arg0) throws SienaException {
		// TODO Implement patterns... maybe... I kinda doubt it.
	}
}
