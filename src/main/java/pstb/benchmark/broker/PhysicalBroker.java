package pstb.benchmark.broker;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import pstb.benchmark.Physical;
import pstb.benchmark.broker.padres.PSTBBrokerPADRES;
import pstb.creation.PhysicalTopology;
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
	private static final Logger log = LogManager.getLogger(PhysicalBroker.class);
	
	public static void main(String[] args)
	{
		Long currentTime = System.currentTimeMillis();
		String formattedTime = PSTBUtil.DATE_FORMAT.format(currentTime);
		ThreadContext.put("broker", formattedTime);
		
		Physical helper = new Physical(log, logHeader);
		boolean argCheck = helper.parseArguments(args);
		if(!argCheck)
		{
			log.error(logHeader + "Not given proper arguments");
			System.exit(PSTBError.B_ARGS);
		}
		
		String givenContext = helper.getContext();
		ThreadContext.put("broker", givenContext);
		Thread.currentThread().setName(givenContext);
		
		// Now let's get the Broker Object
		PSTBBrokerPADRES givenBroker = null;
		
		boolean socketCreationCheck = helper.createSocket(); 
		if(!socketCreationCheck)
		{
			System.exit(PSTBError.B_SOCKET);
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
			System.exit(PSTBError.B_SOCKET);
		}
		
		String givenBrokerName = helper.getName();
		PSTBUtil.sendStringAcrossSocket(connOut, givenBrokerName, log, logHeader);
		
		Object check = helper.getObjectFromMaster();
		if(check == null)
		{
			log.error(logHeader + "Didn't get object from master!");
			System.exit(PSTBError.B_OBJECT);
		}
		givenBroker = (PSTBBrokerPADRES) check;
		
		// Have broker, will run (assuming everything is working)
		boolean functionSuccessful = givenBroker.createBroker();
		if(!functionSuccessful)
		{
			log.error(logHeader + "error creating broker");
			System.exit(PSTBError.B_CREATE);
		}
		
		functionSuccessful = givenBroker.startBroker();
		if(!functionSuccessful)
		{
			log.error(logHeader + "error starting broker");
			System.exit(PSTBError.B_START);
		}
		
		log.info(logHeader + "broker " + givenBroker.getName() + " started!");
	}
}

