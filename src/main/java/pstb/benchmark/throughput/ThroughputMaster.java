/**
 * 
 */
package pstb.benchmark.throughput;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import pstb.benchmark.object.client.PSClientMode;
import pstb.creation.server.PSTBServer;
import pstb.util.PSTBUtil;

/**
 * @author adecaria
 *
 */
public class ThroughputMaster extends Thread {
	// Constants
	private final Double TOLERANCE_LIMIT = 0.5;
	private final int INIT_MESSAGES_PER_SECOND = 10;
	private final int MPS_SUM_VALUE = 5;
	private final int LOC_CLIENT_NAME = 0;
	private final int LOC_DELAY = 1;
//	private final int LOC_TODO = 2;
	
	// Just Master - Database
	private HashMap<String, PSClientMode> database;
	private int numPubs;
	private int numSubs;
	
	// Just Master - Message Rate
	private long messagesPerSecond;
	
	// Just Master - Delay
	private Double firstDelay;
	
	// Multiple threads touch
	private ArrayList<String> subDelays;
	private boolean experimentRunning;
	private Long messageDelay;
	
	// Locks for above
	protected ReentrantLock lockSD = new ReentrantLock();
	protected ReentrantLock lockER = new ReentrantLock();
	protected ReentrantLock lockMD = new ReentrantLock();
	
	// Server stuff
	private Integer port;
	private ServerSocket objectConnection;
	
	// Logger
	private String benchmarkStartTime;
	private String logHeader = "TPMaster: ";
	private Logger log = LogManager.getLogger(PSTBServer.class);
	
	public ThroughputMaster(HashMap<String, PSClientMode> givenDatabase, Integer givenPort, String givenBST)
	{
		database = givenDatabase;
		numPubs = 0;
		numSubs = 0;
		database.forEach((clientIName, clientIMode)->{
			if(clientIMode.equals(PSClientMode.TPPub))
			{
				numPubs++;
			}
			else if(clientIMode.equals(PSClientMode.TPSub))
			{
				numSubs++;
			}
		});
		
		messagesPerSecond = INIT_MESSAGES_PER_SECOND;
		updateMessageDelay();
		
		firstDelay = null;
		
		subDelays = new ArrayList<String>();
		experimentRunning = true;
		
		port = givenPort;
		objectConnection = null;
		
		benchmarkStartTime = givenBST;
	}
	
	public PSClientMode getClientMode(String clientName)
	{
		return database.get(clientName);
	}
	
	public boolean containsClient(String clientName) {
		return database.containsKey(clientName);
	}
	
	private void updateMessagesPerSecond()
	{
		messagesPerSecond += MPS_SUM_VALUE;
	}
	
	private void updateMessageDelay()
	{
		Double messagesPerSecondPub = ((double) messagesPerSecond) / numPubs;
		Double secondsPubPerMessage = 1 / messagesPerSecondPub;
		messageDelay = (long) (secondsPubPerMessage * PSTBUtil.SEC_TO_MILLISEC);
		
		log.info(logHeader 
				+ "messagesPerSecond = " + messagesPerSecond
				+ " messagesPerSecondPub = " + messagesPerSecondPub 
				+ " secondsPubsPerMessage = " + secondsPubPerMessage
				+ " messageDelay = " + messageDelay);
	}
		
	public Long getMessageDelay()
	{
		Long temp;
		try
		{
			lockSD.lock();
			temp = messageDelay;
		}
		finally {
			lockSD.unlock();
		}
		
		return temp;
	}
	
	public void addToSubDelays(String newDelay)
	{
		try
		{
			lockSD.lock();
			subDelays.add(newDelay);
		}
		finally {
			lockSD.unlock();
		}
	}
	
	public void resetSubDelays()
	{
		try
		{
			lockSD.lock();
			subDelays.clear();
		}
		finally {
			lockSD.unlock();
		}
	}
	
	public boolean isExperimentRunning()
	{
		try
		{
			lockER.lock();
			return experimentRunning;
		}
		finally {
			lockER.unlock();
		}
	}
	
	public void stopExperiment()
	{
		try
		{
			lockER.lock();
			experimentRunning = false;
		}
		finally {
			lockER.unlock();
		}
	}
	
	public void startExperiment()
	{
		try
		{
			lockER.lock();
			experimentRunning = true;
		}
		finally {
			lockER.unlock();
		}
	}
	
	public ArrayList<String> getCurrentSubDelays()
	{
		try
		{
			lockSD.lock();
			return subDelays;
		}
		finally {
			lockSD.unlock();
		}
	}
	
	public void run()
	{
		String serverName = "Server-" + benchmarkStartTime; 
		setName(serverName);
		ThreadContext.put("server", serverName);
		Thread.currentThread().setName(serverName);
		
		log.info(logHeader + "Server started.");
		
		log.info(logHeader + messageDelay);
		
		log.debug(logHeader + "Attempting to create socket.");
		boolean socketCheck = createServerSocket();
		if(!socketCheck)
		{
			endServerFailure(logHeader + "Couldn't create socket!", null, false);
		}
		log.info(logHeader + "Socket created.");
		
		int numClients = database.size();
		int roundNum = 0;
		while(experimentRunning)
		{
			log.debug(logHeader + "Beginning round " + roundNum + "...");
			
			CountDownLatch missingDelays = new CountDownLatch(numClients);
			CountDownLatch masterComplete = new CountDownLatch(1);
			CountDownLatch threadsActive = new CountDownLatch(numClients);
			log.info(logHeader + "Latches set.");
			
			log.debug(logHeader + "Attempting to send all clients the current message delay...");
			int i = 0;
			while(i < numClients)
			{
				log.debug(logHeader + "Waiting for " + (numClients - i) +  " more connections ...");
				Socket objectPipe = null;
				try
				{
					objectPipe = objectConnection.accept();
				}
				catch(IOException e)
				{
					endServerFailure(logHeader + "Couldn't create an object pipe: ", e, true);
				}
				
				log.info(logHeader + "Got a new connection.");
				
				Thread.UncaughtExceptionHandler nodeHandlerExceptionNet = new Thread.UncaughtExceptionHandler() {
					@Override
					public void uncaughtException(Thread t, Throwable e) {
						log.error(logHeader + "Error in the thread " + t.getName() + ": ", e);
					}
				};
				
				TPHandler clientIHandler = new TPHandler(objectPipe, missingDelays, masterComplete, threadsActive, this);
				clientIHandler.setUncaughtExceptionHandler(nodeHandlerExceptionNet);
				clientIHandler.start();
							
				i++;
			}
			log.info(logHeader + "All clients connected.");
			
			log.debug(logHeader + "Waiting for delays to arrive...");
			try 
			{
				missingDelays.await();
			} 
			catch (InterruptedException e) 
			{
				endServerFailure(logHeader + "Error waiting for clients: ", e, true);
			}
			log.info(logHeader + "All delays receieved.");
			
			// Read all the delays
			log.debug(logHeader + "Beginning to process delays...");
			ArrayList<String> currentSubDelays = getCurrentSubDelays();
			int numDelays = currentSubDelays.size();
			double currentDelay = 0;
			int numNull = 0;
			for(int j = 0 ; j < numDelays ; j++)
			{
				String delayJ = currentSubDelays.get(j);
				String[] brokenDelayJ = delayJ.split("-");
				Double actualDelayJ = PSTBUtil.checkIfDouble(brokenDelayJ[LOC_DELAY], false, null);
				if(actualDelayJ == null || actualDelayJ.isNaN())
				{
					String clientJ = brokenDelayJ[LOC_CLIENT_NAME];
					PSClientMode clientJMode = database.get(clientJ);
					if(clientJMode.equals(PSClientMode.TPSub))
					{
						numNull++;
					}
				}
				else
				{
					currentDelay += actualDelayJ;
				}
			}
			if(numNull > 0)
			{
				if(numNull != numSubs)
				{
					endServerFailure(logHeader + "Null delay!", null, false);
				}
			}
			else
			{
				currentDelay /= numDelays;
				
				// Should we stop yes or no?
				if(firstDelay == null)
				{
					log.info(logHeader + "First delay is " + currentDelay + ".");
					firstDelay = currentDelay;
				}
				else
				{
					Double ratio = firstDelay / currentDelay;
					log.info(logHeader + "Current delay = " + currentDelay + " | Ratio is " + ratio + ".");
					
					if(ratio <= TOLERANCE_LIMIT)
					{
						log.info(logHeader + "Tolerance limit reached.");
						stopExperiment();
					}
				}
				
				if(isExperimentRunning())
				{
					log.info(logHeader + "Updating message delay.");
					updateMessagesPerSecond();
					updateMessageDelay();
				}
			}
			log.info(logHeader + "Calucaltions complete.");
			resetSubDelays();
			masterComplete.countDown();
			
			log.debug(logHeader + "Waiting for handler to finish...");
			try 
			{
				threadsActive.await();
			} 
			catch (InterruptedException e) 
			{
				endServerFailure(logHeader + "Error waiting for all delays: ", e, true);
			}
			log.info(logHeader + "All clients should have the current message delay.");
			
			log.info(logHeader + "Round " + roundNum + " complete.");
			roundNum++;
		}

		try 
		{
			objectConnection.close();
		} 
		catch (IOException eIO) 
		{
			log.error("Error closing objectPipe: ", eIO);
			endServerFailure("Error closing objectPipe: ", eIO, true);
		}
		
		log.info(logHeader + "Server ending.");
	}
		
	private boolean createServerSocket()
	{
		try
		{
			objectConnection = new ServerSocket(port);
		}
		catch (IOException e) 
		{
			log.error(logHeader + "Couldn't create a serverSocket to pass node objects: ", e);
			return false;
		}
		
		return true;
	}
	
	private void endServerFailure(String record, Exception givenException, boolean exceptionPresent)
	{
		if(exceptionPresent)
		{
			log.error(record, givenException);
		}
		else
		{
			log.error(record);
		}
		
		if(objectConnection != null)
		{
			try 
			{
				objectConnection.close();
			} 
			catch (IOException eIO) 
			{
				log.error("Error closing objectPipe: ", eIO);
			}
		}
		
		throw new RuntimeException(record, givenException);
	}
}
