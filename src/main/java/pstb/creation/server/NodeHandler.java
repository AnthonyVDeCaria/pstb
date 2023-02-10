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

import pstb.benchmark.object.PSNode;
import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 *
 */
public class NodeHandler extends Thread
{
    private enum ServerMode {
        Object, Init, Done; 
    }
    
    // Other Variables
    private ObjectServer master;
    private Socket objectPipe;
    
    // Mercy Latches
    private CountDownLatch start;
    
    // Controlled Latches
    private CountDownLatch nodeInitalized;
    private CountDownLatch nodeHandlerFinished;
    
    // Updated Variables
    private ServerMode sMode;
    
    private final String logHeader = "NodeHandler: ";
    private final Logger log = LogManager.getLogger(ObjectServer.class);
    
    public NodeHandler(ObjectServer givenServer, Socket givenPipe, CountDownLatch nodeInitalizedSignal, 
            CountDownLatch startSignal, 
            CountDownLatch nodeHandlerFinishedSignal)
    {
        master = givenServer;
        objectPipe = givenPipe;
        
        start = startSignal;
        
        nodeInitalized = nodeInitalizedSignal;
        nodeHandlerFinished = nodeHandlerFinishedSignal;
        
        sMode = ServerMode.Object;
    }
    
    @Override
    public void run()
    {
        Integer shit = master.getAndUpdateNHI();
        String serverName = master.getName() + "-" + shit;
        ThreadContext.put("server", serverName);
        Thread.currentThread().setName(serverName);
        
        log.debug("Starting Node Handler...");
        
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
            
            while((inputLine = bufferedIn.readLine()) != null)
            {
                if(inputLine.equals(PSTBUtil.ERROR))
                {
                    nodeHandlerFailed(logHeader + "Node is reporting an error!", null, false);
                }
                
                if(sMode.equals(ServerMode.Object))
                {
                    log.debug(logHeader + "Object mode.");
                    
                    nodeName = inputLine;
                    PSNode nodeObject = master.getNode(nodeName);
                        
                    if(nodeObject == null)
                    {
                        nodeHandlerFailed(logHeader + "Process requested " + nodeName + " - a name not in the database!", null, false);
                    }
                    
                    log.debug(logHeader + "Sending object data to node " + inputLine + "...");
                    PSTBUtil.sendObject(nodeObject, pipeOut, log, logHeader);
                    log.debug(logHeader + "Object data sent to node " + nodeName + ".");
                    
                    log.debug(logHeader + "Changing mode...");
                    sMode = ServerMode.Init;
                }
                else if(sMode.equals(ServerMode.Init))
                {
                    log.debug(logHeader + "Init mode.");
                    
                    if(!inputLine.equals(PSTBUtil.INIT))
                    {
                        nodeHandlerFailed(logHeader + "Init not received from node " + nodeName + " - receieved " + inputLine + "!", 
                            null, false);
                    }
                    log.info(logHeader + "Init received from node " + nodeName + ".");
                    
                    log.debug(logHeader + "Letting server know node has initialized...");
                    nodeInitalized.countDown();
                    log.debug(logHeader + "Server should know.");
                    
                    if(master.isClientServer())
                    {
                        try 
                        {
                            start.await();
                        } 
                        catch (InterruptedException e) 
                        {
                            nodeHandlerFailed(logHeader + "Interrupted waiting for start signal: ", e, true);
                        }
                        
                        log.debug(logHeader + "Sending " + nodeName + " the start signal...");
                        PSTBUtil.sendStringAcrossSocket(pipeOut, PSTBUtil.START);
                        log.info(logHeader + "Start signal sent to " + nodeName + ".");
                    }
                    
                    try 
                    {
                        objectPipe.close();
                    } 
                    catch (IOException e) 
                    {
                        nodeHandlerFailed(logHeader + "Couldn't get close the pipe OutputStream: ", e, true);
                    }
                    
                    sMode = ServerMode.Done;
                    break;
                }
                else
                {
                    nodeHandlerFailed(logHeader + "Invaild mode " + sMode + "!", null, false);
                }
            }
        } 
        catch (IOException e) 
        {
            nodeHandlerFailed(logHeader + "Couldn't get connected node's name: ", e, true);
        }
        
        if(!sMode.equals(ServerMode.Done))
        {
            nodeHandlerFailed(logHeader + "SMode is not proper upon exiting!", null, false);
        }
        
        log.debug(logHeader + "Letting server know we're complete...");
        nodeHandlerFinished.countDown();
        log.debug(logHeader + "Server should know.");
        
        log.info(logHeader + "Object loop complete.");
    }
    
    private void nodeHandlerFailed(String record, Exception givenException, boolean exceptionPresent)
    {
        nodeInitalized.countDown();
        nodeHandlerFinished.countDown();
        
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
