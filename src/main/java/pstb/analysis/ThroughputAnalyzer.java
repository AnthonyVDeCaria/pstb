package pstb.analysis;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
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
	
	// Constants - Analysis
	private static final CharSequence PROBLEMATIC_DIARY_COMPONENT = "_ONE_String0P_";
	
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
			logger.fatal(logHeader + "Couldn't collect all the diary files!"); 
			System.exit(PSTBError.A_COLLECT);
		}
		if(bookshelf.isEmpty())
		{
			logger.fatal(logHeader + "No diary files exist!"); 
			System.exit(PSTBError.A_COLLECT);
		}
		logger.debug("All diaries collected.");
		
		if(printDiaries.booleanValue())
		{
			logger.debug("Printing all diaries to file...");
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
					logger.fatal(logHeader + "Couldn't create a diaries folder: ", e);
					System.exit(PSTBError.A_DIARY);
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
				logger.fatal(logHeader + "Couldn't records all of the diaries to a file: ", e);
				System.exit(PSTBError.A_DIARY);
			}
			
			logger.debug("All diaries now in files!");
		}
		
		if(createReport)
		{
			ArrayList<String> masterDiaries = getAffiliatedThroughputDiaries("null", "null", "null", "null", "null", 
					"null", "null", "null", PSTBUtil.MASTER);
			ArrayList<DiaryHeader> reportableDHs = getProperDHs();
			
			int numMasterDiaries = masterDiaries.size();
			for(int i = 0 ; i < numMasterDiaries ; i++)
			{
				String diaryI = masterDiaries.get(i);
				reportableDHs.forEach((dhJ)->{
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
				});
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
				System.exit(PSTBError.A_ANALYSIS);
			}
			logger.debug("Got requested analysises.");
			
			logger.debug("Beginning to execute analysis...");
			ArrayList<HashMap<AnalysisInput, String>> requestedDiaries = requestedAnalysis.getRequestedComponents();
			if(requestedDiaries.isEmpty())
			{
				logger.fatal(logHeader + "The Analysis file is both parsed and not parsed!");
				System.exit(PSTBError.A_ANALYSIS);
			}
			int numRequestedDairies = requestedDiaries.size();
			
			// Loop through each line of the AnalysisFile
			for(int i = 0 ; i < numRequestedDairies ; i++)
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
				int numDiaries = requestedDiaryNames.size();
				
				if(numDiaries <= 0)
				{
					logger.fatal(logHeader + "No diaries exist for this analysis!"); 
					System.exit(PSTBError.A_ANALYSIS);
				}
				
				// Determine how many constants we have
				String firstDiaryName = requestedDiaryNames.remove(0);
				while(firstDiaryName.contains(PROBLEMATIC_DIARY_COMPONENT))
				{
					requestedDiaryNames.add(firstDiaryName);
					firstDiaryName = requestedDiaryNames.get(0);
				}
				int numProperParts = NUM_THROUGHPUT_STRINGS - 1; // Ignore the NODE_NAME
				int numDiariesLessOne = numDiaries - 1;
				String[] brokenFDN = firstDiaryName.split(PSTBUtil.CONTEXT_SEPARATOR);
				ArrayList<String> constants = new ArrayList<String>(Arrays.asList(brokenFDN));
				constants.remove(LOC_NODE_NAME);
				for(int j = 0 ; j < numProperParts ; j++)
				{
					String subStringJ = brokenFDN[j];
					String testStringJ = subStringJ + PSTBUtil.CONTEXT_SEPARATOR;
					
					for(int k = 0 ; k < numDiariesLessOne ; k++)
					{
						String diaryNameK = requestedDiaryNames.get(k);
						if(!diaryNameK.contains(testStringJ))
						{
							// If we're looking at the Attribute Ratio and dNJ's Number of Attributes is One -> do nothing
							// As One_String0P == One_String50P == One_String100P
							if(j != LOC_ATTRIBUTE_RATIO || !diaryNameK.contains("_One_"))
							{
								constants.remove(subStringJ);
								break;
							}
						}
					}
				}
				requestedDiaryNames.add(firstDiaryName);
				String constant = String.join(",", constants);
				logger.debug(logHeader + "Constants are " + constant + ".");
				// Constants identified
				
				// Get data
				ArrayList<DiaryHeader> keyDHS = getProperDHs();
				int numKeyDHS = keyDHS.size();
				for(int j = 0 ; j < numKeyDHS ; j++)
				{
					DiaryHeader dhJ = keyDHS.get(j);
					logger.debug(logHeader + "Creating " + dhJ + " graph.");
					boolean isFinalThroughput = dhJ.equals(DiaryHeader.FinalThroughput);
					
					TreeMap<String, ArrayList<Point2D.Double>> graphData = new TreeMap<String, ArrayList<Point2D.Double>>();
					TreeMap<String, Double> finalTPData = new TreeMap<String, Double>();
					for(int k = 0 ; k < numDiaries ; k++)
					{
						String diaryNameK = requestedDiaryNames.get(k);
						
						String[] brokenDNJ = diaryNameK.split("_");
						ArrayList<String> temp = new ArrayList<String>(Arrays.asList(brokenDNJ));
						ArrayList<String> variables = new ArrayList<String>(Arrays.asList(brokenDNJ));
						temp.remove(LOC_NODE_NAME);
						variables.remove(LOC_NODE_NAME);
						temp.forEach((var)->{
							if(constants.contains(var))
							{
								variables.remove(var);
							}
						});
						String variable = String.join(",", variables);
						
						logger.debug(logHeader + "Extracting data from " + variable + ".");
						PSTBThroughputAO aoK = extractThroughputObject(diaryNameK, dhJ);
						if(isFinalThroughput)
						{
							PSTBFinalThroughput ftoK = (PSTBFinalThroughput) aoK;
							Double dataK = ftoK.getValue();
							finalTPData.put(variable, dataK);
						}
						else
						{
							PSTBTwoPoints tpoK = (PSTBTwoPoints) aoK;
							ArrayList<Point2D.Double> dataK = tpoK.getDataset();
							if(dataK.isEmpty())
							{
								logger.fatal(logHeader + "Data is missing!"); 
								System.exit(PSTBError.A_ANALYSIS);
							}
							graphData.put(variable, dataK);
						}
					}
					// Data received
					
					// Send data to python
					Path folderPath = Paths.get(tpAnalysisFolderString);
					if(!Files.exists(folderPath))
					{
						try 
						{
							Files.createDirectories(folderPath);
						} 
						catch (Exception e) 
						{
							logger.fatal(logHeader + "Couldn't create directory: ", e);
							System.exit(PSTBError.A_ANALYSIS);
						}
					}
					
					ArrayList<String> tempCommand = new ArrayList<String>();
					tempCommand.add("python");
					tempCommand.add(tpAnalysisFolderString);
					tempCommand.add(dhJ.toString() + " given " + constant);
					if(isFinalThroughput)
					{
						tempCommand.add(1, "finalTP.py");
						tempCommand.add("Messages/sec");
						tempCommand.add(finalTPData.keySet().stream()
								.collect(Collectors.joining("|")));
						tempCommand.add(finalTPData.values().stream()
								.map(k -> String.valueOf(k))
								.collect(Collectors.joining("|")));
						tempCommand.add(PSTBUtil.BENCHMARK_NUMBER_REGEX);
					}
					else
					{
						tempCommand.add(1, "graph2.py");
						tempCommand.add("Input Rate (messages/sec)");
						
						if(dhJ.equals(DiaryHeader.CurrentThroughput))
						{
							tempCommand.add("Current Throughput (messages/sec)");
						}
						else if(dhJ.equals(DiaryHeader.AverageThroughput))
						{
							tempCommand.add("Average Throughput (messages/sec)");
						}
						else if(dhJ.equals(DiaryHeader.Secant))
						{
							tempCommand.add("Secant (unitless)");
						}
						else if(dhJ.equals(DiaryHeader.CurrentRatio))
						{
							tempCommand.add("Ratio (unitless)");
						}
						else
						{
							tempCommand.add("Latency (sec)");
						}
						
						ArrayList<ArrayList<String>> allXs = new ArrayList<ArrayList<String>>();
						ArrayList<ArrayList<String>> allYs = new ArrayList<ArrayList<String>>();
						ArrayList<String> allLabels = new ArrayList<String>();
						graphData.forEach((graph, data)->{
							int numPoints = data.size();
							ArrayList<String> xI = new ArrayList<String>();
							ArrayList<String> yI = new ArrayList<String>();
							
							for(int a = 0 ; a < numPoints ; a++)
							{
								Point2D.Double coOrdinateA = data.get(a);
								Double xA = coOrdinateA.getX();
								Double yA = coOrdinateA.getY();
								
								xI.add(xA.toString());
								yI.add(yA.toString());
							}
							
							allXs.add(xI);
							allYs.add(yI);
							allLabels.add(graph);
						});
						
						ArrayList<String> allXsTemp = new ArrayList<String>();
						allXs.forEach((t)->{
							String e = t.stream()
									.collect(Collectors.joining("+"));
							allXsTemp.add(e);
						});
						String combinedXs = allXsTemp.stream()
								.collect(Collectors.joining("|"));
						tempCommand.add(combinedXs);
						
						ArrayList<String> allYsTemp = new ArrayList<String>();
						allYs.forEach((t)->{
							String e = t.stream()
									.collect(Collectors.joining("+"));
							allYsTemp.add(e);
						});
						String combinedYs = allYsTemp.stream()
								.collect(Collectors.joining("|"));
						tempCommand.add(combinedYs);
						
						tempCommand.add(graphData.keySet().stream()
								.collect(Collectors.joining("|")));
					}
					
					String[] command = tempCommand.toArray(new String[tempCommand.size()]);
					Boolean graphCheck = PSTBUtil.createANewProcess(command, logger, true, true,
							"Couldn't create graph process!", 
							"Graph complete.", 
							"Graph process failed!");
					if(graphCheck == null || !graphCheck)
					{
						logger.fatal(logHeader + "Couldn't print graphs!");
						System.exit(PSTBError.A_ANALYSIS);
					}
					
					System.out.println("");
				}
				}
				
			logger.debug("Analysis complete.");
		}
	}
	
	/**
	 * Gets a certain serialized diary object 
	 * a
	 * 
	 * 
	 * nd adds it to the "bookshelf" 
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
			catch (Exception e) 
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
			catch (Exception e) 
			{
				logger.error(logHeader + "Error closing FileInputStream: ", e);
				return false;
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
		boolean nullBN = requestedBN.equals("null");
		boolean nullTPF = requestedTPF.equals("null");
		boolean nullDFV = requestedDFV.equals("null");
		boolean nullP = requestedP.equals("null");
		boolean nullPL = requestedPL.equals("null");
		boolean nullMS = requestedMS.equals("null");
		boolean nullNA = requestedNA.equals("null");
		boolean nullAR = requestedAR.equals("null");
		boolean nullNN = requestedNN.equals("null");
		
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
			nameTestArray[LOC_PERIOD_LENGTH] = "\\d+";
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
		ClientDiary diary = bookshelf.get(givenDiaryName);
		String name = requestedAnalysis.toString() + PSTBUtil.CONTEXT_SEPARATOR + givenDiaryName;
		
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
		
		DiaryHeader givenAODH = givenAO.getAssociatedDH();
		
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
			
			for(int i = 0 ; i < numPoints ; i++)
			{
				Point2D.Double coOrdinateJ = data.get(i);
				Double xI = coOrdinateJ.getX();
				Double yI = coOrdinateJ.getY();
				
				x[i] = xI.toString();
				y[i] = yI.toString();
			}
			
			command[2] = "throughput";
			command[3] = folderString;
			command[4] = titleName;
			command[5] = "Input Rate (messages/sec)";
			if(givenAODH.equals(DiaryHeader.CurrentThroughput))
			{
				command[6] = "Current Throughput (messages/sec)";
			}
			else if(givenAODH.equals(DiaryHeader.AverageThroughput))
			{
				command[6] = "Average Throughput (messages/sec)";
			}
			else if(givenAODH.equals(DiaryHeader.Secant))
			{
				command[6] = "Secant (unitless)";
			}
			else if(givenAODH.equals(DiaryHeader.CurrentRatio))
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
	
	private static ArrayList<DiaryHeader> getProperDHs()
	{
		ArrayList<DiaryHeader> retVal = new ArrayList<DiaryHeader>();
		retVal.add(DiaryHeader.CurrentThroughput);
		retVal.add(DiaryHeader.AverageThroughput);
		retVal.add(DiaryHeader.RoundLatency);
		retVal.add(DiaryHeader.Secant);
		retVal.add(DiaryHeader.CurrentRatio);
		retVal.add(DiaryHeader.FinalThroughput);
		
		return retVal;
	}
}
