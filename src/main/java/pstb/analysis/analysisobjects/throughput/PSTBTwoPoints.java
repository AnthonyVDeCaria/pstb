/**
 * 
 */
package pstb.analysis.analysisobjects.throughput;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import pstb.analysis.diary.DiaryHeader;
import pstb.util.PSTBUtil;
import pstb.util.PSTBUtil.TimeType;

/**
 * @author padres-dev-4187
 *
 */
public class PSTBTwoPoints extends PSTBThroughputAO {
	// Variables
	private ArrayList<Point2D.Double> dataset;
	
	public PSTBTwoPoints(DiaryHeader givenHeader)
	{
		super(givenHeader);
		dataset = new ArrayList<Point2D.Double>();
	}
	
	public ArrayList<Point2D.Double> getDataset()
	{
		return dataset;
	}
	
	@Override
	public boolean completeRecord(Path givenFilePath) {
		if(dataset.isEmpty())
		{
			log.error("No data exists in " + name + " to record!");
			return true;
		}
		
		if(associated == null)
		{
			log.error("Don't know what data is contained!");
			return false;
		}
		else if(!PSTBUtil.isDHThroughputGraphable(associated))
		{
			log.error("Given DiaryHeader is illegal!");
			return false;
		}
		
		DecimalFormat pointFormat = new DecimalFormat("0.00");
		
		for(int i = 0 ; i < dataset.size() ; i++)
		{
			Point2D.Double pointI = dataset.get(i);
			
			Double x = pointI.getX();
			Double y = pointI.getY();
			
			String lineI = null;
			if(associated.equals(DiaryHeader.RoundLatency))
			{
				String convertedTime = PSTBUtil.createTimeString(y.longValue(), TimeType.Milli, TimeUnit.SECONDS);
				lineI = associated.toString() + " = " + convertedTime +
						"	" + "Message Rate = " + pointFormat.format(x) + " messages/sec\n";
			}
			else if(associated.equals(DiaryHeader.Secant))
			{
				lineI = associated.toString() + " = " + pointFormat.format(y) +
						"	" + "Message Rate = " + pointFormat.format(x) + " messages/sec\n";
			}
			else
			{
				lineI = associated.toString() + " = " + pointFormat.format(y) + " messages/sec" +
						"	" + "Message Rate = " + pointFormat.format(x) + " messages/sec\n";
			}
			
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

	public void handleDataPoints(Double x, Double y) {
		Point2D.Double newPoint = new Point2D.Double(x, y);
		dataset.add(newPoint);
	}

	@Override
	public void handleDataPoint(Double givenDataPoint) {
		// NULL
	}

}
