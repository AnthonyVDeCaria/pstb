package pstb.benchmark;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import pstb.util.PSTBError;
import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 * 
 * The Process Class
 * Calls the Broker functions after the broker object has been created
 * @see PhysicalTopology
 * 
 * Algorithm
 * Search the args for the name flag
 * If it's not there
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
		String givenRunNumber = null;
		String givenObjectPort = null;
		
		for (int i = 0; i < args.length; i++) 
		{
			if (args[i].equals("-n")) 
			{
				givenBrokerName = args[i + 1];
			}
			if (args[i].equals("-r")) 
			{
				givenRunNumber = args[i + 1];
			}
			if (args[i].equals("-p")) 
			{
				givenObjectPort = args[i + 1];
			}
		}
		
		if(givenBrokerName == null)
		{
			logger.error(logHeader + "no name was given");
			System.exit(PSTBError.ERROR_ARGS_B);
		}
		if(givenRunNumber == null)
		{
			logger.error(logHeader + "no Run Number was given");
			System.exit(PSTBError.ERROR_ARGS_B);
		}
		if(givenObjectPort == null)
		{
			logger.error(logHeader + "no Object Port was given");
			System.exit(PSTBError.ERROR_ARGS_B);
		}
		
		String context = givenRunNumber + "-" + givenBrokerName;
		ThreadContext.put("broker", context);
		Thread.currentThread().setName(context);
		
		boolean local = givenObjectPort.equals("null");
		ServerSocket socketConnection = null;
		Socket pipe = null;
		
		PSBrokerPADRES givenBroker = null;
		
		InputStream in = null;
		if(local)
		{
			try
			{
				in = new FileInputStream("/tmp/" + givenBrokerName + ".cli");
			}
			catch (FileNotFoundException e) 
			{
				logger.error(logHeader + "Couldn't create input file for client object " + givenBrokerName + ": ", e);
				System.exit(PSTBError.ERROR_FILE_B);
			}
		}
		else
		{
			Integer objectPort = PSTBUtil.checkIfInteger(givenObjectPort, true, logger);
			
			if(objectPort == null)
			{
				logger.error(logHeader + "given Object port is not an Integer!");
				System.exit(PSTBError.ERROR_ARGS_B);
			}
			else
			{
				try
				{
					socketConnection = new ServerSocket(objectPort);
					pipe = socketConnection.accept();
					
					in = pipe.getInputStream();
				}
				catch (IOException e)
				{
					logger.error(logHeader + "error generating socket InputStream: ", e);
					System.exit(PSTBError.ERROR_IO_B);
				}
			}
		}
		
		if(in == null)
		{
			logger.error(logHeader + "Input error!");
			System.exit(PSTBError.ERROR_IO_C);
		}
		else
		{
			try 
			{
				ObjectInputStream oISIn = new ObjectInputStream(in);
				givenBroker = (PSBrokerPADRES) oISIn.readObject();
				oISIn.close();
				in.close();
			}
			catch (IOException e)
			{
				logger.error(logHeader + "error accessing ObjectInputStream ", e);
				System.exit(PSTBError.ERROR_IO_B);
			}
			catch(ClassNotFoundException e)
			{
				logger.error(logHeader + "can't find class ", e);
				System.exit(PSTBError.ERROR_CNF_B);
			}
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
		
		logger.info(logHeader + "broker " + givenBroker.getName() + " started");
	}
}
