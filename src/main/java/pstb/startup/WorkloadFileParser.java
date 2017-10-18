/**
 * @author padres-dev-4187
 *
 */
package pstb.startup;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.util.PSActionType;
import pstb.util.PSTBUtil;
import pstb.util.Workload;
import pstb.util.PSAction;

public class WorkloadFileParser {
	private Workload wload;
	private ArrayList<String> pubWorkloadFilesPaths;
	private String subWorkloadFilePath;
	
	private PSAction fileAd; // this exists on a per file basis
	
	private final int MINSEGMENTSNUM = 3;
	private final int MAXSEGMENTSNUM = 4;
	private final int LOC_ACTION_DELAY = 0;
	private final int LOC_CLIENT_ACTION = 1;
	private final int LOC_ATTRIBUTES = 2;
	private final int LOC_PAYLOAD_TIME_ACTIVE = 3;
	
	private final String logHeader = "Workload Parser: ";
	private static final Logger logger = LogManager.getRootLogger();
	
	public enum WorkloadFileType
	{
		P, S;
	}
	
	/**
	 * FilePath constructor
	 * @param nSWFP - the new path to the subscriber workload file
	 * @param nPWFP - the array containing all the paths to the publisher workload files
	 */
	public WorkloadFileParser()
	{
		wload = new Workload();
	}
	
	/**
	 * Gets the Workload object
	 * @return the Workload
	 */
	public Workload getWorkload()
	{
		return wload;
	}
	
	public void setPubWorkloadFilesPaths(ArrayList<String> givenPWFP)
	{
		pubWorkloadFilesPaths = givenPWFP;
	}
	
	public void setSubWorkloadFilePath(String givenSWFP)
	{
		subWorkloadFilePath = givenSWFP;
	}
	
	public boolean parseWorkloadFiles(WorkloadFileType requestedWF)
	{
		boolean parseSuccessful = true;
		if(requestedWF == WorkloadFileType.S)
		{
			BufferedReader sWFReader = null;
			try
			{
				sWFReader = new BufferedReader(new FileReader(subWorkloadFilePath));
			}
			catch (IOException e) 
			{
				logger.error(logHeader + "Cannot find file", e);
				return false;
			}
			
			parseSuccessful = parseGivenWorkloadFile(sWFReader, requestedWF);
			if(!parseSuccessful)
			{
				logger.error(logHeader + "Error in file " + subWorkloadFilePath);
			}
		}
		else
		{
			ArrayList<FileReader> fileList = tryToAccessWorkloadFiles(pubWorkloadFilesPaths);
			
			if(fileList != null)
			{
				for(int i = 0 ; i < fileList.size() ; i++)
				{
					BufferedReader iTHFileReader = new BufferedReader(fileList.get(i));
					parseSuccessful = parseGivenWorkloadFile(iTHFileReader, requestedWF);
					if(!parseSuccessful)
					{
						logger.error(logHeader + "Error in publisher file " + i);
					}
				}
			}
			else
			{
				logger.error(logHeader + "Error reading Publisher Files");
				return false;
			}
		}
		
		return parseSuccessful;
	}
	
	private boolean parseGivenWorkloadFile(BufferedReader givenFile, WorkloadFileType requestedWF)
	{
		boolean isParseSuccessful = true;
		String line = null;
		int linesRead = 0;
		fileAd = new PSAction();
		
		try
		{
			while( (line = givenFile.readLine()) != null)
			{
				linesRead++;
				String[] splitLine = line.split("	");
				if(checkProperLength(splitLine))
				{
					Long actionDelay = PSTBUtil.checkIfLong(splitLine[LOC_ACTION_DELAY], false, null);
					
					if(actionDelay != null)
					{
						PSActionType linesCA = checkProperClientAction(splitLine[LOC_CLIENT_ACTION].toUpperCase(), requestedWF);
						if(linesCA != null)
						{
							if(splitLine.length == MAXSEGMENTSNUM)
							{
								Long payloadOrTA = checkValidPayloadOrTimeActive(splitLine[LOC_PAYLOAD_TIME_ACTIVE], linesCA);
								
								if(payloadOrTA != null)
								{
									boolean addCheck = addActionToWorkload(actionDelay, linesCA, 
																			splitLine[LOC_ATTRIBUTES], payloadOrTA);
									if(!addCheck)
									{
										isParseSuccessful = false;
										logger.error(logHeader + "error adding " + linesRead + " to the workload");
									}
								}
								else
								{
									isParseSuccessful = false;
									logger.error(logHeader + "line " + linesRead + " has an incorrect payload "
											+ "or time active value");
								}
							}
							else
							{
								boolean addCheck = addActionToWorkload(actionDelay, linesCA, 
																		splitLine[LOC_ATTRIBUTES], PSTBUtil.INFINITY);
								if(!addCheck)
								{
									isParseSuccessful = false;
									logger.error(logHeader + "error adding " + linesRead + " to the workload");
								}
							}
						}
						else
						{
							isParseSuccessful = false;
							logger.error(logHeader + "line " + linesRead + " has an incorrect Client Action");
						}
					}
					else
					{
						isParseSuccessful = false;
						logger.error(logHeader + "line " + linesRead + " has an incorrect action delay");
					}
				}
				else
				{
					isParseSuccessful = false;
					logger.error(logHeader + "line " + linesRead + " is not the proper length");
				}
			}
			givenFile.close();
		}
		catch (IOException e) 
		{
			// we SHOULD never get here, but if we do
			logger.error(logHeader + "Major error", e);
			return false;
		}
		
		return isParseSuccessful;
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
	private PSActionType checkProperClientAction(String supposedClientAction, WorkloadFileType fileType)
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
		
		if(test.equals(PSActionType.R) || test.equals(PSActionType.U) || test.equals(PSActionType.V))
		{
			logger.error(logHeader + supposedClientAction + " is a Client Action that should not be "
					+ "submitted by the user.");
			return null;
		}
		
		if(
				test.equals(PSActionType.S) && fileType.equals(WorkloadFileType.P)
				|| test.equals(PSActionType.U) && fileType.equals(WorkloadFileType.P)
				|| test.equals(PSActionType.P) && fileType.equals(WorkloadFileType.S)
				|| test.equals(PSActionType.A) && fileType.equals(WorkloadFileType.S)
				|| test.equals(PSActionType.V) && fileType.equals(WorkloadFileType.S)
			)
		{
			logger.error(logHeader + " a " + fileType + " shouldn't have " + supposedClientAction + " actions");
			return null;
		}
		
		return test;
	}
	
	private Long checkValidPayloadOrTimeActive(String sPOrTA, PSActionType givenAction)
	{
		if(givenAction == PSActionType.R || givenAction == PSActionType.U || givenAction == PSActionType.V)
		{
			logger.error(logHeader + " action " + givenAction + " shouldn't have a payload size or time active value");
			return null;
		}
		
		return PSTBUtil.checkIfLong(sPOrTA, false, null);
	}
	
	/**
	 * Attempts to read all the PubWorkloadFiles
	 * @return null if a file cannot be read; an ArrayList of FileReaders if successful
	 */
	private ArrayList<FileReader> tryToAccessWorkloadFiles(ArrayList<String> pubWorkloadFilesPaths)
	{
		ArrayList<FileReader> temp = new ArrayList<FileReader>();
		for(int i = 0 ; i < pubWorkloadFilesPaths.size(); i++)
		{
			FileReader iTHFR = null;
			try
			{
				iTHFR = new FileReader(pubWorkloadFilesPaths.get(i));
			}
			catch (FileNotFoundException e) 
			{
				logger.error(logHeader + "Cannot find file", e);
				temp.clear();
				return temp;
			}
			temp.add(iTHFR);
		}
		
		return temp;
	}
	
	private boolean addActionToWorkload(Long actionDelay, PSActionType actionType, String attributes, Long payloadOrTA)
	{
		PSAction newAction = new PSAction();
		newAction.setActionDelay(actionDelay);
		newAction.setAttributes(attributes);
		newAction.setActionType(actionType);
		
		switch(actionType)
		{
			case A:
			{
				if(!fileAd.getAttributes().isEmpty())
				{
					logger.error(logHeader + "an advertiser already exists");
					return false;
				}
				else
				{
					if(payloadOrTA != null)
					{
						newAction.setTimeActive(payloadOrTA);
					}
					
					wload.updateAdvertisementWorkload(newAction);
					fileAd = newAction;
					break;
				}
			}
			case P:
			{
				if(fileAd.getAttributes().isEmpty())
				{
					logger.error(logHeader + "no Advertisement was given");
					return false;
				}
				else
				{
					if(payloadOrTA != null)
					{
						newAction.setPayloadSize(payloadOrTA);
					}
					wload.updatePublicationWorkload(fileAd, newAction);
					break;
				}
			}
			case S:
			{
				if(payloadOrTA != null)
				{
					newAction.setTimeActive(payloadOrTA);
				}
				wload.updateSubscriptionWorkload(newAction);
				break;
			}
			default:
			{
				logger.error(logHeader + "Switch case defaulted");
				return false;
			}
		}
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
