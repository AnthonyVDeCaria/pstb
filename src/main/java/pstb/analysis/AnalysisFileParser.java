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
	private ArrayList<HashMap<AnalysisInput, ArrayList<Object>>> requestedAnalysis;
	private String analysisFileString;
	
	private final int NUM_SEGMENTS = 9;
	private final int LOC_ANALYSIS_TYPE = 0;
	private final int LOC_DIARY_HEADER = 1;
	private final int LOC_PS_ACTION_TYPE = 2;
	private final int LOC_TOPO_FILE_PATH = 3;
	private final int LOC_DISTRIBUTED_FLAG = 4;
	private final int LOC_PROTOCOL = 5;
	private final int LOC_RUN_LENGTH = 6;
	private final int LOC_RUN_NUMBER = 7;
	private final int LOC_CLIENT_NAME = 8;
	
	String logHeader = "AnalysisParser: ";
	Logger log = LogManager.getRootLogger();
	
	/**
	 * Empty Constructor
	 */
	public AnalysisFileParser()
	{
		requestedAnalysis = new ArrayList<HashMap<AnalysisInput, ArrayList<Object>>>();
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
	public ArrayList<HashMap<AnalysisInput, ArrayList<Object>>> getRequestedAnalysis()
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
						HashMap<AnalysisInput, ArrayList<Object>> requested = checkProperTypes(splitLine);
						if(!requested.equals(null))
						{
							AnalysisType requestedAT = (AnalysisType) requested.get(AnalysisInput.AnalysisType).get(0);
							DiaryHeader requestedDH = (DiaryHeader) requested.get(AnalysisInput.DiaryHeader).get(0);
							PSActionType requestedPSAT = (PSActionType) requested.get(AnalysisInput.PSActionType).get(0);
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
	private HashMap<AnalysisInput, ArrayList<Object>> checkProperTypes(String[] splitLine)
	{
		HashMap<AnalysisInput, ArrayList<Object>> retVal = new HashMap<AnalysisInput, ArrayList<Object>>();
		Object tempObject = null;
		
		ArrayList<Object> listAnalysisType = new ArrayList<Object>();
		ArrayList<Object> listDiaryHeader = new ArrayList<Object>();
		ArrayList<Object> listPSActionType = new ArrayList<Object>();
		ArrayList<Object> listTopology = new ArrayList<Object>();
		ArrayList<Object> listDistributed = new ArrayList<Object>();
		ArrayList<Object> listProtocol = new ArrayList<Object>();
		ArrayList<Object> listRunLength = new ArrayList<Object>();
		ArrayList<Object> listRunNumber = new ArrayList<Object>();
		ArrayList<Object> listClientName = new ArrayList<Object>();
		
		String topologies = splitLine[LOC_TOPO_FILE_PATH];
		String distributedFlags = splitLine[LOC_DISTRIBUTED_FLAG];
		String protocols = splitLine[LOC_PROTOCOL];
		String runLengths = splitLine[LOC_RUN_LENGTH];
		String runNumbers = splitLine[LOC_RUN_NUMBER];
		String clientNames = splitLine[LOC_CLIENT_NAME];
		
		String[] splitTopologies = topologies.split(",");
		String[] splitDistributedFlags = distributedFlags.split(",");
		String[] splitProtocols = protocols.split(",");
		String[] splitRunLengths = runLengths.split(",");
		String[] splitRunNumbers = runNumbers.split(",");
		String[] splitClientNames = clientNames.split(",");
		
		boolean error = false;
		
		// Analysis Type Parsing
		try
		{
			tempObject = AnalysisType.valueOf(splitLine[LOC_ANALYSIS_TYPE]);
		}
		catch(Exception e)
		{
			log.error(logHeader + "Given string isn't a AnalysisType");
			error = true;
		}
		if(!tempObject.equals(null))
		{
			listAnalysisType.add(tempObject);
			retVal.put(AnalysisInput.AnalysisType, listAnalysisType);
		}
		tempObject = null;
		
		// Diary Header Parsing
		try
		{
			tempObject = DiaryHeader.valueOf(splitLine[LOC_DIARY_HEADER]);
		}
		catch(Exception e)
		{
			log.error(logHeader + "Given string isn't a DiaryHeader");
			error = true;
		}
		if(!tempObject.equals(null))
		{
			listDiaryHeader.add(tempObject);
			retVal.put(AnalysisInput.DiaryHeader, listDiaryHeader);
		}
		tempObject = null;
		
		// PSActionType
		try
		{
			tempObject = PSActionType.valueOf(splitLine[LOC_PS_ACTION_TYPE]);
		}
		catch(Exception e)
		{
			log.error(logHeader + "Given string isn't a PSActionType");
			error = true;
		}
		if(!tempObject.equals(null))
		{
			listPSActionType.add(tempObject);
			retVal.put(AnalysisInput.PSActionType, listPSActionType);
		}
		tempObject = null;
		
		int numTopologies = splitTopologies.length;
		if(numTopologies == 1 && splitTopologies[0] == null)
		{
			retVal.put(AnalysisInput.TopologyFilePath, null);
		}
		else
		{
			for(int i = 0 ; i < numTopologies ; i++)
			{
				String topologyI = splitTopologies[i];
				if(topologyI.equals("null"))
				{
					log.error(logHeader + "TopologyFile " + i + " is null when there are other topologies requested");
					error = true;
				}
				else
				{
					if(new File(topologyI).isFile()) // This should be changed to seeing if it's in the BenchmarkConfig
					{
						listTopology.add(PSTBUtil.cleanTPF(splitTopologies[i]));
					}
					else
					{
						log.error(logHeader + "Cannot find given TopologyFile " + i + " in the system");
						listTopology.clear();
						error = true;
					}
				}
			}
			
			if(!error)
			{
				retVal.put(AnalysisInput.TopologyFilePath, listTopology);
			}
		}
		tempObject = null;
				
		int numDistributedFlags = splitDistributedFlags.length;
		if(numDistributedFlags == DistributedFlagValue.values().length)
		{
			log.warn(logHeader + "This was not necessary - typing null would have sufficed.");
			retVal.put(AnalysisInput.DistributedFlag, null);
		}
		else if(numDistributedFlags == 1 && splitDistributedFlags[0].equals("null"))
		{
			retVal.put(AnalysisInput.DistributedFlag, null);
		}
		else
		{
			for(int i = 0 ; i < numDistributedFlags ; i++)
			{
				String distributedFlagI = splitDistributedFlags[i];
				
				// Check if it should be distributed
				
				try
				{
					tempObject = DistributedFlagValue.valueOf(distributedFlagI);
					listDistributed.add(tempObject);
				}
				catch(Exception e)
				{
					log.error(logHeader + "Given string " + i + " isn't a DistributedFlagValue,"
									+ " or is null with other FlagValues!");
					listDistributed.clear();
					error = true;
				}
			}
			
			if(!error)
			{
				retVal.put(AnalysisInput.DistributedFlag, listDistributed);
			}
		}
		
		int numProtocols = splitProtocols.length;
		if(numProtocols == NetworkProtocol.values().length)
		{
			log.warn(logHeader + "This was not necessary - typing null would have sufficed.");
			retVal.put(AnalysisInput.Protocol, null);
		}
		else if(numProtocols == 1 && splitProtocols[0].equals("null"))
		{
			retVal.put(AnalysisInput.Protocol, null);
		}
		else
		{
			for(int i = 0 ; i < numProtocols ; i++)
			{
				// Check if this protocol is in BenchmarkConfig
				
				try
				{
					tempObject = NetworkProtocol.valueOf(splitProtocols[i]);
					listProtocol.add(tempObject);
				}
				catch(Exception e)
				{
					log.error(logHeader + "Given string " + i + " isn't a NetworkProtocol, "
									+ "or is null with other Protocols!");
					listProtocol.clear();
					error = true;
				}
			}
			
			if(!error)
			{
				retVal.put(AnalysisInput.Protocol, listProtocol);
			}
		}
		
		int numRunLengths = splitRunLengths.length;
		if(numRunLengths == 1 && splitRunLengths[0].equals("null"))
		{
			retVal.put(AnalysisInput.RunLength, null);
		}
		else
		{
			for(int i = 0 ; i < numRunLengths ; i++)
			{
				try
				{
					tempObject = Long.valueOf(splitRunLengths[i]);
					Long test = (Long) tempObject;
					
					if(test.compareTo(0L) > 0)
					{
						listRunLength.add(tempObject);
					}
					else
					{
						log.error(logHeader + "Given string " + i + " is a Long less than 1");
						listRunLength.clear();
						error = true;
					}
				}
				catch(Exception e)
				{
					log.error(logHeader + "Given string isn't a Long, or is null with other Run Lengths");
					listRunLength.clear();
					error = true;
				}
			}
			
			if(!error)
			{
				retVal.put(AnalysisInput.RunLength, listRunLength);
			}
		}
		
		int numRunNumbers = splitRunNumbers.length;
		if(numRunNumbers == 1 && splitRunNumbers[0].equals("null"))
		{
			retVal.put(AnalysisInput.RunNumber, null);
		}
		else
		{
			for(int i = 0 ; i < numRunNumbers ; i++)
			{
				try
				{
					tempObject = Long.valueOf(splitRunNumbers[i]);
					Long test = (Long) tempObject;
					
					if(test.compareTo(0L) > -1)
					{
						listRunNumber.add(tempObject);
					}
					else
					{
						log.error(logHeader + "Given string " + i + " is a Long less than 0");
						listRunNumber.clear();
						error = true;
					}
				}
				catch(Exception e)
				{
					log.error(logHeader + "Given string " + i + " isn't a Long, or is null with other Run Numbers");
					listRunNumber.clear();
					error = true;
				}
			}
			
			if(!error)
			{
				retVal.put(AnalysisInput.RunNumber, listRunNumber);
			}
		}
		
		int numClientNames = splitClientNames.length;
		if(numClientNames == 1 && splitClientNames[0].equals("null"))
		{
			retVal.put(AnalysisInput.ClientName, null);
		}
		else
		{
			for(int i = 0 ; i < numClientNames ; i++)
			{
				String clientNameI = splitClientNames[i];
				if(clientNameI.equals("null"))
				{
					log.error(logHeader + "ClientName " + i + " is null when there are other ClientNames requested");
					error = true;
				}
				else
				{
					// This should be changed to seeing if it's in the BenchmarkConfig
					listClientName.add(clientNameI);
				}
			}
			
			if(!error)
			{
				retVal.put(AnalysisInput.ClientName, listClientName);
			}
		}
		
		if(error)
		{
			retVal.clear();
			retVal = null;
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
