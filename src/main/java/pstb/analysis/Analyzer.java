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
 */
public class Analyzer {
	private HashMap<String, ClientDiary> bookshelf;
	private ArrayList<Object> analyzedInformation;
	private ArrayList<AnalysisType> analyzedCheck;
	private Logger log = null;
	private String logHeader = "Analysis: ";
	
	private final String analysisFolderString = System.getProperty("user.dir") + "/analysis/";
	private final String diariesFolderString = analysisFolderString + "diaries/";
	
	public Analyzer(Logger logger)
	{
		log = logger;
		bookshelf = new HashMap<String, ClientDiary>();
		analyzedInformation = new ArrayList<Object>();
		analyzedCheck = new ArrayList<AnalysisType>();
	}
	
	public ArrayList<Object> getAnalyzedInformation()
	{
		return analyzedInformation;
	}
	
	public boolean collectDiaryAndAddToBookshelf (String diaryPath)
	{
		ClientDiary tiedDiary = readDiaryFile(diaryPath);
		
		if(tiedDiary == null)
		{
			return false;
		}
		else
		{
			addDiaryToBookshelf(diaryPath, tiedDiary);
			return true;
		}
	}
	
	public ClientDiary readDiaryFile(String diaryPath)
	{
		ClientDiary diaryI = null;
		try
		{
			FileInputStream fileIn = new FileInputStream("/tmp/" + diaryPath + ".dia");
			ObjectInputStream oISIn = new ObjectInputStream(fileIn);
			diaryI = (ClientDiary) oISIn.readObject();
			oISIn.close();
			fileIn.close();
		} 
		catch (FileNotFoundException e) 
		{
			log.error(logHeader + "couldn't find serialized diary file ", e);
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
	
	public void addDiaryToBookshelf(String diaryPath, ClientDiary givenDiary)
	{
		bookshelf.put(diaryPath, givenDiary);
	}
	
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
			bookshelf.forEach((diaryPath, diary)->
			{
				String pathString = diariesFolderString + diaryPath + ".txt";
				Path printedDiaryPath = Paths.get(pathString);
				
				try 
				{
					Files.deleteIfExists(printedDiaryPath);
				} 
				catch (IOException e)
				{
					throw new IllegalArgumentException("IO couldn't delete file " + pathString);
				}
				
				boolean check = diary.printDiary(printedDiaryPath, log);
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
	
	public boolean executeAnalysis(ArrayList<HashMap<AnalysisInput, Object>> requestedAnalysis)
	{
		for(int i = 0 ; i < requestedAnalysis.size(); i++)
		{
			Object objectI = null;
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
			
			ArrayList<String> requestedDiaryNames = generateDiaryPathsList(requestedTPF, requestedDFV, requestedP, requestedRL,
																			requestedRN, requestedCN);
			
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
	
	public boolean printAll()
	{
		for(int i = 0 ; i < analyzedCheck.size() ; i++)
		{
			switch(analyzedCheck.get(i))
			{
				case DelayHistogram:
				{
					Histogram temp = (Histogram) analyzedInformation.get(i);
					temp.printHistogram();
					break;
				}
				case AverageDelay:
				{
					Long temp = (Long) analyzedInformation.get(i);
					System.out.println(temp.toString());
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
	
	public ArrayList<String> generateDiaryPathsList(String topologyFilePath, DistributedFlagValue distributedFlag,
														NetworkProtocol protocol, Long runLength, Integer runNumber, 
														String clientName)
	{		
		ArrayList<String> retVal = new ArrayList<String>();
		String diaryPathTestString = new String();
		
		if(topologyFilePath != null)
		{
			diaryPathTestString += (PSTBUtil.cleanTPF(topologyFilePath) + "-");
		}
		else
		{
			log.error(logHeader + "generateDiaryPathsList() needs a topologyFilePath at minimum"); 
			return retVal;
		}
		
		if(distributedFlag != null)
		{
			diaryPathTestString += (distributedFlag + "-");
		}
		else
		{
			diaryPathTestString += "\\w+-";
		}
		
		if(protocol != null)
		{
			diaryPathTestString += (protocol + "-");
		}
		else
		{
			diaryPathTestString += "\\w+-";
		}
		
		if(runLength != null)
		{
			diaryPathTestString += (runLength + "-");
		}
		else
		{
			diaryPathTestString += "\\w+-";
		}
		
		if(runNumber != null)
		{
			diaryPathTestString += (runNumber + "-");
		}
		else
		{
			diaryPathTestString += "\\w+-";
		}
		
		if(clientName != null)
		{
			diaryPathTestString += clientName;
		}
		else
		{
			diaryPathTestString += "\\w+";
		}
		
		Pattern diaryPathTest = Pattern.compile(diaryPathTestString);
		Iterator<String> bookshelfIt = bookshelf.keySet().iterator();
		
		for( ; bookshelfIt.hasNext() ; )
		{
			String diaryPathI = bookshelfIt.next(); 
			if(diaryPathTest.matcher(diaryPathI).matches())
			{
				retVal.add(diaryPathI);
			}
		}
		
		return retVal;
	}
	
	public Histogram developDelayHistogram(ArrayList<String> diaryPaths, PSActionType typeToAnalyse, DiaryHeader delayType)
	{
		if( !delayType.equals(DiaryHeader.ActionDelay) && !delayType.equals(DiaryHeader.MessageDelay) )
		{
			log.error(logHeader + "That DiaryHeader is not a delay value.");
			return null;
		}
		
		Histogram retVal = new Histogram();
		
		for(int i = 0 ; i < diaryPaths.size() ; i++)
		{
			String diaryPathI = diaryPaths.get(i);
			
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
	
	public Long developAverageDelay(ArrayList<String> diaryPaths, PSActionType typeToAnalyse, DiaryHeader delayType)
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
		
		Long retVal = new Long(0L);
		int k = 0;
		
		for(int i = 0 ; i < diaryPaths.size() ; i++)
		{
			String diaryPathI = diaryPaths.get(i);
			
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
							retVal += pageJ.getDelay(delayType);
							k++;
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
		
		return retVal / k;
	}
}
