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
	// Scenario
	PSActionType,
	TimeActionStarted, TimeFunctionReturned,
	StartedAction, EndedAction, ActionDelay,
	MessageID, Attributes, PayloadSize, 
	TimeActiveStarted, TimeActiveEnded,
	TimeMessageCreated, TimeMessageReceived, MessageDelay,
	
	// Throughput
	Round, MessageRate, 
	RoundLatency, MessagesReceievedRound, MessagesReceievedTotal,
	CurrentThroughput, Secant, AverageThroughput, FinalThroughput,
	Y0, Y1, X0, X1, CurrentRatio
}
