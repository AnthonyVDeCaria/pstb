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
	
	public SIENABrokerProcess(String givenName, String givenContext, String givenIPAddress, Integer givenPort, PSEngine givenEngine, 
			NodeRole givenRole, boolean areWeDistributed, String givenUsername, Socket givenConnection, OutputStream givenOut, 
			Logger givenLog, String givenLogHeader, String givenThreadContextString)
	{
		super(givenName, givenContext, givenIPAddress, givenPort,givenEngine, givenRole, areWeDistributed, givenUsername, 
				givenConnection, givenOut, givenLog, givenLogHeader, givenThreadContextString);
		
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
		Boolean scriptCheck = PSTBUtil.createANewProcess(runCommand, log, false,
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

	@Override
	protected boolean connect() {
		String brokerIURI = actualNode.getBrokerURI();
		String[] bIsNeighboursURIs = actualNode.getNeighbourURIS();
		
		for(int j = 0 ; j < bIsNeighboursURIs.length ; j++)
		{
			String brokerJURI = bIsNeighboursURIs[j];
			String brokerJID = actualNode.getID(brokerJURI);
			
			String[] connectCommand = {"./connectSBrokers.sh", brokerIURI, brokerJID, brokerJURI};
			
			Boolean connectCheck = PSTBUtil.createANewProcess(connectCommand, log, false, 
					"Problem connecting brokers " + brokerIURI + " & " + brokerJURI + ":", 
					"Connected brokers " + brokerIURI + " & " + brokerJURI + ".",
					"Couldn't connect brokers " + brokerIURI + " & " + brokerJURI + "!");
			if(connectCheck == null || !connectCheck.booleanValue())
			{
				return false;
			}
		}
		
		return true;
	}

}