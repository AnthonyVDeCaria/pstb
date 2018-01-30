/**
 * 
 */
package pstb.benchmark.process.broker;

import java.io.OutputStream;
import java.net.Socket;

import org.apache.logging.log4j.Logger;

import pstb.benchmark.object.PSNode;
import pstb.benchmark.object.broker.PSBrokerPADRES;
import pstb.startup.config.SupportedEngines.PSEngine;
import pstb.startup.topology.NodeRole;

/**
 * @author padres-dev-4187
 *
 */
public class PADRESBrokerProcess extends PSTBBrokerProcess {
	private PSBrokerPADRES actualNode;
	
	public PADRESBrokerProcess(String givenName, String givenContext, String givenIPAddress, Integer givenPort, PSEngine givenEngine, 
			NodeRole givenRole, boolean areWeDistributed, String givenUsername, Socket givenConnection, OutputStream givenOut, 
			Logger givenLog, String givenLogHeader, String givenThreadContextString)
	{
		super(givenName, givenContext, givenIPAddress, givenPort,givenEngine, givenRole, areWeDistributed, givenUsername, 
				givenConnection, givenOut, givenLog, givenLogHeader, givenThreadContextString);
		
		actualNode = null;
	}

	@Override
	protected boolean setup(PSNode givenNode) {
		actualNode = (PSBrokerPADRES) givenNode;
		
		boolean createCheck = actualNode.createBroker();
		if(!createCheck)
		{
			log.error(logHeader + "Couldn't create the PADRES Broker!");
			return false;
		}
		
		return true;
	}

	@Override
	protected boolean run() {
		if(actualNode == null)
		{
			log.error(logHeader + "No PADRES Broker exists to run!");
			return false;
		}
		
		boolean startCheck = actualNode.startBroker();
		if(!startCheck)
		{
			log.error(logHeader + "Couldn't start broker!");
			return false;
		}
		
		return true;
	}

	@Override
	protected boolean connect() {
		return true;
	}

}
