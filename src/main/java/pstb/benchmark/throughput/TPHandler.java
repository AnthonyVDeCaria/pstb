/**
 * 
 */
package pstb.benchmark.throughput;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import pstb.creation.server.PSTBServer;
import pstb.util.PSTBUtil;

/**
 * @author adecaria
 *
 */
public class TPHandler extends Thread {
	private Socket objectPipe;
	private CountDownLatch delayAdded;
	private CountDownLatch masterReady;
	private CountDownLatch threadEnding;
	private ThroughputMaster masterOfPuppets;
	
	private String logHeader = "TPHandler: ";
	private Logger log = LogManager.getLogger(PSTBServer.class);

	public TPHandler(Socket givenOP, CountDownLatch givenDD, CountDownLatch givenMR, CountDownLatch givenTE, ThroughputMaster givenTM)
	{
		objectPipe = givenOP;
		delayAdded = givenDD;
		masterReady = givenMR;
		threadEnding = givenTE;
		masterOfPuppets = givenTM;
	}
	
	public void run()
	{
		String serverName = masterOfPuppets.getName();
		ThreadContext.put("server", serverName);
		Thread.currentThread().setName(serverName);
		
		OutputStream pipeOut = null;	
		try
		{
			pipeOut = objectPipe.getOutputStream();
		}
		catch (IOException e) 
		{
			log.error(logHeader + "Couldn't create output stream: ", e);
			endFailedThread(logHeader + "Couldn't create output stream: ", e, true);
		}
		
		try 
		{
			BufferedReader bufferedIn = new BufferedReader(new InputStreamReader(objectPipe.getInputStream()));
			String inputLine = new String();
			
			while ((inputLine = bufferedIn.readLine()) != null)
			{
				log.info(logHeader + "Received " + inputLine + ".");
				masterOfPuppets.addToSubDelays(inputLine);
				delayAdded.countDown();
				
				try
				{
					masterReady.await();
				}
				catch (InterruptedException e) 
				{
					endFailedThread(logHeader + "Couldn't create output stream: ", e, true);
				}
				
				if(masterOfPuppets.isExperimentRunning())
				{
					Long messageDelay = masterOfPuppets.getMessageDelay();
					while(messageDelay == null)
					{
						messageDelay = masterOfPuppets.getMessageDelay();
					}
					String messageDelayString = messageDelay.toString();
					log.info(logHeader + "Sending client " + inputLine + " message delay " + messageDelayString + "...");
					PSTBUtil.sendStringAcrossSocket(pipeOut, messageDelayString);
					log.debug(logHeader + "Delay sent to client " + inputLine + ".");
				}
				else
				{
					log.info(logHeader + "Telling client " + inputLine + " to stop...");
					PSTBUtil.sendStringAcrossSocket(pipeOut, PSTBUtil.STOP);
					log.debug(logHeader + "Told client " + inputLine + ".");
				}
				
				break;
			}
		}
		catch (IOException e) 
		{
			endFailedThread(logHeader + "Couldn't connect: ", e, true);
		}
		
		String inputLine = this.toString();
		log.debug(logHeader + "Ending connection with " + inputLine + "...");
		try 
		{
			objectPipe.close();
		} 
		catch (IOException e) 
		{
			endFailedThread(logHeader + "Couldn't get close the pipe OutputStream: ", e, true);
		}
		log.debug(logHeader + "Connection ended with " + inputLine + ".");
		
		threadEnding.countDown();
	}
	
	private void endFailedThread(String record, Exception givenException, boolean exceptionPresent)
	{
		if(exceptionPresent)
		{
			log.error(record, givenException);
		}
		else
		{
			log.error(record);
		}
		
		try 
		{
			objectPipe.close();
		} 
		catch (IOException eIO) 
		{
			log.error("Error closing objectPipe: ", eIO);
		}
		
		throw new RuntimeException(record, givenException);
	}
}
