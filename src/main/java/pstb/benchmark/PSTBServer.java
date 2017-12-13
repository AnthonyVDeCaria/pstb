/**
 * 
 */
package pstb.benchmark;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author padres-dev-4187
 *
 */
public class PSTBServer extends Thread
{
	private CountDownLatch startSignal;
	
	private HashMap<String, PSBrokerPADRES> brokerData;
	private HashMap<String, PSClientPADRES> clientData;
	
	private Integer port;
	private ServerSocket objectConnection;
	
	private String logHeader = "Server: ";
	private Logger serverLog = LogManager.getLogger(PSTBServer.class);;
	
	public PSTBServer()
	{
		startSignal = null;
		
		brokerData = new HashMap<String, PSBrokerPADRES>();
		clientData = new HashMap<String, PSClientPADRES>();
		
		port = null;
		objectConnection = null;
	}
	
	public void setStartSignal(CountDownLatch givenStartSignal)
	{
		startSignal = givenStartSignal;
	}
	
	public void setBrokerData(HashMap<String, PSBrokerPADRES> givenBrokers)
	{
		brokerData = givenBrokers;
		serverLog.debug(logHeader + "Broker data set.");
	}
	
	public void setClientData(HashMap<String, PSClientPADRES> givenClients)
	{
		clientData = givenClients;
		serverLog.debug(logHeader + "Client data set.");
	}
	
	public Integer generatePort()
	{
		port = 4444;
		return port;
	}
	
	public Integer getPort()
	{
		return port;
	}
	
	public void clearDatasets()
	{
		brokerData.clear();
		clientData.clear();
	}
	
	public void run()
	{
		setName("PSTBServer");
		
		while (brokerData.isEmpty() || clientData.isEmpty())
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
		int numNodes = numBrokers + clientData.size();
		CountDownLatch objectSent = new CountDownLatch(numNodes);
		CountDownLatch brokerObjectSent = new CountDownLatch(numBrokers);
		
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
			
			NodeHandler oH = new NodeHandler(objectPipe, objectSent, brokerObjectSent, startSignal, this);
			oH.setUncaughtExceptionHandler(nodeHandlerExceptionNet);		
			oH.start();
						
			numConnectedNodes++;
		}
		
		try 
		{
			objectSent.await();
		} 
		catch (InterruptedException e) 
		{
			endServerFailure(logHeader + "Error waiting for nodes: ", e, true);
		}
		serverLog.info(logHeader + "All objects sent.");
		
		serverLog.debug(logHeader + "Sending start signal.");
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
		
		return true;
	}
	
	public ServerPacket getNode(String givenName)
	{
		if(brokerData.containsKey(givenName))
		{
			ServerPacket broker = new ServerPacket(brokerData.get(givenName), false);
			return broker;
		}
		else if(clientData.containsKey(givenName))
		{
			ServerPacket client = new ServerPacket(clientData.get(givenName), true);
			return client;
		}
		else
		{
			return null;
		}
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
