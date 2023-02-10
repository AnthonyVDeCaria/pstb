/**
 * 
 */
package pstb.creation.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
//import java.net.SocketException;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import pstb.benchmark.object.PSNode;

/**
 * @author padres-dev-4187
 *
 */
public class ObjectServer extends Thread
{
    // Constants
//    private final int STD_TIMEOUT = 20000; // Milliseconds
    
    // Database Variables 
    private HashMap<String, PSNode> data;
    private boolean clientServer;
    
    // Other Variables
    private ServerSocket objectConnection;
    
    // Mercy Latches
    // none
    
    // Controlled Latches
    private CountDownLatch objServerComplete;
    
    // Logger
    private String context;
    private Integer nhI = 0;
    private ReentrantLock lockNHI = new ReentrantLock();
    private String logHeader = "ObjectServer: ";
    private Logger serverLog = LogManager.getRootLogger();
    
    public ObjectServer(HashMap<String, PSNode> nodeObjects, boolean givenServerClassification,
            String givenContext,
            CountDownLatch objServerCompleteSignal,
            ServerSocket givenSS)
    {
        data = nodeObjects;
        clientServer = givenServerClassification;
        
        context = givenContext;
        
        objServerComplete = objServerCompleteSignal;
        
        objectConnection = givenSS;
    }
    
    public int numNodes()
    {
        return data.size();
    }
    
    public void clearData()
    {
        data.clear();
    }
    
    public PSNode getNode(String givenName)
    {
        PSNode retVal = null;
        if(data.containsKey(givenName))
        {
            retVal = data.get(givenName);
        }
        
        return retVal;
    }
    
    public Integer getAndUpdateNHI()
    {
        Integer retVal = null;
        lockNHI.lock();
        try
        {
            retVal = nhI;
            nhI++;
        }
        finally
        {
            lockNHI.unlock();
        }
        
        return retVal;
    }
    
    public boolean isClientServer()
    {
        return clientServer;
    }
    
    public void run()
    {
        setName(context);
        ThreadContext.put("server", context);
        Thread.currentThread().setName(context);
        
        serverLog.info(logHeader + "Server started.");
        
        int numNodes = data.size();
        CountDownLatch nodeHandlerFinished = new CountDownLatch(numNodes);
        CountDownLatch nodeInitialized = new CountDownLatch(numNodes);
        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean noErrors = new AtomicBoolean(true);
        
        int numConnectedNodes = 0;
        while(numConnectedNodes < numNodes)
        {
            Socket objectPipe = null;
            try
            {
                objectPipe = objectConnection.accept();
            }
            catch(IOException e)
            {
                endServerFailure(logHeader + "Couldn't create an object pipe: ", e, true);
            }
            
            Thread.UncaughtExceptionHandler nodeHandlerExceptionNet = new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    serverLog.error(logHeader + "Error in the thread " + t.getName() + ": ", e);
                    noErrors.set(false);
                }
            };
            
            serverLog.debug(logHeader + "Creating a new NH.");
            NodeHandler oH = new NodeHandler(this, objectPipe, nodeInitialized, start, nodeHandlerFinished);
            oH.setUncaughtExceptionHandler(nodeHandlerExceptionNet);
            oH.start();
                        
            numConnectedNodes++;
            serverLog.debug(logHeader + "Waiting for " + (numNodes - numConnectedNodes) + " more nodes..." );
        }
        
        serverLog.debug(logHeader + "Waiting for nodes to initialze...");
        try 
        {
            nodeInitialized.await();
        } 
        catch (InterruptedException e) 
        {
            endServerFailure(logHeader + "Error waiting for nodes to initalize: ", e, true);
        }
        serverLog.debug(logHeader + "All nodes have initialized.");
        
        serverLog.debug(logHeader + "Sending start signal...");
        start.countDown();
        serverLog.debug(logHeader + "Start signal sent.");
        
        serverLog.debug(logHeader + "Waiting for nodeHandlers to finish...");
        try 
        {
            nodeHandlerFinished.await();
        } 
        catch (InterruptedException e) 
        {
            endServerFailure(logHeader + "Error waiting for clients: ", e, true);
        }
        serverLog.debug(logHeader + "All nodeHandlers are finished.");
        
        serverLog.debug(logHeader + "Checking for errors...");
        if(!noErrors.get())
        {
            String end = logHeader + "Some node handlers failed - exiting!";
            endServerFailure(end, null, false);
        }
        serverLog.debug(logHeader + "Setup succesful.");
        
        serverLog.debug(logHeader + "Letting control know...");
        objServerComplete.countDown();
        serverLog.debug(logHeader + "Control should know.");
        
        serverLog.info(logHeader + "Object Server ending.");
    }
    
    private void endServerFailure(String record, Exception givenException, boolean exceptionPresent)
    {
        objServerComplete.countDown();
        throw new RuntimeException(record, givenException);
    }
}

