/**
 * 
 */
package pstb.creation.server.padres;

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
import pstb.creation.server.ServerPacket;
import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 *
 */
public class PADRESNodeHandler extends Thread
{
	private Socket objectPipe;
	private CountDownLatch wait;
	private CountDownLatch broker;
	private CountDownLatch start;
	private PADRESServer master;
	
	private String mode;
	private final String MODE_O = "OBJECT";
	private final String MODE_I = "INIT";
	
	private final String logHeader = "ObjectHandler: ";
	private final Logger log = LogManager.getLogger(PSTBServer.class);
	
	public PADRESNodeHandler(Socket givenPipe, CountDownLatch clientsWaitingSignal, CountDownLatch brokersCompleteSignal, 
							CountDownLatch startSignal, PADRESServer givenServer)
	{
		objectPipe = givenPipe;
		wait = clientsWaitingSignal;
		broker = brokersCompleteSignal;
		start = startSignal;
		master = givenServer;
		
		mode = MODE_O;
	}
	
	public void run()
	{
		String serverName = master.getName();
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
				if(mode == MODE_O)
				{
					nodeName = inputLine;
					ServerPacket nodeObject = master.getNode(nodeName);
						
					if(nodeObject == null)
			        {
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
					
					log.debug(logHeader + "Sending object data to node " + inputLine + "...");
					PSTBUtil.sendObject(nodeObject.getNode(), inputLine, pipeOut, log, logHeader);
					log.info(logHeader + "Object data sent to node " + nodeName + ".");
					
					if(!client)
					{
						broker.countDown();
						break;
					}
					else
					{
						mode = MODE_I;
					}
				}
				else if(mode == MODE_I)
				{
					if(!inputLine.equals(PSTBUtil.INIT))
					{
						endFailedThread(logHeader + "Init not received from node " + nodeName + "!", null, false);
					}
					
					log.debug(logHeader + "Letting master know " + nodeName + " is waiting for start...");
					wait.countDown();
					log.info(logHeader + "Master should know " + nodeName + " is waiting.");
					break;
				}
				else
				{
					endFailedThread(logHeader + "Invaild mode " + mode + "!", null, false);
				}
			}
		} 
		catch (IOException e) 
		{
			endFailedThread(logHeader + "Couldn't get connected node's name: ", e, true);
		}
		log.info(logHeader + "Object loop complete.");
		
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
		
		wait.countDown();
		
		throw new RuntimeException(record, givenException);
	}
}
