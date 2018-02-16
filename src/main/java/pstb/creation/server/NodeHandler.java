/**
 * 
 */
package pstb.creation.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 *
 */
public class NodeHandler extends Thread
{
	private PSTBServer master;
	private Socket objectPipe;
	
	private CountDownLatch clientsNotWaiting;
	private CountDownLatch brokersNotStarted;
	private CountDownLatch brokersNotLinked;
	private CountDownLatch start;
	
	private Boolean nodeIsClient;
	
	private enum ServerMode {
		Object, Init, Done; 
	}
	
	private ServerMode mode;
	
	private final String logHeader = "NodeHandler: ";
	private final Logger log = LogManager.getLogger(PSTBServer.class);
	
	public NodeHandler(Socket givenPipe, CountDownLatch clientsReadySignal, CountDownLatch brokersStartedSignal,
			CountDownLatch brokersReadySignal, CountDownLatch startSignal, PSTBServer givenServer)
	{
		objectPipe = givenPipe;
		clientsNotWaiting = clientsReadySignal;
		brokersNotStarted = brokersStartedSignal;
		brokersNotLinked = brokersReadySignal;
		start = startSignal;
		master = givenServer;
		
		nodeIsClient = null;
		
		mode = ServerMode.Object;
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
		
		String nodeName = new String();
		try 
		{
			BufferedReader bufferedIn = new BufferedReader(new InputStreamReader(objectPipe.getInputStream()));
			String inputLine = new String();
			
			while ((inputLine = bufferedIn.readLine()) != null)
			{
				if(mode.equals(ServerMode.Object))
				{
					nodeName = inputLine;
					PSTBServerPacket nodeObject = master.getNode(nodeName);
						
					if(nodeObject == null)
			        {
			        	endFailedThread(logHeader + "Process requested " + nodeName + " - a name not in the database!", null, false);
			        }
					
					nodeIsClient = new Boolean(nodeObject.isClient());
					
					if(nodeIsClient)
					{
						try 
						{
							brokersNotLinked.await();
						} 
						catch (InterruptedException e) 
						{
							endFailedThread(logHeader + "Interrupted waiting for brokers to complete signal: ", e, true);
						}
					}
					
					log.debug(logHeader + "Sending object data to node " + inputLine + "...");
					PSTBUtil.sendObject(nodeObject.getNode(), inputLine, pipeOut, log, logHeader);
					log.info(logHeader + "Object data sent to node " + nodeName + ".");
					
					mode = ServerMode.Init;
				}
				else if(mode.equals(ServerMode.Init))
				{
					if(!inputLine.equals(PSTBUtil.INIT))
					{
						endFailedThread(logHeader + "Init not received from node " + nodeName + "!", null, false);
					}
					log.info(logHeader + "Init received from node " + nodeName + ".");
					
					if(nodeIsClient == null)
					{
						// This should never trigger, but just in case
						endFailedThread(logHeader + "Unknown what " + nodeName + " is!", null, false);
					}
					else if(!nodeIsClient.booleanValue())
					{
						brokersNotStarted.countDown();
					}
					else
					{
						log.debug(logHeader + "Letting server know client " + nodeName + " is waiting for start...");
						clientsNotWaiting.countDown();
						log.info(logHeader + "Server should know client " + nodeName + " is waiting.");
						
						try 
						{
							start.await();
						} 
						catch (InterruptedException e) 
						{
							endFailedThread(logHeader + "Interrupted waiting for start signal: ", e, true);
						}
						
						log.debug(logHeader + "Sending " + nodeName + " the start signal...");
						PSTBUtil.sendStringAcrossSocket(pipeOut, PSTBUtil.START, log, logHeader);
						log.info(logHeader + "Start signal sent to " + nodeName + ".");
					}
					
					mode = ServerMode.Done;
				}
				else if(mode.equals(ServerMode.Done))
				{
					try 
					{
						objectPipe.close();
					} 
					catch (IOException e) 
					{
						endFailedThread(logHeader + "Couldn't get close the pipe OutputStream: ", e, true);
					}
					
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
		
		brokersNotStarted.countDown();
		clientsNotWaiting.countDown();
		
		throw new RuntimeException(record, givenException);
	}
}
