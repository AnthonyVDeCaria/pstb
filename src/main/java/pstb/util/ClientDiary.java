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
	 * Creates a new diary entry
	 * @param givenAction - the action the client is accomplishing; be it Advertise, Publish, etc
	 * @param attributes - the Attributes associated with this action
	 * @return the new diary entry
	 */
	public void addDiaryEntryToDiary(DiaryEntry givenDE) 
	{
		diary.add(givenDE);
	}
	
	/**
	 * Gets a diary entry given it's associated Message ID
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
	 * @return
	 */
	public int size() {
		return diary.size();
	}
	
	/**
	 * @param log 
	 * 
	 */
	public boolean printDiary(Path givenFilePath, Logger log)
	{
		int diarySize = diary.size();
		
		String openingLine = "There are " + diarySize + " entries\n\n";
		try
		{
			if(Files.exists(givenFilePath))
			{
				Files.write(givenFilePath, openingLine.getBytes(), StandardOpenOption.APPEND);
			}
			else
			{
				Files.write(givenFilePath, openingLine.getBytes());
			}
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
				if(Files.exists(givenFilePath))
				{
					Files.write(givenFilePath, intro.getBytes(), StandardOpenOption.APPEND);
				}
				else
				{
					Files.write(givenFilePath, intro.getBytes());
				}
			}
			catch(IOException e)
			{
				log.error("ClientDiary: Error writing intro " + i, e);
				return false;
			}
			
			boolean check = diary.get(i).printPage(givenFilePath, log);
			if(!check)
			{
				return false;
			}
			
			try
			{
				String newLine = "\n";
				if(Files.exists(givenFilePath))
				{
					Files.write(givenFilePath, newLine.getBytes(), StandardOpenOption.APPEND);
				}
				else
				{
					Files.write(givenFilePath, newLine.getBytes());
				}
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
