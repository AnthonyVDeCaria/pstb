/**
 * @author padres-dev-4187
 *
 */
package pstb.startup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.util.ClientAction;
import pstb.util.NodeRole;
import pstb.util.PSTBUtil;
import pstb.util.Workload;

public class WorkloadFileParser {
	private String workloadFilePath;
	private HashMap<ClientAction, ArrayList<Workload>> actions;
	
	private final int SEGMENTSNUM = 3;
	private final int LOC_CLIENT_ACTION = 0;
	private final int LOC_ATTRIBUTES = 1;
	private final int LOC_PAYLOAD_TIME_ACTIVE = 2;
	
	private final String logHeader = "Workload Parser: ";
	private static final Logger logger = LogManager.getRootLogger();
	
	/**
	 * FilePath constructor
	 * @param nWFP - the new Workload File Path
	 */
	public WorkloadFileParser(String nWFP)
	{
		workloadFilePath = nWFP;
		actions = new HashMap<ClientAction, ArrayList<Workload>>();
		
		ArrayList<Workload> advertisements = new ArrayList<Workload>();
//		ArrayList<Workload> unadvertisements = new ArrayList<Workload>();
		ArrayList<Workload> publications = new ArrayList<Workload>();
		ArrayList<Workload> subscriptions = new ArrayList<Workload>();
//		ArrayList<Workload> unsubscriptions = new ArrayList<Workload>();
		
		actions.put(ClientAction.A, advertisements);
//		actions.put(ClientAction.V, unadvertisements);
		actions.put(ClientAction.P, publications);
		actions.put(ClientAction.S, subscriptions);
//		actions.put(ClientAction.U, unsubscriptions);
	}
	
	/**
	 * Updates the actions map with the new Workload
	 * @param givenAction - the action being updated
	 * @param newWorkload - the new workload that must be done
	 */
	private void updateActions(ClientAction givenAction, Workload newWorkload)
	{
		ArrayList<Workload> givenWorkloadList = actions.get(givenAction);
		givenWorkloadList.add(newWorkload);
		actions.put(givenAction, givenWorkloadList);
	}
	
	/**
	 * Parses the given workload file
	 * @param clientType - the type of clients this workload file is supposed to influence
	 * @return true is everything's ok; false otherwise
	 */
	public boolean parse(NodeRole clientType)
	{
		boolean isParseSuccessful = true;
		String line = null;
		int linesRead = 0;
		
		try
		{
			BufferedReader wFReader = new BufferedReader(new FileReader(workloadFilePath));
			while( (line = wFReader.readLine()) != null)
			{
				linesRead++;
				String[] splitLine = line.split("	");

				if(checkProperLength(splitLine))
				{
					if(checkPayloadTimeActiveInt(splitLine[LOC_PAYLOAD_TIME_ACTIVE]))
					{
						ClientAction givenClientAction = checkProperClientAction(splitLine[LOC_CLIENT_ACTION].toUpperCase(), clientType);
						if(givenClientAction != null)
						{
							Workload newWorkload = new Workload();
							newWorkload.setAttributes(splitLine[LOC_ATTRIBUTES]);
							
							if(givenClientAction.equals(ClientAction.A) || givenClientAction.equals(ClientAction.S))
							{
								Integer timeActive = Integer.parseInt(splitLine[LOC_PAYLOAD_TIME_ACTIVE]);
								newWorkload.setPayloadSize(timeActive);
							}
							else if(givenClientAction.equals(ClientAction.P))
							{
								Integer payload = Integer.parseInt(splitLine[LOC_PAYLOAD_TIME_ACTIVE]);
								newWorkload.setPayloadSize(payload);
							}
							// don't need to check for others - it should be given by checkProperClientAction
										
							updateActions(givenClientAction, newWorkload);
						}
						else
						{
							isParseSuccessful = false;
							actions.clear();
							logger.error(logHeader + "line " + linesRead + " has an incorrect Client Action");
						}
					}
					else
					{
						isParseSuccessful = false;
						actions.clear();
						logger.error(logHeader + "line " + linesRead + " has an incorrect Payload/Time Active value");
					}
				}
				else
				{
					isParseSuccessful = false;
					actions.clear();
					logger.error(logHeader + "line " + linesRead + " is not the proper length");
				}
			}
			wFReader.close();
		}
		catch (IOException e) 
		{
			isParseSuccessful = false;
			logger.error(logHeader + "Cannot find file", e);
		}
		
		return isParseSuccessful;
	}
	
	public void printActions()
	{
		logger.info("For workloadFile "+ workloadFilePath);
		actions.forEach((action, workloadList)->{
			Integer wLLS = workloadList.size();
			if(wLLS > 0)
			{
				logger.info("Action " + action + " has these workloads:");
			}
			for(int i = 0 ; i < wLLS; i++)
			{
				logger.info("Attributes: " + workloadList.get(i).getAttributes());
				logger.info("PayloadSize: " + workloadList.get(i).getPayloadSize());
				logger.info("TimeActive: " + workloadList.get(i).getTimeActive());
			}
		});
	}
	
	/**
	 * Determines if the given line has been properly written
	 * - i.e. contains a client action, attributes 
	 * and either a payload size or a time active - 
	 * by seeing if the split line has the right number of segments
	 * @param splitFileLine - the split line
	 * @return true if it does; false otherwise
	 */
	private boolean checkProperLength(String[] splitFileline)
	{
		return (splitFileline.length == SEGMENTSNUM);
	}
	
	/**
	 * Determines if the given client action is a both client action
	 * and a client action that makes sense for the Client
	 * i.e. a publisher doesn't have any subscribe requests
	 * @param supposedClientAction - the Client Action being tested
	 * @param clientType - the type of client
	 * @return null on failure; the given Client Action otherwise
	 */
	private ClientAction checkProperClientAction(String supposedClientAction, NodeRole clientType)
	{
		ClientAction test = null;
		try 
		{
			test = ClientAction.valueOf(supposedClientAction);
		}
		catch(IllegalArgumentException e)
		{
			logger.error(logHeader + supposedClientAction + " is not a valid Client Action.", e);
			return null;
		}
		
		if(test.equals(ClientAction.R) || test.equals(ClientAction.U) || test.equals(ClientAction.V))
		{
			logger.error(logHeader + supposedClientAction + " is a Client Action that should not be "
					+ "submitted by the user.");
			return null;
		}
		
		if(
				test.equals(ClientAction.S) && clientType.equals(NodeRole.P)
				|| test.equals(ClientAction.U) && clientType.equals(NodeRole.P)
				|| test.equals(ClientAction.P) && clientType.equals(NodeRole.S)
				|| test.equals(ClientAction.A) && clientType.equals(NodeRole.S)
				|| test.equals(ClientAction.V) && clientType.equals(NodeRole.S)
			)
		{
			logger.error(logHeader + " a " + clientType + " cannot " + supposedClientAction);
			return null;
		}
		
		return test;
	}
	
	/**
	 * Checks if the payload/time active string is an integer
	 * @param givenPayloadTimeActive - the string to test
	 * @return true if int; false otherwise
	 */
	private boolean checkPayloadTimeActiveInt(String givenPayloadTimeActive)
	{
		return PSTBUtil.isInteger(givenPayloadTimeActive, false);
	}
}
