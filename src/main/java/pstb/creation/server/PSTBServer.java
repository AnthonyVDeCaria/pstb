/**
 * 
 */
package pstb.creation.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import pstb.benchmark.object.PSNode;
import pstb.benchmark.object.client.PSClient;
import pstb.benchmark.object.client.PSClientMode;
import pstb.benchmark.throughput.ThroughputMaster;
import pstb.startup.config.BenchmarkMode;

/**
 * @author padres-dev-4187
 *
 */
public class PSTBServer extends Thread
{
	// Constants
	private final int STD_TIMEOUT = 20000; // Milliseconds
	
	private HashMap<String, PSNode> brokerData;
	private HashMap<String, PSNode> clientData;
	
	private CountDownLatch start;
	private CountDownLatch incompleteBrokers;
	private CountDownLatch brokersLinked;
	
	private Integer port;
	private ServerSocket objectConnection;
	
	private String benchmarkStartTime;
	
	private BenchmarkMode mode;
	
	private String logHeader = "PSTBServer: ";
	private Logger serverLog = LogManager.getLogger(PSTBServer.class);
	
	public PSTBServer(HashMap<String, PSNode> brokerObjects, HashMap<String, PSNode> clientObjects, CountDownLatch startSignal, 
			CountDownLatch brokersStartedSignal, CountDownLatch brokersLinkedSignal, String givenBST, BenchmarkMode givenMode)
	{
		brokerData = brokerObjects;
		clientData = clientObjects;
		
		start = startSignal;
		incompleteBrokers = brokersStartedSignal;
		brokersLinked = brokersLinkedSignal;
		
		port = null;
		objectConnection = null;
		
		benchmarkStartTime = givenBST;
		
		mode = givenMode;
	}
	
	public int numBrokers()
	{
		return brokerData.size();
	}
	
	public Integer generatePort()
	{
		port = 4444;
		return port;
	}
	
	public void clearDatasets()
	{
		brokerData.clear();
		clientData.clear();
	}
	
	public PSTBServerPacket getNode(String givenName)
	{
		if(brokerData.containsKey(givenName))
		{
			PSTBServerPacket broker = new PSTBServerPacket(brokerData.get(givenName), false);
			return broker;
		}
		else if(clientData.containsKey(givenName))
		{
			PSTBServerPacket client = new PSTBServerPacket(clientData.get(givenName), true);
			return client;
		}
		else
		{
			return null;
		}
	}
	
	public Integer getPort()
	{
		return port;
	}
	
	public void run()
	{
		String serverName = "Server-" + benchmarkStartTime; 
		setName(serverName);
		ThreadContext.put("server", serverName);
		Thread.currentThread().setName(serverName);
		
		serverLog.info(logHeader + "Server started.");
		
		serverLog.debug(logHeader + "Attempting to create socket.");
		boolean socketCheck = createServerSocket();
		if(!socketCheck)
		{
			endServerFailure(logHeader + "Couldn't create socket!", null, false);
		}
		serverLog.info(logHeader + "Socket created.");
		
		int numBrokers = brokerData.size();
		int numClients = clientData.size();
		int numNodes = numBrokers + numClients;
		CountDownLatch incompleteClients = new CountDownLatch(numClients);
		CountDownLatch startSent = new CountDownLatch(numClients);
		
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
				}
			};
			
			NodeHandler oH = new NodeHandler(objectPipe, incompleteClients, incompleteBrokers, brokersLinked, start, startSent, this);
			oH.setUncaughtExceptionHandler(nodeHandlerExceptionNet);		
			oH.start();
						
			numConnectedNodes++;
		}
		
		try 
		{
			incompleteClients.await();
		} 
		catch (InterruptedException e) 
		{
			endServerFailure(logHeader + "Error waiting for clients: ", e, true);
		}
		serverLog.info(logHeader + "All objects sent - clients are now waiting for start.");
		
		serverLog.debug(logHeader + "Sending start signal...");
		start.countDown();
		serverLog.info(logHeader + "Start signal sent.");
		
		try 
		{
			objectConnection.close();
		} 
		catch (IOException eIO) 
		{
			serverLog.error("Error closing objectPipe: ", eIO);
			throw new RuntimeException("Error closing objectPipe: ", eIO);
		}
		
		try 
		{
			startSent.await();
		} 
		catch (InterruptedException e) 
		{
			endServerFailure(logHeader + "Error waiting for clients: ", e, true);
		}
		
		serverLog.info(logHeader + "Server ending.");
		
		if(mode.equals(BenchmarkMode.Throughput))
		{
			serverLog.debug(logHeader + "Starting Throughput Master...");
			
			HashMap<String, PSClientMode> tmDatabase = new HashMap<String, PSClientMode>();
			
			clientData.forEach((clientIsName, clientIShell)->{
				PSClient clientI = (PSClient) clientIShell;
				PSClientMode clientIsMode = clientI.getClientMode();
				
				tmDatabase.put(clientIsName, clientIsMode);
			});
			
			Thread.UncaughtExceptionHandler nodeHandlerExceptionNet = new Thread.UncaughtExceptionHandler() {
				@Override
				public void uncaughtException(Thread t, Throwable e) {
					serverLog.error(logHeader + "Error in the thread " + t.getName() + ": ", e);
				}
			};
			
			ThroughputMaster newServer = new ThroughputMaster(tmDatabase, port, benchmarkStartTime);
			newServer.setUncaughtExceptionHandler(nodeHandlerExceptionNet);		
			newServer.start();
			
			serverLog.info(logHeader + "Throughput Master started.");
		}
	}
	
	private boolean createServerSocket()
	{
		if(port == null)
		{
			serverLog.warn(logHeader + "No port generated - generating port.");
			generatePort();
		}
		
		try
		{
			objectConnection = new ServerSocket(port);
		}
		catch (IOException e) 
		{
			serverLog.error(logHeader + "Couldn't create a serverSocket to pass node objects: ", e);
			return false;
		}
		
		try 
		{
			objectConnection.setSoTimeout(STD_TIMEOUT);
		} 
		catch (SocketException e) 
		{
			serverLog.error(logHeader + "Couldn't sete a timeout period to the serverSocket: ", e);
			return false;
		}
		
		return true;
	}
	
	private void endServerFailure(String record, Exception givenException, boolean exceptionPresent)
	{
		if(exceptionPresent)
		{
			serverLog.error(record, givenException);
		}
		else
		{
			serverLog.error(record);
		}
		
		if(objectConnection != null)
		{
			try 
			{
				objectConnection.close();
			} 
			catch (IOException eIO) 
			{
				serverLog.error("Error closing objectPipe: ", eIO);
			}
		}
		
		throw new RuntimeException(record, givenException);
	}

}
