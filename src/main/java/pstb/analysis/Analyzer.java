package pstb.analysis;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
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
	private Logger log = LogManager.getRootLogger();
	private String logHeader = "Analysis: ";
	
	public Analyzer()
	{
		bookshelf = new HashMap<String, ClientDiary>();
	}
	
	public boolean populateBookshelf(ArrayList<String> clientNames)
	{
		for(int i = 0 ; i < clientNames.size(); i++)
		{
			String clientNameI = clientNames.get(i);
			ClientDiary diaryI = readDiaryFile(clientNameI);
			
			if(diaryI == null)
			{
				bookshelf.clear();
				return false;
			}
			else
			{
				bookshelf.put(clientNameI, diaryI);
			}
		}
		
		return true;
	}
	
	private ClientDiary readDiaryFile(String clientNameI)
	{
		ClientDiary diaryI = null;
		try
		{
			FileInputStream fileIn = new FileInputStream("/tmp/" + clientNameI + ".dia");
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
	
	public boolean printAllDiaries()
	{	
		try
		{
			bookshelf.forEach((clientName, diary)->
			{
				String pathString = "/analysis/diaries/" + clientName + ".txt";
				Path clientIsDiaryPath = Paths.get(pathString);
				
				boolean check = diary.printDiary(clientIsDiaryPath);
				if(!check)
				{
					throw new IllegalArgumentException();
				}
			});
		}
		catch(IllegalArgumentException e)
		{
			log.error(logHeader + "error printing all diaries.");
			return false;
		}
		
		return true;
	}
	
	public Histogram developDelayHistogram(ArrayList<String> clientNames, PSActionType typeToAnalyse, DiaryHeader delayType)
	{
		if( (delayType != DiaryHeader.AckDelay) 
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
