/**
 * 
 */
package pstb.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.Logger;

import pstb.util.PSActionType;

/**
 * @author padres-dev-4187
 * 
 * Extends the histogram class to allow the inclusion of a name
 * @see Histogram
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
