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
import pstb.util.PSTBUtil;

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
			System.exit(PSTBError.B_SETUP);
		}
		log.info(logHeader + "Broker setup.");
		
		log.debug(logHeader + "Attempting to run broker...");
		boolean runCheck = run();
		if(!runCheck)
		{
			log.error("Couldn't run broker!");
			System.exit(PSTBError.B_RUN);
		}
		log.info(logHeader + "Broker running.");
		
		initalized();
		
		log.debug(logHeader + "Attempting to get link signal from master...");
		String connect = readConnection();
		if(connect == null || !connect.equals(PSTBUtil.LINK))
		{
			log.error(logHeader + "Didn't get link signal from master!");
			System.exit(PSTBError.B_LINK);
		}
		log.info(logHeader + "Link signal received.");
		
		log.debug(logHeader + "Attempting to connect with other brokers...");
		boolean connectCheck = connect();
		if(!connectCheck)
		{
			log.error("Couldn't connect brokers together!");
			System.exit(PSTBError.B_CONNECT);
		}
		log.info(logHeader + "Connected to network.");
		
		log.debug(logHeader + "Letting master know we've connected to network...");
		PSTBUtil.sendStringAcrossSocket(connOut, PSTBUtil.LINK, log, logHeader);
		log.info(logHeader + "Master should know.");
	}
	
	protected abstract boolean connect();
}
