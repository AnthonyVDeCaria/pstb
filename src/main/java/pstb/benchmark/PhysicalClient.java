package pstb.benchmark;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
	private static final String localhost = "localhost";
	private static final String logHeader = "PhyClient: ";
	private static final Logger logger = LogManager.getLogger(PhysicalClient.class);
	
	/**
	 * The Main function
	 * @param args - the process arguments
	 */
	public static void main(String[] args)
	{
		String givenClientName = null;
		String givenDiaryName = null;
		String masterMachineName = null;
		
		for (int i = 0; i < args.length; i++) 
		{
			if (args[i].equals("-n")) 
			{
				givenClientName = args[i + 1];
			}
			if (args[i].equals("-d")) 
			{
				givenDiaryName = args[i + 1];
			}
			if (args[i].equals("-m")) 
			{
				masterMachineName = args[i + 1];
			}
		}
		
		if(givenClientName == null)
		{
			logger.error(logHeader + "no Client Name was given!");
			System.exit(PSTBError.ERROR_ARGS_C);
		}
		if(givenDiaryName == null)
		{
			logger.error(logHeader + "no Diary Name was given!");
			System.exit(PSTBError.ERROR_ARGS_C);
		}
		if(masterMachineName == null)
		{
			logger.error(logHeader + "no masterMachineName was given!");
			System.exit(PSTBError.ERROR_ARGS_C);
		}
		
		ThreadContext.put("client", givenDiaryName);
		Thread.currentThread().setName(givenDiaryName);
		
		// Now let's get the Client Object
		PSClientPADRES givenClient = null;	
		try 
		{
			FileInputStream fileIn = new FileInputStream(givenClientName + ".cli");
			ObjectInputStream oISIn = new ObjectInputStream(fileIn);
			givenClient = (PSClientPADRES) oISIn.readObject();
			oISIn.close();
			fileIn.close();
		}
		catch (FileNotFoundException e) 
		{
			logger.error(logHeader + "Couldn't find " + givenClientName + " client object file: ", e);
			System.exit(PSTBError.ERROR_FILE_C);
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
		
		// Now that we have the client, let's set it's logger and get this show on the road
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
		
		// Now that the experiment is over, let's send the diary back to master
		String diaryName = givenClient.generateDiaryName();
		
		if(diaryName == null)
		{
			logger.error(logHeader + "error generating a diary name for client " + givenClientName);
			System.exit(PSTBError.ERROR_DIARY);
		}
		
		logger.info(logHeader + "returning a diary object with name " + diaryName);
		FileOutputStream out = null;
		try 
		{
			out = new FileOutputStream(diaryName + ".dia");
		} 
		catch (FileNotFoundException e) 
		{
			logger.error(logHeader + "Error creating FileOutputStream for diary: ", e);
			System.exit(PSTBError.ERROR_FILE_C);
		}
		
		functionCheck = PSTBUtil.sendObject(givenClient.getDiary(), diaryName, out, logger, logHeader);
		if(!functionCheck)
		{
			logger.error(logHeader + "error sending a diary to master from client " + givenClientName);
			System.exit(PSTBError.ERROR_DIARY);
		}
		
		try 
		{
			out.close();
		} 
		catch (IOException e) 
		{
			logger.error(logHeader + "error closing diary OutputStream: ", e);
			System.exit(PSTBError.ERROR_IO_C);
		}
		
		if(!masterMachineName.equals(localhost))
		{
			String[] command = {"scripts/nodeSendUpstream.sh", "adecaria", masterMachineName, "dia"};
			boolean sendDiaryCheck = PSTBUtil.createANewProcess(command, logger, 
																	"Error creating process to send " + givenClientName + "'s diary: ", 
																	"Sent " + givenClientName + "'s diary upstream.", 
																	"Couldn't send " + givenClientName + "'s diary upstream.");
			
			if(!sendDiaryCheck)
			{
				logger.error(logHeader + "error sending diary!");
				System.exit(PSTBError.ERROR_IO_C);
			}
		}
		
		logger.info("Successful run with client " + givenClientName);
		System.exit(0);
	}
}
