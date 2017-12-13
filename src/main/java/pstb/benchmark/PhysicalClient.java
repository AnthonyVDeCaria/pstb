package pstb.benchmark;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
	private static final Logger log = LogManager.getLogger(PhysicalClient.class);
	
	/**
	 * The Main function
	 * @param args - the process arguments
	 */
	public static void main(String[] args)
	{
		Long currentTime = System.currentTimeMillis();
		String formattedTime = PSTBUtil.DATE_FORMAT.format(currentTime);
		ThreadContext.put("client", formattedTime);
		
		Physical helper = new Physical(log, logHeader);
		boolean argCheck = helper.parseArguments(args);
		if(!argCheck)
		{
			log.error(logHeader + "Not given proper arguments");
			System.exit(PSTBError.C_ARGS);
		}
		
		String context = helper.getContext();
		ThreadContext.put("client", context);
		Thread.currentThread().setName(context);
		
		// Now let's get the Client Object
		PSClientPADRES givenClient = null;
		
		boolean socketCreationCheck = helper.createSocket(); 
		if(!socketCreationCheck)
		{
			System.exit(PSTBError.C_SOCKET);
		}
		Socket connection = helper.getMasterConnection();
		
		OutputStream connOut = null;
		try
		{
			connOut = connection.getOutputStream();
		}
		catch(IOException e)
		{
			log.error(logHeader + "Couldn't create an OutputStream from connection!");
			System.exit(PSTBError.C_SOCKET);
		}
		
		String givenClientName = helper.getName();
		PSTBUtil.sendStringAcrossSocket(connOut, givenClientName, log, logHeader);
		
		Object check = helper.getObjectFromMaster();
		if(check == null)
		{
			log.error(logHeader + "Didn't get object from master!");
			System.exit(PSTBError.C_OBJECT);
		}
		givenClient = (PSClientPADRES) check;
		
		// Now that we have the client, let's set it's logger and get this show on the road
		givenClient.addLogger(log);
		
		boolean functionCheck = givenClient.initialize(true);
		if(!functionCheck)
		{
			log.error(logHeader + "Couldn't initialize client " + givenClientName);
			System.exit(PSTBError.C_INIT);
		}
		
		String start = helper.readConnection();
		if(start == null || !start.equals(PSTBUtil.START))
		{
			log.error(logHeader + "Didn't get start signal from master!");
			System.exit(PSTBError.C_START);
		}
		
		functionCheck = givenClient.startRun();
		if(!functionCheck)
		{
			log.error(logHeader + "Couldn't conduct experiment in client " + givenClientName);
			System.exit(PSTBError.C_RUN);
		}
		
		givenClient.disconnect();
		
		functionCheck = givenClient.shutdown();
		if(!functionCheck)
		{
			log.error(logHeader + "error shutting down client " + givenClientName);
			System.exit(PSTBError.C_SHUT);
		}
		
		// Now that the experiment is over, let's send the diary back to master
		String diaryName = givenClient.generateDiaryName();
		
		if(diaryName == null)
		{
			log.error(logHeader + "error generating a diary name for client " + givenClientName);
			System.exit(PSTBError.C_DIARY);
		}
		
		log.info(logHeader + "returning a diary object with name " + diaryName);
		FileOutputStream out = null;
		try 
		{
			out = new FileOutputStream(diaryName + ".dia");
		} 
		catch (FileNotFoundException e) 
		{
			log.error(logHeader + "Error creating FileOutputStream for diary: ", e);
			System.exit(PSTBError.C_DIARY);
		}
		
		functionCheck = PSTBUtil.sendObject(givenClient.getDiary(), diaryName, out, log, logHeader);
		if(!functionCheck)
		{
			log.error(logHeader + "error sending a diary to master from client " + givenClientName);
			System.exit(PSTBError.C_DIARY);
		}
		
		try 
		{
			out.close();
		} 
		catch (IOException e) 
		{
			log.error(logHeader + "error closing diary OutputStream: ", e);
			System.exit(PSTBError.C_DIARY);
		}
		
		if(helper.areWeDistributed())
		{
			String ipAddress = helper.getMasterIPAddress();
			String[] command = {"scripts/nodeSendUpstream.sh", helper.getUser(), ipAddress, "dia"};
			boolean sendDiaryCheck = PSTBUtil.createANewProcess(command, log, false,
																	"Error creating process to send " + givenClientName + "'s diary: ", 
																	"Sent " + givenClientName + "'s diary upstream.", 
																	"Couldn't send " + givenClientName + "'s diary upstream.");
			if(!sendDiaryCheck)
			{
				log.error(logHeader + "error sending diary!");
				System.exit(PSTBError.C_DIARY);
			}
		}
		
		log.info("Successful run with client " + givenClientName);
		System.exit(0);
	}
}
