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
	// Always
	BenchmarkStartTime, TopologyFilePath, DistributedFlag, Protocol,
	// Scenario
	RunLength, RunNumber, ClientName, AnalysisType, PSActionType,
	// Throughput
	PeriodLength, MessageSize, NumAttribute, AttributeRatio, DiaryHeader
}
