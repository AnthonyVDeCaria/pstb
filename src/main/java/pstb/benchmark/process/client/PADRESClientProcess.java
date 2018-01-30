/**
 * 
 */
package pstb.benchmark.process.client;

import java.io.OutputStream;
import java.net.Socket;

import org.apache.logging.log4j.Logger;

import pstb.benchmark.object.PSNode;
import pstb.benchmark.object.client.padres.PSClientPADRES;
import pstb.startup.config.SupportedEngines.PSEngine;
import pstb.startup.topology.NodeRole;

/**
 * @author padres-dev-4187
 *
 */
public class PADRESClientProcess extends PSTBClientProcess {
	private PSClientPADRES actualNode;
	
	public PADRESClientProcess(String givenName, String givenContext, String givenIPAddress, Integer givenPort, PSEngine givenEngine, 
			NodeRole givenRole, boolean areWeDistributed, String givenUsername, Socket givenConnection, OutputStream givenOut, 
			Logger givenLog, String givenLogHeader, String givenThreadContextString)
	{
		super(givenName, givenContext, givenIPAddress, givenPort,givenEngine, givenRole, areWeDistributed, givenUsername, 
				givenConnection, givenOut, givenLog, givenLogHeader, givenThreadContextString);
		
		actualNode = null;
	}
	
	@Override
	protected boolean setup(PSNode givenNode)
	{
		actualNode = (PSClientPADRES) givenNode;
		
		// Now that we have the client, let's get this show on the road
		log.debug(logHeader + "Attempting to initialize PADRES Client " + nodeName + "...");
		boolean initCheck = actualNode.initialize(true);
		if(!initCheck)
		{
			log.error(logHeader + "Couldn't initialize PADRES Client" + nodeName);
			return false;
		}
		log.info(logHeader + nodeName + " initialized.");
		
		return true;
	}
	
	@Override
	protected boolean run()
	{
		if(actualNode == null)
		{
			log.error(logHeader + "No PADRES Client exists to run experiment!");
			return false;
		}
		
		boolean runCheck = actualNode.startRun();
		if(!runCheck)
		{
			log.error(logHeader + "Run failed in client " + nodeName + "!");
			return false;
		}
		
		log.info(logHeader + "Run complete.");
		return true;
	}
	
	@Override
	protected boolean cleanup()
	{
		if(actualNode == null)
		{
			log.error(logHeader + "No PADRES Client exists to disconnect!");
			return false;
		}
		
		actualNode.disconnect();
		
		boolean shutdownCheck = actualNode.shutdown();
		if(!shutdownCheck)
		{
			log.error(logHeader + "Error shutting down client " + nodeName);
			return false;
		}
		
		return true;
	}

}
