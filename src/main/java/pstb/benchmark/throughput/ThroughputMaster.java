/**
 * 
 */
package pstb.benchmark.throughput;

import java.awt.geom.Point2D;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import pstb.analysis.diary.ClientDiary;
import pstb.analysis.diary.DiaryEntry;
import pstb.benchmark.object.client.PSClientMode;
import pstb.creation.server.PSTBServer;
import pstb.util.PSTBError;
import pstb.util.PSTBUtil;

/**
 * @author adecaria
 *
 */
public class ThroughputMaster extends Thread {
	// Constants
	private final double TOLERANCE_LIMIT = 0.1;
	private final double INIT_MESSAGES_PER_SECOND = 10.0;
	private final int FUCKED_UP_LIMIT = 4;
	private final int MR_CONSTANT = 20;
//	private final int LOC_CLIENT_NAME = 0;
	private final int LOC_NUMBER_MESSAGES_RECEIEVED = 1;
	private final int LOC_DELAY = 2;
	private final int QUEUE_SIZE = 4;
	
	// Just Master - Database
	private HashMap<String, PSClientMode> database;
	private int numPubs;
	private int numSubs;
	private Long periodLength;
	
	// Just Master - Message Rate
	private Double messageRate;
	
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
	private String context;
	private String logHeader = "TPMaster: ";
	private Logger log = LogManager.getLogger(PSTBServer.class);
	
	public ThroughputMaster(HashMap<String, PSClientMode> givenDatabase, Long givenPL, Integer givenPort, String givenContext)
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
		
		periodLength = givenPL;
		
		messageRate = INIT_MESSAGES_PER_SECOND;
		updateMessageDelay();
		
		firstDelay = null;
		
		subDelays = new ArrayList<String>();
		experimentRunning = true;
		
		port = givenPort;
		objectConnection = null;
		
		context = givenContext;
	}
	
	public PSClientMode getClientMode(String clientName)
	{
		return database.get(clientName);
	}
	
	public boolean containsClient(String clientName) {
		return database.containsKey(clientName);
	}
	
	private void updateMessageRate(Double givenDir)
	{
		messageRate += MR_CONSTANT * givenDir;
	}
	
	private void updateMessageDelay()
	{
		Double messagesPerSecondPub = ((double) messageRate) / numPubs;
		Double secondsPubPerMessage = 1 / messagesPerSecondPub;
		messageDelay = (long) (secondsPubPerMessage * PSTBUtil.SEC_TO_NANOSEC);
		
		log.debug(logHeader 
				+ "messagesPerSecond = " + messageRate
				+ " messagesPerSecondPub = " + messagesPerSecondPub 
				+ " secondsPubsPerMessage = " + secondsPubPerMessage
				+ " messageDelay = " + messageDelay);
	}
		
	public Long getMessageDelay()
	{
		Long temp;
		try
		{
			lockMD.lock();
			temp = messageDelay;
		}
		finally {
			lockMD.unlock();
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
		String serverName = "Server-" + context; 
		setName(serverName);
		ThreadContext.put("server", serverName);
		Thread.currentThread().setName(serverName);
		
		log.info(logHeader + "Server started.");
		
		ClientDiary serverDiary = new ClientDiary();
		
		log.debug(logHeader + "Attempting to create socket.");
		boolean socketCheck = createServerSocket();
		if(!socketCheck)
		{
			endServerFailure(logHeader + "Couldn't create socket!", null, false);
		}
		log.debug(logHeader + "Socket created.");
		
		ArrayBlockingQueue<Point2D.Double> queue = new ArrayBlockingQueue<Point2D.Double>(QUEUE_SIZE);
		queue.add(new Point2D.Double());
		int numClients = database.size();
		int roundNum = 0;
		int fuckedUpCounter = 0;
		while(experimentRunning)
		{
			log.info(logHeader + "Beginning round " + roundNum + "...");
			DiaryEntry entryI = new DiaryEntry();
			entryI.setRound(roundNum);
			
			CountDownLatch missingDelays = new CountDownLatch(numClients);
			CountDownLatch masterComplete = new CountDownLatch(1);
			CountDownLatch threadsActive = new CountDownLatch(numClients);
			log.debug(logHeader + "Latches set.");
			
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
				
				log.debug(logHeader + "Got a new connection.");
				
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
			log.debug(logHeader + "All clients connected.");
			
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
			if(roundNum != 0)
			{
				log.debug(logHeader + "Beginning to process delays...");
				log.debug(logHeader + "fuc = " + fuckedUpCounter);
				ArrayList<String> currentSubDelays = getCurrentSubDelays();
				double currentDelay = 0;
				int currentNMR = 0;
				int numDelays = currentSubDelays.size();
				for(int j = 0 ; j < numDelays ; j++)
				{
					String messageJ = currentSubDelays.get(j);
					String[] brokenMessageJ = messageJ.split("_");
					Integer messagesReceivedJ = PSTBUtil.checkIfInteger(brokenMessageJ[LOC_NUMBER_MESSAGES_RECEIEVED], false, null);
					Double delayJ = PSTBUtil.checkIfDouble(brokenMessageJ[LOC_DELAY], false, null);
					if(messagesReceivedJ == null || delayJ == null)
					{
						endServerFailure(logHeader + "Null delay received!", null, false);
					}
					else
					{
						currentNMR += messagesReceivedJ;
						currentDelay += delayJ;
					}
				}
				
				if(firstDelay == null)
				{
					currentDelay /= numSubs;
					log.debug(logHeader + "First delay is " + currentDelay + ".");
					firstDelay = currentDelay;
				}
				
				Double denominator = (roundNum * periodLength.doubleValue() / PSTBUtil.SEC_TO_NANOSEC);
				Double currentThroughput = currentNMR / denominator;
				log.info(logHeader + "currentThroughput = " + currentThroughput + " | currentNMR = " + currentNMR 
						+ " | denominator = " + denominator);
				entryI.setCurrentThroughput(currentThroughput);
				if(currentThroughput <= 0.0)
				{
					fuckedUpCounter++;
					
					if(fuckedUpCounter > FUCKED_UP_LIMIT)
					{
						entryI.setAverageThroughput(Double.NaN);
						stopExperiment();
					}
				}
				else
				{
					Point2D.Double startingPoint = null;
					if(queue.remainingCapacity() != 0)
					{
						startingPoint = queue.peek();
					}
					else
					{
						startingPoint = queue.remove();
					}
					
					if(startingPoint == null)
					{
						endServerFailure(logHeader + "Queue got empty!", null, false);
					}
					else
					{
						Double sNumerator = currentThroughput - startingPoint.getY();
						Double sDenominator = messageRate - startingPoint.getX();
						Double secant = sNumerator / sDenominator;
						log.info(logHeader + "secant = " + secant + " | num = " + sNumerator + " | dem = " + sDenominator 
								+ " | startY = " + startingPoint.getY() + " | startX = " + startingPoint.getX());
						entryI.setSecant(secant);
						
						Double avgNum = 0.0;
						Object[] currentPairs = queue.toArray();
						int numPairs = queue.size();
						for(int j = 0 ; j < numPairs ; j++)
						{
							Point2D.Double pairI = (Point2D.Double) currentPairs[j];
							avgNum += pairI.getY();
						}
						Double avg = avgNum / numPairs;
						entryI.setAverageThroughput(avg);
						
						if(secant == null || secant.isNaN())
						{
							endServerFailure(logHeader + "Null delay received!", null, false);
						}
						else if(secant < TOLERANCE_LIMIT)
						{
							stopExperiment();
						}
						else
						{
							log.debug(logHeader + "Updating queue.");
							Point2D.Double newPoint = new Point2D.Double(messageRate, currentThroughput);
							queue.add(newPoint);
							
							log.debug(logHeader + "Updating message delay.");
							updateMessageRate(secant);
							updateMessageDelay();
						}
					}
				}
			}
			
			log.debug(logHeader + "Calucaltions complete.");
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
			log.debug(logHeader + "All clients should have the current message delay.");
			
			serverDiary.addDiaryEntryToDiary(entryI);
			
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
		
		recordDiary(serverDiary);
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
	
	private void recordDiary(ClientDiary givenDiary)
	{
		FileOutputStream out = null;
		try 
		{
			out = new FileOutputStream(context + ".dia");
		} 
		catch (FileNotFoundException e) 
		{
			endServerFailure(logHeader + "Couldn't create a FileOutputStream to record diary object: ", e, true);
		}
		
		boolean diaryCheck = PSTBUtil.sendObject(givenDiary, out, log, logHeader);
		if(!diaryCheck)
		{
			endServerFailure(logHeader + "Couldn't record server " + context + "'s diary object!", null, false);
		}
		log.info(logHeader + "Diary Inserted.");
		
		try 
		{
			out.close();
		} 
		catch (IOException e) 
		{
			endServerFailure(logHeader + "error closing diary OutputStream: ", e, true);
		}
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
		
		PSTBUtil.killAllProcesses(false, null, log);
		System.exit(PSTBError.M_EXPERIMENT);
	}
}
