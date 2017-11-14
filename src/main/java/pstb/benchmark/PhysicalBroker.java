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
 * The Process Class
 * Calls the Broker functions after the broker object has been created
 * @see PhysicalTopology
 * 
 * Algorithm
 * Search the args for the name flag
 * 	Exit with error
 * Else
 * 	Try to find associated broker object file
 * 	Can't
 * 		Exit with error
 * 	Can
 * 		Attempt to create broker
 * 		Attempt to start broker
 *
 * 		Any of these attempts fail
 * 			Exit with error
 * 		Otherwise
 * 			Float until killed
 */
public class PhysicalBroker {
	private static final String logHeader = "PhyBroker: ";
	private static final Logger logger = LogManager.getLogger(PhysicalBroker.class);
	
	public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException
	{
		String givenBrokerName = null;
		String givenContext = null;
		String masterMachineName = null;
		
		for (int i = 0; i < args.length; i++) 
		{
			if (args[i].equals("-n")) 
			{
				givenBrokerName = args[i + 1];
			}
			if (args[i].equals("-c")) 
			{
				givenContext = args[i + 1];
			}
			if (args[i].equals("-m")) 
			{
				masterMachineName = args[i + 1];
			}
		}
		
		if(givenBrokerName == null)
		{
			logger.error(logHeader + "no name was given!");
			System.exit(PSTBError.ERROR_ARGS_B);
		}
		if(givenContext == null)
		{
			logger.error(logHeader + "no context was given!");
			System.exit(PSTBError.ERROR_ARGS_B);
		}
		if(masterMachineName == null)
		{
			logger.error(logHeader + "no masterMachineName was given!");
			System.exit(PSTBError.ERROR_ARGS_B);
		}
		
		ThreadContext.put("broker", givenContext);
		Thread.currentThread().setName(givenContext);
		
		// Now let's get the Broker Object
		PSBrokerPADRES givenBroker = null;
		try 
		{
			FileInputStream fileIn = new FileInputStream(givenBrokerName + ".bro");
			ObjectInputStream oISIn = new ObjectInputStream(fileIn);
			givenBroker = (PSBrokerPADRES) oISIn.readObject();
			oISIn.close();
			fileIn.close();
		}
		catch (FileNotFoundException e) 
		{
			logger.error(logHeader + "Couldn't find " + givenBrokerName + "broker object file: ", e);
			System.exit(PSTBError.ERROR_FILE_B);
		}
		catch (IOException e)
		{
			logger.error(logHeader + "error accessing ObjectInputStream: ", e);
			System.exit(PSTBError.ERROR_IO_B);
		}
		catch(ClassNotFoundException e)
		{
			logger.error(logHeader + "can't find class: ", e);
			System.exit(PSTBError.ERROR_CNF_B);
		}
		
		givenBroker.setBrokerLogger(logger);
		
		boolean functionSuccessful = givenBroker.createBroker();
		if(!functionSuccessful)
		{
			logger.error(logHeader + "error creating broker");
			System.exit(PSTBError.ERROR_CREATE_B);
		}
		
		functionSuccessful = givenBroker.startBroker();
		if(!functionSuccessful)
		{
			logger.error(logHeader + "error starting broker");
			System.exit(PSTBError.ERROR_START_B);
		}
		
		logger.info(logHeader + "broker " + givenBroker.getName() + " started!");
	}
}

