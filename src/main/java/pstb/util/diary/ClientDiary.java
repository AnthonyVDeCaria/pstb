package pstb.util.diary;

import java.util.ArrayList;

public class ClientDiary implements java.io.Serializable
{
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
	 * Gets a diary entry given it's associated client action and attributes
	 * @param clientAction - the associated client action 
	 * @param attributes - the associated attributes
	 * @return Either the given diary entry, or null
	 */
	public DiaryEntry getDiaryEntryGivenCAA(String clientAction, String attributes)
	{
		DiaryEntry appropriateDiary = null;
		for(int i = 0; i < diary.size() ; i++)
		{
			DiaryEntry iTHEntry = diary.get(i);
			if(iTHEntry.containsValue(clientAction) && iTHEntry.containsValue(attributes))
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
}
