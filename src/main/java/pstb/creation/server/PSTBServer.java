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
	
	private CountDownLatch startSignal;
	
	private Integer port;
	private ServerSocket objectConnection;
	
	private int runNumber;
	
	private String logHeader = "PSTBServer: ";
	private Logger serverLog = LogManager.getLogger(PSTBServer.class);
	
	public PSTBServer(int givenRunNumber)
	{
		brokerData = new HashMap<String, PSNode>();
		clientData = new HashMap<String, PSNode>();
		
		startSignal = null;
		
		port = null;
		objectConnection = null;
		
		runNumber = givenRunNumber;
	}
	
	public void setBrokerData(HashMap<String, PSNode> brokerObjects)
	{
		brokerData = brokerObjects;
	}
	
	public void setClientData(HashMap<String, PSNode> givenClients)
	{
		clientData = givenClients;
	}
	
	public void setStartSignal(CountDownLatch givenStartSignal)
	{
		startSignal = givenStartSignal;
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
		String serverName = "Server-" + runNumber; 
		setName(serverName);
		ThreadContext.put("server", serverName);
		Thread.currentThread().setName(serverName);
		
		while (clientData.isEmpty())
		{
			serverLog.debug(logHeader + "Waiting for data.");
			try 
			{
				Thread.sleep(1000);
			} 
			catch (InterruptedException e) 
			{
				endServerFailure(logHeader + "Failure waiting for data: ", e, true);
			}
		}
		
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
		CountDownLatch clientsNotWaiting = new CountDownLatch(numClients);
		CountDownLatch brokersNotRunning = new CountDownLatch(numBrokers);
		
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
			
			NodeHandler oH = new NodeHandler(objectPipe, clientsNotWaiting, brokersNotRunning, startSignal, this);
			oH.setUncaughtExceptionHandler(nodeHandlerExceptionNet);		
			oH.start();
						
			numConnectedNodes++;
		}
		
		try 
		{
			clientsNotWaiting.await();
		} 
		catch (InterruptedException e) 
		{
			endServerFailure(logHeader + "Error waiting for clients: ", e, true);
		}
		serverLog.info(logHeader + "All objects sent - clients are now waiting for start.");
		
		serverLog.debug(logHeader + "Sending start signal...");
		startSignal.countDown();
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
		
		serverLog.info(logHeader + "Server ending.");
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
