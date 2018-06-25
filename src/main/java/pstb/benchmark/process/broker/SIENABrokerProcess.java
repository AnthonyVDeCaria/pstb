/**
 * 
 */
package pstb.benchmark.process.broker;

import java.io.OutputStream;
import java.net.Socket;

import org.apache.logging.log4j.Logger;

import pstb.benchmark.object.PSNode;
import pstb.benchmark.object.broker.PSBrokerSIENA;
import pstb.startup.config.SupportedEngines.PSEngine;
import pstb.startup.topology.NodeRole;
import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 *
 */
public class SIENABrokerProcess extends PSTBBrokerProcess {
	private PSBrokerSIENA actualNode;
	
	public SIENABrokerProcess(String givenName, String givenContext, String givenIPAddress, Integer givenPort, 
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
	protected boolean setup(PSNode givenNode) {
		actualNode = (PSBrokerSIENA) givenNode;
		
		return true;
	}

	@Override
	protected boolean run() {
		if(actualNode == null)
		{
			log.error(logHeader + "No PADRES Broker exists to run!");
			return false;
		}
		
		String brokerURI = actualNode.getBrokerURI();
		
		String[] runCommand = {"./startSBroker.sh", nodeName, brokerURI};
		Boolean scriptCheck = PSTBUtil.createANewProcess(runCommand, log, true, true,
				"Couldn't run SIENA Broker process :", 
				"SIENA Broker process process successfull.", 
				"SIENA Broker process process failed!"
			);
		
		if(scriptCheck == null)
		{
			return false;
		}
		else
		{
			return scriptCheck.booleanValue();
		}
	}

}
