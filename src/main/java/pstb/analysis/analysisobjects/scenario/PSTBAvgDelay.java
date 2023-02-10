/**
 * 
 */
package pstb.analysis.analysisobjects.scenario;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import pstb.analysis.Analyzer;
import pstb.startup.workload.PSActionType;
import pstb.util.PSTBUtil;
import pstb.util.PSTBUtil.TimeType;

/**
 * @author padres-dev-4187
 * 
 * A file that allows us to store an average delay
 * and a name associated with it 
 * @see Analyzer
 */
public class PSTBAvgDelay extends PSTBScenarioAO {
    private Long value;
    
    private Long sum;
    private int instances;

    /**
     * Constructor
     */
    public PSTBAvgDelay() {
        super();
        value = null;
        logHeader = "PSTBAvgDelay: ";
        
        sum = 0L;
        instances = 0;
    }
    
    /**
     * Sets the value
     * 
     * @param value the value to set
     */
    public void setValue(Long givenValue) {
        value = givenValue;
    }

    /**
     * Gets the value
     * 
     * @return the value
     */
    public Long getValue() {
        return value;
    }
    
    @Override
    public boolean completeRecord(Path givenFilePath)
    {
        if(value == null)
        {
            log.error(logHeader + "No average value exists!");
            return true;
        }
        
        String correctedValue = null;
        
        if(type.equals(PSActionType.R))
        {
            correctedValue = PSTBUtil.createTimeString(value, TimeType.Milli, TimeUnit.MILLISECONDS);
        }
        else
        {
            correctedValue = PSTBUtil.createTimeString(value, TimeType.Nano, TimeUnit.MILLISECONDS);
        }
        
        String line = correctedValue + " (" + value + ")";        
        try
        {
            Files.deleteIfExists(givenFilePath);
            Files.write(givenFilePath, line.getBytes());
        }
        catch(IOException e)
        {
            log.error(logHeader + " error writing average delay to the requested file: ", e);
            return false;
        }
        
        return true;
    }

    @Override
    public void handleDataPoint(Long givenDataPoint) {
        sum += givenDataPoint;
        instances++;
        
        value = sum / instances;
    }
}
