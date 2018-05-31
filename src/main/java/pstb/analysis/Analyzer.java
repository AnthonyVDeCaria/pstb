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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.analysis.AnalysisFileParser.AnalysisFileExtension;
import pstb.analysis.analysisobjects.PSTBAnalysisObject;
import pstb.analysis.analysisobjects.scenario.PSTBAvgDelay;
import pstb.analysis.analysisobjects.scenario.PSTBDataCounter;
import pstb.analysis.analysisobjects.scenario.PSTBHistogram;
import pstb.analysis.analysisobjects.scenario.PSTBScenarioAO;
import pstb.analysis.analysisobjects.throughput.PSTBFinalThroughput;
import pstb.analysis.analysisobjects.throughput.PSTBThroughputAO;
import pstb.analysis.analysisobjects.throughput.PSTBTwoPoints;
import pstb.analysis.diary.ClientDiary;
import pstb.analysis.diary.DiaryEntry;
import pstb.analysis.diary.DiaryHeader;
import pstb.analysis.diary.DistributedFlagValue;
import pstb.benchmark.object.client.padres.PSClientPADRES;
import pstb.startup.config.AttributeRatio;
import pstb.startup.config.MessageSize;
import pstb.startup.config.NetworkProtocol;
import pstb.startup.config.NumAttribute;
import pstb.startup.workload.PSActionType;
import pstb.util.PSTBError;
import pstb.util.PSTBUtil;
import pstb.util.PSTBUtil.TimeType;

/**
 * @author padres-dev-4187
 * 
 * Analyzes the information contained within the ClientDiaries.
 * As such, it has functions to access and sort these Diaries 
 * as well as key analysis functions.
 */
public class Analyzer {
	// Constants - Startup
	private final static int NUM_ARGS = 2;
	private final static int LOC_PRNT = 0;
	private final static int LOC_FILE = 1;
	
	// Constants - General Diary Names
	private static final int LOC_BENCHMARK_NUMBER = 0;
	private static final int LOC_TOPO_FILE_PATH = 1;
	private static final int LOC_DISTRIBUTED_FLAG = 2;
	private static final int LOC_PROTOCOL = 3;
	
	// Constants - Scenario Diary Names
	private static final int NUM_SCENARIO_STRINGS = 7;
	private static final int LOC_RUN_LENGTH = 4;
	private static final int LOC_RUN_NUMBER = 5;
	private static final int LOC_CLIENT_NAME = 6;
	
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
	private static final String scenarioStub = "scenario/";
	private static final String delayCounterStub = "delayCounter/";
	private static final String frequencyCounterStub = "freqCounter/";
	private static final String avgDelayStub = "avgDelay/";
	private static final String histogramStub = "histogram/";
	private static final String throughputStub = "throughput/";
	private static final String currentThroughputStub = "currentThroughput/";
	private static final String averageThroughputStub = "averageThroughput/";
	private static final String secantStub = "secant/";
	private static final String finalThroughputStub = "finalThroughput/";
	private static final String roundLatencyStub = "roundLatency/";
	private static final String crStub = "currentRatio/";
	
	// Variables - Key Components
	private static HashMap<String, ClientDiary> bookshelf = new HashMap<String, ClientDiary>();
	private static ArrayList<PSTBAnalysisObject> analyzedInformation = new ArrayList<PSTBAnalysisObject>();
	private static ArrayList<AnalysisType> analyzedCheckScenario = new ArrayList<AnalysisType>();
	private static ArrayList<DiaryHeader> analyzedCheckThroughput = new ArrayList<DiaryHeader>();
	
	// Variables - Folder Strings
	private static String diariesFolderString;
	private static String scenarioFolderString;
	private static String delayFolderString;
	private static String frequencyFolderString;
	private static String avgDelayFolderString;
	private static String histogramFolderString;
	private static String throughputFolderString;
	private static String currentThroughputFolderString;
	private static String averageThroughputFolderString;
	private static String secantFolderString;
	private static String finalThroughputFolderString;
	private static String roundLatencyFolderString;
	private static String crFolderString;
	
	// Logger
	private static final Logger logger = LogManager.getRootLogger();
	private static final String logHeader = "Analysis: ";
	
	public static void main(String[] args)
	{
		// Input parsing
		int numArgs = args.length;
		boolean conductAnalysis = true;
		Boolean printDiaries = null;
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
			logger.info(logHeader + "The Print flag was not set properly!");
			System.exit(PSTBError.A_ARGS);
		}
		
		String argsFile = args[LOC_FILE];
		if(argsFile.equals("null"))
		{
			conductAnalysis = false;
		}
		else
		{
			analysisFileString = argsFile;
		}
		
		logger.info(logHeader + "Beginning analysis...");
		
		Long currTime = System.currentTimeMillis();
		String currFolderString = PSTBUtil.DATE_FORMAT.format(currTime) + "/";
		
		diariesFolderString = analysisFolderString + currFolderString + diariesStub;
		
		scenarioFolderString = analysisFolderString + currFolderString + scenarioStub;
		delayFolderString = scenarioFolderString + delayCounterStub;
		frequencyFolderString = scenarioFolderString + frequencyCounterStub;
		avgDelayFolderString = scenarioFolderString + avgDelayStub;
		histogramFolderString = scenarioFolderString + histogramStub;
		
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
				System.exit(PSTBError.A_DIARY);
			}
			logger.info("All diaries now in files!");
		}
		
		if(conductAnalysis)
		{
			logger.info("Parsing analysis file...");
			AnalysisFileParser requestedAnalysis = new AnalysisFileParser();
			requestedAnalysis.setAnalysisFileString(analysisFileString);
			boolean analysisParseCheck = requestedAnalysis.parse();
			if(!analysisParseCheck)
			{
				logger.error("Error parsing the analysis file!");
				System.exit(PSTBError.A_ANALYSIS_FILE_PARSE);
			}
			logger.info("Got requested analysises.");
			
			AnalysisFileExtension ext = requestedAnalysis.getExtension();
			if(ext == null)
			{
				logger.error(logHeader + "The Analysis file's extension hasn't been parsed!");
				System.exit(PSTBError.A_ANALYSIS_FILE_PARSE);
			}
			boolean isScenario = ext.equals(AnalysisFileExtension.sin);
			
			logger.info("Beginning to execute these analysises...");
			boolean analysisCheck = executeAnalysis(requestedAnalysis, isScenario);
			if(!analysisCheck)
			{
				logger.error("Analysis Failed!");
				System.exit(PSTBError.A_ANALYSIS);
			}
			logger.info("Analysis complete.");
			
			logger.info("Storing this analysis into a file...");
			boolean recordAnalysisCheck = true;
			if(isScenario)
			{
				recordAnalysisCheck = recordScenarioObjects();
			}
			else
			{
				recordAnalysisCheck = recordThroughputObjects();
			}
			if(!recordAnalysisCheck)
			{
				logger.error(logHeader + "Couldn't record the analysis to a file!"); 
				System.exit(PSTBError.A_RECORD_ANALYSIS);
			}
			logger.info("All analysis in files.");
			
			if(true)
			{
				logger.info("Starting graphs...");
				int numAO = analyzedInformation.size();
				for(int i = 0; i < numAO ; i++)
				{
					String[] command = new String[11];
					command[0] = "python";
					command[1] = "graph.py";
					boolean createGraph = false;
					
					if(isScenario)
					{
						AnalysisType atI = analyzedCheckScenario.get(i);
						if(atI.equals(AnalysisType.DelayCounter))
						{
							PSTBDataCounter temp = (PSTBDataCounter) analyzedInformation.get(i);
							Map<Long, Integer> t = temp.getFrequency();
							PSActionType tempsType = temp.getType();
							
							if(t.size() > 1)
							{
								createGraph = true;
								ArrayList<Long> x = new ArrayList<Long>();
								ArrayList<Integer> y = new ArrayList<Integer>();
								
								t.forEach((tX, tY)->{
									x.add(tX);
									y.add(tY);
								});
								
								command[2] = "delayCounter";
								command[3] = delayFolderString;
								command[4] = temp.getName();
								if(tempsType.equals(PSActionType.R))
								{
									command[5] = "Delay (ms)";
								}
								else
								{
									command[5] = "Delay (ns)";
								}
								command[6] = "Frequency";
								command[7] = Arrays.toString(x.toArray());
								command[8] = "int";
								command[9] = Arrays.toString(y.toArray());
								command[10] = "int";
							}
							
						}
						else if(atI.equals(AnalysisType.Histogram))
						{
							PSTBHistogram temp = (PSTBHistogram) analyzedInformation.get(i);
							
							int[] y = temp.getHistogram();
							PSActionType tempsType = temp.getType();
							
							if(y != null)
							{
								createGraph = true;
								int yLength = y.length;
								
								String[] x = new String[yLength];
								
								long floorValue = temp.getFloorValue();
								Double range = temp.getRange();
								
								for(int j = 0 ; j < yLength ; j++)
								{
									Double binFloor = floorValue + range*j;
									Double binCeiling = floorValue + range*(j+1);
									
									String convertedFloor = null;
									String convertedCeiling = null;
									
									if(tempsType.equals(PSActionType.R))
									{
										convertedFloor = PSTBUtil.createTimeString(binFloor.longValue(), TimeType.Milli, 
												TimeUnit.MILLISECONDS);
										convertedCeiling = PSTBUtil.createTimeString(binCeiling.longValue(), TimeType.Milli, 
												TimeUnit.MILLISECONDS);
									}
									else
									{
										convertedFloor = PSTBUtil.createTimeString(binFloor.longValue(), TimeType.Nano, 
												TimeUnit.MILLISECONDS);
										convertedCeiling = PSTBUtil.createTimeString(binCeiling.longValue(), TimeType.Nano, 
												TimeUnit.MILLISECONDS);
									}
									
									x[j] = convertedFloor + " - " + convertedCeiling;
								}
								
								command[2] = "histogram";
								command[3] = histogramFolderString;
								command[4] = temp.getName();
								command[5] = "";
								command[6] = "Frequency";
								command[7] = Arrays.toString(x);
								command[8] = "string";
								command[9] = Arrays.toString(y);
								command[10] = "int";
							}
						}
					}
					else
					{
						DiaryHeader dhI = analyzedCheckThroughput.get(i);
						
						if(!dhI.equals(DiaryHeader.FinalThroughput))
						{
							PSTBTwoPoints aoI = (PSTBTwoPoints) analyzedInformation.get(i);
							
							ArrayList<Point2D.Double> data = aoI.getDataset();
							int numPoints = data.size();
							if(numPoints > 1)
							{
								createGraph = true;
								
								String[] x = new String[numPoints];
								String[] y = new String[numPoints];
								
								for(int j = 0 ; j < numPoints ; j++)
								{
									Point2D.Double coOrdinateJ = data.get(j);
									Double xJ = coOrdinateJ.getX();
									Double yJ = coOrdinateJ.getY();
									
									x[j] = xJ.toString();
									y[j] = yJ.toString();
								}
								
								command[2] = "throughput";
								if(dhI.equals(DiaryHeader.CurrentThroughput))
								{
									command[3] = currentThroughputFolderString;
									command[6] = "Current Throughput (messages/sec)";
								}
								else if(dhI.equals(DiaryHeader.AverageThroughput))
								{
									command[3] = averageThroughputFolderString;
									command[6] = "Average Throughput (messages/sec)";
								}
								else if(dhI.equals(DiaryHeader.Secant))
								{
									command[3] = secantFolderString;
									command[6] = "Secant (unitless)";
								}
								else if(dhI.equals(DiaryHeader.CurrentRatio))
								{
									command[3] = crFolderString;
									command[6] = "Ratio (unitless)";
								}
								else
								{
									command[3] = roundLatencyFolderString;
									command[6] = "Latency (sec)";
								}
								command[4] = aoI.getName();
								command[5] = "Input Rate (messages/sec)";
								command[7] = Arrays.toString(x);
								command[8] = "float";
								command[9] = Arrays.toString(y);
								command[10] = "float";
							}
							
						}
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
							System.exit(PSTBError.A_REPORT);
						}
					}
				}
				logger.info("Graphs complete.");
			}
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
			bookshelf.forEach((diaryName, diary)->
			{
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
	
	/**
	 * Executes the analysis requested by the user
	 * @see AnalysisFileParser
	 * 
	 * @param requestedAnalysis - the converted analysis file 
	 * @return false on an error; true otherwise
	 */
	private static boolean executeAnalysis(AnalysisFileParser givenParser, boolean isScenario)
	{
		ArrayList<HashMap<AnalysisInput, ArrayList<Object>>> requestedAnalysis = givenParser.getRequestedAnalysis();
		if(requestedAnalysis.isEmpty())
		{
			logger.error(logHeader + "The Analysis file hasn't been parsed!");
			return false;
		}
		
		// Loop through each line of the AnalysisFile
		for(int i = 0 ; i < requestedAnalysis.size(); i++)
		{
			HashMap<AnalysisInput, ArrayList<Object>> analysisI = requestedAnalysis.get(i);
			
			// Prepare general Diary Stuff
			ArrayList<Object> requestedBN = analysisI.get(AnalysisInput.BenchmarkNumber);
			ArrayList<Object> requestedTFS = analysisI.get(AnalysisInput.TopologyFilePath);
			ArrayList<Object> requestedDFV = analysisI.get(AnalysisInput.DistributedFlag);
			ArrayList<Object> requestedP = analysisI.get(AnalysisInput.Protocol);
			String BN = null;
			String TFS = null;
			String DFV = null;
			String P = null;
			if(requestedBN != null)
			{
				BN = Arrays.toString(requestedBN.toArray()).replace("[", "").replace("]", "");
			}
			if(requestedTFS != null)
			{
				TFS = Arrays.toString(requestedTFS.toArray()).replace("[", "").replace("]", "");
			}
			if(requestedDFV != null)
			{
				DFV = Arrays.toString(requestedDFV.toArray()).replace("[", "").replace("]", "");
			}
			if(requestedP != null)
			{
				P = Arrays.toString(requestedP.toArray()).replace("[", "").replace("]", "");
			}
			
			boolean exCheck = true;
			// Handle situation dependant diary stuff
			if(isScenario)
			{
				exCheck = executeScenarioAnalysis(analysisI, requestedBN, requestedTFS, requestedDFV, requestedP, BN, TFS, DFV, P);
			}
			else
			{
				exCheck = exceuteThroughputAnalysis(analysisI, requestedBN, requestedTFS, requestedDFV, requestedP);
			}
			
			if(!exCheck)
			{
				logger.error(logHeader + "Smaller analysis failed!");
				return false;
			}
		}
		
		return true;
	}
	
	private static boolean executeScenarioAnalysis(HashMap<AnalysisInput, ArrayList<Object>> analysisI,
			ArrayList<Object> requestedBN, ArrayList<Object> requestedTFS, 
			ArrayList<Object> requestedDFV, ArrayList<Object> requestedP,
			String BN, String TFS, String DFV, String P)
	{
		ArrayList<Object> requestedRL = analysisI.get(AnalysisInput.RunLength);
		ArrayList<Object> requestedRN = analysisI.get(AnalysisInput.RunNumber);
		ArrayList<Object> requestedCN = analysisI.get(AnalysisInput.ClientName);
		
		ArrayList<String> requestedDiaryNames = getAffiliatedScenarioDiaries(requestedBN, requestedTFS, requestedDFV, requestedP, 
				requestedRL, requestedRN, requestedCN);
		
		String RL = null;
		String RN = null;
		String CN = null;
		
		if(requestedRL != null)
		{
			RL = Arrays.toString(requestedRL.toArray()).replace("[", "").replace("]", "");
		}
		if(requestedRN != null)
		{
			RN = Arrays.toString(requestedRN.toArray()).replace("[", "").replace("]", "");
		}
		if(requestedCN != null)
		{
			CN = Arrays.toString(requestedCN.toArray()).replace("[", "").replace("]", "");
		}
		
		String analysisObjectName = BN + "_" + TFS + "_" + DFV + "_" + P + "_" + RL + "_" + RN + "_" + CN;
		
		ArrayList<Object> requestedATList = analysisI.get(AnalysisInput.AnalysisType);
		if(requestedATList == null)
		{
			requestedATList = new ArrayList<Object>();
			requestedATList.add(AnalysisType.AverageDelay);
			requestedATList.add(AnalysisType.DelayCounter);
			requestedATList.add(AnalysisType.FrequencyCounter);
			requestedATList.add(AnalysisType.Histogram);
		}
		int numATs = requestedATList.size();
		
		ArrayList<Object> requestedPSATList = analysisI.get(AnalysisInput.PSActionType);
		if(requestedPSATList == null)
		{
			requestedPSATList = new ArrayList<Object>();
			requestedPSATList.add(PSActionType.A);
			requestedPSATList.add(PSActionType.V);
			requestedPSATList.add(PSActionType.S);
			requestedPSATList.add(PSActionType.U);
			requestedPSATList.add(PSActionType.P);
			requestedPSATList.add(PSActionType.R);
		}
		int numPSATs = requestedPSATList.size();
		
		PSTBScenarioAO analysisObjectI = null;
		for(int j = 0 ; j < numATs ; j++)
		{
			AnalysisType atJ = (AnalysisType) requestedATList.get(j);
			if(atJ.equals(AnalysisType.DelayCounter))
			{
				analysisObjectI = new PSTBDataCounter(true);
			}
			else if(atJ.equals(AnalysisType.FrequencyCounter))
			{
				analysisObjectI = new PSTBDataCounter(false);
			}
			else if(atJ.equals(AnalysisType.AverageDelay))
			{
				analysisObjectI = new PSTBAvgDelay();
			}
			else if(atJ.equals(AnalysisType.Histogram))
			{
				analysisObjectI = new PSTBHistogram();
			}
			else
			{
				logger.error(logHeader + "Invalid AnalysisType requested - execution!");
				analyzedInformation.clear();
				return false;
			}
			
			for(int k = 0 ; k < numPSATs ; k++)
			{
				PSActionType psatK = (PSActionType) requestedPSATList.get(k);
				analysisObjectI.setName(psatK + "_" + analysisObjectName);
				analysisObjectI.setType(psatK);
				
				for(int l = 0 ; l < requestedDiaryNames.size() ; l++)
				{
					String diaryNameL = requestedDiaryNames.get(l);
					if(!bookshelf.containsKey(diaryNameL))
					{
						logger.error(logHeader + diaryNameL + " isn't in the bookshelf!");
						return false;
					}
					
					ClientDiary diaryL = bookshelf.get(diaryNameL);
					for(int m = 0 ; m < diaryL.size() ; m++)
					{
						DiaryEntry pageM = diaryL.getDiaryEntryI(m);
						PSActionType pageMsActionType = pageM.getPSActionType();
						
						if(pageMsActionType == null)
						{
							logger.error(logHeader + "Diary Page is missing an associated Action Type!");
							return false;
						}
						else if(pageMsActionType.equals(psatK))
						{
							Long associatedDelay = null;
							if(psatK.equals(PSActionType.R))
							{
								associatedDelay = pageM.getMessageDelay();
							}
							else
							{
								associatedDelay = pageM.getActionDelay();
							}
							
							analysisObjectI.handleDataPoint(associatedDelay);
						}
					}
				}
				
				// Let's add this analyzed object to the list, along with recording what analysis we accomplished
				analyzedInformation.add(analysisObjectI);
				analyzedCheckScenario.add(atJ);
			}
		}
		
		return true;
	}
	
	private static boolean exceuteThroughputAnalysis(HashMap<AnalysisInput, ArrayList<Object>> analysisI,
			ArrayList<Object> requestedBN, ArrayList<Object> requestedTFS, 
			ArrayList<Object> requestedDFV, ArrayList<Object> requestedP)
	{
		ArrayList<Object> requestedPL = analysisI.get(AnalysisInput.PeriodLength);
		ArrayList<Object> requestedMS = analysisI.get(AnalysisInput.MessageSize);
		ArrayList<Object> requestedNA = analysisI.get(AnalysisInput.NumAttribute);
		ArrayList<Object> requestedAR = analysisI.get(AnalysisInput.AttributeRatio);
		
		ArrayList<String> requestedDiaryNames = getAffiliatedThroughputDiaries(requestedBN, requestedTFS, requestedDFV, 
				requestedP, requestedPL, requestedMS, requestedNA, requestedAR);
		
		ArrayList<Object> requestedDHList = analysisI.get(AnalysisInput.DiaryHeader);
		if(requestedDHList == null)
		{
			requestedDHList = new ArrayList<Object>();
			requestedDHList.add(DiaryHeader.CurrentThroughput);
			requestedDHList.add(DiaryHeader.AverageThroughput);
			requestedDHList.add(DiaryHeader.FinalThroughput);
			requestedDHList.add(DiaryHeader.Secant);
			requestedDHList.add(DiaryHeader.RoundLatency);
			requestedDHList.add(DiaryHeader.CurrentRatio);
		}
		int numDH = requestedDHList.size();
		
		PSTBThroughputAO analysisObjectI = null;
		int numDiaries = requestedDiaryNames.size();
		for(int j = 0 ; j < numDiaries ; j++)
		{
			String diaryNameJ = requestedDiaryNames.get(j);
			ClientDiary diaryJ = bookshelf.get(diaryNameJ);
			
			logger.debug(logHeader + "Working on object " + diaryNameJ + "...");
			
			for(int k = 0 ; k < numDH ; k++)
			{
				DiaryHeader dhK = (DiaryHeader) requestedDHList.get(k);
				String name = dhK.toString() + "_" + diaryNameJ;
				
				boolean dhJIsFinal = dhK.equals(DiaryHeader.FinalThroughput);
				if(dhJIsFinal)
				{
					analysisObjectI = new PSTBFinalThroughput();
				}
				else
				{
					analysisObjectI = new PSTBTwoPoints(dhK);
				}
				analysisObjectI.setName(name);
				
				for(int l = 0 ; l < diaryJ.size() ; l++)
				{
					DiaryEntry pageL = diaryJ.getDiaryEntryI(l);
					
					Double dhMessageRate = pageL.getMessageRate();
					
					Double dhValueL = null;
					if(dhJIsFinal)
					{
						dhValueL = pageL.getFinalThroughput();
						analysisObjectI.handleDataPoint(dhValueL);
					}
					else
					{
						if(dhK.equals(DiaryHeader.CurrentThroughput))
						{
							dhValueL = pageL.getCurrentThroughput();
						}
						else if(dhK.equals(DiaryHeader.AverageThroughput))
						{
							dhValueL = pageL.getAverageThroughput();
						}
						else if(dhK.equals(DiaryHeader.Secant))
						{
							dhValueL = pageL.getSecant();
						}
						else if(dhK.equals(DiaryHeader.FinalThroughput))
						{
							dhValueL = pageL.getFinalThroughput();
						}
						else if(dhK.equals(DiaryHeader.RoundLatency))
						{
							dhValueL = pageL.getRoundLatency();
						}
						else if(dhK.equals(DiaryHeader.CurrentRatio))
						{
							dhValueL = pageL.getCurrentRatio();
						}
						if(dhValueL != null)
						{
							analysisObjectI.handleDataPoints(dhMessageRate, dhValueL);
						}
					}
				}
				
				analyzedInformation.add(analysisObjectI);
				analyzedCheckThroughput.add(dhK);
			}
		}
		
		return true;
	}
	
	/**
	 * Given a set of parameters, gets a bunch of ClientDiary Paths that match
	 * If null is inputed, it will be assumed that the user wants all of that type of parameter
	 * For example, if a null if given for distributedFlag, all diaries that match the other 5 parameters
	 * will be added.
	 * 
	 * @param requestedTPF - The topology file associated with these Diaries
	 * @param requestedDFV - the DistributedFlagValue associated with these Diaries
	 * @param requestedP - the NetworkProtocol associated with these Diaries
	 * @param requestedRL - the runLength associated with these Diaries
	 * @param requestedRN - the runNumber associated with these Diaries
	 * @param requestedCN - the ClientName associated with these Diaries
	 * @return the list of matching diary names
	 */
	private static ArrayList<String> getAffiliatedScenarioDiaries(ArrayList<Object> requestedBN,
			ArrayList<Object> requestedTPF, ArrayList<Object> requestedDFV,
			ArrayList<Object> requestedP, ArrayList<Object> requestedRL, 
			ArrayList<Object> requestedRN, ArrayList<Object> requestedCN)
	{	
		// NOTE:	null here means that we want all references to that variable
		// 		Example - if requestedTPF is null, then we want all to look at all the topology files that these diary files have 
		
		ArrayList<String> retVal = new ArrayList<String>();
		String[] nameTestArray = new String[NUM_SCENARIO_STRINGS];
		
		// Which lists are null?
		boolean nullBN = (requestedBN == null);
		boolean nullTPF = (requestedTPF == null);
		boolean nullDFV = (requestedDFV == null);
		boolean nullP = (requestedP == null);
		boolean nullRL = (requestedRL == null);
		boolean nullRN = (requestedRN == null);
		boolean nullCN = (requestedCN == null);
		
		int numBN = 1;
		int numTPF = 1;
		int numDFV = 1;
		int numP = 1;
		int numRL = 1;
		int numRN = 1;
		int numCN = 1;
		
		// Set nums
		if(!nullBN)
		{
			numBN = requestedBN.size();
		}
		if(!nullTPF)
		{
			numTPF = requestedTPF.size();
		}
		if(!nullDFV)
		{
			numDFV = requestedDFV.size();
		}
		if(!nullP)
		{
			numP = requestedP.size();
		}
		if(!nullRL)
		{
			numRL = requestedRL.size();
		}
		if(!nullRN)
		{
			numRN = requestedRN.size();
		}
		if(!nullCN)
		{
			numCN = requestedCN.size();
		}
		
		// Loop through "all lists" to add its String to the Regex
		for(int iBN = 0 ; iBN < numBN ; iBN++)
		{
			if(nullBN)
			{
				nameTestArray[LOC_BENCHMARK_NUMBER] = PSTBUtil.BENCHMARK_NUMBER_REGEX;
			}
			else
			{
				nameTestArray[LOC_BENCHMARK_NUMBER] = (String) requestedBN.get(iBN);
			}
			
			for(int iTPF = 0 ; iTPF < numTPF ; iTPF++)
			{
				if(nullTPF)
				{
					nameTestArray[LOC_TOPO_FILE_PATH] = "\\w+";
				}
				else
				{
					nameTestArray[LOC_TOPO_FILE_PATH] = (String) requestedTPF.get(iTPF);
				}
				
				for(int iDFV = 0 ; iDFV < numDFV ; iDFV++)
				{
					if(nullDFV)
					{
						nameTestArray[LOC_DISTRIBUTED_FLAG] = "\\w+";
					}
					else
					{
						nameTestArray[LOC_DISTRIBUTED_FLAG] = ((DistributedFlagValue) requestedDFV.get(iDFV)).toString();
					}
					
					for(int iP = 0 ; iP < numP ; iP++)
					{
						if(nullP)
						{
							nameTestArray[LOC_PROTOCOL] = "\\w+";
						}
						else
						{
							nameTestArray[LOC_PROTOCOL] = ((NetworkProtocol) requestedP.get(iP)).toString();
						}
						
						for(int iRL = 0 ; iRL < numRL ; iRL++)
						{
							if(nullRL)
							{
								nameTestArray[LOC_RUN_LENGTH] = "\\w+";
							}
							else
							{
								nameTestArray[LOC_RUN_LENGTH] = ((Long) requestedRL.get(iRL)).toString();
							}
							
							for(int iRN = 0 ; iRN < numRN ; iRN++)
							{
								if(nullRN)
								{
									nameTestArray[LOC_RUN_NUMBER] = "\\w+";
								}
								else
								{
									nameTestArray[LOC_RUN_NUMBER] = ((Long) requestedRN.get(iRN)).toString();
								}
								
								for(int iCN = 0 ; iCN < numCN ; iCN++)
								{
									if(nullCN)
									{
										nameTestArray[LOC_CLIENT_NAME] = "\\w+";
									}
									else
									{
										nameTestArray[LOC_CLIENT_NAME] = (String) requestedCN.get(iCN);
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
								}
							}
						}
					}
				}
			}
		}
		
		return retVal;
	}
	
	private static ArrayList<String> getAffiliatedThroughputDiaries(ArrayList<Object> requestedBN,
			ArrayList<Object> requestedTPF, ArrayList<Object> requestedDFV,
			ArrayList<Object> requestedP, ArrayList<Object> requestedPL, 
			ArrayList<Object> requestedMS, ArrayList<Object> requestedNA, ArrayList<Object> requestedAR)
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
		
		int numBN = 1;
		int numTPF = 1;
		int numDFV = 1;
		int numP = 1;
		int numPL = 1;
		int numMS = 1;
		int numNA = 1;
		int numAR = 1;
		
		// Set nums
		if(!nullBN)
		{
			numBN = requestedBN.size();
		}
		if(!nullTPF)
		{
			numTPF = requestedTPF.size();
		}
		if(!nullDFV)
		{
			numDFV = requestedDFV.size();
		}
		if(!nullP)
		{
			numP = requestedP.size();
		}
		if(!nullPL)
		{
			numPL = requestedPL.size();
		}
		if(!nullMS)
		{
			numMS = requestedMS.size();
		}
		if(!nullNA)
		{
			numNA = requestedNA.size();
		}
		if(!nullAR)
		{
			numAR = requestedAR.size();
		}
		
		nameTestArray[LOC_NODE_NAME] = PSTBUtil.MASTER;
		
		// Loop through all lists to add its String to the Regex
		for(int iBN = 0 ; iBN < numBN ; iBN++)
		{
			if(nullBN)
			{
				nameTestArray[LOC_BENCHMARK_NUMBER] = PSTBUtil.BENCHMARK_NUMBER_REGEX;
			}
			else
			{
				nameTestArray[LOC_BENCHMARK_NUMBER] = (String) requestedBN.get(iBN);
			}
			
			for(int iTPF = 0 ; iTPF < numTPF ; iTPF++)
			{
				if(nullTPF)
				{
					nameTestArray[LOC_TOPO_FILE_PATH] = "\\w+";
				}
				else
				{
					nameTestArray[LOC_TOPO_FILE_PATH] = (String) requestedTPF.get(iTPF);
				}
				
				for(int iDFV = 0 ; iDFV < numDFV ; iDFV++)
				{
					if(nullDFV)
					{
						nameTestArray[LOC_DISTRIBUTED_FLAG] = "\\w+";
					}
					else
					{
						nameTestArray[LOC_DISTRIBUTED_FLAG] = ((DistributedFlagValue) requestedDFV.get(iDFV)).toString();
					}
					
					for(int iP = 0 ; iP < numP ; iP++)
					{
						if(nullP)
						{
							nameTestArray[LOC_PROTOCOL] = "\\w+";
						}
						else
						{
							nameTestArray[LOC_PROTOCOL] = ((NetworkProtocol) requestedP.get(iP)).toString();
						}
						
						for(int iPL = 0 ; iPL < numPL ; iPL++)
						{
							if(nullPL)
							{
								nameTestArray[LOC_PERIOD_LENGTH] = "\\w+";
							}
							else
							{
								nameTestArray[LOC_PERIOD_LENGTH] = ((Long) requestedPL.get(iPL)).toString();
							}
							
							for(int iMS = 0 ; iMS < numMS ; iMS++)
							{
								if(nullMS)
								{
									nameTestArray[LOC_MESSAGE_SIZE] = "\\w+";
								}
								else
								{
									nameTestArray[LOC_MESSAGE_SIZE] = ((MessageSize) requestedMS.get(iMS)).toString();
								}
								
								for(int iNA = 0 ; iNA < numNA ; iNA++)
								{
									if(nullNA)
									{
										nameTestArray[LOC_NUM_ATTRIBUTE] = "\\w+";
									}
									else
									{
										nameTestArray[LOC_NUM_ATTRIBUTE] = ((NumAttribute) requestedNA.get(iNA)).toString();
									}
									
									for(int iAR = 0 ; iAR < numAR ; iAR++)
									{
										if(nullAR)
										{
											nameTestArray[LOC_ATTRIBUTE_RATIO] = "\\w+";
										}
										else
										{
											nameTestArray[LOC_ATTRIBUTE_RATIO] = ((AttributeRatio) requestedAR.get(iAR)).toString();
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
									}
								}
							}
						}
					}
				}
			}
		}
		
		return retVal;
	}
	
	/**
	 * Prints the information contained within the analyzedInformation object to a file
	 * 
	 * @return false on an error; true otherwise
	 */
	private static boolean recordScenarioObjects()
	{		
		Path delayFolderPath = Paths.get(delayFolderString);
		Path frequencyFolderPath = Paths.get(frequencyFolderString);
		Path avgDelayFolderPath = Paths.get(avgDelayFolderString);
		Path histogramFolderPath = Paths.get(histogramFolderString);
		
		// If the folders don't exist - create them
		boolean delayCheck = PSTBUtil.createFolder(delayFolderPath, logger, logHeader);
		if(!delayCheck)
		{
			logger.error(logHeader + "Couldn't create delay counter folder!");
			return false;
		}
		
		boolean frequencyCheck = PSTBUtil.createFolder(frequencyFolderPath, logger, logHeader);
		if(!frequencyCheck)
		{
			logger.error(logHeader + "Couldn't create frequency counter folder!");
			return false;
		}
		
		boolean avgDelayCheck = PSTBUtil.createFolder(avgDelayFolderPath, logger, logHeader);
		if(!avgDelayCheck)
		{
			logger.error(logHeader + "Couldn't create average delay folder!");
			return false;
		}
		
		boolean histogramCheck = PSTBUtil.createFolder(histogramFolderPath, logger, logHeader);
		if(!histogramCheck)
		{
			logger.error(logHeader + "Couldn't create histogram folder!");
			return false;
		}
		
		// For each analysis we did, call its affiliated "record" function
		// NOTE:	this function assumes that analyzedCheck[i] is the analysis that resulted in object analyzedInformation[i]
		// 		i.e. if analyzedCheck[7] is DelayCounter, then analyzedInformation[7] is a PSTBDataCounter Object
		for(int i = 0 ; i < analyzedCheckScenario.size() ; i++)
		{
			PSTBAnalysisObject temp = analyzedInformation.get(i);
			
			String objectFileString = null;
			switch(analyzedCheckScenario.get(i))
			{
				case DelayCounter:
				{
					objectFileString = delayFolderString + temp.getName() + ".txt";
					break;
				}
				case FrequencyCounter:
				{	
					objectFileString = frequencyFolderString + temp.getName() + ".txt";
					break;
				}
				case AverageDelay:
				{
					objectFileString = avgDelayFolderString + temp.getName() + ".txt";
					break;
				}
				case Histogram:
				{
					objectFileString = histogramFolderString + temp.getName() + ".txt";
					break;
				}
				default:
				{
					logger.error(logHeader + "Invalid AnalysisType requested - recording data!");
					return false;
				}
			}
			
			Path objectFilePath = Paths.get(objectFileString);
			boolean check = temp.recordAO(objectFilePath);
			if(!check)
			{
				logger.error(logHeader + "Couldn't print AnalysisObject " + i + "!");
				return false;
			}
		}
		
		return true;
	}
	
	private static boolean recordThroughputObjects()
	{		
		Path currentThroughputFolderPath = Paths.get(currentThroughputFolderString);
		Path averageThroughputFolderPath = Paths.get(averageThroughputFolderString);
		Path roundDelayFolderPath = Paths.get(roundLatencyFolderString);
		Path finalThroughputFolderPath = Paths.get(finalThroughputFolderString);
		Path secantFolderPath = Paths.get(secantFolderString);
		Path currentRatioPath = Paths.get(crFolderString);
		
		// If the folders don't exist - create them
		boolean currentThroughputCheck = PSTBUtil.createFolder(currentThroughputFolderPath, logger, logHeader);
		if(!currentThroughputCheck)
		{
			logger.error(logHeader + "Couldn't create current throughput folder!");
			return false;
		}
		
		boolean averageThroughputCheck = PSTBUtil.createFolder(averageThroughputFolderPath, logger, logHeader);
		if(!averageThroughputCheck)
		{
			logger.error(logHeader + "Couldn't create average throughput folder!");
			return false;
		}
		
		boolean roundDelayCheck = PSTBUtil.createFolder(roundDelayFolderPath, logger, logHeader);
		if(!roundDelayCheck)
		{
			logger.error(logHeader + "Couldn't create round delay folder!");
			return false;
		}
		
		boolean finalThroughputCheck = PSTBUtil.createFolder(finalThroughputFolderPath, logger, logHeader);
		if(!finalThroughputCheck)
		{
			logger.error(logHeader + "Couldn't create final throughput folder!");
			return false;
		}
		
		boolean secantCheck = PSTBUtil.createFolder(secantFolderPath, logger, logHeader);
		if(!secantCheck)
		{
			logger.error(logHeader + "Couldn't create secant folder!");
			return false;
		}
		
		boolean currentRatioCheck = PSTBUtil.createFolder(currentRatioPath, logger, logHeader);
		if(!currentRatioCheck)
		{
			logger.error(logHeader + "Couldn't cretae current ratio folder!");
		}
		
		// For each analysis we did, call its affiliated "record" function
		// NOTE:	this function assumes that analyzedCheck[i] is the analysis that resulted in object analyzedInformation[i]
		// 		i.e. if analyzedCheck[7] is DelayCounter, then analyzedInformation[7] is a PSTBDataCounter Object
		for(int i = 0 ; i < analyzedCheckThroughput.size() ; i++)
		{
			PSTBAnalysisObject temp = analyzedInformation.get(i);
			
			String objectFileString = null;
			switch(analyzedCheckThroughput.get(i))
			{
				case CurrentThroughput:
				{
					objectFileString = currentThroughputFolderString + temp.getName() + ".txt";
					break;
				}
				case AverageThroughput:
				{	
					objectFileString = averageThroughputFolderString + temp.getName() + ".txt";
					break;
				}
				case RoundLatency:
				{
					objectFileString = roundLatencyFolderString + temp.getName() + ".txt";
					break;
				}
				case FinalThroughput:
				{
					objectFileString = finalThroughputFolderString + temp.getName() + ".txt";
					break;
				}
				case Secant:
				{
					objectFileString = secantFolderString + temp.getName() + ".txt";
					break;
				}
				case CurrentRatio:
				{
					objectFileString = crFolderString + temp.getName() + ".txt";
					break;
				}
				default:
				{
					logger.error(logHeader + "Invalid AnalysisType requested - recording data!");
					return false;
				}
			}
			
			Path objectFilePath = Paths.get(objectFileString);
			boolean check = temp.recordAO(objectFilePath);
			if(!check)
			{
				logger.error(logHeader + "Couldn't print AnalysisObject " + i + "!");
				return false;
			}
		}
		
		return true;
	}
}
