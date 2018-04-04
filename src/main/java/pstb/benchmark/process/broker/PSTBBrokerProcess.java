package pstb.benchmark.process.broker;

import java.io.OutputStream;
import java.net.Socket;

import org.apache.logging.log4j.Logger;

import pstb.benchmark.object.PSNode;
import pstb.benchmark.process.PSTBProcess;
import pstb.creation.topology.PADRESTopology;
import pstb.startup.config.SupportedEngines.PSEngine;
import pstb.startup.topology.NodeRole;
import pstb.util.PSTBError;

/**
 * @author padres-dev-4187
 * 
 * The Process Class
 * Calls the Broker functions after the broker object has been created
 * @see PADRESTopology
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
public abstract class PSTBBrokerProcess extends PSTBProcess{
	public PSTBBrokerProcess(String givenName, String givenContext, String givenIPAddress, Integer givenPort, PSEngine givenEngine, 
			NodeRole givenRole, boolean areWeDistributed, String givenUsername, Socket givenConnection, OutputStream givenOut, 
			Logger givenLog, String givenLogHeader, String givenThreadContextString) 
	{
		super(givenName, givenContext, givenIPAddress, givenPort,givenEngine, givenRole, areWeDistributed, givenUsername, 
				givenConnection, givenOut, givenLog, givenLogHeader, givenThreadContextString);
	}
	
	@Override
	public void complete(PSNode givenNode)
	{
		log.debug(logHeader + "Attempting to setup broker...");
		boolean setupCheck = setup(givenNode);
		if(!setupCheck)
		{
			log.error("Couldn't setup broker!");
			error();
			System.exit(PSTBError.B_SETUP);
		}
		log.info(logHeader + "Broker setup.");
		
		log.debug(logHeader + "Attempting to run broker...");
		boolean runCheck = run();
		if(!runCheck)
		{
			log.error("Couldn't run broker!");
			error();
			System.exit(PSTBError.B_RUN);
		}
		log.info(logHeader + "Broker running.");
		
		initalized();
	}
}

