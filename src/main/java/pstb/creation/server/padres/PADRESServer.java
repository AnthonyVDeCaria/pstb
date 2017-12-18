/**
 * 
 */
package pstb.creation.server.padres;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.ThreadContext;

import pstb.benchmark.broker.padres.PSTBBrokerPADRES;
import pstb.benchmark.client.padres.PSTBClientPADRES;
import pstb.creation.server.PSTBServer;
import pstb.creation.server.ServerPacket;

/**
 * @author padres-dev-4187
 *
 */
public class PADRESServer extends PSTBServer {
	
	private HashMap<String, PSTBBrokerPADRES> brokerData;
	private HashMap<String, PSTBClientPADRES> clientData;
	
	private String logHeader = "PADRESServer: ";

	public PADRESServer(int givenRunNumber) {
		super(givenRunNumber);
		
		brokerData = new HashMap<String, PSTBBrokerPADRES>();
		clientData = new HashMap<String, PSTBClientPADRES>();
	}
	
	public void setBrokerData(HashMap<String, PSTBBrokerPADRES> givenBrokers)
	{
		brokerData = givenBrokers;
		serverLog.debug(logHeader + "Broker data set.");
	}
	
	public void setClientData(HashMap<String, PSTBClientPADRES> givenClients)
	{
		clientData = givenClients;
		serverLog.debug(logHeader + "Client data set.");
	}
	
	public void clearDatasets()
	{
		brokerData.clear();
		clientData.clear();
	}
	
	public void run()
	{
		String serverName = "PADRESServer-" + runNumber; 
		setName(serverName);
		ThreadContext.put("server", serverName);
		Thread.currentThread().setName(serverName);
		
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
		boolean socketCheck = super.createServerSocket();
		if(!socketCheck)
		{
			endServerFailure(logHeader + "Couldn't create socket!", null, false);
		}
		serverLog.info(logHeader + "Socket created.");
		
		int numBrokers = brokerData.size();
		int numClients = clientData.size();
		int numNodes = numBrokers + numClients;
		CountDownLatch clientsWaiting = new CountDownLatch(numClients);
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
			
			PADRESNodeHandler oH = new PADRESNodeHandler(objectPipe, clientsWaiting, brokerObjectSent, startSignal, this);
			oH.setUncaughtExceptionHandler(nodeHandlerExceptionNet);		
			oH.start();
						
			numConnectedNodes++;
		}
		
		try 
		{
			clientsWaiting.await();
		} 
		catch (InterruptedException e) 
		{
			endServerFailure(logHeader + "Error waiting for nodes: ", e, true);
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

}
