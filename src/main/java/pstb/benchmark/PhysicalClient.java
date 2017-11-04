package pstb.benchmark;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
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
 * The Client Process
 * Calls the client functions after the client object has been created
 * @see PhysicalTopology
 *
 * Algorithm
 * Search the args for the name flag
 * If it's not there
 * 	Exit with error
 * Else
 * 	Try to find associated client object file
 * 	Can't
 * 		Exit with error
 * 	Can
 * 		Attempt to initialize client
 * 		Attempt to connect client
 * 		Attempt to start client
 * 		Disconnect client
 * 		Attempt to shutdown client
 *
 * 		Any of these attempts fail
 * 			Exit with error
 * 		Otherwise
 * 			Exit with success
 */
public class PhysicalClient {
	private static final String logHeader = "PhyClient: ";
	private static final Logger logger = LogManager.getLogger(PhysicalClient.class);
	
	/**
	 * The Main function
	 * @param args - the process arguments
	 */
	public static void main(String[] args)
	{
		String givenClientName = null;
		String givenRunNumber = null;
		String givenObjectPort = null;
		
		for (int i = 0; i < args.length; i++) 
		{
			if (args[i].equals("-n")) 
			{
				givenClientName = args[i + 1];
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
		
		if(givenClientName == null)
		{
			logger.error(logHeader + "no name was given");
			System.exit(PSTBError.ERROR_ARGS_C);
		}
		if(givenRunNumber == null)
		{
			logger.error(logHeader + "no Run Number was given");
			System.exit(PSTBError.ERROR_ARGS_C);
		}
		if(givenObjectPort == null)
		{
			logger.error(logHeader + "no Object Port was given");
			System.exit(PSTBError.ERROR_ARGS_C);
		}
		
		String context = givenRunNumber + "-" + givenClientName;
		ThreadContext.put("client", context);
		Thread.currentThread().setName(context);
				
		boolean local = givenObjectPort.equals("null");
		ServerSocket socketConnection = null;
		Socket pipe = null;
		
		PSClientPADRES givenClient = null;
		
		InputStream in = null;
		if(local)
		{
			try
			{
				in = new FileInputStream("/tmp/" + givenClientName + ".cli");
			}
			catch (FileNotFoundException e) 
			{
				logger.error(logHeader + "Couldn't create input file for client object " + givenClientName + ": ", e);
				System.exit(PSTBError.ERROR_FILE_C);
			}
		}
		else
		{
			Integer objectPort = PSTBUtil.checkIfInteger(givenObjectPort, true, logger);
			
			if(objectPort == null)
			{
				logger.error(logHeader + "given Object port is not an Integer!");
				System.exit(PSTBError.ERROR_ARGS_C);
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
					System.exit(PSTBError.ERROR_IO_C);
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
				givenClient = (PSClientPADRES) oISIn.readObject();
				oISIn.close();
				in.close();
			} 
			catch (IOException e)
			{
				logger.error(logHeader + "error accessing ObjectInputStream: ", e);
				System.exit(PSTBError.ERROR_IO_C);
			}
			catch(ClassNotFoundException e)
			{
				logger.error(logHeader + "can't find class: ", e);
				System.exit(PSTBError.ERROR_CNF_C);
			}
		}
				
		givenClient.addLogger(logger);
		
		boolean functionCheck = givenClient.initialize(true);
		if(!functionCheck)
		{
			logger.error(logHeader + "couldn't initialize client " + givenClientName);
			System.exit(PSTBError.ERROR_INIT_C);
		}
		
		functionCheck = givenClient.startRun();
		if(!functionCheck)
		{
			logger.error(logHeader + "error running client " + givenClientName);
			System.exit(PSTBError.ERROR_START_C);
		}
		
		givenClient.disconnect();
		
		functionCheck = givenClient.shutdown();
		if(!functionCheck)
		{
			logger.error(logHeader + "error shutting down client " + givenClientName);
			System.exit(PSTBError.ERROR_SHUT_C);
		}
		
		String diaryName = givenClient.generateDiaryName();
		if(diaryName == null)
		{
			logger.error(logHeader + "error generating a diary name for client " + givenClientName);
			System.exit(PSTBError.ERROR_DIARY);
		}
		logger.info(logHeader + "creating a diary object with name " + diaryName);
		OutputStream out = null;
		if(local)
		{
			try 
			{
				out = new FileOutputStream("tmp/" + diaryName + ".dia");
			} 
			catch (FileNotFoundException e) 
			{
				logger.error(logHeader + "Error creating FileOutputStream for diary: ", e);
				System.exit(PSTBError.ERROR_FILE_C);
			}
		}
		else
		{
			try 
			{
				out = pipe.getOutputStream();
			} 
			catch (IOException e) 
			{
				logger.error(logHeader + "Error creating socket OutputStream for diary: ", e);
				System.exit(PSTBError.ERROR_IO_C);
			}
		}
		
		if(out == null)
		{
			logger.error(logHeader + "couldn't generate OutputStream for diaries!");
			System.exit(PSTBError.ERROR_IO_C);
		}
		
		functionCheck = PSTBUtil.sendObject(givenClient.getDiary(), diaryName, out, logger, logHeader);
		if(!functionCheck)
		{
			logger.error(logHeader + "error generating a diary file for client " + givenClientName);
			System.exit(PSTBError.ERROR_DIARY);
		}
		
		try 
		{
			out.close();
		} 
		catch (IOException e) 
		{
			logger.error(logHeader + "error closing OutputStream: ", e);
			System.exit(PSTBError.ERROR_IO_C);
		}
		
		logger.info("Successful run with client " + givenClientName);
		System.exit(0);
	}
}
