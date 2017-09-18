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

import pstb.util.ClientAction;
import pstb.util.NodeRole;
import pstb.util.PSTBUtil;
import pstb.util.Workload;
import pstb.util.PSAction;

public class WorkloadFileParser {
	private String subWorkloadFilePath;
	ArrayList<String> pubWorkloadFilesPaths;
	private Workload wload;
	
	private final int SEGMENTSNUM = 3;
	private final int LOC_CLIENT_ACTION = 0;
	private final int LOC_ATTRIBUTES = 1;
	private final int LOC_PAYLOAD_TIME_ACTIVE = 2;
	
	private final String logHeader = "Workload Parser: ";
	private static final Logger logger = LogManager.getRootLogger();
	
	/**
	 * FilePath constructor
	 * @param nSWFP - the new path to the subscriber workload file
	 * @param nPWFP - the array containing all the paths to the publisher workload files
	 */
	public WorkloadFileParser(String nSWFP, ArrayList<String> nPWFP)
	{
		subWorkloadFilePath = nSWFP;
		pubWorkloadFilesPaths = nPWFP;
		wload = new Workload();
	}
	
	/**
	 * Parses the given subscriber workload file
	 * @param clientType - the type of clients this workload file is supposed to influence
	 * @return true is everything's ok; false otherwise
	 */
	public boolean parseSubscriberFile()
	{
		boolean isParseSuccessful = true;
		String line = null;
		int linesRead = 0;
		
		try
		{
			BufferedReader sWFReader = new BufferedReader(new FileReader(subWorkloadFilePath));
			while( (line = sWFReader.readLine()) != null)
			{
				linesRead++;
				String[] splitLine = line.split("	");

				if(checkProperLength(splitLine))
				{
					if(checkProperClientAction(splitLine[LOC_CLIENT_ACTION].toUpperCase(), NodeRole.S) == ClientAction.S)
					{
						Long timeActive = PSTBUtil.checkIfLong(splitLine[LOC_PAYLOAD_TIME_ACTIVE], false);
						
						if(timeActive != null)
						{
							PSAction newSub = new PSAction();
							newSub.setAttributes(splitLine[LOC_ATTRIBUTES]);
							newSub.setTimeActive(timeActive);
							wload.updateWorkloadS(newSub);
						}
						else
						{
							isParseSuccessful = false;
							logger.error(logHeader + "line " + linesRead + " has an incorrect Time Active value");
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
					logger.error(logHeader + "line " + linesRead + " is not the proper length");
				}
			}
			sWFReader.close();
		}
		catch (IOException e) 
		{
			logger.error(logHeader + "Cannot find file", e);
			return false;
		}
		
		return isParseSuccessful;
	}
	
	public boolean parsePublisherFiles()
	{
		boolean isParseSuccessful = true;
		String line = null;
		int linesRead = 0;
		
		ArrayList<FileReader> fileList = tryToAccessPubWorkloadFiles();
		
		if(fileList != null)
		{
			for(int i = 0 ; i < fileList.size() ; i++)
			{
				BufferedReader iTHFileReader = new BufferedReader(fileList.get(i));
				try
				{
					while( (line = iTHFileReader.readLine()) != null)
					{
						linesRead++;
						String[] splitLine = line.split("	");
						PSAction fileAd = new PSAction();
						
						if(checkProperLength(splitLine))
						{
							if(checkProperClientAction(splitLine[LOC_CLIENT_ACTION].toUpperCase(), NodeRole.P) 
									== ClientAction.A)
							{
								Long timeActive = PSTBUtil.checkIfLong(splitLine[LOC_PAYLOAD_TIME_ACTIVE], false);
								
								if(timeActive != null)
								{
									PSAction newAd = new PSAction();
									newAd.setAttributes(splitLine[LOC_ATTRIBUTES]);
									newAd.setTimeActive(timeActive);
									fileAd = newAd;
									wload.updateWorkloadA(newAd);
								}
								else
								{
									isParseSuccessful = false;
									logger.error(logHeader + "line " + linesRead + " has an incorrect Time Active value");
								}
							}
							else if(checkProperClientAction(splitLine[LOC_CLIENT_ACTION].toUpperCase(), NodeRole.P) 
									== ClientAction.P)
							{
								if(!fileAd.getAttributes().isEmpty())
								{
									Integer payloadSize = PSTBUtil.checkIfInteger(splitLine[LOC_PAYLOAD_TIME_ACTIVE], false);
									
									if(payloadSize != null)
									{
										PSAction newPub = new PSAction();
										newPub.setAttributes(splitLine[LOC_ATTRIBUTES]);
										newPub.setPayloadSize(payloadSize);
										wload.updateWorkloadP(fileAd.getAttributes(), newPub);
									}
									else
									{
										isParseSuccessful = false;
										logger.error(logHeader + "line " + linesRead + " has an incorrect Payload Size value");
									}	
								}
								else
								{
									isParseSuccessful = false;
									logger.error(logHeader + "line " + linesRead + " is referencing an Ad that doesn't exist");
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
							logger.error(logHeader + "line " + linesRead + " is not the proper length");
						}
					}
					iTHFileReader.close();
				}
				catch (IOException e) 
				{
					// we SHOULD never get here, but if we do
					logger.error(logHeader + "Major error", e);
					return false;
				}
			}
		}
		else
		{
			logger.error(logHeader + "Error reading Publisher Files");
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
	
	private ArrayList<FileReader> tryToAccessPubWorkloadFiles()
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
}
