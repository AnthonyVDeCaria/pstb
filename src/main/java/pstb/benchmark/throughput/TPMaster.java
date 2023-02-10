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
import pstb.util.PSTBUtil;

/**
 * @author adecaria
 *
 */
public class TPMaster extends Thread {
    // Constants
    private final double TOLERANCE_LIMIT = 0.1;
    private final double INIT_MESSAGES_PER_SECOND = 10.0;
    private final int RECEIVE_ERROR_LIMIT = 4;
    private final int MR_CONSTANT = 20;
//    private final int LOC_CLIENT_NAME = 0;
    private final int LOC_NUMBER_MESSAGES_RECEIEVED = 1;
    private final int LOC_DELAY = 2;
    private final int QUEUE_SIZE = 10;
    
    // Just Master - Database
    private HashMap<String, PSClientMode> database;
    private int numPubs;
    private int numSubs;
    private Long periodLength;
    
    // Just Master - Message Rate
    private Double messageRate;
    
    // Just Master - Delays
    private Double finalThroughput;
    
    // Multiple threads touch
    private ArrayList<String> subMessages;
    private boolean experimentRunning;
    private Long messageDelay;
    
    // Locks for above
    protected ReentrantLock lockSM = new ReentrantLock();
    protected ReentrantLock lockSD = new ReentrantLock();
    protected ReentrantLock lockER = new ReentrantLock();
    protected ReentrantLock lockMD = new ReentrantLock();
    
    // Server stuff
    private ServerSocket objectConnection;
    
    // Logger
    private String context;
    private String logHeader = "TPMaster: ";
    private Logger log = LogManager.getRootLogger();
    
    public TPMaster(HashMap<String, PSClientMode> givenDatabase, Long givenPL, String givenContext, ServerSocket givenSS)
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
        
        finalThroughput = Double.NaN;
        
        subMessages = new ArrayList<String>();
        experimentRunning = true;
        
        objectConnection = givenSS;
        
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
        Double messagesPerSecondPub = messageRate / numPubs;
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
        lockMD.lock();
        try
        {
            temp = messageDelay;
        }
        finally {
            lockMD.unlock();
        }
        
        return temp;
    }
    
    public void addToSubMessages(String newMPS)
    {
        lockSM.lock();
        try
        {
            subMessages.add(newMPS);
        }
        finally {
            lockSM.unlock();
        }
    }
    
    public void resetSubMessages()
    {
        lockSM.lock();
        try
        {
            subMessages.clear();
        }
        finally {
            lockSM.unlock();
        }
    }
    
    public boolean isExperimentRunning()
    {
        lockER.lock();
        try
        {
            return experimentRunning;
        }
        finally {
            lockER.unlock();
        }
    }
    
    public void stopExperiment()
    {
        lockER.lock();
        try
        {
            experimentRunning = false;
        }
        finally {
            lockER.unlock();
        }
    }
    
    public void startExperiment()
    {
        lockER.lock();
        try
        {
            experimentRunning = true;
        }
        finally {
            lockER.unlock();
        }
    }
    
    public ArrayList<String> getCurrentSubMessages()
    {
        lockSM.lock();
        try
        {
            return subMessages;
        }
        finally {
            lockSM.unlock();
        }
    }
    
    public void run()
    {
        setName(context);
        ThreadContext.put("server", context);
        Thread.currentThread().setName(context);
        
        log.info(logHeader + "Master started.");
        
        ClientDiary serverDiary = new ClientDiary();
        ArrayBlockingQueue<Point2D.Double> queue = new ArrayBlockingQueue<Point2D.Double>(QUEUE_SIZE);
        queue.add(new Point2D.Double());
        int numClients = database.size();
        int roundNum = 0;
        int receivedErrorCounter = 0;
        while(experimentRunning)
        {
            log.info(logHeader + "Beginning round " + roundNum + "...");
            DiaryEntry entryI = new DiaryEntry();
            entryI.setRound(roundNum);
            entryI.setMessageRate(messageRate);
            
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
                    masterFailure(logHeader + "Couldn't create an object pipe: ", e);
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
                masterFailure(logHeader + "Error waiting for clients: ", e);
            }
            log.info(logHeader + "All delays receieved.");
            
            // Read all the delays
            if(roundNum != 0)
            {
                log.debug(logHeader + "Beginning to process delays...");
                log.debug(logHeader + "rec = " + receivedErrorCounter);
                
                ArrayList<String> currentSubMPS = getCurrentSubMessages();
                double currentLatency = 0;
                int currentNMR = 0;
                int numMPS = currentSubMPS.size();
                for(int j = 0 ; j < numMPS ; j++)
                {
                    String messageJ = currentSubMPS.get(j);
                    String[] brokenMessageJ = messageJ.split("_");
                    Integer messagesReceivedJ = PSTBUtil.checkIfInteger(brokenMessageJ[LOC_NUMBER_MESSAGES_RECEIEVED], false, null);
                    Double delayJ = PSTBUtil.checkIfDouble(brokenMessageJ[LOC_DELAY], false, null);
                    if(messagesReceivedJ == null || delayJ == null)
                    {
                        masterFailure(logHeader + "Null delay received!", null);
                    }
                    else
                    {
                        currentNMR += messagesReceivedJ;
                        currentLatency += delayJ;
                    }
                }
                
                Double averageLatency = currentLatency / numSubs;
                entryI.setRoundLatency(averageLatency);
                                
                Double denominator = (roundNum * periodLength.doubleValue() / PSTBUtil.SEC_TO_NANOSEC);
                Double currentThroughput = currentNMR / denominator;
                log.info(logHeader + "currentThroughput = " + currentThroughput + " | currentNMR = " + currentNMR 
                        + " | denominator = " + denominator);
                entryI.setCurrentThroughput(currentThroughput);
                if(currentThroughput <= 0.0)
                {
                    receivedErrorCounter++;
                    
                    if(receivedErrorCounter > RECEIVE_ERROR_LIMIT)
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
                        masterFailure(logHeader + "Queue got empty!", null);
                    }
                    else
                    {
                        entryI.setY1(currentThroughput);
                        entryI.setX1(messageRate);
                        
                        Double currentRatio = currentThroughput / messageRate;
                        entryI.setCurrentRatio(currentRatio);
                        
                        Double y0 = startingPoint.getY();
                        Double x0 = startingPoint.getX();
                        entryI.setY0(y0);
                        entryI.setX0(x0);
                        
                        Double sNumerator = currentThroughput - y0;
                        Double sDenominator = messageRate - x0;
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
                            masterFailure(logHeader + "Null delay received!", null);
                        }
                        else if(secant < TOLERANCE_LIMIT)
                        {
                            stopExperiment();
                            finalThroughput = avg;
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
                
                serverDiary.addDiaryEntryToDiary(entryI);
            }
            
            log.debug(logHeader + "Calucaltions complete.");
            resetSubMessages();
            masterComplete.countDown();
            
            log.debug(logHeader + "Waiting for handler to finish...");
            try 
            {
                threadsActive.await();
            } 
            catch (InterruptedException e) 
            {
                masterFailure(logHeader + "Error waiting for all delays: ", e);
            }
            log.debug(logHeader + "All clients should have the current message delay.");
            
            log.info(logHeader + "Round " + roundNum + " complete.");
            roundNum++;
        }
        
        DiaryEntry finalEntry = new DiaryEntry();
        finalEntry.setFinalThroughput(finalThroughput);
        serverDiary.addDiaryEntryToDiary(finalEntry);
        
        recordDiary(serverDiary);
        
        log.info(logHeader + "Server ending.");
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
            masterFailure(logHeader + "Couldn't create a FileOutputStream to record diary object: ", e);
        }
        
        boolean diaryCheck = PSTBUtil.sendObject(givenDiary, out, log, logHeader);
        if(!diaryCheck)
        {
            masterFailure(logHeader + "Couldn't record server " + context + "'s diary object!", null);
        }
        log.info(logHeader + "Diary Inserted.");
        
        try 
        {
            out.close();
        } 
        catch (IOException e) 
        {
            masterFailure(logHeader + "error closing diary OutputStream: ", e);
        }
    }
    
    private void masterFailure(String record, Exception givenException)
    {
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
