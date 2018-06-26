package pstb.analysis.diary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.logging.log4j.Logger;

import pstb.startup.workload.PSActionType;
import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 * 
 * Allows the Client to record data during a run.
 */
public class ClientDiary implements java.io.Serializable
{
	// Constants
	private static final long serialVersionUID = 1L;
	
	// Key Variable
	ArrayList<DiaryEntry> diary;
	String timeCreated;
	
	/**
	 * Empty Constructor
	 */
	public ClientDiary()
	{
		diary = new ArrayList<DiaryEntry>();
		Long currTime = System.currentTimeMillis();
		timeCreated = PSTBUtil.DATE_FORMAT.format(currTime);
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
	 * @param givenMID - the associated messageID
	 * @return Either the given diary entry, or null
	 */
	public DiaryEntry getDiaryEntryGivenMessageID(String givenMID)
	{
		DiaryEntry appropriateDiary = null;
		for(int i = 0; i < diary.size() ; i++)
		{
			DiaryEntry iTHEntry = diary.get(i);
			if(iTHEntry.containsValue(givenMID))
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
	 * @param givenAction - the associated PSActionType
	 * @param givenAttri - the associated attributes
	 * @return Either the given diary entry, or null
	 */
	public DiaryEntry getDiaryEntryGivenActionTypeNAttributes(PSActionType givenAction, String givenAttri, Logger log)
	{
		DiaryEntry appropriateDiary = null;
		for(int i = 0; i < diary.size() ; i++)
		{
			DiaryEntry iTHEntry = diary.get(i);
			if(iTHEntry == null)
			{
				log.error("Diary contains an entry that's null!");
				break;
			}
			if(iTHEntry.containsValue(givenAction) && iTHEntry.containsValue(givenAttri))
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
		
		String openingLine = "Created at " + timeCreated + "\n"
				+ "There are " + diarySize + " entries\n" 
				+ "\n";
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

	public void clear() {
		diary.clear();
	}
	
	public void removeDiaryEntiresWithGivenPSActionType(PSActionType givenAT)
	{
		Iterator<DiaryEntry> itD = diary.iterator();
		while(itD.hasNext())
		{
			DiaryEntry entryI = itD.next();
			PSActionType entryIsAT = entryI.getPSActionType();
			if(entryIsAT != null && entryIsAT.equals(givenAT))
			{
				itD.remove();
			}
		}
	}
}
