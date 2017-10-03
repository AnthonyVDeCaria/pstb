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
public class PhysicalBroker {
	private static final String logHeader = "PhyBroker: ";
	private static final Logger phyBrokerLogger = LogManager.getRootLogger();
	
	public static void main(String[] args)
	{
		String givenBrokerName = null;
		
		for (int i = 0; i < args.length; i++) 
		{
			if (args[i].equals("-n")) 
			{
				givenBrokerName = args[i + 1];
				break;
			}	
		}
		
		Long startTime = System.nanoTime();
		
		PSBrokerPADRES givenBroker = null;
		try 
		{
			FileInputStream fileIn = new FileInputStream("/tmp/" + givenBrokerName + ".ser");
			ObjectInputStream oISIn = new ObjectInputStream(fileIn);
			givenBroker = (PSBrokerPADRES) oISIn.readObject();
			oISIn.close();
			fileIn.close();
		} 
		catch (FileNotFoundException e) 
		{
			phyBrokerLogger.error(logHeader + "couldn't find serialized broker file ", e);
			return;
		}
		catch (IOException e)
		{
			phyBrokerLogger.error(logHeader + "error accessing ObjectInputStream ", e);
			return;
		}
		catch(ClassNotFoundException e)
		{
			phyBrokerLogger.error(logHeader + "can't find class ", e);
			return;
		}
		
		boolean functionSuccessful = givenBroker.createBroker();
		
		if(!functionSuccessful)
		{
			phyBrokerLogger.error(logHeader + "error creating broker");
			return;
		}
		
		functionSuccessful = givenBroker.startBroker();
		
		if(!functionSuccessful)
		{
			phyBrokerLogger.error(logHeader + "error starting broker");
			return;
		}
		
		Long currentTime = System.nanoTime();
		
		while(currentTime - startTime < givenBroker.getRunLength())
		{
			currentTime = System.nanoTime();
		}
	}
}
