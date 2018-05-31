package pstb.analysis;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.analysis.analysisobjects.throughput.PSTBFinalThroughput;
import pstb.analysis.analysisobjects.throughput.PSTBThroughputAO;
import pstb.analysis.analysisobjects.throughput.PSTBTwoPoints;
import pstb.analysis.diary.ClientDiary;
import pstb.analysis.diary.DiaryEntry;
import pstb.analysis.diary.DiaryHeader;
import pstb.analysis.diary.DistributedFlagValue;
import pstb.benchmark.object.client.padres.PSClientPADRES;
import pstb.util.PSTBError;
import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 * 
 * Analyzes the information contained within the ClientDiaries.
 * As such, it has functions to access and sort these Diaries 
 * as well as key analysis functions.
 */
public class ThroughputAnalyzer {
	// Constants - Startup
	private final static int NUM_ARGS = 3;
	private final static int LOC_PRNT = 0;
	private final static int LOC_GRPH = 1;
	private final static int LOC_FILE = 2;
	
	// Constants - General Diary Names
	private static final int LOC_BENCHMARK_NUMBER = 0;
	private static final int LOC_TOPO_FILE_PATH = 1;
	private static final int LOC_DISTRIBUTED_FLAG = 2;
	private static final int LOC_PROTOCOL = 3;
	
	// Constants - Throughput Diary Names
	private static final int NUM_THROUGHPUT_STRINGS = 9;
	private static final int LOC_PERIOD_LENGTH = 4;
	private static final int LOC_MESSAGE_SIZE = 5;
	private static final int LOC_NUM_ATTRIBUTE = 6;
	private static final int LOC_ATTRIBUTE_RATIO = 7;
	private static final int LOC_NODE_NAME = 8;
	
	// Constants - Folder Names
	private static final String analysisFolderString = System.getProperty("user.dir") + "/analysis/";
	private static final String diariesStub = "diaries/";
	private static final String throughputStub = "throughput/";
	private static final String currentThroughputStub = "currentThroughput/";
	private static final String averageThroughputStub = "averageThroughput/";
	private static final String secantStub = "secant/";
	private static final String finalThroughputStub = "finalThroughput/";
	private static final String roundLatencyStub = "roundLatency/";
	private static final String crStub = "currentRatio/";
	
	// Variables - Key Components
	private static HashMap<String, ClientDiary> bookshelf = new HashMap<String, ClientDiary>();
	
	// Variables - Folder Strings
	private static String diariesFolderString;
	private static String throughputFolderString;
	private static String currentThroughputFolderString;
	private static String averageThroughputFolderString;
	private static String secantFolderString;
	private static String finalThroughputFolderString;
	private static String roundLatencyFolderString;
	private static String crFolderString;
	
	// Variables - Folder Booleans
	private static boolean currentThroughputBool = false;
	private static boolean averageThroughputBool = false;
	private static boolean secantBool = false;
	private static boolean finalThroughputBool = false;
	private static boolean roundLatencyBool = false;
	private static boolean crBool = false; 
	
	// Logger
	private static final Logger logger = LogManager.getRootLogger();
	private static final String logHeader = "Analysis: ";
	
	public static void main(String[] args)
	{
		// Input parsing
		int numArgs = args.length;
		Boolean printDiaries = null;
		boolean graph = false;
		ArrayList<DiaryHeader> graphRequests = new ArrayList<DiaryHeader>();
		boolean conductAnalysis = false;
		String analysisFileString = null;
		
		if(numArgs != NUM_ARGS) 
		{
			logger.info(logHeader + "Not enough arguments provided!");
			System.exit(PSTBError.A_ARGS);
		}
		
		String printFlag = args[LOC_PRNT];
		if(printFlag.equals("true"))
		{
			printDiaries = new Boolean(true);
		}
		else if(printFlag.equals("false"))
		{
			printDiaries = new Boolean(false);
		}
		else
		{
			logger.error(logHeader + "The Print flag was not set properly!");
			System.exit(PSTBError.A_ARGS);
		}
		
		String requestedGraphs = args[LOC_GRPH];
		if(!requestedGraphs.equals("null"))
		{
			String[] brokenRG = requestedGraphs.split(",");
			int numRG = brokenRG.length;
			for(int i = 0 ; i < numRG ; i++)
			{
				String bRGI = brokenRG[i];
				
				DiaryHeader temp = null;
				try
				{
					temp = DiaryHeader.valueOf(bRGI);
				}
				catch(Exception e)
				{
					logger.error(logHeader + bRGI + "is not a DiaryHeader!");
					System.exit(PSTBError.A_ARGS);
				}
				
				if(PSTBUtil.isDHImproper(temp) || temp.equals(DiaryHeader.FinalThroughput))
				{
					logger.error(logHeader + bRGI + "is not a graphable DiaryHeader!");
					System.exit(PSTBError.A_ARGS);
				}
				
				graphRequests.add(temp);
			}
			graph = true;
		}
		
		String argsFile = args[LOC_FILE];
		if(!argsFile.equals("null"))
		{
			conductAnalysis = true;
			analysisFileString = argsFile;
		}
		// Parse complete
		
		logger.info(logHeader + "Beginning analysis...");
		
		Long currTime = System.currentTimeMillis();
		String currFolderString = PSTBUtil.DATE_FORMAT.format(currTime) + "/";
		
		diariesFolderString = analysisFolderString + currFolderString + diariesStub;
		
		throughputFolderString = analysisFolderString + currFolderString + throughputStub;
		currentThroughputFolderString = throughputFolderString + currentThroughputStub;
		averageThroughputFolderString = throughputFolderString + averageThroughputStub;
		secantFolderString = throughputFolderString + secantStub;
		finalThroughputFolderString = throughputFolderString + finalThroughputStub;
		roundLatencyFolderString = throughputFolderString + roundLatencyStub;
		crFolderString = throughputFolderString + crStub;
		
		logger.info("Collecting diaries...");
		boolean collectCheck = collectDiaries();
		if(!collectCheck)
		{
			logger.error(logHeader + "Couldn't collect all the diary files!"); 
			System.exit(PSTBError.A_COLLECT);
		}
		logger.info("All diaries collected.");
		
		if(printDiaries.booleanValue())
		{
			logger.info("Printing all diaries to file...");
			boolean recordDiaryCheck = recordAllDiaries();
			if(!recordDiaryCheck)
			{
				logger.error(logHeader + "Couldn't record the diary files!"); 
				System.exit(PSTBError.A_RECORD_DIARY);
			}
			logger.info("All diaries now in files!");
		}
		
		if(graph)
		{
			boolean graphCheck = graph(graphRequests);
			if(!graphCheck)
			{
				logger.error(logHeader + "Error graphing!"); 
				System.exit(PSTBError.A_GRAPH);
			}
		}
		
		if(conductAnalysis)
		{
			logger.info("Parsing analysis file...");
			ThroughputAnalysisFileParser requestedAnalysis = new ThroughputAnalysisFileParser();
			requestedAnalysis.setAnalysisFileString(analysisFileString);
			boolean analysisParseCheck = requestedAnalysis.parse();
			if(!analysisParseCheck)
			{
				logger.error("Error parsing the analysis file!");
				System.exit(PSTBError.A_ANALYSIS_FILE_PARSE);
			}
			logger.info("Got requested analysises.");
			
			logger.info("Beginning to execute analysis...");
			boolean analysisCheck = executeAnalysis(requestedAnalysis);
			if(!analysisCheck)
			{
				logger.error("Analysis Failed!");
				System.exit(PSTBError.A_ANALYSIS);
			}
			logger.info("Analysis complete.");
		}
	}
	
	/**
	 * Gets a certain serialized diary object 
	 * and adds it to the "bookshelf" 
	 * - a collection of ClientDiaries
	 * 
	 * @param diaryName - the name associated the diary object
	 * @see PSClientPADRES
	 * @return an ArrayList of diaries if everything works properly; null otherwise
	 */
	private static boolean collectDiaries()
	{
		// Get the diary files
		File diaryFolder = new File(System.getProperty("user.dir"));
		File[] listFiles = diaryFolder.listFiles((d, s) -> {
			return s.endsWith(".dia");
		});
		
		// Loop through the files and add them to the "Bookshelf"
		for(int i = 0 ; i < listFiles.length ; i++)
		{
			File diaryI = listFiles[i];
			String diaryIName = diaryI.toString().replace(".dia", "").replace(System.getProperty("user.dir"), "").replace("/", "");
			
			FileInputStream in = null;
			try 
			{
				in = new FileInputStream(diaryI);
			} 
			catch (FileNotFoundException e) 
			{
				logger.error(logHeader + "Error creating new file input stream to read diary: ", e);
				return false;
			}
			
			ClientDiary tiedDiary = PSTBUtil.readDiaryObject(in, logger, logHeader);
			if(tiedDiary != null)
			{
				bookshelf.put(diaryIName, tiedDiary);
			}
			else
			{
				logger.error(logHeader + "Error getting diary " + diaryIName + "!");
				return false;
			}
			
			try 
			{
				in.close();
			} 
			catch (IOException e) 
			{
				logger.error(logHeader + "Error closing FileInputStream: ", e);
				return false;
			}
			
		}
		
		return true;
	}
	
	/**
	 * Prints all the diaries on the bookshelf into files
	 * 
	 * @return false on an error; true otherwise
	 */
	private static boolean recordAllDiaries()
	{	
		// Try to create a diaries folder
		Path diaryFolderPath = Paths.get(diariesFolderString);
		if(Files.notExists(diaryFolderPath))
		{
			try 
			{
				Files.createDirectories(diaryFolderPath);
			} 
			catch (IOException e) 
			{
				logger.error(logHeader + "Couldn't create a diaries folder: ", e);
				return false;
			}
		}
		
		// Try to record diaries to this file
		try
		{
			bookshelf.forEach((diaryName, diary)->{
				String diaryFileString = diariesFolderString + diaryName + ".txt";
				Path diaryFilePath = Paths.get(diaryFileString);
				
				// If any old diaries exist here - delete them
				try 
				{
					Files.deleteIfExists(diaryFilePath);
				} 
				catch (IOException e)
				{
					throw new IllegalArgumentException("IO couldn't delete file " + diaryFileString + "!");
				}
				
				boolean check = diary.recordDiary(diaryFilePath, logger);
				if(!check)
				{
					throw new IllegalArgumentException();
				}
			});
		}
		catch(IllegalArgumentException e)
		{
			logger.error(logHeader + "Couldn't records all of the diaries to a file: ", e);
			return false;
		}
		
		return true;
	}
	
	private static boolean createFolder(String givenFolderString)
	{
		Path givenFolderPath = Paths.get(givenFolderString);
		
		// If the folders don't exist - create them
		boolean createCheck = PSTBUtil.createFolder(givenFolderPath, logger, logHeader);
		if(!createCheck)
		{
			logger.error(logHeader + "Couldn't create current throughput folder!");
			return false;
		}
		
		return true;
	}
	
	private static boolean graph(ArrayList<DiaryHeader> requestedGraphs)
	{
		logger.info("Starting graphs...");
		int numRequestes = requestedGraphs.size();
		
		ArrayList<String> masterDiaries = getAffiliatedThroughputDiaries(null, null, null, null, null, null, null, null, 
				PSTBUtil.MASTER);
		int numDiaries = masterDiaries.size();
		
		for(int i = 0; i < numDiaries ; i++)
		{
			String diaryNameI = masterDiaries.get(i);
			
			String[] command = new String[11];
			command[0] = "python";
			command[1] = "graph.py";
			boolean createGraph = false;
			
			for(int j = 0 ; j < numRequestes ; j++)
			{
				DiaryHeader dhJ = requestedGraphs.get(j);
				PSTBTwoPoints aoJ = (PSTBTwoPoints) extractThroughputObject(diaryNameI, dhJ);
				
				if(aoJ == null)
				{
					return false;
				}
				recordThroughputObject(aoJ);
				
				ArrayList<Point2D.Double> data = aoJ.getDataset();
				int numPoints = data.size();
				if(numPoints > 1)
				{
					createGraph = true;
					
					String[] x = new String[numPoints];
					String[] y = new String[numPoints];
					
					for(int k = 0 ; k < numPoints ; k++)
					{
						Point2D.Double coOrdinateJ = data.get(k);
						Double xK = coOrdinateJ.getX();
						Double yK = coOrdinateJ.getY();
						
						x[k] = xK.toString();
						y[k] = yK.toString();
					}
					
					command[2] = "throughput";
					if(dhJ.equals(DiaryHeader.CurrentThroughput))
					{
						command[3] = currentThroughputFolderString;
						command[6] = "Current Throughput (messages/sec)";
						
						if(!currentThroughputBool)
						{
							currentThroughputBool = createFolder(currentThroughputFolderString);
							if(!currentThroughputBool)
							{
								return false;
							}
						}
					}
					else if(dhJ.equals(DiaryHeader.AverageThroughput))
					{
						command[3] = averageThroughputFolderString;
						command[6] = "Average Throughput (messages/sec)";
						
						if(!averageThroughputBool)
						{
							averageThroughputBool = createFolder(averageThroughputFolderString);
							if(!averageThroughputBool)
							{
								return false;
							}
						}
					}
					else if(dhJ.equals(DiaryHeader.Secant))
					{
						command[3] = secantFolderString;
						command[6] = "Secant (unitless)";
						
						if(!secantBool)
						{
							secantBool = createFolder(secantFolderString);
							if(!secantBool)
							{
								return false;
							}
						}
					}
					else if(dhJ.equals(DiaryHeader.CurrentRatio))
					{
						command[3] = crFolderString;
						command[6] = "Ratio (unitless)";
						
						if(!crBool)
						{
							crBool = createFolder(crFolderString);
							if(!crBool)
							{
								return false;
							}
						}
					}
					else
					{
						command[3] = roundLatencyFolderString;
						command[6] = "Latency (sec)";
						
						if(!roundLatencyBool)
						{
							roundLatencyBool = createFolder(roundLatencyFolderString);
							if(!roundLatencyBool)
							{
								return false;
							}
						}
					}
					command[4] = aoJ.getName();
					command[5] = "Input Rate (messages/sec)";
					command[7] = Arrays.toString(x);
					command[8] = "float";
					command[9] = Arrays.toString(y);
					command[10] = "float";
				}
				
				if(createGraph)
				{
					Boolean graphCheck = PSTBUtil.createANewProcess(command, logger, true, false,
							"Couldn't create graph process!", 
							"Graph complete.", 
							"Graph process failed!");
					if(graphCheck == null || !graphCheck)
					{
						logger.error("Graph failed!");
						return false;
					}
				}
			}
			
			logger.info("Graphs complete.");
		}
		
		return true;
	}
	
	/**
	 * Executes the analysis requested by the user
	 * @see AnalysisFileParser
	 * 
	 * @param requestedAnalysis - the converted analysis file 
	 * @return false on an error; true otherwise
	 */
	private static boolean executeAnalysis(ThroughputAnalysisFileParser givenParser)
	{
		ArrayList<HashMap<AnalysisInput, String>> requestedDiaries = givenParser.getRequestedComponents();
		if(requestedDiaries.isEmpty())
		{
			logger.error(logHeader + "The Analysis file hasn't been parsed!");
			return false;
		}
		
		// Loop through each line of the AnalysisFile
		for(int i = 0 ; i < requestedDiaries.size(); i++)
		{
			HashMap<AnalysisInput, String> analysisI = requestedDiaries.get(i);
			
			// Prepare general Diary Stuff
			String requestedBN = analysisI.get(AnalysisInput.BenchmarkNumber);
			String requestedTFS = analysisI.get(AnalysisInput.TopologyFilePath);
			String requestedDFV = analysisI.get(AnalysisInput.DistributedFlag);
			String requestedP = analysisI.get(AnalysisInput.Protocol);
			String requestedPL = analysisI.get(AnalysisInput.PeriodLength);
			String requestedMS = analysisI.get(AnalysisInput.MessageSize);
			String requestedNA = analysisI.get(AnalysisInput.NumAttribute);
			String requestedAR = analysisI.get(AnalysisInput.AttributeRatio);
			
			ArrayList<String> requestedDiaryNames = getAffiliatedThroughputDiaries(requestedBN, requestedTFS, requestedDFV, 
					requestedP, requestedPL, requestedMS, requestedNA, requestedAR, PSTBUtil.MASTER);
			int numDiaryies = requestedDiaryNames.size();
			for(int j = 0 ; j < numDiaryies ; j++)
			{
				String nameJ = requestedDiaryNames.get(j);
				PSTBThroughputAO completedAnalysis = extractThroughputObject(nameJ, DiaryHeader.FinalThroughput);
				if(completedAnalysis == null)
				{
					logger.error(logHeader + "Smaller analysis failed!");
					return false;
				}
			}
			
		}
		
		return true;
	}
	
	private static ArrayList<String> getAffiliatedThroughputDiaries(String requestedBN, String requestedTPF, String requestedDFV,
			String requestedP, String requestedPL, String requestedMS, String requestedNA, String requestedAR, String requestedNN)
	{	
		// NOTE:	null here means that we want all references to that variable
		// 		Example - if requestedTPF is null, then we want all to look at all the topology files that these diary files have 
		
		ArrayList<String> retVal = new ArrayList<String>();
		String[] nameTestArray = new String[NUM_THROUGHPUT_STRINGS];
		
		// Which lists are null?
		boolean nullBN = (requestedBN == null);
		boolean nullTPF = (requestedTPF == null);
		boolean nullDFV = (requestedDFV == null);
		boolean nullP = (requestedP == null);
		boolean nullPL = (requestedPL == null);
		boolean nullMS = (requestedMS == null);
		boolean nullNA = (requestedNA == null);
		boolean nullAR = (requestedAR == null);
		boolean nullNN = (requestedNN == null);
		
		// Loop through all lists to add its String to the Regex
		if(nullBN)
		{
			nameTestArray[LOC_BENCHMARK_NUMBER] = PSTBUtil.BENCHMARK_NUMBER_REGEX;
		}
		else
		{
			nameTestArray[LOC_BENCHMARK_NUMBER] = requestedBN;
		}
		
		if(nullTPF)
		{
			nameTestArray[LOC_TOPO_FILE_PATH] = "[a-zA-Z0-9-]+";
		}
		else
		{
			nameTestArray[LOC_TOPO_FILE_PATH] = requestedTPF;
		}
		
		if(nullDFV)
		{
			String value = Stream.of(DistributedFlagValue.values())
					.map(k -> String.valueOf(k))
					.collect(Collectors.joining("", "[", "]"));
			nameTestArray[LOC_DISTRIBUTED_FLAG] = value;
		}
		else
		{
			nameTestArray[LOC_DISTRIBUTED_FLAG] = requestedDFV;
		}
		
		if(nullP)
		{
			nameTestArray[LOC_PROTOCOL] = "\\w+";
		}
		else
		{
			nameTestArray[LOC_PROTOCOL] = requestedP;
		}
		
		if(nullPL)
		{
			nameTestArray[LOC_PERIOD_LENGTH] = "\\w+";
		}
		else
		{
			nameTestArray[LOC_PERIOD_LENGTH] = requestedPL;
		}
		
		if(nullMS)
		{
			nameTestArray[LOC_MESSAGE_SIZE] = "\\w+";
		}
		else
		{
			nameTestArray[LOC_MESSAGE_SIZE] = requestedMS;
		}
		
		if(nullNA)
		{
			nameTestArray[LOC_NUM_ATTRIBUTE] = "\\w+";
		}
		else
		{
			nameTestArray[LOC_NUM_ATTRIBUTE] = requestedNA;
		}
		
		if(nullAR)
		{
			nameTestArray[LOC_ATTRIBUTE_RATIO] = "\\w+";
		}
		else
		{
			nameTestArray[LOC_ATTRIBUTE_RATIO] = requestedAR;
		}
		
		if(nullNN)
		{
			nameTestArray[LOC_NODE_NAME] = "\\w+";
		}
		else
		{
			nameTestArray[LOC_NODE_NAME] = requestedNN;
		}
		
		String nameTestString = String.join(PSTBUtil.CONTEXT_SEPARATOR, nameTestArray);
		
		Pattern nameTest = Pattern.compile(nameTestString);
		Iterator<String> bookshelfIt = bookshelf.keySet().iterator();
		
		for( ; bookshelfIt.hasNext() ; )
		{
			String diaryNameI = bookshelfIt.next(); 
			if(nameTest.matcher(diaryNameI).matches())
			{
				retVal.add(diaryNameI);
			}
		}
		
		return retVal;
	}
	
	private static PSTBThroughputAO extractThroughputObject(String givenDiaryName, DiaryHeader requestedAnalysis)
	{
		logger.debug(logHeader + "Working on object " + givenDiaryName + "...");

		ClientDiary diary = bookshelf.get(givenDiaryName);
		String name = requestedAnalysis.toString() + "_" + givenDiaryName;
		
		boolean checkRA = PSTBUtil.isDHImproper(requestedAnalysis);
		if(checkRA)
		{
			logger.error(logHeader + requestedAnalysis + " is an improper request!");
			return null;
		}
		
		PSTBThroughputAO retVal = null;
		boolean isRAFinal = requestedAnalysis.equals(DiaryHeader.FinalThroughput);
		if(isRAFinal)
		{
			retVal = new PSTBFinalThroughput();
		}
		else
		{
			retVal = new PSTBTwoPoints(requestedAnalysis);
		}
		retVal.setName(name);
		
		for(int i = 0 ; i < diary.size() ; i++)
		{
			DiaryEntry pageI = diary.getDiaryEntryI(i);
			
			Double dhMessageRate = pageI.getMessageRate();
			
			Double dhValueI = null;
			if(isRAFinal)
			{
				dhValueI = pageI.getFinalThroughput();
				retVal.handleDataPoint(dhValueI);
			}
			else
			{
				if(requestedAnalysis.equals(DiaryHeader.CurrentThroughput))
				{
					dhValueI = pageI.getCurrentThroughput();
				}
				else if(requestedAnalysis.equals(DiaryHeader.AverageThroughput))
				{
					dhValueI = pageI.getAverageThroughput();
				}
				else if(requestedAnalysis.equals(DiaryHeader.Secant))
				{
					dhValueI = pageI.getSecant();
				}
				else if(requestedAnalysis.equals(DiaryHeader.FinalThroughput))
				{
					dhValueI = pageI.getFinalThroughput();
				}
				else if(requestedAnalysis.equals(DiaryHeader.RoundLatency))
				{
					dhValueI = pageI.getRoundLatency();
				}
				else if(requestedAnalysis.equals(DiaryHeader.CurrentRatio))
				{
					dhValueI = pageI.getCurrentRatio();
				}
				
				if(dhValueI != null)
				{
					retVal.handleDataPoints(dhMessageRate, dhValueI);
				}
			}
		}
		
		return retVal;
	}
	
	private static boolean recordThroughputObject(PSTBThroughputAO givenTO)
	{
		String objectFileString = null;
		switch(givenTO.getAssociatedDH())
		{
			case CurrentThroughput:
			{
				if(!currentThroughputBool)
				{
					currentThroughputBool = createFolder(currentThroughputFolderString);
					if(!currentThroughputBool)
					{
						return false;
					}
				}
				
				objectFileString = currentThroughputFolderString + givenTO.getName() + ".txt";
				break;
			}
			case AverageThroughput:
			{	
				if(!averageThroughputBool)
				{
					averageThroughputBool = createFolder(averageThroughputFolderString);
					if(!averageThroughputBool)
					{
						return false;
					}
				}
				
				objectFileString = averageThroughputFolderString + givenTO.getName() + ".txt";
				break;
			}
			case RoundLatency:
			{
				if(!roundLatencyBool)
				{
					roundLatencyBool = createFolder(roundLatencyFolderString);
					if(!roundLatencyBool)
					{
						return false;
					}
				}
				
				objectFileString = roundLatencyFolderString + givenTO.getName() + ".txt";
				break;
			}
			case FinalThroughput:
			{
				if(!finalThroughputBool)
				{
					finalThroughputBool = createFolder(finalThroughputFolderString);
					if(!finalThroughputBool)
					{
						return false;
					}
				}
				
				objectFileString = finalThroughputFolderString + givenTO.getName() + ".txt";
				break;
			}
			case Secant:
			{
				if(!secantBool)
				{
					secantBool = createFolder(secantFolderString);
					if(!secantBool)
					{
						return false;
					}
				}
				
				objectFileString = secantFolderString + givenTO.getName() + ".txt";
				break;
			}
			case CurrentRatio:
			{
				if(!crBool)
				{
					crBool = createFolder(crFolderString);
					if(!crBool)
					{
						return false;
					}
				}
				
				objectFileString = crFolderString + givenTO.getName() + ".txt";
				break;
			}
			default:
			{
				logger.error(logHeader + "Invalid AnalysisType requested - recording data!");
				return false;
			}
		}
		
		Path objectFilePath = Paths.get(objectFileString);
		boolean check = givenTO.recordAO(objectFilePath);
		if(!check)
		{
			logger.error(logHeader + "Couldn't print AnalysisObject " + givenTO.getName() + "!");
			return false;
		}
		
		return true;
	}
}
