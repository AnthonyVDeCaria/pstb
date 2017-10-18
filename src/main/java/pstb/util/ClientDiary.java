package pstb.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

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
	 * 
	 */
	public boolean printDiary(Path givenFilePath)
	{
		for(int i = 0; i < diary.size() ; i++)
		{
			String line = "Page " + i + ":\n";
			
			try
			{
				Files.write(givenFilePath, line.getBytes());
				boolean check = diary.get(i).printPage(givenFilePath);
				if(!check)
				{
					return false;
				}
			}
			catch(IOException e)
			{
				return false;
			}
		}
		
		return true;
	}
}
