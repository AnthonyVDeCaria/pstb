package pstb.analysis;

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

import pstb.analysis.analysisobjects.PSTBAnalysisObject;
import pstb.analysis.analysisobjects.PSTBAvgDelay;
import pstb.analysis.analysisobjects.PSTBDataCounter;
import pstb.analysis.analysisobjects.PSTBHistogram;
import pstb.analysis.diary.ClientDiary;
import pstb.analysis.diary.DiaryEntry;
import pstb.analysis.diary.DiaryHeader;
import pstb.analysis.diary.DistributedFlagValue;
import pstb.benchmark.object.client.padres.PSClientPADRES;
import pstb.startup.config.NetworkProtocol;
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
	
	// Constants - Diary Names
	private static final int NUM_STRINGS = 7;
	private static final int LOC_BENCHMARK_START_TIME = 0;
	private static final int LOC_TOPO_FILE_PATH = 1;
	private static final int LOC_DISTRIBUTED_FLAG = 2;
	private static final int LOC_PROTOCOL = 3;
	private static final int LOC_RUN_LENGTH = 4;
	private static final int LOC_RUN_NUMBER = 5;
	private static final int LOC_CLIENT_NAME = 6;
	
	// Constants - Folder Names
	private static final String analysisFolderString = System.getProperty("user.dir") + "/analysis/";
	private static final String diariesStub = "diaries/";
	private static final String analyzedStub = "analyzed/";
	private static final String delayCounterStub = "delayCounter/";
	private static final String frequencyCounterStub = "freqCounter/";
	private static final String avgDelayStub = "avgDelay/";
	private static final String histogramStub = "histogram/";
	
	// Variables - Key Components
	private static HashMap<String, ClientDiary> bookshelf = new HashMap<String, ClientDiary>();
	private static ArrayList<PSTBAnalysisObject> analyzedInformation = new ArrayList<PSTBAnalysisObject>();
	private static ArrayList<AnalysisType> analyzedCheck = new ArrayList<AnalysisType>();
	
	// Variables - Folder Strings
	private static String analyzedFolderString;
	private static String diariesFolderString;
	private static String delayFolderString;
	private static String frequencyFolderString;
	private static String avgDelayFolderString;
	private static String histogramFolderString;
	
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
		
		analyzedFolderString = analysisFolderString + currFolderString + analyzedStub;
		diariesFolderString = analysisFolderString + currFolderString + diariesStub;
		delayFolderString = analyzedFolderString + delayCounterStub;
		frequencyFolderString = analyzedFolderString + frequencyCounterStub;
		avgDelayFolderString = analyzedFolderString + avgDelayStub;
		histogramFolderString = analyzedFolderString + histogramStub;
		
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
		
		if(conductAnalysis)
		{
			logger.info("Parsing analysis file...");
			AnalysisFileParser brainReader = new AnalysisFileParser();
			brainReader.setAnalysisFileString(analysisFileString);
			boolean analysisParseCheck = brainReader.parse();
			if(!analysisParseCheck)
			{
				logger.error("Error parsing the analysis file!");
				System.exit(PSTBError.A_ANALYSIS_FILE_PARSE);
			}
			logger.info("Got requested analysises.");
			
			logger.info("Beginning to execute these analysises...");
			boolean analysisCheck = executeAnalysis(brainReader.getRequestedAnalysis());
			if(!analysisCheck)
			{
				logger.error("Analysis Failed!");
				System.exit(PSTBError.A_ANALYSIS);
			}
			logger.info("Analysis complete.");
			
			logger.info("Storing this analysis into a file...");
			boolean recordAnalysisCheck = recordAllAnalyzedInformation();
			if(!recordAnalysisCheck)
			{
				logger.error(logHeader + "Couldn't record the analysis to a file!"); 
				System.exit(PSTBError.A_RECORD_ANALYSIS);
			}
			logger.info("All analysis in files.");
			
			logger.info("Starting graphs...");
			int numAO = analyzedInformation.size();
			for(int i = 0; i < numAO ; i++)
			{
				AnalysisType atI = analyzedCheck.get(i);
				
				String[] command = new String[11];
				command[0] = "python";
				command[1] = "graph.py";
				
				if(atI.equals(AnalysisType.DelayCounter))
				{
					PSTBDataCounter temp = (PSTBDataCounter) analyzedInformation.get(i);
					Map<Long, Integer> t = temp.getFrequency();
					PSActionType tempsType = temp.getType();
					
					if(t.size() > 1)
					{
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
						command[8] = "long";
						command[9] = Arrays.toString(y.toArray());
						command[10] = "long";
						
						PSTBUtil.createANewProcess(command, logger, false, false,
								"Couldn't create graph process!", 
								"Graph complete.", 
								"Graph process failed!");
					}
					
				}
				else if(atI.equals(AnalysisType.Histogram))
				{
					PSTBHistogram temp = (PSTBHistogram) analyzedInformation.get(i);
					
					int[] y = temp.getHistogram();
					PSActionType tempsType = temp.getType();
					
					if(y != null)
					{
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
								convertedFloor = PSTBUtil.createTimeString(binFloor.longValue(), TimeType.Milli, TimeUnit.MILLISECONDS);
								convertedCeiling = PSTBUtil.createTimeString(binCeiling.longValue(), TimeType.Milli, TimeUnit.MILLISECONDS);
							}
							else
							{
								convertedFloor = PSTBUtil.createTimeString(binFloor.longValue(), TimeType.Nano, TimeUnit.MILLISECONDS);
								convertedCeiling = PSTBUtil.createTimeString(binCeiling.longValue(), TimeType.Nano, TimeUnit.MILLISECONDS);
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
						command[10] = "long";
						
						PSTBUtil.createANewProcess(command, logger, true, false,
								"Couldn't create graph process!", 
								"Graph complete.", 
								"Graph process failed!");
					}
				}
			}
			logger.info("Graphs complete.");
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
	private static boolean executeAnalysis(ArrayList<HashMap<AnalysisInput, ArrayList<Object>>> requestedAnalysis)
	{
		// Loop through each line of the AnalysisFile
		for(int i = 0 ; i < requestedAnalysis.size(); i++)
		{
			HashMap<AnalysisInput, ArrayList<Object>> analysisI = requestedAnalysis.get(i);
			
			ArrayList<Object> requestedATList = analysisI.get(AnalysisInput.AnalysisType);
			ArrayList<Object> requestedPSATList =  analysisI.get(AnalysisInput.PSActionType);
			ArrayList<Object> requestedDHList =  analysisI.get(AnalysisInput.DiaryHeader);
			
			// There should be only one AnalysisType/PSActionType/DiaryHeader selected for each line of the AnalysisFile
			if(requestedATList.size() != 1)
			{
				logger.error(logHeader + "There are multiple requested AnalysisTypes!"); 
				return false;
			}
			if(requestedPSATList.size() != 1)
			{
				logger.error(logHeader + "There are multiple requested PSActionTypes!"); 
				return false;
			}
			if(requestedDHList.size() != 1)
			{
				logger.error(logHeader + "There are multiple requested DiaryHeaders!"); 
				return false;
			}
			
			AnalysisType requestedAT = (AnalysisType) analysisI.get(AnalysisInput.AnalysisType).get(0);
			PSActionType requestedPSAT = (PSActionType) analysisI.get(AnalysisInput.PSActionType).get(0);
			DiaryHeader requestedDH = (DiaryHeader) analysisI.get(AnalysisInput.DiaryHeader).get(0);
			
			if(!requestedDH.equals(DiaryHeader.ActionDelay) && !requestedDH.equals(DiaryHeader.MessageDelay))
			{
				logger.error(logHeader + "That DiaryHeader is not a delay value.");
				return false;
			}
			
			if(requestedDH.equals(DiaryHeader.ActionDelay) && requestedPSAT.equals(PSActionType.R))
			{
				logger.error(logHeader + "Type mismatch - R's don't have an ActionDelay");
				return false;
			}
			
			if(requestedDH.equals(DiaryHeader.MessageDelay) && !requestedPSAT.equals(PSActionType.R))
			{
				logger.error(logHeader + "Type mismatch - only R has a MessageDelay");
				return false;
			}
			
			ArrayList<Object> requestedBST = analysisI.get(AnalysisInput.BenchmarkStartTime);
			ArrayList<Object> requestedTFS = analysisI.get(AnalysisInput.TopologyFilePath);
			ArrayList<Object> requestedDFV = analysisI.get(AnalysisInput.DistributedFlag);
			ArrayList<Object> requestedP = analysisI.get(AnalysisInput.Protocol);
			ArrayList<Object> requestedRL = analysisI.get(AnalysisInput.RunLength);
			ArrayList<Object> requestedRN = analysisI.get(AnalysisInput.RunNumber);
			ArrayList<Object> requestedCN = analysisI.get(AnalysisInput.ClientName);
			
			// From the given lists of options, get the related diary's names 
			ArrayList<String> requestedDiaryNames = getAffiliatedDiaryNames(requestedBST, requestedTFS, requestedDFV, requestedP, 
					requestedRL, requestedRN, requestedCN);
			
			// Now that we have all of the names, let's do the actual analysis
			PSTBAnalysisObject analysisObjectI = null;
			if(requestedAT.equals(AnalysisType.DelayCounter))
			{
				analysisObjectI = new PSTBDataCounter(true);
			}
			else if(requestedAT.equals(AnalysisType.FrequencyCounter))
			{
				analysisObjectI = new PSTBDataCounter(false);
			}
			else if(requestedAT.equals(AnalysisType.AverageDelay))
			{
				analysisObjectI = new PSTBAvgDelay();
			}
			else if(requestedAT.equals(AnalysisType.Histogram))
			{
				analysisObjectI = new PSTBHistogram();
			}
			else
			{
				logger.error(logHeader + "Invalid AnalysisType requested - execution!");
				analyzedInformation.clear();
				return false;
			}
			
			// AO Name
			String BST = null;
			String TFS = null;
			String DFV = null;
			String P = null;
			String RL = null;
			String RN = null;
			String CN = null;
			
			if(requestedBST != null)
			{
				BST = Arrays.toString(requestedBST.toArray()).replace("[", "").replace("]", "");
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
			
			analysisObjectI.setName(requestedPSAT + "_" + BST + "_" + TFS + "_" + DFV + "_" + P + "_" + RL + "_" + RN + "_" + CN);
			analysisObjectI.setType(requestedPSAT);
			
			for(int j = 0 ; j < requestedDiaryNames.size() ; j++)
			{
				String diaryNameJ = requestedDiaryNames.get(j);
				
				if(!bookshelf.containsKey(diaryNameJ))
				{
					logger.error(logHeader + diaryNameJ + " isn't in the bookshelf!");
					return false;
				}
				
				ClientDiary diaryI = bookshelf.get(diaryNameJ);
				
				for(int k = 0 ; k < diaryI.size() ; k++)
				{
					DiaryEntry pageK = diaryI.getDiaryEntryI(k);
					PSActionType pageKsActionType = pageK.getPSActionType();
					
					if(pageKsActionType == null)
					{
						logger.error(logHeader + "Diary Page is missing an associated Action Type!");
						return false;
					}
					else if(pageKsActionType.equals(requestedPSAT))
					{
						if(pageK.containsKey(requestedDH))
						{
							Long associatedDelay = pageK.getDelay(requestedDH);
							analysisObjectI.handleDataPoint(associatedDelay);
						}
						else
						{
							logger.error(logHeader + "There is a page where there is no " + requestedDH
									+ " data for Action Type " + requestedPSAT + "!");
							return false;
						}
					}
				}
			}
			
			// Let's add this analyzed object to the list, along with recording what analysis we accomplished
			analyzedInformation.add(analysisObjectI);
			analyzedCheck.add(requestedAT);
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
	private static ArrayList<String> getAffiliatedDiaryNames(ArrayList<Object> requestedBST,
			ArrayList<Object> requestedTPF, ArrayList<Object> requestedDFV,
			ArrayList<Object> requestedP, ArrayList<Object> requestedRL, 
			ArrayList<Object> requestedRN, ArrayList<Object> requestedCN)
	{	
		// NOTE:	null here means that we want all references to that variable
		// 		Example - if requestedTPF is null, then we want all to look at all the topology files that these diary files have 
		
		ArrayList<String> retVal = new ArrayList<String>();
		String[] nameTestArray = new String[NUM_STRINGS];
		
		// Which lists are null?
		boolean nullBST = (requestedBST == null);
		boolean nullTPF = (requestedTPF == null);
		boolean nullDFV = (requestedDFV == null);
		boolean nullP = (requestedP == null);
		boolean nullRL = (requestedRL == null);
		boolean nullRN = (requestedRN == null);
		boolean nullCN = (requestedCN == null);
		
		int numBST = 1;
		int numTPF = 1;
		int numDFV = 1;
		int numP = 1;
		int numRL = 1;
		int numRN = 1;
		int numCN = 1;
		
		// Set nums
		if(!nullBST)
		{
			numBST = requestedBST.size();
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
		for(int iBST = 0 ; iBST < numBST ; iBST++)
		{
			if(nullBST)
			{
				nameTestArray[LOC_BENCHMARK_START_TIME] = PSTBUtil.DATE_REGEX;
			}
			else
			{
				nameTestArray[LOC_BENCHMARK_START_TIME] = (String) requestedBST.get(iBST);
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
	
	/**
	 * Prints the information contained within the analyzedInformation object to a file
	 * 
	 * @return false on an error; true otherwise
	 */
	private static boolean recordAllAnalyzedInformation()
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
		for(int i = 0 ; i < analyzedCheck.size() ; i++)
		{
			PSTBAnalysisObject temp = analyzedInformation.get(i);
			
			String objectFileString = null;
			switch(analyzedCheck.get(i))
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
}
