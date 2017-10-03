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
public class PhysicalBroker {
	private static final String logHeader = "PhyBroker: ";
	private static final Logger phyBrokerLogger = LogManager.getRootLogger();
	
	public static void main(String[] args)
	{
		Long startTime = System.nanoTime();
		ObjectInputStream in = null;
		try
		{
			in = new ObjectInputStream(System.in);
		}
		catch (IOException e)
		{
			phyBrokerLogger.error(logHeader + "error accessing ObjectInputStream ", e);
			return;
		}
		
		PSBrokerPADRES givenBroker = null;
		
		try
		{
			givenBroker = (PSBrokerPADRES) in.readObject();
		}
		catch(ClassNotFoundException e)
		{
			phyBrokerLogger.error(logHeader + "can't find class ", e);
			return;
		} 
		catch (IOException e) 
		{
			phyBrokerLogger.error(logHeader + "error accessing readObject() ", e);
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
