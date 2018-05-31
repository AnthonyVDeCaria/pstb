/**
 * 
 */
package pstb.analysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.analysis.diary.DistributedFlagValue;
import pstb.startup.config.AttributeRatio;
import pstb.startup.config.MessageSize;
import pstb.startup.config.NetworkProtocol;
import pstb.startup.config.NumAttribute;
import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 * 
 * Reads and Parses an Analysis File
 * Creating a List of requested analysis.
 * @see etc/defaultAnalysis.txt
 */
public class ThroughputAnalysisFileParser {
	// Constants - General 
	private final int LOC_BENCHMARK_NUMBER = 0;
	private final int LOC_TOPO_FILE_PATH = 1;
	private final int LOC_DISTRIBUTED_FLAG = 2;
	private final int LOC_PROTOCOL = 3;
	
	// Constants - Throughput
	private final int NUM_SEGMENTS_THROUGHPUT = 8;
	private final int LOC_PERIOD_LENGTH = 4;
	private final int LOC_MESSAGE_SIZE = 5;
	private final int LOC_NUM_ATTRIBUTES = 6;
	private final int LOC_ATTRIBUTE_RATIO = 7;
	
	// Input
	private String analysisFileString;
	
	// Output
	private ArrayList<HashMap<AnalysisInput, String>> requestedAnalysis;
	
	String logHeader = "Analysis Parser: ";
	Logger log = LogManager.getRootLogger();
	
	/**
	 * Empty Constructor
	 */
	public ThroughputAnalysisFileParser()
	{
		requestedAnalysis = new ArrayList<HashMap<AnalysisInput, String>>();
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
	public ArrayList<HashMap<AnalysisInput, String>> getRequestedComponents()
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
			log.error(logHeader + "No path to the Analysis file was given!");
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
					String[] splitLine = line.split(PSTBUtil.COLUMN_SEPARATOR);
					
					if(checkLineLength(splitLine.length))
					{
						HashMap<AnalysisInput, String> requested = developRequestMatrix(splitLine);
						if(requested != null)
						{
							log.trace(logHeader + "Line " + linesRead + "'s syntax checks out.");
							requestedAnalysis.add(requested);
						}
						else
						{
							isParseSuccessful = false;
							log.error(logHeader + "Error in Line " + linesRead + " - Error with Types!");
						}
					}
					else
					{
						isParseSuccessful = false;
						log.error(logHeader + "Error in Line " + linesRead + " - Length isn't correct!");
					}
				}
			}
			readerAF.close();
		} 
		catch (IOException e) 
		{
			isParseSuccessful = false;
			log.error(logHeader + "Cannot find file!", e);
		}
		return isParseSuccessful;
	}
	
	/**
	 * Checks if the user input for the given line is proper. 
	 * If it is, returns a HashMap containing all of these requests.
	 * 
	 * @param splitLine - the line from the analysis file broken apart 
	 * @return null on error; a HashMap of the different requests otherwise
	 */
	private HashMap<AnalysisInput, String> developRequestMatrix(String[] splitLine)
	{
		HashMap<AnalysisInput, String> retVal = new HashMap<AnalysisInput, String>();
				
		String givenBN = splitLine[LOC_BENCHMARK_NUMBER];
		String givenT = splitLine[LOC_TOPO_FILE_PATH];
		String givenDF = splitLine[LOC_DISTRIBUTED_FLAG];
		String givenP = splitLine[LOC_PROTOCOL];
		String givenPL = splitLine[LOC_PERIOD_LENGTH];
		String givenMS = splitLine[LOC_MESSAGE_SIZE];
		String givenNA = splitLine[LOC_NUM_ATTRIBUTES];
		String givenAR = splitLine[LOC_ATTRIBUTE_RATIO];
		
		boolean error = false;
		
		// Benchmark Start Times
		if(givenBN.equals("null"))
		{
			retVal.put(AnalysisInput.BenchmarkNumber, null);
		}
		else
		{
			Pattern bstTest = Pattern.compile(PSTBUtil.BENCHMARK_NUMBER_REGEX);
			if(!bstTest.matcher(givenBN).matches())
			{
				log.error(logHeader + "Given BenchmarkNumber " + givenBN + " is not valid!");
				error = true;
			}
		}
		
		// Topologies
		if(givenT.equals("null"))
		{
			log.error(logHeader + "Given TopologyFile " + givenBN + " is not valid!");
			error = true;
		}
		
		// Distributed
		try
		{
			DistributedFlagValue.valueOf(givenDF);
		}
		catch(Exception e)
		{
			log.error(logHeader + "Given string " + givenDF + " isn't a DistributedFlagValue!");
			error = true;
		}
		
		// Protocols
		try
		{
			NetworkProtocol.valueOf(givenP);
		}
		catch(Exception e)
		{
			log.error(logHeader + "Given string " + givenP + " isn't a valid NetworkProtocol!");
			error = true;
		}
		
		// PeriodLengths
		Long temp = PSTBUtil.checkIfLong(givenPL, true, log);
		if(temp == null)
		{
			log.error(logHeader + "Given string " + givenPL + " isn't a Long!");
			error = true;
			
			// Should also be compared to BenchmarkConfig
		}
		else if(temp.compareTo(0L) <= 0)
		{
			log.error(logHeader + "Given string " + givenPL + " is a Long less than 1!");
			error = true;
		}
		
		// MessageSize
		try
		{
			MessageSize.valueOf(givenMS);
		}
		catch(Exception e)
		{
			log.error(logHeader + "Given string " + givenMS + " isn't a valid MessageSize!");
			error = true;
		}
		
		// NumAttribute
		try
		{
			NumAttribute.valueOf(givenNA);
		}
		catch(Exception e)
		{
			log.error(logHeader + "Given string " + givenNA + " isn't a valid NumAttribute!");
			error = true;
		}
		
		// AttributesRatio
		try
		{
			AttributeRatio.valueOf(givenAR);
		}
		catch(Exception e)
		{
			log.error(logHeader + "Given string " + givenAR + " isn't a valid AttributeRatio!");
			error = true;
		}
		
		if(error)
		{
			retVal.clear();
			retVal = null;
		}
		else
		{
			retVal.put(AnalysisInput.BenchmarkNumber, givenBN);
			String cleanT = PSTBUtil.cleanTFS(givenT);
			retVal.put(AnalysisInput.TopologyFilePath, cleanT);
			retVal.put(AnalysisInput.DistributedFlag, givenDF);
			retVal.put(AnalysisInput.Protocol, givenP);
			retVal.put(AnalysisInput.PeriodLength, givenPL);
			retVal.put(AnalysisInput.MessageSize, givenMS);
			retVal.put(AnalysisInput.NumAttribute, givenNA);
			retVal.put(AnalysisInput.AttributeRatio, givenAR);
		}
		
		return retVal;
	}
	
	private boolean checkLineLength(int lineLength)
	{
		return lineLength == NUM_SEGMENTS_THROUGHPUT;
	}
	
//	private ArrayList<Object> fuck(String[] dick, Enum givenEnum)
//	{
//		ArrayList<Object> retVal = new ArrayList<Object>();
//		
//		int numPSATs = dick.length;
//		if(numPSATs == givenEnum.values().length)
//		{
//			log.warn(logHeader + "This was not necessary - typing null would have sufficed.");
//			return null;
//		}
//		else if(numPSATs == 1 && dick[0].equals("null"))
//		{
//			return null;
//		}
//		else
//		{
//			for(int i = 0 ; i < numPSATs ; i++)
//			{
//				PSActionType temp = null;
//				try
//				{
//					temp = PSActionType.valueOf(dick[i]);
//					retVal.add(temp);
//				}
//				catch(Exception e)
//				{
//					log.error(logHeader + "Given string " + i + " isn't a PSActionType, "
//									+ "or is null with other PSActionTypes!");
//					retVal.clear();
//				}
//			}
//			
//			return retVal;
//		}
//	}
}
