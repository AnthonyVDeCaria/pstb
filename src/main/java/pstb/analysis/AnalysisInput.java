/**
 * 
 */
package pstb.analysis;

/**
 * @author padres-dev-4187
 * 
 * The enum narrows the Analysis File input space.
 * Only these types of inputs are allowed.
 */
public enum AnalysisInput {
	AnalysisType, DiaryHeader, 
	BenchmarkStartTime,
	TestType, TopologyFilePath, DistributedFlag, Protocol, 
	PSActionType, RunLength, RunNumber, ClientName,
	PeriodLength, MessageSize, NumAttributes, AttributeRatio
}
