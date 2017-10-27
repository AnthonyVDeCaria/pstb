package pstb.analysis;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;

import pstb.util.ClientDiary;
import pstb.util.DiaryEntry;
import pstb.util.DiaryEntry.DiaryHeader;
import pstb.util.DistributedFlagValue;
import pstb.util.NetworkProtocol;
import pstb.util.PSActionType;
import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 * 
 * Analyzes the information contained within the ClientDiaries.
 * As such, it has functions to access these Diaries 
 * As well as functions to sort them 
 * on top of some analysis functions.
 */
public class Analyzer {
	private HashMap<String, ClientDiary> bookshelf;
	private ArrayList<Object> analyzedInformation;
	private ArrayList<AnalysisType> analyzedCheck;
	private Logger log = null;
	private String logHeader = "Analysis: ";
	
	private final String analysisFolderString = System.getProperty("user.dir") + "/analysis/";
	private final String diariesFolderString = analysisFolderString + "diaries/";
	private final String analyzedFolderString = analysisFolderString + "analyzed/";
	private final String histogramFolderString = analyzedFolderString + "histogram/";
	private final String avgDelayFolderString = analyzedFolderString + "avgDelay/";
	
	/**
	 * Constructor
	 * 
	 * @param logger - the Logger that should be utilized for logging 
	 */
	public Analyzer(Logger logger)
	{
		log = logger;
		bookshelf = new HashMap<String, ClientDiary>();
		analyzedInformation = new ArrayList<Object>();
		analyzedCheck = new ArrayList<AnalysisType>();
	}
	
	/**
	 * AnalyzedInformation getter
	 * 
	 * @return the analyzed Object List
	 */
	public ArrayList<Object> getAnalyzedInformation()
	{
		return analyzedInformation;
	}
	
	/**
	 * Gets a certain serialized diary object 
	 * and adds it to the "bookshelf" 
	 * - a collection of ClientDiaries
	 * 
	 * @param diaryName - the name associated the diary object
	 * @see PSClientPADRES
	 * @return true if the book has been added; false otherwise
	 */
	public boolean collectDiaryAndAddToBookshelf (String diaryName)
	{
		ClientDiary tiedDiary = readDiaryObject(diaryName);
		
		if(tiedDiary == null)
		{
			return false;
		}
		else
		{
			addDiaryToBookshelf(diaryName, tiedDiary);
			return true;
		}
	}
	
	/**
	 * Deserializes a ClientDiary object
	 * 
	 * @param diaryName - the name of this diary
	 * @return null on failure; the requested diary otherwise
	 */
	public ClientDiary readDiaryObject(String diaryName)
	{
		ClientDiary diaryI = null;
		try
		{
			FileInputStream fileIn = new FileInputStream("/tmp/" + diaryName + ".dia");
			ObjectInputStream oISIn = new ObjectInputStream(fileIn);
			diaryI = (ClientDiary) oISIn.readObject();
			oISIn.close();
			fileIn.close();
		} 
		catch (FileNotFoundException e) 
		{
			log.error(logHeader + "couldn't find serialized diary object ", e);
			return null;
		}
		catch (IOException e)
		{
			log.error(logHeader + "error accessing ObjectInputStream ", e);
			return null;
		}
		catch(ClassNotFoundException e)
		{
			log.error(logHeader + "can't find class ", e);
			return null;
		}
		
		return diaryI;
	}
	
	/**
	 * Adds a Diary to the Bookshelf
	 * (Basically a fancy HashMap put())
	 * 
	 * @param diaryName - the ClientDiary object's name
	 * @param givenDiary - the ClientDiary object itself
	 */
	public void addDiaryToBookshelf(String diaryName, ClientDiary givenDiary)
	{
		bookshelf.put(diaryName, givenDiary);
	}
	
	/**
	 * Prints all the diaries on the bookshelf into files
	 * 
	 * @return false on an error; true otherwise
	 */
	public boolean printAllDiaries()
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
				log.error(logHeader + "error creating diaries folder", e);
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
				
				boolean check = diary.printDiary(diaryFilePath, log);
				if(!check)
				{
					throw new IllegalArgumentException();
				}
			});
		}
		catch(IllegalArgumentException e)
		{
			log.error(logHeader + "error printing all diaries", e);
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
	public boolean executeAnalysis(ArrayList<HashMap<AnalysisInput, Object>> requestedAnalysis)
	{
		for(int i = 0 ; i < requestedAnalysis.size(); i++)
		{
			HashMap<AnalysisInput, Object> analysisI = requestedAnalysis.get(i);
			
			AnalysisType requestedAT = (AnalysisType) analysisI.get(AnalysisInput.AnalysisType);
			PSActionType requestedPSAT = (PSActionType) analysisI.get(AnalysisInput.PSActionType);
			DiaryHeader requestedDH = (DiaryHeader) analysisI.get(AnalysisInput.DiaryHeader);
			String requestedTPF = (String) analysisI.get(AnalysisInput.TopologyFilePath);
			DistributedFlagValue requestedDFV = (DistributedFlagValue) analysisI.get(AnalysisInput.DistributedFlag);
			NetworkProtocol requestedP = (NetworkProtocol) analysisI.get(AnalysisInput.Protocol);
			Long requestedRL = (Long) analysisI.get(AnalysisInput.RunLength);
			Integer requestedRN = (Integer) analysisI.get(AnalysisInput.RunNumber);
			String requestedCN = (String) analysisI.get(AnalysisInput.ClientName);
			
			ArrayList<String> requestedDiaryNames = getAffiliatedDiaryNames(requestedTPF, requestedDFV, requestedP, requestedRL,
																			requestedRN, requestedCN);
			
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
					log.error(logHeader + "Invalid AnalysisType requested");
					analyzedInformation.clear();
					return false;
			}
			
			analyzedInformation.add(objectI);
			analyzedCheck.add(requestedAT);
		}
		
		return true;
	}
	
	/**
	 * Prints the information contained within the analyzedInformation object
	 * 
	 * @return false on an error; true otherwise
	 */
	public boolean printAllAnalyzedInformation()
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
				log.error(logHeader + "error creating histogram folder", e);
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
				log.error(logHeader + "error creating average delay folder", e);
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
					
					String histogramFileString = histogramFolderString + temp.getHistogramName() +"-" + numHistograms + ".txt";
					Path histogramFilePath = Paths.get(histogramFileString);
					
					try
					{
						Files.deleteIfExists(histogramFilePath);
					} 
					catch (IOException e)
					{
						throw new IllegalArgumentException("IO couldn't delete file " + histogramFileString);
					}
					
					boolean check = temp.printHistogram(histogramFilePath);
					if(!check)
					{
						log.error(logHeader + "Error printing Histogram " + i);
						return false;
					}
					
					numHistograms++;
					break;
				}
				case AverageDelay:
				{
					PSTBDelay temp = (PSTBDelay) analyzedInformation.get(i);
					String avgDelayFileString = avgDelayFolderString + temp.getName() +"-" + numAvgDelays + ".txt";
					Path avgDelayFilePath = Paths.get(avgDelayFileString);
					
					try
					{
						Files.deleteIfExists(avgDelayFilePath);
					} 
					catch (IOException e)
					{
						throw new IllegalArgumentException("IO couldn't delete file " + avgDelayFileString);
					}
					
					boolean check = temp.printDelay(avgDelayFilePath);
					if(!check)
					{
						log.error(logHeader + "Error printing Average Delay " + i);
						return false;
					}
					
					numAvgDelays++;
					break;
				}
				default:
				{
					log.error(logHeader + "Invalid AnalysisType requested");
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
	 * @param topologyFileString - The topology file associated with these Diaries
	 * @param distributedFlag - the DistributedFlagValue associated with these Diaries
	 * @param protocol - the NetworkProtocol associated with these Diaries
	 * @param runLength - the runLength associated with these Diaries
	 * @param runNumber - the runNumber associated with these Diaries
	 * @param clientName - the ClientName associated with these Diaries
	 * @return the list of matching diary names
	 */
	public ArrayList<String> getAffiliatedDiaryNames(String topologyFileString, DistributedFlagValue distributedFlag,
														NetworkProtocol protocol, Long runLength, Integer runNumber, 
														String clientName)
	{		
		ArrayList<String> retVal = new ArrayList<String>();
		String nameTestString = new String();
		
		if(topologyFileString != null)
		{
			nameTestString += (PSTBUtil.cleanTPF(topologyFileString) + "-");
		}
		else
		{
			nameTestString += "\\w+-";
		}
		
		if(distributedFlag != null)
		{
			nameTestString += (distributedFlag + "-");
		}
		else
		{
			nameTestString += "\\w+-";
		}
		
		if(protocol != null)
		{
			nameTestString += (protocol + "-");
		}
		else
		{
			nameTestString += "\\w+-";
		}
		
		if(runLength != null)
		{
			nameTestString += (runLength + "-");
		}
		else
		{
			nameTestString += "\\w+-";
		}
		
		if(runNumber != null)
		{
			nameTestString += (runNumber + "-");
		}
		else
		{
			nameTestString += "\\w+-";
		}
		
		if(clientName != null)
		{
			nameTestString += clientName;
		}
		else
		{
			nameTestString += "\\w+";
		}
		
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
	
	/**
	 * Develops a delay histogram
	 * 
	 * @param diaryNames - the given set of diaries to develop the histogram from
	 * @param typeToAnalyse - the type of action to get the delay of
	 * @param delayType - the type of delay
	 * @return the associated histogram
	 */
	public PSTBHistogram developDelayHistogram(ArrayList<String> diaryNames, PSActionType typeToAnalyse, DiaryHeader delayType)
	{
		if( !delayType.equals(DiaryHeader.ActionDelay) && !delayType.equals(DiaryHeader.MessageDelay) )
		{
			log.error(logHeader + "That DiaryHeader is not a delay value.");
			return null;
		}
		
		PSTBHistogram retVal = new PSTBHistogram();
		retVal.setHistogramName(typeToAnalyse.toString() + "-" + delayType.toString());
		
		for(int i = 0 ; i < diaryNames.size() ; i++)
		{
			String diaryPathI = diaryNames.get(i);
			
			if(!bookshelf.containsKey(diaryPathI))
			{
				log.error(logHeader + "No diary exists from " + diaryPathI);
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
							log.error(logHeader + "There is a page where there is no " + delayType.toString()
										+ " data for Action Type " + typeToAnalyse.toString());
							return null;
						}
					}
				}
				else
				{
					log.error(logHeader + "Diary Page is missing an associated Action Type");
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
	public PSTBDelay developAverageDelay(ArrayList<String> diaryNames, PSActionType typeToAnalyse, DiaryHeader delayType)
	{
		if( !delayType.equals(DiaryHeader.ActionDelay) && !delayType.equals(DiaryHeader.MessageDelay) )
		{
			log.error(logHeader + "That DiaryHeader is not a delay value.");
			return null;
		}
		
		if(delayType.equals(DiaryHeader.ActionDelay) && typeToAnalyse.equals(PSActionType.R))
		{
			log.error(logHeader + "Type mismatch - R's don't have an ActionDelay");
			return null;
		}
		
		if(delayType.equals(DiaryHeader.MessageDelay) && !typeToAnalyse.equals(PSActionType.R))
		{
			log.error(logHeader + "Type mismatch - only R has a MessageDelay");
			return null;
		}
		
		Long totalDelay = new Long(0L);
		double instances = 0;
		PSTBDelay retVal = new PSTBDelay();
		retVal.setName(typeToAnalyse.toString() + "-" + delayType.toString());
		
		for(int i = 0 ; i < diaryNames.size() ; i++)
		{
			String diaryPathI = diaryNames.get(i);
			
			if(!bookshelf.containsKey(diaryPathI))
			{
				log.error(logHeader + "No diary exists from " + diaryPathI);
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
							log.error(logHeader + "There is a page where there is no " + delayType.toString()
										+ " data for Action Type " + typeToAnalyse.toString());
							return null;
						}
					}
				}
				else
				{
					log.error(logHeader + "Diary Page is missing an associated Action Type");
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
