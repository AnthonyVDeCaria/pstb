package pstb.benchmark;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import pstb.util.PSTBError;
import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 *
 * The Client Process
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
	private static final Logger phyClientLogger = LogManager.getLogger(PhysicalClient.class);
	
	/**
	 * The Main function
	 * @param args - the process arguments
	 */
	public static void main(String[] args)
	{
		String givenClientName = null;
		String givenRunNumber = null;
		
		boolean nameWasGiven = false;
		boolean runWasGiven = false;
		for (int i = 0; i < args.length; i++) 
		{
			if (args[i].equals("-n")) 
			{
				givenClientName = args[i + 1];
				nameWasGiven = true;
			}
			if (args[i].equals("-r")) 
			{
				givenRunNumber = args[i + 1];
				runWasGiven = true;
			}
		}
		
		if(!nameWasGiven)
		{
			phyClientLogger.error(logHeader + "no name was given");
			System.exit(PSTBError.ERROR_ARGS_C);
		}
		if(!runWasGiven)
		{
			phyClientLogger.error(logHeader + "no Run Number was given");
			System.exit(PSTBError.ERROR_ARGS_C);
		}
		
		String context = givenRunNumber + "-" + givenClientName;
		ThreadContext.put("client", context);
		
		PSClientPADRES givenClient = null;
		try 
		{
			FileInputStream fileIn = new FileInputStream("/tmp/" + givenClientName + ".cli");
			ObjectInputStream oISIn = new ObjectInputStream(fileIn);
			givenClient = (PSClientPADRES) oISIn.readObject();
			oISIn.close();
			fileIn.close();
		} 
		catch (FileNotFoundException e) 
		{
			phyClientLogger.error(logHeader + "couldn't find serialized client file ", e);
			System.exit(PSTBError.ERROR_FILE_C);
		}
		catch (IOException e)
		{
			phyClientLogger.error(logHeader + "error accessing ObjectInputStream ", e);
			System.exit(PSTBError.ERROR_IO_C);
		}
		catch(ClassNotFoundException e)
		{
			phyClientLogger.error(logHeader + "can't find class ", e);
			System.exit(PSTBError.ERROR_CNF_C);
		}
		
		givenClient.addLogger(phyClientLogger);
				
		boolean functionCheck = givenClient.initialize(true);
		if(!functionCheck)
		{
			phyClientLogger.error(logHeader + "couldn't initialize client " + givenClientName);
			System.exit(PSTBError.ERROR_INIT_C);
		}
		
//		functionCheck = givenClient.connect();
//		if(!functionCheck)
//		{
//			phyClientLogger.error(logHeader + "couldn't connect client " + givenClientName + " to its brokers");
//			System.exit(PSTBError.ERROR_CONN_C);
//		}
		
		functionCheck = givenClient.startRun();
		if(!functionCheck)
		{
			phyClientLogger.error(logHeader + "error running client " + givenClientName);
			System.exit(PSTBError.ERROR_START_C);
		}
		
		givenClient.disconnect();
		
		functionCheck = givenClient.shutdown();
		if(!functionCheck)
		{
			phyClientLogger.error(logHeader + "error shutting down client " + givenClientName);
			System.exit(PSTBError.ERROR_SHUT_C);
		}
		
		String diaryName = givenClient.generateDiaryName();
		
		functionCheck = PSTBUtil.createObjectFile(givenClient.getDiary(), diaryName, ".dia", phyClientLogger, logHeader);
		if(!functionCheck)
		{
			phyClientLogger.error(logHeader + "error generating a diary file for client " + givenClientName);
			System.exit(PSTBError.ERROR_DIARY);
		}
		
		phyClientLogger.info("Successful run with client " + givenClientName);
		System.exit(0);
	}
}
