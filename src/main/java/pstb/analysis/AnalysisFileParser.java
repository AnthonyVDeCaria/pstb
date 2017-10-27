/**
 * 
 */
package pstb.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.util.DiaryEntry.DiaryHeader;
import pstb.util.DistributedFlagValue;
import pstb.util.NetworkProtocol;
import pstb.util.PSActionType;
import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 *
 */
public class AnalysisFileParser {
	private ArrayList<HashMap<AnalysisInput, Object>> requestedAnalysis;
	private String analysisFileString;
	
	public final int NUM_SEGMENTS = 9;
	public final int LOC_ANALYSIS_TYPE = 0;
	public final int LOC_DIARY_HEADER = 1;
	public final int LOC_PS_ACTION_TYPE = 2;
	public final int LOC_TOPO_FILE_PATH = 3;
	public final int LOC_DISTRIBUTED_FLAG = 4;
	public final int LOC_PROTOCOL = 5;
	public final int LOC_RUN_LENGTH = 6;
	public final int LOC_RUN_NUMBER = 7;
	public final int LOC_CLIENT_NAME = 8;
	
	String logHeader = "AnalysisParser: ";
	Logger log = LogManager.getRootLogger();
	
	/**
	 * Empty Constructor
	 */
	public AnalysisFileParser()
	{
		requestedAnalysis = new ArrayList<HashMap<AnalysisInput, Object>>();
		analysisFileString = new String();
	}
	
	/**
	 * Sets the AnalysisFileString
	 * 
	 * @param newAFS the new AnalysisFileString
	 */
	public void setAnalysisFileString(String newAFS)
	{
		analysisFileString = newAFS;
	}
	
	/**
	 * Gets the requestedAnalysis
	 * 
	 * @return the requestedAnalysis
	 */
	public ArrayList<HashMap<AnalysisInput, Object>> getRequestedAnalysis()
	{
		return requestedAnalysis;
	}
	
	/**
	 * Parses the given Analysis File
	 * and extracts the analysis requested 
	 * 
	 * @return false on error; true otherwise
	 */
	public boolean parse()
	{		
		if(analysisFileString.isEmpty())
		{
			log.error(logHeader + "No path to the Analysis file was given");
			return false;
		}
		
		boolean isParseSuccessful = true;
		String line = null;
		int linesRead = 0;
		
		try {
			BufferedReader readerAF = new BufferedReader(new FileReader(analysisFileString));
			while( (line = readerAF.readLine()) != null)
			{
				linesRead++;
				
				if(!PSTBUtil.checkIfLineIgnorable(line))
				{
					String[] splitLine = line.split("	");
					
					if(splitLine.length == NUM_SEGMENTS)
					{
						HashMap<AnalysisInput, Object> requested = checkProperTypes(splitLine);
						if(!requested.isEmpty())
						{
							AnalysisType requestedAT = (AnalysisType) requested.get(AnalysisInput.AnalysisType);
							DiaryHeader requestedDH = (DiaryHeader) requested.get(AnalysisInput.DiaryHeader);
							PSActionType requestedPSAT = (PSActionType) requested.get(AnalysisInput.PSActionType);
							if(checkProperRelationships(splitLine, requestedAT, requestedDH, requestedPSAT))
							{
								log.trace(logHeader + "Line " + linesRead + "'s syntax checks out.");
								requestedAnalysis.add(requested);
							}
							else
							{
								isParseSuccessful = false;
								log.error(logHeader + "Error in Line " + linesRead + " - Error with relationships");
							}
						}
						else
						{
							isParseSuccessful = false;
							log.error(logHeader + "Error in Line " + linesRead + " - Error with Types");
						}
					}
					else
					{
						isParseSuccessful = false;
						log.error(logHeader + "Error in Line " + linesRead + " - Length isn't correct");
					}
				}
			}
			readerAF.close();
		} 
		catch (IOException e) 
		{
			isParseSuccessful = false;
			log.error(logHeader + "Cannot find file", e);
		}
		return isParseSuccessful;
	}
	
	/**
	 * 
	 * 
	 * @param splitLine
	 * @return
	 */
	private HashMap<AnalysisInput, Object> checkProperTypes(String[] splitLine)
	{
		HashMap<AnalysisInput, Object> retVal = new HashMap<AnalysisInput, Object>(); 
		Object temp = new Object();
		boolean error = false;
		
		try
		{
			temp = AnalysisType.valueOf(splitLine[LOC_ANALYSIS_TYPE]);
			retVal.put(AnalysisInput.AnalysisType, temp);
		}
		catch(Exception e)
		{
			log.error(logHeader + "Given string isn't a AnalysisType");
			error = true;
		}
		
		try
		{
			temp = DiaryHeader.valueOf(splitLine[LOC_DIARY_HEADER]);
			retVal.put(AnalysisInput.DiaryHeader, temp);
		}
		catch(Exception e)
		{
			log.error(logHeader + "Given string isn't a DiaryHeader");
			error = true;
		}
		
		try
		{
			temp = PSActionType.valueOf(splitLine[LOC_PS_ACTION_TYPE]);
			retVal.put(AnalysisInput.PSActionType, temp);
		}
		catch(Exception e)
		{
			log.error(logHeader + "Given string isn't a PSActionType");
			error = true;
		}
		
		if(!new File(splitLine[LOC_TOPO_FILE_PATH]).isFile())
		{
			log.error(logHeader + "Cannot find given TopologyFile in the system");
			error = true;
		}
		else
		{
			retVal.put(AnalysisInput.TopologyFilePath, splitLine[LOC_TOPO_FILE_PATH]);
		}
		
		try
		{
			temp = DistributedFlagValue.valueOf(splitLine[LOC_DISTRIBUTED_FLAG]);
			retVal.put(AnalysisInput.DistributedFlag, temp);
		}
		catch(Exception e)
		{
			if(!splitLine[LOC_DISTRIBUTED_FLAG].equals("null"))
			{
				log.error(logHeader + "Given string isn't a DistributedFlagValue");
				error = true;
			}
			else
			{
				retVal.put(AnalysisInput.DistributedFlag, null);
			}
		}
		
		try
		{
			temp = NetworkProtocol.valueOf(splitLine[LOC_PROTOCOL]);
			retVal.put(AnalysisInput.Protocol, temp);
		}
		catch(Exception e)
		{
			if(!splitLine[LOC_PROTOCOL].equals("null"))
			{
				log.error(logHeader + "Given string isn't a NetworkProtocol");
				error = true;
			}
			else
			{
				retVal.put(AnalysisInput.Protocol, null);
			}
		}
		
		try
		{
			temp = Long.valueOf(splitLine[LOC_RUN_LENGTH]);
			retVal.put(AnalysisInput.RunLength, temp);
		}
		catch(Exception e)
		{
			if(!splitLine[LOC_RUN_LENGTH].equals("null"))
			{
				log.error(logHeader + "Given string isn't a Long");
				error = true;
			}
			else
			{
				retVal.put(AnalysisInput.RunLength, null);
			}
		}
		
		try
		{
			temp = Integer.valueOf(splitLine[LOC_RUN_NUMBER]);
			retVal.put(AnalysisInput.RunNumber, temp);
		}
		catch(Exception e)
		{
			if(!splitLine[LOC_RUN_NUMBER].equals("null"))
			{
				log.error(logHeader + "Given string isn't an Integer");
				error = true;
			}
			else
			{
				retVal.put(AnalysisInput.RunNumber, null);
			}
		}
		
		if(splitLine[LOC_CLIENT_NAME].equals("null"))
		{
			retVal.put(AnalysisInput.ClientName, null);
		}
		else
		{
			retVal.put(AnalysisInput.ClientName, splitLine[LOC_CLIENT_NAME]);
		}
		
		if(error)
		{
			retVal.clear();
		}
		
		return retVal;
	}
	
	private boolean checkProperRelationships(String[] splitLine, AnalysisType givenAT, DiaryHeader givenDH, PSActionType givenPSAT)
	{
		boolean relationshipsProper = true;
		
		switch(givenAT)
		{
			case AverageDelay:
			{
				if(!givenDH.equals(DiaryHeader.MessageDelay) && !givenDH.equals(DiaryHeader.ActionDelay))
				{
					log.error(logHeader + "Improper 'delays' requested"); 
					relationshipsProper = false;
				}
				if(givenDH.equals(DiaryHeader.MessageDelay) && !givenPSAT.equals(PSActionType.R))
				{
					log.error(logHeader + "Attempting to find an average MessageDelay for something other than a received message"); 
					relationshipsProper = false;
				}
				if(givenDH.equals(DiaryHeader.ActionDelay) && givenPSAT.equals(PSActionType.R))
				{
					log.error(logHeader + "Attempting to find an average ActionDelay for something that won't have one"); 
					relationshipsProper = false;
				}
				break;
			}
			case DelayHistogram:
			{
				if(!givenDH.equals(DiaryHeader.MessageDelay) && !givenDH.equals(DiaryHeader.ActionDelay))
				{
					log.error(logHeader + "Improper 'delays' requested"); 
					relationshipsProper = false;
				}
				if(givenDH.equals(DiaryHeader.MessageDelay) && !givenPSAT.equals(PSActionType.R))
				{
					log.error(logHeader + "Attempting to create a MessageDelay histogram "
								+ "for something other than a received message"); 
					relationshipsProper = false;
				}
				if(givenDH.equals(DiaryHeader.ActionDelay) && givenPSAT.equals(PSActionType.R))
				{
					log.error(logHeader + "Attempting to find an ActionDelay histogram for something that won't have one"); 
					relationshipsProper = false;
				}
				break;
			}
			default:
				log.error(logHeader + "Case statement broke"); 
				relationshipsProper = false;
				break;
		}
		
		return relationshipsProper;
	}
}
