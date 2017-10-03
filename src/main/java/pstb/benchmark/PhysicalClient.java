/**
 * 
 */
package pstb.benchmark;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
		String givenClientName = null;
		
		for (int i = 0; i < args.length; i++) 
		{
			if (args[i].equals("-n")) 
			{
				givenClientName = args[i + 1];
				break;
			}	
		}
		
		PSClientPADRES givenClient = null;
 
		try 
		{
			FileInputStream fileIn = new FileInputStream("/tmp/" + givenClientName + ".ser");
			ObjectInputStream oISIn = new ObjectInputStream(fileIn);
			givenClient = (PSClientPADRES) oISIn.readObject();
			oISIn.close();
			fileIn.close();
		} 
		catch (FileNotFoundException e) 
		{
			phyClientLogger.error(logHeader + "couldn't find serialized client file ", e);
			return;
		}
		catch (IOException e)
		{
			phyClientLogger.error(logHeader + "error accessing ObjectInputStream ", e);
			return;
		}
		catch(ClassNotFoundException e)
		{
			phyClientLogger.error(logHeader + "can't find class ", e);
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
