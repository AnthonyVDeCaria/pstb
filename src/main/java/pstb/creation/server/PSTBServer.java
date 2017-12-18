/**
 * 
 */
package pstb.creation.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

/**
 * @author padres-dev-4187
 *
 */
public class PSTBServer extends Thread
{
	protected CountDownLatch startSignal;
	
	protected Integer port;
	protected ServerSocket objectConnection;
	
	protected int runNumber;
	
	private String logHeader = "Server: ";
	protected Logger serverLog = LogManager.getLogger(PSTBServer.class);
	
	public PSTBServer(int givenRunNumber)
	{
		startSignal = null;
		
		port = null;
		objectConnection = null;
		
		runNumber = givenRunNumber;
		
		String serverName = "PADRESServer-" + runNumber; 
		setName(serverName);
		ThreadContext.put("server", serverName);
		Thread.currentThread().setName(serverName);
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
	
	public Integer getPort()
	{
		return port;
	}
	
	protected boolean createServerSocket()
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
	
	protected void endServerFailure(String record, Exception givenException, boolean exceptionPresent)
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
