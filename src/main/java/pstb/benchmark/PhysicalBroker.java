package pstb.benchmark;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import pstb.util.PSTBError;

/**
 * @author padres-dev-4187
 *
 */
public class PhysicalBroker {
	private static final String logHeader = "PhyBroker: ";
	private static final Logger phyBrokerLogger = LogManager.getLogger(PhysicalBroker.class);
	
	public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException
	{
		String givenBrokerName = null;
		
		boolean nameWasGiven = false;
		for (int i = 0; i < args.length; i++) 
		{
			if (args[i].equals("-n")) 
			{
				givenBrokerName = args[i + 1];
				nameWasGiven = true;
				break;
			}
		}
		
		if(!nameWasGiven)
		{
			phyBrokerLogger.error(logHeader + "no name was given");
			System.exit(PSTBError.ERROR_NO_NAME_B);
		}
		
		ThreadContext.put("broker", givenBrokerName);
		
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
			System.exit(PSTBError.ERROR_FILE_B);
		}
		catch (IOException e)
		{
			phyBrokerLogger.error(logHeader + "error accessing ObjectInputStream ", e);
			System.exit(PSTBError.ERROR_IO_B);
		}
		catch(ClassNotFoundException e)
		{
			phyBrokerLogger.error(logHeader + "can't find class ", e);
			System.exit(PSTBError.ERROR_CNF_B);
		}
		
		givenBroker.setBrokerLogger(phyBrokerLogger);
		
		boolean functionSuccessful = givenBroker.createBroker();
		
		if(!functionSuccessful)
		{
			phyBrokerLogger.error(logHeader + "error creating broker");
			System.exit(PSTBError.ERROR_CREATE_B);
		}
		
		functionSuccessful = givenBroker.startBroker();
		
		if(!functionSuccessful)
		{
			phyBrokerLogger.error(logHeader + "error starting broker");
			System.exit(PSTBError.ERROR_START_B);
		}
		
		phyBrokerLogger.info(logHeader + "broker " + givenBroker.getName() + " started");
	}
}
