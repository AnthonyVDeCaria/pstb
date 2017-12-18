package pstb.analysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.analysis.diary.ClientDiary;
import pstb.analysis.diary.DiaryEntry;
import pstb.analysis.diary.DistributedFlagValue;
import pstb.analysis.diary.DiaryEntry.DiaryHeader;
import pstb.benchmark.client.padres.PSClientPADRES;
import pstb.startup.config.NetworkProtocol;
import pstb.startup.workload.PSActionType;
import pstb.util.PSTBError;
import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 * 
 * Analyzes the information contained within the ClientDiaries.
 * As such, it has functions to access and sort these Diaries 
 * as well as key analysis functions.
 */
public class Analyzer {
	private static final Logger logger = LogManager.getRootLogger();
	private static final String logHeader = "Analysis: ";
	
	private static final int NUM_STRINGS = 6;
	private static final int LOC_TOPO_FILE_PATH = 0;
	private static final int LOC_DISTRIBUTED_FLAG = 1;
	private static final int LOC_PROTOCOL = 2;
	private static final int LOC_RUN_LENGTH = 3;
	private static final int LOC_RUN_NUMBER = 4;
	private static final int LOC_CLIENT_NAME = 5;
	
	private static HashMap<String, ClientDiary> bookshelf = new HashMap<String, ClientDiary>();
	private static ArrayList<Object> analyzedInformation = new ArrayList<Object>();
	private static ArrayList<AnalysisType> analyzedCheck = new ArrayList<AnalysisType>();
	
	private static final String analysisFolderString = System.getProperty("user.dir") + "/analysis/";
	private static final String diariesStub = "diaries/";
	private static final String analyzedStub = "analyzed/";
	private static final String histogramStub = "histogram/";
	private static final String avgDelayStub = "avgDelay/";
	
	private static String analyzedFolderString;
	private static String diariesFolderString;
	private static String histogramFolderString;
	private static String avgDelayFolderString;
	
	public static void main(String[] args)
	{
		String analysisFileString = new String(); 
		
		for (int i = 0; i < args.length; i++) 
		{
			if (args[i].equals("-f")) 
			{
				analysisFileString = args[i + 1];
			}
		}
		
		if(analysisFileString.isEmpty())
		{
			logger.error(logHeader + "no analysis file was given!");
			System.exit(PSTBError.A_ARGS);
		}
		
		logger.info(logHeader + "Beginning analysis...");
		
		Long currTime = System.currentTimeMillis();
		String currFolderString = PSTBUtil.DATE_FORMAT.format(currTime) + "/";
		
		analyzedFolderString = analysisFolderString + currFolderString + analyzedStub;
		diariesFolderString = analysisFolderString + currFolderString + diariesStub;
		histogramFolderString = analyzedFolderString + histogramStub;
		avgDelayFolderString = analyzedFolderString + avgDelayStub;
		
		logger.info("Collecting diaries...");
		boolean collectCheck = collectDiaries();
		if(!collectCheck)
		{
			logger.error(logHeader + "Couldn't collect all the diary files!"); 
			System.exit(PSTBError.A_COLLECT);
		}
		logger.info("All diaries collected.");
		
		logger.info("Printing all diaries to file...");
		boolean recordDiaryCheck = recordAllDiaries();
		if(!recordDiaryCheck)
		{
			logger.error(logHeader + "Couldn't record the diary files!"); 
			System.exit(PSTBError.A_RECORD_DIARY);
		}
		logger.info("All diaries now in files!");
		
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
		logger.info("All analsys in files.");
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
		File diaryFolder = new File(System.getProperty("user.dir"));
		File[] listFiles = diaryFolder.listFiles((d, s) -> {
			return s.endsWith(".dia");
		});
		
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
	 * Adds a Diary to the Bookshelf
	 * (Basically a fancy HashMap put())
	 * 
	 * @param diaryName - the ClientDiary object's name
	 * @param givenDiary - the ClientDiary object itself
	 */
//	public void addDiaryToBookshelf(String diaryName, ClientDiary givenDiary)
//	{
//		bookshelf.put(diaryName, givenDiary);
//	}
	
	/**
	 * Prints all the diaries on the bookshelf into files
	 * 
	 * @return false on an error; true otherwise
	 */
	private static boolean recordAllDiaries()
	{	
		Path diaryFolderPath = Paths.get(diariesFolderString);
		if(Files.notExists(diaryFolderPath))
		{
			try 
			{
				Files.createDirectories(diaryFolderPath);
			} 
			catch (IOException e) 
			{
				logger.error(logHeader + "error creating diaries folder", e);
				return false;
			}
		}
		
		try
		{
			bookshelf.forEach((diaryName, diary)->
			{
				String diaryFileString = diariesFolderString + diaryName + ".txt";
				Path diaryFilePath = Paths.get(diaryFileString);
				
				try 
				{
					Files.deleteIfExists(diaryFilePath);
				} 
				catch (IOException e)
				{
					throw new IllegalArgumentException("IO couldn't delete file " + diaryFileString);
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
			logger.error(logHeader + "error printing all diaries", e);
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
		for(int i = 0 ; i < requestedAnalysis.size(); i++)
		{
			HashMap<AnalysisInput, ArrayList<Object>> analysisI = requestedAnalysis.get(i);
			
			ArrayList<Object> requestedATList = analysisI.get(AnalysisInput.AnalysisType);
			ArrayList<Object> requestedPSATList =  analysisI.get(AnalysisInput.PSActionType);
			ArrayList<Object> requestedDHList =  analysisI.get(AnalysisInput.DiaryHeader);
			
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
			
			ArrayList<Object> requestedTPF = analysisI.get(AnalysisInput.TopologyFilePath);
			ArrayList<Object> requestedDFV = analysisI.get(AnalysisInput.DistributedFlag);
			ArrayList<Object> requestedP = analysisI.get(AnalysisInput.Protocol);
			ArrayList<Object> requestedRL = analysisI.get(AnalysisInput.RunLength);
			ArrayList<Object> requestedRN = analysisI.get(AnalysisInput.RunNumber);
			ArrayList<Object> requestedCN = analysisI.get(AnalysisInput.ClientName);
			
			ArrayList<String> requestedDiaryNames = getAffiliatedDiaryNames(requestedTPF, requestedDFV, requestedP, 
																				requestedRL, requestedRN, requestedCN);
			
			Object objectI = null;
			switch(requestedAT)
			{
				case DelayHistogram:
				{
					objectI = developDelayHistogram(requestedDiaryNames, requestedPSAT, requestedDH);
					break;
				}
				case AverageDelay:
				{
					objectI = developAverageDelay(requestedDiaryNames, requestedPSAT, requestedDH);
					break;
				}
				default:
					logger.error(logHeader + "Invalid AnalysisType requested");
					analyzedInformation.clear();
					return false;
			}
			
			analyzedInformation.add(objectI);
			analyzedCheck.add(requestedAT);
		}
		
		return true;
	}
	
	/**
	 * Prints the information contained within the analyzedInformation object to a file
	 * 
	 * @return false on an error; true otherwise
	 */
	private static boolean recordAllAnalyzedInformation()
	{
		int numHistograms = 0;
		int numAvgDelays = 0;
		
		Path histogramFolderPath = Paths.get(histogramFolderString);
		Path avgDelayFolderPath = Paths.get(avgDelayFolderString);
		
		if(Files.notExists(histogramFolderPath))
		{
			try 
			{
				Files.createDirectories(histogramFolderPath);
			} 
			catch (IOException e) 
			{
				logger.error(logHeader + "error creating histogram folder", e);
				return false;
			}
		}
		
		if(Files.notExists(avgDelayFolderPath))
		{
			try 
			{
				Files.createDirectories(avgDelayFolderPath);
			} 
			catch (IOException e) 
			{
				logger.error(logHeader + "error creating average delay folder", e);
				return false;
			}
		}
		
		for(int i = 0 ; i < analyzedCheck.size() ; i++)
		{
			switch(analyzedCheck.get(i))
			{
				case DelayHistogram:
				{
					PSTBHistogram temp = (PSTBHistogram) analyzedInformation.get(i);
					
					String histogramFileString = histogramFolderString + temp.getHistogramName() + "-" + numHistograms + ".txt";
					Path histogramFilePath = Paths.get(histogramFileString);
					
					boolean check = temp.recordPSTBHistogram(histogramFilePath, logger);
					if(!check)
					{
						logger.error(logHeader + "Error printing Histogram " + i);
						return false;
					}
					
					numHistograms++;
					break;
				}
				case AverageDelay:
				{
					PSTBAvgDelay temp = (PSTBAvgDelay) analyzedInformation.get(i);
					String avgDelayFileString = avgDelayFolderString + temp.getName() +"-" + numAvgDelays + ".txt";
					Path avgDelayFilePath = Paths.get(avgDelayFileString);
					
					boolean check = temp.recordAvgDelay(avgDelayFilePath, logger);
					if(!check)
					{
						logger.error(logHeader + "Error printing Average Delay " + i);
						return false;
					}
					
					numAvgDelays++;
					break;
				}
				default:
				{
					logger.error(logHeader + "Invalid AnalysisType requested");
					return false;
				}
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
	private static ArrayList<String> getAffiliatedDiaryNames(ArrayList<Object> requestedTPF, ArrayList<Object> requestedDFV,
														ArrayList<Object> requestedP, ArrayList<Object> requestedRL, 
														ArrayList<Object> requestedRN, ArrayList<Object> requestedCN)
	{		
		ArrayList<String> retVal = new ArrayList<String>();
		String[] nameTestArray = new String[NUM_STRINGS];
		
		boolean nullTPF = requestedTPF == null;
		boolean nullDFV = requestedDFV == null;
		boolean nullP = requestedP == null;
		boolean nullRL = requestedRL == null;
		boolean nullRN = requestedRN == null;
		boolean nullCN = requestedCN == null;
		
		int numTPF = 1;
		int numDFV = 1;
		int numP = 1;
		int numRL = 1;
		int numRN = 1;
		int numCN = 1;
		
		// Set nums
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
		
		// Loop through them all
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
								
								String nameTestString = String.join(PSTBUtil.DIARY_SEPARATOR, nameTestArray);
								
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
		
		return retVal;
	}
	
	/**
	 * Develops a delay histogram
	 * NOTE: Certain delays only exist for certain PSActionTypes
	 * The MessageDelay is only for R PSActions
	 * While R PSActions do NOT have an ActionDelay - everyone else does.
	 * 
	 * @param diaryNames - a list of diaries that will be combed over
	 * @param typeToAnalyse - the type of PSActionType requested to look at
	 * @param delayType - the type of delay: Action or Message
	 * @return the associated histogram
	 */
	private static PSTBHistogram developDelayHistogram(ArrayList<String> diaryNames, PSActionType typeToAnalyse, DiaryHeader delayType)
	{
		if( !delayType.equals(DiaryHeader.ActionDelay) && !delayType.equals(DiaryHeader.MessageDelay) )
		{
			logger.error(logHeader + "That DiaryHeader is not a delay value.");
			return null;
		}
		
		PSTBHistogram retVal = new PSTBHistogram();
		retVal.setHistogramName(typeToAnalyse.toString() + "-" + delayType.toString());
		retVal.setHistogramType(typeToAnalyse);
				
		for(int i = 0 ; i < diaryNames.size() ; i++)
		{
			String diaryPathI = diaryNames.get(i);
			
			if(!bookshelf.containsKey(diaryPathI))
			{
				logger.error(logHeader + "No diary exists from " + diaryPathI);
				return null;
			}
			
			ClientDiary diaryI = bookshelf.get(diaryPathI);
			
			for(int j = 0 ; j < diaryI.size() ; j++)
			{
				DiaryEntry pageJ = diaryI.getDiaryEntryI(j);
				if(pageJ.containsKey(DiaryHeader.PSActionType))
				{
					if(pageJ.getPSActionType().equals(typeToAnalyse))
					{
						if(pageJ.containsKey(delayType))
						{
							retVal.addOccurrence(pageJ.getDelay(delayType).doubleValue());
						}
						else
						{
							logger.error(logHeader + "There is a page where there is no " + delayType.toString()
										+ " data for Action Type " + typeToAnalyse.toString());
							return null;
						}
					}
				}
				else
				{
					logger.error(logHeader + "Diary Page is missing an associated Action Type");
					return null;
				}
			}
		}
		
		return retVal;
	}
	
	/**
	 * Determines the average delay for a given delay type
	 * NOTE: Certain delays only exist for certain PSActionTypes
	 * The MessageDelay is only for R PSActions
	 * While R PSActions do NOT have an ActionDelay - everyone else does.
	 * 
	 * @param diaryNames - a list of diaries that will be combed over
	 * @param typeToAnalyse - the type of PSActionType requested to look at
	 * @param delayType - the type of delay: Action or Message
	 * @return the average delay
	 */
	private static PSTBAvgDelay developAverageDelay(ArrayList<String> diaryNames, PSActionType typeToAnalyse, DiaryHeader delayType)
	{
		if( !delayType.equals(DiaryHeader.ActionDelay) && !delayType.equals(DiaryHeader.MessageDelay) )
		{
			logger.error(logHeader + "That DiaryHeader is not a delay value.");
			return null;
		}
		
		if(delayType.equals(DiaryHeader.ActionDelay) && typeToAnalyse.equals(PSActionType.R))
		{
			logger.error(logHeader + "Type mismatch - R's don't have an ActionDelay");
			return null;
		}
		
		if(delayType.equals(DiaryHeader.MessageDelay) && !typeToAnalyse.equals(PSActionType.R))
		{
			logger.error(logHeader + "Type mismatch - only R has a MessageDelay");
			return null;
		}
		
		Long totalDelay = new Long(0L);
		double instances = 0;
		PSTBAvgDelay retVal = new PSTBAvgDelay();
		retVal.setName(typeToAnalyse.toString() + "-" + delayType.toString());
		retVal.setDelayType(typeToAnalyse);
				
		for(int i = 0 ; i < diaryNames.size() ; i++)
		{
			String diaryPathI = diaryNames.get(i);
			
			if(!bookshelf.containsKey(diaryPathI))
			{
				logger.error(logHeader + "No diary exists from " + diaryPathI);
				return null;
			}
			
			ClientDiary diaryI = bookshelf.get(diaryPathI);
			
			for(int j = 0 ; j < diaryI.size() ; j++)
			{
				DiaryEntry pageJ = diaryI.getDiaryEntryI(j);
				if(pageJ.containsKey(DiaryHeader.PSActionType))
				{
					if(pageJ.getPSActionType() == typeToAnalyse)
					{
						if(pageJ.containsKey(delayType))
						{
							totalDelay += pageJ.getDelay(delayType);
							instances++;
						}
						else
						{
							logger.error(logHeader + "There is a page where there is no " + delayType.toString()
										+ " data for Action Type " + typeToAnalyse.toString());
							return null;
						}
					}
				}
				else
				{
					logger.error(logHeader + "Diary Page is missing an associated Action Type");
					return null;
				}
			}
		}
		
		if(instances > 0)
		{
			retVal.setValue((long) (totalDelay / instances));
		}
		
		return retVal;
	}
}
