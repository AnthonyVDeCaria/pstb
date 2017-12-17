/**
 * @author padres-dev-4187
 *
 */
package pstb.startup.workload;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.util.PSTBUtil;

public class WorkloadFileParser {
	private String workloadFileString;
	private ArrayList<PSAction> workload;
	
	private final int MINSEGMENTSNUM = 3;
	private final int MAXSEGMENTSNUM = 4;
	private final int LOC_ACTION_DELAY = 0;
	private final int LOC_CLIENT_ACTION = 1;
	private final int LOC_ATTRIBUTES = 2;
	private final int LOC_PAYLOAD_TIME_ACTIVE = 3;
	
	private final int NO_PAYLOAD = 0;
	
	private final String logHeader = "Workload Parser: ";
	private Logger logger = LogManager.getRootLogger();
	
	/**
	 * Constructor
	 * 
	 * @param log - the Logger that is requested we log to
	 */
	public WorkloadFileParser()
	{
		workloadFileString = new String();
		workload = new ArrayList<PSAction>();
	}
	
	public WorkloadFileParser(String givenWorkloadFileString)
	{
		workloadFileString = givenWorkloadFileString;
		workload = new ArrayList<PSAction>();
	}
	
	/**
	 * Sets the WorkloadFileString
	 * 
	 * @param givenWFS - a String representation of the workload file's Path
	 */
	public void setWorkloadFileString(String givenWFS)
	{
		workloadFileString = givenWFS;
	}
	
	/**
	 * Gets the workload
	 * 
	 * @return the PSAction ArrayList
	 */
	public ArrayList<PSAction> getWorkload()
	{
		return workload;
	}
	
	/**
	 * Parses the Workload file as stated by the requestedWF
	 * 
	 * @param requestedWF - the type of Workload file that is to be parsed
	 * @return false on failure; true otherwise
	 */
	public boolean parse()
	{
		boolean parseSuccessful = true;
		
		if(workloadFileString.isEmpty())
		{
			logger.error(logHeader + "No workload file was given!");
			return false;
		}
		
		BufferedReader readerWF = null;
		String line = null;
		int linesRead = 0;
		try
		{
			readerWF = new BufferedReader(new FileReader(workloadFileString));
			
			while( (line = readerWF.readLine()) != null)
			{
				linesRead++;
				String[] splitLine = line.split("	");
				if(!checkProperLength(splitLine))
				{
					parseSuccessful = false;
					logger.error(logHeader + "line " + linesRead + " is not the proper length");
				}
				else
				{
					Long actionDelay = PSTBUtil.checkIfLong(splitLine[LOC_ACTION_DELAY], false, null);
					
					if(actionDelay == null)
					{
						parseSuccessful = false;
						logger.error(logHeader + "line " + linesRead + " has an incorrect action delay");
					}
					else
					{
						PSActionType lineIsActionType = checkProperClientAction(splitLine[LOC_CLIENT_ACTION].toUpperCase());
						if(lineIsActionType == null)
						{
							parseSuccessful = false;
							logger.error(logHeader + "line " + linesRead + " has an incorrect Client Action");
						}
						else
						{
							if(splitLine.length != MAXSEGMENTSNUM)
							{
								boolean addCheck = addActionToWorkload(actionDelay, lineIsActionType, splitLine[LOC_ATTRIBUTES], 
																			Long.MAX_VALUE, NO_PAYLOAD);
								if(!addCheck)
								{
									parseSuccessful = false;
									logger.error(logHeader + "error adding " + linesRead + " to the workload");
								}
							}
							else
							{
								Long timeActive = null; 
								Integer payloadSize = null;
								
								if(lineIsActionType.equals(PSActionType.P))
								{
									payloadSize = PSTBUtil.checkIfInteger(splitLine[LOC_PAYLOAD_TIME_ACTIVE], false, null);
									if(payloadSize == null)
									{
										parseSuccessful = false;
										logger.error(logHeader + "line " + linesRead + " has an incorrect payload size!");
									}
								}
								else if(lineIsActionType.equals(PSActionType.A) || lineIsActionType.equals(PSActionType.S))
								{
									timeActive = PSTBUtil.checkIfLong(splitLine[LOC_PAYLOAD_TIME_ACTIVE], false, null);
									if(timeActive == null)
									{
										parseSuccessful = false;
										logger.error(logHeader + "line " + linesRead + " has an incorrect time active value!");
									}
								}
								else
								{
									logger.warn(logHeader + "line " + linesRead + " shouldn't have a third column - ignoring it.");
								}
								
								boolean addCheck = addActionToWorkload(actionDelay, lineIsActionType, splitLine[LOC_ATTRIBUTES], 
										timeActive, payloadSize);
								if(!addCheck)
								{
									parseSuccessful = false;
									logger.error(logHeader + "error adding " + linesRead + " to the workload!");
								}
							}
						}
					}
				}
			}
			readerWF.close();
		}
		catch (IOException e) 
		{
			logger.error(logHeader + "Cannot find file: ", e);
			return false;
		}
		
		return parseSuccessful;
	}
	
	/**
	 * Determines if the given line has been properly written
	 * - i.e. contains a client action, attributes 
	 * and either a payload size or a time active - 
	 * by seeing if the split line has the right number of segments
	 * 
	 * @param splitFileLine - the split line
	 * @return true if it does; false otherwise
	 */
	private boolean checkProperLength(String[] splitFileline)
	{
		return (splitFileline.length == MINSEGMENTSNUM) || (splitFileline.length == MAXSEGMENTSNUM);
	}
	
	/**
	 * Determines if the given client action is a both client action
	 * and a client action that makes sense for the Client
	 * i.e. a publisher doesn't have any subscribe requests
	 * 
	 * @param supposedClientAction - the Client Action being tested
	 * @param fileType - the type of file
	 * @return null on failure; the given Client Action otherwise
	 */
	private PSActionType checkProperClientAction(String supposedClientAction)
	{
		PSActionType test = null;
		try 
		{
			test = PSActionType.valueOf(supposedClientAction);
		}
		catch(IllegalArgumentException e)
		{
			logger.error(logHeader + supposedClientAction + " is not a valid Client Action.", e);
			return null;
		}
		
		if(test.equals(PSActionType.R))
		{
			logger.error(logHeader + supposedClientAction + " is a Client Action that should not be "
					+ "submitted by the user.");
			return null;
		}
		
		return test;
	}
	
	/**
	 * Adds and action to the Workload
	 * 
	 * @param actionDelay - the desired delay
	 * @param actionType - the desired PSActionType
	 * @param attributes - the desired attributes
	 * @param payloadOrTA - the desired Payload/TimeActive
	 * @return false on error; true otherwise
	 */
	private boolean addActionToWorkload(Long actionDelay, PSActionType actionType, String attributes, Long timeActive, Integer payloadSize)
	{
		PSAction newAction = new PSAction();
		newAction.setActionDelay(actionDelay);
		newAction.setAttributes(attributes);
		newAction.setActionType(actionType);
		
		if(actionType.equals(PSActionType.A) || actionType.equals(PSActionType.S))
		{
			if(timeActive != null)
			{
				if(timeActive < (Long.MAX_VALUE / PSTBUtil.MILLISEC_TO_NANOSEC))
				{
					timeActive *= PSTBUtil.MILLISEC_TO_NANOSEC;
				}
				newAction.setTimeActive(timeActive);
			}
			else
			{
				logger.error(logHeader + "given Time Active value is null when it shouldn't be!");
				return false;
			}
		}
		else if(actionType.equals(PSActionType.P))
		{
			if(payloadSize != null)
			{
				newAction.setPayloadSize(payloadSize);
			}
			else
			{
				logger.error(logHeader + "given Payload Size value is null when it shouldn't be!");
				return false;
			}
		}
		
		workload.add(newAction);
		return true;
	}
	
//	private boolean checkIfAttributesAreCorrect(ClientAction givenCA, String unsplitAttributes)
//	{
//		boolean areAttributesCorrect = false;
//		
//		String[] splitAttri = unsplitAttributes.split("],");
//		
//		if(!splitAttri[0].equals("[class,eq,\"oneITS\"]") && !splitAttri[0].equals("[class,eq,'oneITS']"))
//		{
//			areAttributesCorrect = false;
//			logger.error(logHeader + "First attribute isn't [class,eq,\"oneITS\"] or [class,eq,'oneITS']");
//		}
//		
//		return areAttributesCorrect;
//	}
}
