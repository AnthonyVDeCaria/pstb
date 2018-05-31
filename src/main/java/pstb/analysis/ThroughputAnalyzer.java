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
	private static final String reportStub = "reports/";
	private static final String analysisStub = "analysis/";
	
	// Variables - Key Components
	private static HashMap<String, ClientDiary> bookshelf = new HashMap<String, ClientDiary>();
	
	// Variables - Folder Strings
	private static String diariesFolderString;
	private static String reportFolderString;
	private static String tpAnalysisFolderString;
	
	// Logger
	private static final Logger logger = LogManager.getRootLogger();
	private static final String logHeader = "Analysis: ";
	
	public static void main(String[] args)
	{
		// Input parsing
		int numArgs = args.length;
		Boolean printDiaries = null;
		Boolean createReport = null;
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
		
		String reportFlag = args[LOC_GRPH];
		if(reportFlag.equals("true"))
		{
			createReport = new Boolean(true);
		}
		else if(reportFlag.equals("false"))
		{
			createReport = new Boolean(false);
		}
		else
		{
			logger.error(logHeader + "The Report flag was not set properly!");
			System.exit(PSTBError.A_ARGS);
		}
		
		String argsFile = args[LOC_FILE];
		if(!argsFile.equals("null"))
		{
			conductAnalysis = true;
			analysisFileString = argsFile;
		}
		
		if((printDiaries == false) && (createReport == false) && (conductAnalysis == false))
		{
			logger.error(logHeader + "No request was submitted!");
			System.exit(PSTBError.A_ARGS);
		}
		// Parse complete
		
		logger.info(logHeader + "Beginning analysis...");
		
		Long currTime = System.currentTimeMillis();
		String currFolderString = PSTBUtil.DATE_FORMAT.format(currTime) + "/";
		
		diariesFolderString = analysisFolderString + currFolderString + diariesStub;
		reportFolderString = analysisFolderString + currFolderString + reportStub;
		tpAnalysisFolderString = analysisFolderString + currFolderString + analysisStub;
		
		logger.debug("Collecting diaries...");
		boolean collectCheck = collectDiaries();
		if(!collectCheck)
		{
			logger.error(logHeader + "Couldn't collect all the diary files!"); 
			System.exit(PSTBError.A_COLLECT);
		}
		if(bookshelf.isEmpty())
		{
			logger.error(logHeader + "No diary files exist!"); 
			System.exit(PSTBError.A_COLLECT);
		}
		logger.debug("All diaries collected.");
		
		if(printDiaries.booleanValue())
		{
			logger.debug("Printing all diaries to file...");
			boolean recordDiaryCheck = recordAllDiaries();
			if(!recordDiaryCheck)
			{
				logger.error(logHeader + "Couldn't record the diary files!"); 
				System.exit(PSTBError.A_DIARY);
			}
			logger.debug("All diaries now in files!");
		}
		
		if(createReport)
		{
			ArrayList<String> masterDiaries = getAffiliatedThroughputDiaries(null, null, null, null, null, null, null, null, 
					PSTBUtil.MASTER);
			
			int numMasterDiaries = masterDiaries.size();
			for(int i = 0 ; i < numMasterDiaries ; i++)
			{
				String diaryI = masterDiaries.get(i);
				for(DiaryHeader dhJ : DiaryHeader.values())
				{
					if(PSTBUtil.isDHThroughput(dhJ))
					{
						PSTBThroughputAO tpoIJ = extractThroughputObject(diaryI, dhJ);
						if(tpoIJ == null)
						{
							System.exit(PSTBError.A_REPORT);
						}
						
						String[] brokenDiary = diaryI.split(PSTBUtil.CONTEXT_SEPARATOR);
						String folderString = reportFolderString
								+ brokenDiary[LOC_TOPO_FILE_PATH] + "/"
								+ brokenDiary[LOC_DISTRIBUTED_FLAG] + "/"
								+ brokenDiary[LOC_PROTOCOL] + "/"
								+ brokenDiary[LOC_PERIOD_LENGTH] + "/"
								+ brokenDiary[LOC_MESSAGE_SIZE] + "/"
								+ brokenDiary[LOC_NUM_ATTRIBUTE] + "/"
								+ brokenDiary[LOC_ATTRIBUTE_RATIO] + "/";
						String title = brokenDiary[LOC_BENCHMARK_NUMBER] + PSTBUtil.CONTEXT_SEPARATOR + tpoIJ.getAssociatedDH();
						
						Path folderPath = Paths.get(folderString);
						if(!Files.exists(folderPath))
						{
							try 
							{
								Files.createDirectories(folderPath);
							} 
							catch (Exception e) 
							{
								logger.fatal(logHeader + "Couldn't create directories: ", e);
								System.exit(PSTBError.A_REPORT);
							}
						}
						
						boolean recordCheck = recordThroughputObject(folderString, title, tpoIJ);
						if(!recordCheck)
						{
							System.exit(PSTBError.A_REPORT);
						}
						
						if(PSTBUtil.isDHThroughputGraphable(dhJ))
						{
							PSTBTwoPoints temp = (PSTBTwoPoints) tpoIJ;
							boolean graphCheck = simpleGraph(folderString, title, temp);
							if(!graphCheck)
							{
								System.exit(PSTBError.A_REPORT);
							}
						}
					}
				}
			}
		}
		
		if(conductAnalysis)
		{
			logger.debug("Parsing analysis file...");
			ThroughputAnalysisFileParser requestedAnalysis = new ThroughputAnalysisFileParser();
			requestedAnalysis.setAnalysisFileString(analysisFileString);
			boolean analysisParseCheck = requestedAnalysis.parse();
			if(!analysisParseCheck)
			{
				logger.error("Error parsing the analysis file!");
				System.exit(PSTBError.A_ANALYSIS_FILE_PARSE);
			}
			logger.debug("Got requested analysises.");
			
			logger.debug("Beginning to execute analysis...");
			boolean analysisCheck = executeAnalysis(requestedAnalysis);
			if(!analysisCheck)
			{
				logger.error("Analysis Failed!");
				System.exit(PSTBError.A_ANALYSIS);
			}
			logger.debug("Analysis complete.");
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
		
		boolean checkRA = PSTBUtil.isDHThroughput(requestedAnalysis);
		if(!checkRA)
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
	
	private static boolean recordThroughputObject(String folderString, String title, PSTBThroughputAO givenTO)
	{
		String objectFileString = folderString + title + ".txt";
		Path objectFilePath = Paths.get(objectFileString);
		boolean check = givenTO.recordAO(objectFilePath);
		if(!check)
		{
			logger.fatal(logHeader + "Couldn't print AnalysisObject " + givenTO.getName() + "!");
			return false;
		}
		
		return true;
	}
	
	private static boolean simpleGraph(String folderString, String titleName, PSTBTwoPoints givenAO)
	{
		if(givenAO == null)
		{
			return false;
		}
		
		boolean createGraph = false;
		
		DiaryHeader f = givenAO.getAssociatedDH();
		
		String[] command = new String[11];
		command[0] = "python";
		command[1] = "graph.py";
		
		ArrayList<Point2D.Double> data = givenAO.getDataset();
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
			command[3] = folderString;
			command[4] = titleName;
			command[5] = "Input Rate (messages/sec)";
			if(f.equals(DiaryHeader.CurrentThroughput))
			{
				command[6] = "Current Throughput (messages/sec)";
			}
			else if(f.equals(DiaryHeader.AverageThroughput))
			{
				command[6] = "Average Throughput (messages/sec)";
			}
			else if(f.equals(DiaryHeader.Secant))
			{
				command[6] = "Secant (unitless)";
			}
			else if(f.equals(DiaryHeader.CurrentRatio))
			{
				command[6] = "Ratio (unitless)";
			}
			else
			{
				command[6] = "Latency (sec)";
			}
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
				return false;
			}
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
}
