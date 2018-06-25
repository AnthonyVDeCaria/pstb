/**
 * 
 */
package pstb.benchmark.process.client;

import java.io.OutputStream;
import java.net.Socket;

import org.apache.logging.log4j.Logger;

import pstb.benchmark.object.PSNode;
import pstb.benchmark.object.client.siena.PSClientSIENA;
import pstb.startup.config.SupportedEngines.PSEngine;
import pstb.startup.topology.NodeRole;

/**
 * @author padres-dev-4187
 *
 */
public class SIENAClientProcess extends PSTBClientProcess {
	private PSClientSIENA actualNode;
	
	public SIENAClientProcess(String givenName, String givenContext, String givenIPAddress, Integer givenPort, 
			PSEngine givenEngine, NodeRole givenRole, Boolean shouldWeSendDiary,
			boolean areWeDistributed, String givenUsername,
			Socket givenConnection, OutputStream givenOut,
			Logger givenLog, String givenLogHeader, String givenTCS)
	{
		super(givenName, givenContext, givenIPAddress, givenPort, 
				givenEngine, givenRole, shouldWeSendDiary,
				areWeDistributed, givenUsername,
				givenConnection, givenOut,
				givenLog, givenLogHeader, givenTCS);
		
		actualNode = null;
	}
	
	@Override
	protected boolean setup(PSNode givenNode)
	{
		actualNode = (PSClientSIENA) givenNode;
		
		boolean setupCheck = actualNode.setupClient();
		if(!setupCheck)
		{
			log.error(logHeader + "Couldn't setup client " + nodeName);
			return false;
		}
		
		log.info(logHeader + nodeName + " setup.");
		return true;
	}
	
	@Override
	protected boolean run()
	{
		if(actualNode == null)
		{
			log.error(logHeader + "No SIENA Client exists to run experiment!");
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
	
	protected boolean cleanup()
	{
		return true;
	}

}
