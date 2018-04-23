/**
 * 
 */
package pstb.analysis.analysisobjects.scenario;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import pstb.startup.workload.PSActionType;
import pstb.util.PSTBUtil;
import pstb.util.PSTBUtil.TimeType;

/**
 * @author padres-dev-4187
 *
 */
public class PSTBHistogram extends PSTBScenarioAO {
	// Variables
	private ArrayList<Long> dataset;
	private int histogram[];
	private Double range;
	private Long floorValue;
	
	public PSTBHistogram()
	{
		super();
		dataset = new ArrayList<Long>();
		histogram = null;
		range = null;
		floorValue = null;
	}
	
	public void addToDataset(Long datapointToAdd)
	{
		dataset.add(datapointToAdd);
	}
	
	public ArrayList<Long> getDataset()
	{
		return dataset;
	}
	
	public int[] getHistogram()
	{
		return histogram;
	}
	
	public Double getRange()
	{
		return range;
	}
	
	public Long getFloorValue()
	{
		return floorValue;
	}
	
	public boolean buildHistogram()
	{
		if(dataset.isEmpty())
		{
			log.error("No data exists to turn into a histogram!");
			return false;
		}
		
		int numDataPoints = dataset.size();
		Long[] sortedDataset = dataset.toArray(new Long[numDataPoints]);
		Arrays.sort(sortedDataset);
		
		int numberOfBins = calculateNumBins(numDataPoints);
		floorValue = sortedDataset[0];
		range = (sortedDataset[numDataPoints-1].doubleValue() - floorValue.doubleValue() + 1.0) / numberOfBins;
		
		histogram = new int[numberOfBins];
		
		int binNum = 1;
		for(int i = 0 ; i < numDataPoints ; i++)
		{
			Long dataPointI = sortedDataset[i];
			while(dataPointI.doubleValue() > (floorValue + range*binNum))
			{
				binNum++;
			}
			
			histogram[binNum-1]++;
		}
		
		return true;
	}
	
	private int calculateNumBins(int numElements)
	{
		if(numElements <= 30)
		{
			return (int) Math.sqrt(numElements);
		}
		else
		{
			return (int) Math.ceil(Math.log(numElements) / Math.log(2)) + 1;
		}
	}
	
	@Override
	public boolean completeRecord(Path givenFilePath) {
		if(dataset.isEmpty())
		{
			log.error("No data exists to print our histogram!");
			return true;
		}
		
		buildHistogram();
		
		DecimalFormat binFormat = new DecimalFormat("#.#####");
		
		for(int i = 0 ; i < histogram.length ; i++)
		{
			Double binFloor = floorValue + range*i;
			Double binCeiling = floorValue + range*(i+1);
			
			String convertedFloor = null;
			String convertedCeiling = null;
			
			if(type.equals(PSActionType.R))
			{
				convertedFloor = PSTBUtil.createTimeString(binFloor.longValue(), TimeType.Milli, TimeUnit.MILLISECONDS);
				convertedCeiling = PSTBUtil.createTimeString(binCeiling.longValue(), TimeType.Milli, TimeUnit.MILLISECONDS);
			}
			else
			{
				convertedFloor = PSTBUtil.createTimeString(binFloor.longValue(), TimeType.Nano, TimeUnit.MILLISECONDS);
				convertedCeiling = PSTBUtil.createTimeString(binCeiling.longValue(), TimeType.Nano, TimeUnit.MILLISECONDS);
			}
			
			String cleanFloor = binFormat.format(binFloor);
			String cleanCeiling = binFormat.format(binCeiling);
			
			String lineI = convertedFloor + " - " + convertedCeiling 
					+ "	" + "(" + cleanFloor + " - " + cleanCeiling + ")" 
					+ "	" + "->" + " " + histogram[i] + "\n";
			
			try
			{
				Files.write(givenFilePath, lineI.getBytes(), StandardOpenOption.APPEND);
			}
			catch(IOException e)
			{
				log.error(logHeader + "Error writing bin " + i + ": ", e);
				return false;
			}
		}
		
		return true;
	}

	@Override
	public void handleDataPoint(Long givenDataPoint) {
		addToDataset(givenDataPoint);
	}

}
