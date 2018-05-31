/**
 * 
 */
package pstb.analysis.analysisobjects.throughput;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;

import pstb.analysis.diary.DiaryHeader;

/**
 * @author padres-dev-4187
 *
 */
public class PSTBFinalThroughput extends PSTBThroughputAO {
	// Variables
	private Double value;
	
	public PSTBFinalThroughput()
	{
		super(DiaryHeader.FinalThroughput);
		value = null;
	}
	
	@Override
	public boolean completeRecord(Path givenFilePath) {
		if(value == null)
		{
			log.error("No data exists!");
			return true;
		}
		
		DecimalFormat pointFormat = new DecimalFormat("0.00");
		
		String lineI = pointFormat.format(value) + " messages/sec\n";
		try
		{
			Files.write(givenFilePath, lineI.getBytes(), StandardOpenOption.APPEND);
		}
		catch(IOException e)
		{
			log.error(logHeader + "Error writing value: ", e);
			return false;
		}
		
		return true;
	}
	
	@Override
	public void handleDataPoints(Double x, Double y) {
		// null
	}

	@Override
	public void handleDataPoint(Double givenDataPoint) {
		value = givenDataPoint;
	}

}
