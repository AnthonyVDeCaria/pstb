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

import org.apache.logging.log4j.Logger;

import pstb.util.ClientDiary;
import pstb.util.DiaryEntry;
import pstb.util.DiaryEntry.DiaryHeader;
import pstb.util.PSActionType;

/**
 * @author padres-dev-4187
 *
 */
public class Analyzer {
	private HashMap<String, ClientDiary> bookshelf;
	private Logger log = null;
	private String logHeader = "Analysis: ";
	
	private final String analysisFolderString = System.getProperty("user.dir") + "/analysis/";
	private final String diariesFolderString = analysisFolderString + "diaries/";
	
	public Analyzer(Logger logger)
	{
		log = logger;
		bookshelf = new HashMap<String, ClientDiary>();
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
	
	public Histogram developDelayHistogram(ArrayList<String> clientNames, PSActionType typeToAnalyse, DiaryHeader delayType)
	{
		if( (delayType != DiaryHeader.ActionDelay) 
			|| (delayType != DiaryHeader.TimeDifference)
			)
		{
			log.error(logHeader + "That DiaryHeader is not a delay value.");
			return null;
		}
		
		Histogram retVal = new Histogram();
		
		for(int i = 0 ; i < clientNames.size() ; i++)
		{
			String clientNameI = clientNames.get(i);
			
			if(!bookshelf.containsKey(clientNameI))
			{
				log.error(logHeader + "No diary exists from " + clientNameI);
				return null;
			}
			
			ClientDiary clientIsDiary = bookshelf.get(clientNameI);
			
			for(int j = 0 ; j < clientIsDiary.size() ; j++)
			{
				DiaryEntry pageJ = clientIsDiary.getDiaryEntryI(j);
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
}
