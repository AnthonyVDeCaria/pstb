package pstb.benchmark.process.client;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.logging.log4j.Logger;

import pstb.analysis.diary.ClientDiary;
import pstb.benchmark.object.PSNode;
import pstb.benchmark.object.client.PSClient;
import pstb.benchmark.object.client.PSClientMode;
import pstb.benchmark.process.PSTBProcess;
import pstb.creation.topology.PADRESTopology;
import pstb.startup.config.SupportedEngines.PSEngine;
import pstb.startup.topology.NodeRole;
import pstb.util.PSTBError;
import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 *
 * The Client Process
 * Calls the client functions after the client object has been created
 * @see PADRESTopology
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
public abstract class PSTBClientProcess extends PSTBProcess {
	public PSTBClientProcess(String givenName, String givenContext, String givenIPAddress, Integer givenPort, PSEngine givenEngine, 
			NodeRole givenRole, boolean areWeDistributed, String givenUsername, Socket givenConnection, OutputStream givenOut, 
			Logger givenLog, String givenLogHeader, String givenThreadContextString) 
	{
		super(givenName, givenContext, givenIPAddress, givenPort,givenEngine, givenRole, areWeDistributed, givenUsername, 
				givenConnection, givenOut, givenLog, givenLogHeader, givenThreadContextString);
	}
	
	@Override
	public void complete(PSNode givenNode)
	{
		PSClient givenClient = (PSClient) givenNode;
		
		PSClientMode givenMode = givenClient.getClientMode();
		if((givenMode.equals(PSClientMode.TPPub)) || (givenMode.equals(PSClientMode.TPSub)))
		{
			givenClient.setMIP(masterIPAddress);
			givenClient.setMasterPort(portNumber);
		}
		
		log.debug(logHeader + "The requested brokers are:");
		givenClient.getBrokersURIs().forEach((brokerURI)->{
			log.debug(brokerURI);
		});
		
		log.debug(logHeader + "Attempting to setup client...");
		boolean setupCheck = setup(givenClient);
		if(!setupCheck)
		{
			log.error("Couldn't setup client!");
			System.exit(PSTBError.C_SETUP);
		}
		log.debug(logHeader + "Client setup.");
		
		initalized();
		
		log.debug(logHeader + "Attempting to get start signal from master...");
		String start = PSTBUtil.readConnection(connection, log, logHeader);
		if(start == null || !start.equals(PSTBUtil.START))
		{
			log.error(logHeader + "Didn't get start signal from master!");
			System.exit(PSTBError.C_START);
		}
		log.info(logHeader + "Start signal received.");
		
		log.debug(logHeader + "Attempting to run experiment...");
		boolean runCheck = run();
		if(!runCheck)
		{
			log.error("Couldn't run experiment!");
			System.exit(PSTBError.C_RUN);
		}
		log.debug(logHeader + "Run complete.");
		
//		log.debug(logHeader + "Attempting to clean up after experiment...");
//		boolean cleanupCheck = cleanup();
//		if(!cleanupCheck)
//		{
//			log.error("Couldn't cleanup after run!");
//			System.exit(PSTBError.C_CLEANUP);
//		}
//		log.info(logHeader + "Clean up complete.");
		
		log.debug(logHeader + "recording a diary object with name " + context);
		String diaryFileString = context + ".dia";
		FileOutputStream out = null;
		try 
		{
			out = new FileOutputStream(diaryFileString);
		} 
		catch (FileNotFoundException e) 
		{
			log.error(logHeader + "Couldn't create a FileOutputStream to record diary object: ", e);
			System.exit(PSTBError.C_DIARY);
		}
		
		if(givenMode.equals(PSClientMode.TPPub))
		{
			log.error(logHeader + "No diary record needed.");
		}
		else
		{
			ClientDiary currentDiary = givenClient.getDiary();
			boolean diaryCheck = PSTBUtil.sendObject(currentDiary, out, log, logHeader);
			if(!diaryCheck)
			{
				log.error(logHeader + "Couldn't record " + nodeName + "'s diary object!");
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
			
//			if(distributed)
//			{
//				String[] command = {"scripts/sendDiaryUpstream.sh", username, masterIPAddress, diaryFileString};
//				boolean sendDiaryCheck = PSTBUtil.createANewProcess(command, log, false,
//																		"Error creating process to send " + nodeName + "'s diary: ", 
//																		"Sent " + nodeName + "'s diary upstream.", 
//																		"Couldn't send " + nodeName + "'s diary upstream.");
//				if(!sendDiaryCheck)
//				{
//					log.error(logHeader + "error sending diary!");
//					System.exit(PSTBError.C_DIARY);
//				}
//			}
		}
		
		log.info("Successful run with client " + nodeName);
		System.exit(0);
	}
	
	protected abstract boolean cleanup();
}
