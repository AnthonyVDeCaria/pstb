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

import pstb.analysis.diary.DiaryHeader;
import pstb.analysis.diary.DistributedFlagValue;
import pstb.startup.config.AttributeRatio;
import pstb.startup.config.MessageSize;
import pstb.startup.config.NetworkProtocol;
import pstb.startup.config.NumAttribute;
import pstb.startup.workload.PSActionType;
import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 * 
 * Reads and Parses an Analysis File
 * Creating a List of requested analysis.
 * @see etc/defaultAnalysis.txt
 */
public class AnalysisFileParser {
	// Constants - General 
	private final int LOC_BENCHMARK_NUMBER = 0;
	private final int LOC_TOPO_FILE_PATH = 1;
	private final int LOC_DISTRIBUTED_FLAG = 2;
	private final int LOC_PROTOCOL = 3;
	
	// Constants - Scenario
	private final int NUM_SEGMENTS_SCENARIO = 9;
	private final int LOC_RUN_LENGTH = 4;
	private final int LOC_RUN_NUMBER = 5;
	private final int LOC_CLIENT_NAME = 6;
	private final int LOC_ANALYSIS_TYPE = 7;
	private final int LOC_PS_ACTION_TYPE = 8;
	
	// Constants - Throughput
	private final int NUM_SEGMENTS_THROUGHPUT = 9;
	private final int LOC_PERIOD_LENGTH = 4;
	private final int LOC_MESSAGE_SIZE = 5;
	private final int LOC_NUM_ATTRIBUTES = 6;
	private final int LOC_ATTRIBUTE_RATIO = 7;
	private final int LOC_DIARY_HEADER = 8;
	private final int NUM_ALLOWED_DHS = 5;
	
	// Input
	private String analysisFileString;
	
	// Output
	private ArrayList<HashMap<AnalysisInput, ArrayList<Object>>> requestedAnalysis;
	private AnalysisFileExtension afsExtension;
	
	String logHeader = "Analysis Parser: ";
	Logger log = LogManager.getRootLogger();
	
	public enum AnalysisFileExtension
	{
		sin, thp
	}
	
	/**
	 * Empty Constructor
	 */
	public AnalysisFileParser()
	{
		requestedAnalysis = new ArrayList<HashMap<AnalysisInput, ArrayList<Object>>>();
		analysisFileString = new String();
		afsExtension = null;
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
	
	public AnalysisFileExtension getExtension()
	{
		return afsExtension;
	}
	
	public AnalysisFileExtension extractExtension()
	{
		if(analysisFileString == null)
		{
			return null;
		}
		
		String extension = PSTBUtil.getFileExtension(analysisFileString);
		try
		{
			afsExtension = AnalysisFileExtension.valueOf(extension);
		}
		catch(Exception e)
		{
			return null;
		}
		
		return afsExtension;
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
		
		extractExtension();
		if(afsExtension == null)
		{
			log.error(logHeader + "The given file has an incorrect extension!");
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
					
					if(checkLineLength(afsExtension, splitLine.length))
					{
						HashMap<AnalysisInput, ArrayList<Object>> requested = developRequestMatrix(splitLine, afsExtension);
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
	 * @param extension 
	 * @return null on error; a HashMap of the different requests otherwise
	 */
	private HashMap<AnalysisInput, ArrayList<Object>> developRequestMatrix(String[] splitLine, AnalysisFileExtension extension)
	{
		HashMap<AnalysisInput, ArrayList<Object>> retVal = new HashMap<AnalysisInput, ArrayList<Object>>();
		
		ArrayList<Object> listBenchmark = new ArrayList<Object>();
		ArrayList<Object> listTopology = new ArrayList<Object>();
		ArrayList<Object> listDistributed = new ArrayList<Object>();
		ArrayList<Object> listProtocol = new ArrayList<Object>();
		
		String benchmarkNums = splitLine[LOC_BENCHMARK_NUMBER];
		String topologies = splitLine[LOC_TOPO_FILE_PATH];
		String distributedFlags = splitLine[LOC_DISTRIBUTED_FLAG];
		String protocols = splitLine[LOC_PROTOCOL];
		
		String[] splitBNs = benchmarkNums.split(",");
		String[] splitTopologies = topologies.split(",");
		String[] splitDistributedFlags = distributedFlags.split(",");
		String[] splitProtocols = protocols.split(",");
		
		boolean error = false;
		
		// Benchmark Start Times
		int numBNs = splitBNs.length;
		if(numBNs == 1 && splitBNs[0].equals("null"))
		{
			retVal.put(AnalysisInput.BenchmarkNumber, null);
		}
		else
		{
			Pattern bstTest = Pattern.compile(PSTBUtil.BENCHMARK_NUMBER_REGEX);
			
			for(int i = 0 ; i < numBNs ; i++)
			{
				String bnI = splitBNs[i];
				if(bstTest.matcher(bnI).matches())
				{
					listBenchmark.add(bnI);
				}
				else
				{
					log.error(logHeader + "Given BenchmarkNumber " + bnI + " is not valid!");
					listBenchmark.clear();
					error = true;
				}
			}
			
			retVal.put(AnalysisInput.BenchmarkNumber, listBenchmark);
		}
		
		// Topologies
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
					log.error(logHeader + "TopologyFile " + i + " is null when there are other topologies requested!");
					error = true;
				}
				else
				{
					listTopology.add(PSTBUtil.cleanTFS(topologyI));
				}
			}
			
			if(!error)
			{
				retVal.put(AnalysisInput.TopologyFilePath, listTopology);
			}
		}
		
		// Distributed
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
				DistributedFlagValue temp = null;
				try
				{
					temp = DistributedFlagValue.valueOf(distributedFlagI);
					listDistributed.add(temp);
				}
				catch(Exception e)
				{
					log.error(logHeader + "Given string " + distributedFlagI + " isn't a DistributedFlagValue,"
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
		
		// Protocols
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
				NetworkProtocol temp = null;
				try
				{
					temp = NetworkProtocol.valueOf(splitProtocols[i]);
					listProtocol.add(temp);
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
		
		if(extension.equals(AnalysisFileExtension.sin))
		{
			ArrayList<Object> listRunLength = new ArrayList<Object>();
			ArrayList<Object> listRunNumber = new ArrayList<Object>();
			ArrayList<Object> listClientName = new ArrayList<Object>();
			ArrayList<Object> listAnalysisType = new ArrayList<Object>();
			ArrayList<Object> listPSActionType = new ArrayList<Object>();
			
			String runLengths = splitLine[LOC_RUN_LENGTH];
			String runNumbers = splitLine[LOC_RUN_NUMBER];
			String clientNames = splitLine[LOC_CLIENT_NAME];
			String analysisTypes = splitLine[LOC_ANALYSIS_TYPE];
			String psActionTypes = splitLine[LOC_PS_ACTION_TYPE];
			
			String[] splitRunLengths = runLengths.split(",");
			String[] splitRunNumbers = runNumbers.split(",");
			String[] splitClientNames = clientNames.split(",");
			String[] splitATs = analysisTypes.split(",");
			String[] splitPSATs = psActionTypes.split(",");
			
			// RunLengths
			int numRunLengths = splitRunLengths.length;
			if(numRunLengths == 1 && splitRunLengths[0].equals("null"))
			{
				retVal.put(AnalysisInput.RunLength, null);
			}
			else
			{
				for(int i = 0 ; i < numRunLengths ; i++)
				{
					String runLengthI = splitRunLengths[i];
					
					Long temp = PSTBUtil.checkIfLong(runLengthI, true, log);
					if(temp != null)
					{
						if(temp.compareTo(0L) > 0)
						{
							listRunLength.add(temp);
						}
						// Should also be compared to BenchmarkConfig
						else
						{
							log.error(logHeader + "Given string " + runLengthI + " is a Long less than 1!");
							listRunLength.clear();
							error = true;
						}
					}
					else
					{
						log.error(logHeader + "Given string " + runLengthI + " isn't a Long, or is null with other Run Lengths!");
						listRunLength.clear();
						error = true;
					}
				}
				
				if(!error)
				{
					retVal.put(AnalysisInput.RunLength, listRunLength);
				}
			}
			
			// Run Numbers
			int numRunNumbers = splitRunNumbers.length;
			if(numRunNumbers == 1 && splitRunNumbers[0].equals("null"))
			{
				retVal.put(AnalysisInput.RunNumber, null);
			}
			else
			{
				for(int i = 0 ; i < numRunNumbers ; i++)
				{
					String runNumberI = splitRunNumbers[i];
					
					Long temp = PSTBUtil.checkIfLong(runNumberI, false, null);
					if(temp != null)
					{
						if(temp.compareTo(0L) > -1)
						{
							listRunNumber.add(temp);
						}
						else
						{
							log.error(logHeader + "Given string " + runNumberI + " is a Long less than 0!");
							listRunNumber.clear();
							error = true;
						}
					}
					else
					{
						log.error(logHeader + "Given string " + runNumberI + " isn't a Long, or is null with other Run Numbers!");
						listRunNumber.clear();
						error = true;
					}
				}
				
				if(!error)
				{
					retVal.put(AnalysisInput.RunNumber, listRunNumber);
				}
			}
			
			// ClientNames
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
						log.error(logHeader + "ClientName " + i + " is null when there are other ClientNames requested!");
						error = true;
					}
					else
					{
						listClientName.add(clientNameI);
					}
				}
				
				if(!error)
				{
					retVal.put(AnalysisInput.ClientName, listClientName);
				}
			}
			
			// Analysis Types
			int numATs = splitATs.length;
			if(numATs == AnalysisType.values().length)
			{
				log.warn(logHeader + "This was not necessary - typing null would have sufficed.");
				retVal.put(AnalysisInput.AnalysisType, null);
			}
			else if(numATs == 1 && splitATs[0].equals("null"))
			{
				retVal.put(AnalysisInput.AnalysisType, null);
			}
			else
			{
				for(int i = 0 ; i < numATs ; i++)
				{
					AnalysisType temp = null;
					try
					{
						temp = AnalysisType.valueOf(splitATs[i]);
						listAnalysisType.add(temp);
					}
					catch(Exception e)
					{
						log.error(logHeader + "Given string " + i + " isn't an AnalysisType, "
										+ "or is null with other AnalysisTypes!");
						listAnalysisType.clear();
						error = true;
					}
				}
				
				if(!error)
				{
					retVal.put(AnalysisInput.AnalysisType, listAnalysisType);
				}
			}
			
			// PSActionTypes
			int numPSATs = splitPSATs.length;
			if(numPSATs == PSActionType.values().length)
			{
				log.warn(logHeader + "This was not necessary - typing null would have sufficed.");
				retVal.put(AnalysisInput.PSActionType, null);
			}
			else if(numPSATs == 1 && splitPSATs[0].equals("null"))
			{
				retVal.put(AnalysisInput.PSActionType, null);
			}
			else
			{
				for(int i = 0 ; i < numPSATs ; i++)
				{
					PSActionType temp = null;
					try
					{
						temp = PSActionType.valueOf(splitPSATs[i]);
						listPSActionType.add(temp);
					}
					catch(Exception e)
					{
						log.error(logHeader + "Given string " + i + " isn't an PSActionType, "
										+ "or is null with other PSActionTypes!");
						listPSActionType.clear();
						error = true;
					}
				}
				
				if(!error)
				{
					retVal.put(AnalysisInput.PSActionType, listPSActionType);
				}
			}
		}
		else if(extension.equals(AnalysisFileExtension.thp))
		{
			ArrayList<Object> listPeriodLength = new ArrayList<Object>();
			ArrayList<Object> listMessageSize = new ArrayList<Object>();
			ArrayList<Object> listNumAttributes = new ArrayList<Object>();
			ArrayList<Object> listAttributeRatio = new ArrayList<Object>();
			ArrayList<Object> listDiaryHeader = new ArrayList<Object>();
			
			String periodLengths = splitLine[LOC_PERIOD_LENGTH];
			String messageSizes = splitLine[LOC_MESSAGE_SIZE];
			String numAttributes = splitLine[LOC_NUM_ATTRIBUTES];
			String attributeRatios = splitLine[LOC_ATTRIBUTE_RATIO];
			String diaryHeaders = splitLine[LOC_DIARY_HEADER];
			
			String[] splitPeriodLengths = periodLengths.split(",");
			String[] splitMessageSizes = messageSizes.split(",");
			String[] splitNumAttributes = numAttributes.split(",");
			String[] splitARs = attributeRatios.split(",");
			String[] splitDHs = diaryHeaders.split(",");
			
			// PeriodLengths
			int numPeriodLengths = splitPeriodLengths.length;
			if(numPeriodLengths == 1 && splitPeriodLengths[0].equals("null"))
			{
				retVal.put(AnalysisInput.PeriodLength, null);
			}
			else
			{
				for(int i = 0 ; i < numPeriodLengths ; i++)
				{
					String periodLengthI = splitPeriodLengths[i];
					
					Long temp = PSTBUtil.checkIfLong(periodLengthI, true, log);
					if(temp != null)
					{
						if(temp.compareTo(0L) > 0)
						{
							listPeriodLength.add(temp);
						}
						// Should also be compared to BenchmarkConfig
						else
						{
							log.error(logHeader + "Given string " + periodLengthI + " is a Long less than 1!");
							listPeriodLength.clear();
							error = true;
						}
					}
					else
					{
						log.error(logHeader + "Given string " + periodLengthI + " isn't a Long, or is null with other Period Lengths!");
						listPeriodLength.clear();
						error = true;
					}
				}
				
				if(!error)
				{
					retVal.put(AnalysisInput.PeriodLength, listPeriodLength);
				}
			}
			
			// MessageSize
			int numMSs = splitMessageSizes.length;
			if(numMSs == MessageSize.values().length)
			{
				log.warn(logHeader + "This was not necessary - typing null would have sufficed.");
				retVal.put(AnalysisInput.MessageSize, null);
			}
			else if(numMSs == 1 && splitMessageSizes[0].equals("null"))
			{
				retVal.put(AnalysisInput.MessageSize, null);
			}
			else
			{
				for(int i = 0 ; i < numMSs ; i++)
				{
					MessageSize temp = null;
					try
					{
						temp = MessageSize.valueOf(splitMessageSizes[i]);
						listMessageSize.add(temp);
					}
					catch(Exception e)
					{
						log.error(logHeader + "Given string " + i + " isn't a MessageSize, or is null with other MessageSize!");
						listMessageSize.clear();
						error = true;
					}
				}
				
				if(!error)
				{
					retVal.put(AnalysisInput.MessageSize, listMessageSize);
				}
			}
			
			// Num Attribute
			int numNAs = splitNumAttributes.length;
			if(numNAs == NumAttribute.values().length)
			{
				log.warn(logHeader + "This was not necessary - typing null would have sufficed.");
				retVal.put(AnalysisInput.NumAttribute, null);
			}
			else if(numNAs == 1 && splitNumAttributes[0].equals("null"))
			{
				retVal.put(AnalysisInput.NumAttribute, null);
			}
			else
			{
				for(int i = 0 ; i < numNAs ; i++)
				{
					NumAttribute temp = null;
					try
					{
						temp = NumAttribute.valueOf(splitNumAttributes[i]);
					}
					catch(Exception e)
					{
						log.error(logHeader + "Given string " + i + " isn't a NumAttribute, or is null with other NumAttributes!");
						listNumAttributes.clear();
						error = true;
					}
					
					listNumAttributes.add(temp);
				}
				
				if(!error)
				{
					retVal.put(AnalysisInput.NumAttribute, listNumAttributes);
				}
			}

			// Attribute Ratio
			int numARs = splitARs.length;
			if(numARs == AttributeRatio.values().length)
			{
				log.warn(logHeader + "This was not necessary - typing null would have sufficed.");
				retVal.put(AnalysisInput.AttributeRatio, null);
			}
			else if(numARs == 1 && splitARs[0].equals("null"))
			{
				retVal.put(AnalysisInput.AttributeRatio, null);
			}
			else
			{
				for(int i = 0 ; i < numARs ; i++)
				{
					AttributeRatio temp = null;
					try
					{
						temp = AttributeRatio.valueOf(splitARs[i]);
					}
					catch(Exception e)
					{
						log.error(logHeader + "Given string " + i + " isn't an AttributeRatio, or is null with other AttributeRatios!");
						listAttributeRatio.clear();
						error = true;
					}
					
					listAttributeRatio.add(temp);
				}
				
				if(!error)
				{
					retVal.put(AnalysisInput.AttributeRatio, listAttributeRatio);
				}
			}
			
			// DiaryHeader
			int numDHs = splitDHs.length;
			if(numDHs == NUM_ALLOWED_DHS)
			{
				log.warn(logHeader + "This was not necessary - typing null would have sufficed.");
				retVal.put(AnalysisInput.DiaryHeader, null);
			}
			else if(numDHs == 1 && splitDHs[0].equals("null"))
			{
				retVal.put(AnalysisInput.DiaryHeader, null);
			}
			else
			{
				for(int i = 0 ; i < numDHs ; i++)
				{
					String dhI = splitDHs[i];
					DiaryHeader temp = null;
					try
					{
						temp = DiaryHeader.valueOf(dhI);
					}
					catch(Exception e)
					{
						log.error(logHeader + "Given string " + dhI + " isn't a DiaryHeader, or is null with other DiaryHeaders!");
						listDiaryHeader.clear();
						error = true;
					}
					
					if(PSTBUtil.isDHImproper(temp))
					{
						log.error(logHeader + "Given string " + dhI + " is an illegal DiaryHeader!");
						listDiaryHeader.clear();
						error = true;
					}
					else
					{
						listDiaryHeader.add(temp);
					}
				}
				
				if(!error)
				{
					retVal.put(AnalysisInput.DiaryHeader, listDiaryHeader);
				}
			}
		}
		else
		{
			error = true;
		}
		
		if(error)
		{
			retVal.clear();
			retVal = null;
		}
		
		return retVal;
	}
	
	private boolean checkLineLength(AnalysisFileExtension givenMode, int lineLength)
	{
		if(givenMode.equals(AnalysisFileExtension.sin))
		{
			return lineLength == NUM_SEGMENTS_SCENARIO;
		}
		else if(givenMode.equals(AnalysisFileExtension.thp))
		{
			return lineLength == NUM_SEGMENTS_THROUGHPUT;
		}
		else
		{
			return false;
		}
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
