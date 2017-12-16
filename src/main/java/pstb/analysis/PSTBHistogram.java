/**
 * 
 */
package pstb.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.Logger;

import pstb.startup.workload.PSActionType;
import pstb.util.Histogram;

/**
 * @author padres-dev-4187
 * 
 * Extends the histogram class to allow increased functionality crucial to PSTB
 * Such as what type of PSActionType is this Histogram recording, 
 * as well as personalizing the histogram for recording into a file
 * @see Histogram for the proper histogram functions
 */
public class PSTBHistogram extends Histogram {
	private String histogramName;
	private PSActionType histogramType;
	
	private final String logHeader = "PSTBHistogram: ";
	
	public PSTBHistogram()
	{
		super();
		histogramName = new String();
		histogramType = null;
	}

	/**
	 * Sets the histogramName
	 * 
	 * @param givenHN - the new histogramName
	 */
	public void setHistogramName(String givenHN) {
		histogramName = givenHN;
	}
	
	/**
	 * Sets the histogram type
	 * 
	 * @param givenHT - the new histogramType
	 */
	public void setHistogramType(PSActionType givenHT) {
		histogramType = givenHT;
	}
	
	/**
	 * Gets the histogramName
	 * 
	 * @return the histogramName
	 */
	public String getHistogramName() {
		return histogramName;
	}

	/**
	 * Gets the histogramType
	 * 
	 * @return the histogramType
	 */
	public PSActionType getHistogramType() {
		return histogramType;
	}
	
	/**
	 * Writes this PSTB histogram into a file - including providing unit help.
	 * 
	 * @param givenFilePath - the Path we are to write to
	 * @param log - the Logger file we should use if there are errors
	 * @return false on failure; true otherwise
	 */
	public boolean recordPSTBHistogram(Path givenFilePath, Logger log)
	{
		String line = null;
		
		if(histogramType.equals(null))
		{
			log.error(logHeader + "Here is no type for this histogram!");
			return false;
		}
		else if(histogramType.equals(PSActionType.R))
		{
			line = "Units are in milliseconds\n\n";
		}
		else
		{
			line = "Units are in nanoseconds\n\n";
		}
		
		try
		{
			Files.deleteIfExists(givenFilePath);
			Files.write(givenFilePath, line.getBytes());
		}
		catch(IOException e)
		{
			log.error(logHeader + "Error writing to file:", e);
			return false;
		}
		
		boolean check = super.recordHistogram(givenFilePath, log);
		if(!check)
		{
			log.error(logHeader + "Error writing the rest of the histogram to a file!");
			return false;
		}
		
		return true;
	}

}
