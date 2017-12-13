package pstb.util;

/**
 * @author padres-dev-4187
 *
 */
public class PSTBError {
	public static final int M_BENCHMARK = 1;
	public static final int M_WORKLOAD = 2;
	public static final int M_TOPO_LOG = 3;
	public static final int M_TOPO_PHY = 4;
	public static final int M_DISTRIBUTED = 5;
	public static final int M_EXPERIMENT = 6;
	public static final int M_ANALYSIS = 7;
	
	public static final int N_ARGS = 10;
	
	public static final int B_ARGS = 11;
	public static final int B_SOCKET = 12;
	public static final int B_OBJECT = 13;
	public static final int B_ACK = 14;
	public static final int B_CREATE = 15;
	public static final int B_START = 16;
	
	public static final int C_ARGS = 21;
	public static final int C_SOCKET = 22;
	public static final int C_OBJECT = 23;
	public static final int C_ACK = 24;
	public static final int C_INIT = 25;
//	public static final int C_CONN = 25; <- a separate connection function isn't working ATM
	public static final int C_START = 26;
	public static final int C_RUN = 27;
	public static final int C_SHUT = 28;
	public static final int C_DIARY = 29;
	
	public static final int A_ARGS = 31;
	public static final int A_COLLECT = 32;
	public static final int A_RECORD_DIARY = 33;
	public static final int A_ANALYSIS_FILE_PARSE = 34;
	public static final int A_ANALYSIS = 35;
	public static final int A_RECORD_ANALYSIS = 36;
	
}
