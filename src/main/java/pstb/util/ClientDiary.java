package pstb.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import org.apache.logging.log4j.Logger;

public class ClientDiary implements java.io.Serializable
{
	private static final long serialVersionUID = 1L;
	ArrayList<DiaryEntry> diary;
	
	/**
	 * Empty Constructor
	 */
	public ClientDiary()
	{
		diary = new ArrayList<DiaryEntry>();
	}
	
	/**
	 * Adds a new diary entry to this diary
	 * 
	 * @param givenDE - the diary entry to add
	 */
	public void addDiaryEntryToDiary(DiaryEntry givenDE) 
	{
		diary.add(givenDE);
	}
	
	/**
	 * Gets a diary entry given it's associated Message ID
	 * 
	 * @param mID - the associated messageID
	 * @return Either the given diary entry, or null
	 */
	public DiaryEntry getDiaryEntryGivenMessageID(String mID)
	{
		DiaryEntry appropriateDiary = null;
		for(int i = 0; i < diary.size() ; i++)
		{
			DiaryEntry iTHEntry = diary.get(i);
			if(iTHEntry.containsValue(mID))
			{
				appropriateDiary = iTHEntry;
				break;
			}
		}
		return appropriateDiary;
	}
	
	/**
	 * Gets a diary entry given it's associated Action Type and Attributes
	 * 
	 * @param action - the associated PSActionType
	 * @param attri - the associated attributes
	 * @return Either the given diary entry, or null
	 */
	public DiaryEntry getDiaryEntryGivenActionTypeNAttributes(PSActionType action, String attri)
	{
		DiaryEntry appropriateDiary = null;
		for(int i = 0; i < diary.size() ; i++)
		{
			DiaryEntry iTHEntry = diary.get(i);
			if(iTHEntry.containsValue(action.toString()) && iTHEntry.containsValue(attri))
			{
				appropriateDiary = iTHEntry;
				break;
			}
		}
		return appropriateDiary;
	}
	
	
	/**
	 * Gets the Diary Entry at index i
	 * 
	 * @param i - index
	 * @return the associated DiaryEntry
	 */
	public DiaryEntry getDiaryEntryI(int i)
	{
		return diary.get(i);
	}
	
	/**
	 * Returns the size of the Diary
	 * (Basically an extension of the ArrayList<> size function)
	 * 
	 * @return the size of the diary
	 */
	public int size() {
		return diary.size();
	}
	
	/**
	 * Takes the contents of this diary, and writes it to a file
	 * 
	 * @param givenFilePath - the Path of the file to write to
	 * @param log - the Logger to record errors
	 */
	public boolean recordDiary(Path givenFilePath, Logger log)
	{
		int diarySize = diary.size();
		
		String openingLine = "There are " + diarySize + " entries\n\n";
		try
		{
			Files.deleteIfExists(givenFilePath);
			Files.write(givenFilePath, openingLine.getBytes());
		}
		catch(IOException e)
		{
			log.error("ClientDiary: Error writing diarySize", e);
			return false;
		}
		
		for(int i = 0; i < diarySize ; i++)
		{
			String intro = "Page " + i + ":\n";
			try
			{
				Files.write(givenFilePath, intro.getBytes(), StandardOpenOption.APPEND);
			}
			catch(IOException e)
			{
				log.error("ClientDiary: Error writing intro " + i, e);
				return false;
			}
			
			boolean check = diary.get(i).recordPage(givenFilePath, log);
			if(!check)
			{
				return false;
			}
			
			String newLine = "\n";
			try
			{
				Files.write(givenFilePath, newLine.getBytes(), StandardOpenOption.APPEND);
			}
			catch(IOException e)
			{
				log.error("ClientDiary: Error writing newLine " + i, e);
				return false;
			}
		}
		
		return true;
	}
}
