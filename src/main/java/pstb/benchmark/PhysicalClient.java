/**
 * 
 */
package pstb.benchmark;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author padres-dev-4187
 *
 */
public class PhysicalClient {
	private static final String logHeader = "PhyClient: ";
	private static final Logger phyClientLogger = LogManager.getRootLogger();
	
	public static void main(String[] args)
	{
		ObjectInputStream in = null;
		try
		{
			in = new ObjectInputStream(System.in);
		}
		catch (IOException e)
		{
			phyClientLogger.error(logHeader + "error accessing ObjectInputStream ", e);
			return;
		}
		
		PSClientPADRES givenClient = null;
		
		try
		{
			givenClient = (PSClientPADRES) in.readObject();
		}
		catch(ClassNotFoundException e)
		{
			phyClientLogger.error(logHeader + "can't find class ", e);
			return;
		} 
		catch (IOException e) 
		{
			phyClientLogger.error(logHeader + "error accessing readObject() ", e);
			return;
		}
		
		String cName = givenClient.getClientName();
		
		boolean functionCheck = givenClient.initialize();
		
		if(!functionCheck)
		{
			phyClientLogger.error(logHeader + "couldn't initialize client " + cName);
			return;
		}
		
		functionCheck = givenClient.connect();
		
		if(!functionCheck)
		{
			phyClientLogger.error(logHeader + "couldn't connect client " + cName + " to its brokers");
			return;
		}
		
		functionCheck = givenClient.startRun();
		
		if(!functionCheck)
		{
			phyClientLogger.error(logHeader + "error running client" + cName);
			return;
		}
		
		givenClient.disconnect();
		
		functionCheck = givenClient.shutdown();
		
		if(!functionCheck)
		{
			phyClientLogger.error(logHeader + "error shutting down client" + cName);
			return;
		}
		
		phyClientLogger.info("Successful run with client " + cName);
		return;
	}

}
