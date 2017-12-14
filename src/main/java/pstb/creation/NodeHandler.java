/**
 * 
 */
package pstb.creation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 *
 */
public class NodeHandler extends Thread
{
	private Socket objectPipe;
	private PSTBServer master;
	private CountDownLatch sent;
	private CountDownLatch broker;
	private CountDownLatch start;
	
	private final String logHeader = "ObjectHandler: ";
	private final Logger log = LogManager.getLogger(PSTBServer.class);
	
	public NodeHandler(Socket givenPipe, CountDownLatch sentObjectSignal, CountDownLatch brokersCompleteSignal, CountDownLatch startSignal, 
							PSTBServer givenObjectServer)
	{
		objectPipe = givenPipe;
		master = givenObjectServer;
		sent = sentObjectSignal;
		broker = brokersCompleteSignal;
		start = startSignal;
	}
	
	public void run()
	{
		OutputStream pipeOut = null;	
		try
		{
			pipeOut = objectPipe.getOutputStream();
		}
		catch (IOException e) 
		{
			log.error(logHeader + "Couldn't create output stream: ", e);
			throw new RuntimeException(logHeader + "Couldn't create output stream: ", e);
		}
		
		boolean client = false;
		String nodeName = new String();
		try 
		{
			BufferedReader bufferedIn = new BufferedReader(new InputStreamReader(objectPipe.getInputStream()));
			String inputLine = new String();
			
			while ((inputLine = bufferedIn.readLine()) != null)
			{
				nodeName = inputLine;
				ServerPacket nodeObject = master.getNode(nodeName);
					
				if(nodeObject == null)
		        {
		        	PSTBUtil.sendStringAcrossSocket(pipeOut, PSTBUtil.ERROR, log, logHeader);
		        	endFailedThread(logHeader + "Client Process requested a name that doesn't exist!", null, false);
		        }
				
				client = nodeObject.isClient();
				
				if(client)
				{
					try 
					{
						broker.await();
					} 
					catch (InterruptedException e) 
					{
						endFailedThread(logHeader + "Interrupted waiting for brokers to complete signal: ", e, true);
					}
				}
				
				log.debug(logHeader + "Sending object data to node " + inputLine + ".");
				
				PSTBUtil.sendObject(nodeObject.getNode(), inputLine, pipeOut, log, logHeader);
				break;
			}
		} 
		catch (IOException e) 
		{
			endFailedThread(logHeader + "Couldn't get connected node's name: ", e, true);
		}
		log.info(logHeader + "Object loop complete.");
		
		log.debug(logHeader + "Letting master know we've finished sending " + nodeName + " its object.");
		sent.countDown();
		if(!client)
		{
			broker.countDown();
		}
		log.info(logHeader + "Master should know " + nodeName + " has its object.");
		
		// If it's a client, we have to handle synchronization
		if(client)
		{
			try 
			{
				start.await();
			} 
			catch (InterruptedException e) 
			{
				endFailedThread(logHeader + "Interrupted waiting for start signal: ", e, true);
			}
			
			PSTBUtil.sendStringAcrossSocket(pipeOut, PSTBUtil.START, log, logHeader);
		}
		
		try 
		{
			objectPipe.close();
		} 
		catch (IOException e) 
		{
			endFailedThread(logHeader + "Couldn't get close the pipe OutputStream: ", e, true);
		}
		
		return;
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
		
		sent.countDown();
		
		throw new RuntimeException(record, givenException);
	}
}
