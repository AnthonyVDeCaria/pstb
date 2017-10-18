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

import pstb.benchmark.PSClientPADRES;
import pstb.util.ClientDiary;
import pstb.util.DiaryEntry;
import pstb.util.DiaryEntry.DiaryHeader;
import pstb.util.PSActionType;
import pstb.util.PSTBError;

/**
 * @author padres-dev-4187
 *
 */
public class Analyzer {

	private HashMap<String, ClientDiary> bookshelf;
	private Logger log = LogManager.getRootLogger();
	private String logHeader = "Analysis: ";
	
	public boolean populateBookshelf(ArrayList<String> clientNames)
	{
		/*
			The Object code goes here like in PhysicalBroker/PhysicalClient
			Only this time its looped
			And the names are extracted and put as the key in the HashMap
		*/
		return false;
	}
	
	private ClientDiary readClientFile(String givenClientName)
	{
		ClientDiary givenClient = null;
		try 
		{
			FileInputStream fileIn = new FileInputStream("/tmp/" + givenClientName + ".dia");
			ObjectInputStream oISIn = new ObjectInputStream(fileIn);
			givenClient = (ClientDiary) oISIn.readObject();
			oISIn.close();
			fileIn.close();
		} 
		catch (FileNotFoundException e) 
		{
			log.error(logHeader + "couldn't find serialized client file ", e);
			System.exit(PSTBError.ERROR_FILE_C);
		}
		catch (IOException e)
		{
			log.error(logHeader + "error accessing ObjectInputStream ", e);
			System.exit(PSTBError.ERROR_IO_C);
		}
		catch(ClassNotFoundException e)
		{
			log.error(logHeader + "can't find class ", e);
			System.exit(PSTBError.ERROR_CNF_C);
		}
		
		return givenClient;
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
	
	public Histogram developDelayHistogramOneCOneAT(ArrayList<String> clientNames, PSActionType typeToAnalyse, DiaryHeader delayType)
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
