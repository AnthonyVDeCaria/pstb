/**
 * 
 */
package pstb.analysis.diary;

/**
 * @author adecaria
 * 
 * The allowed headers for a given Diary Entry
 *
 */
public enum DiaryHeader {
	PSActionType,
	TimeActionStarted, TimeBrokerFinished,
	StartedAction, EndedAction, ActionDelay,
	MessageID, Attributes, PayloadSize, 
	TimeActiveStarted, TimeActiveEnded,
	TimeMessageCreated, TimeMessageReceived, MessageDelay
}
