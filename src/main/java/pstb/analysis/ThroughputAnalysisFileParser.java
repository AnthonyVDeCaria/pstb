/**
 * 
 */
package pstb.analysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.analysis.diary.DistributedFlagValue;
import pstb.startup.config.AttributeRatio;
import pstb.startup.config.MessageSize;
import pstb.startup.config.NetworkProtocol;
import pstb.startup.config.NumAttribute;
import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 * 
 * Reads and Parses an Analysis File
 * Creating a List of requested analysis.
 * @see etc/defaultAnalysis.txt
 */
public class ThroughputAnalysisFileParser {
    
    // Constants - General 
    private final int LOC_BENCHMARK_NUMBER = 0;
    private final int LOC_TOPO_FILE_PATH = 1;
    private final int LOC_DISTRIBUTED_FLAG = 2;
    private final int LOC_PROTOCOL = 3;
    
    // Constants - Throughput
    private final int NUM_SEGMENTS_THROUGHPUT = 8;
    private final int LOC_PERIOD_LENGTH = 4;
    private final int LOC_MESSAGE_SIZE = 5;
    private final int LOC_NUM_ATTRIBUTES = 6;
    private final int LOC_ATTRIBUTE_RATIO = 7;
    
    // Input
    private String analysisFileString;
    
    // Output
    private ArrayList<HashMap<AnalysisInput, String>> requestedAnalysis;
    
    String logHeader = "Analysis Parser: ";
    Logger log = LogManager.getRootLogger();
    
    /**
     * Empty Constructor
     */
    public ThroughputAnalysisFileParser()
    {
        requestedAnalysis = new ArrayList<HashMap<AnalysisInput, String>>();
        analysisFileString = new String();
    }
    
    /**
     * Sets the AnalysisFileString
     * 
     * @param newAFS the new AnalysisFileString
     */
    public void setAnalysisFileString(String newAFS)
    {
        analysisFileString = newAFS;
    }
    
    /**
     * Gets the requestedAnalysis
     * 
     * @return the requestedAnalysis
     */
    public ArrayList<HashMap<AnalysisInput, String>> getRequestedComponents()
    {
        return requestedAnalysis;
    }
    
    /**
     * Parses the given Analysis File
     * and extracts the analysis requested 
     * 
     * @return false on error; true otherwise
     */
    public boolean parse()
    {        
        if(analysisFileString.isEmpty())
        {
            log.error(logHeader + "No path to the Analysis file was given!");
            return false;
        }
        
        boolean isParseSuccessful = true;
        String line = null;
        int linesRead = 0;
        
        try {
            BufferedReader readerAF = new BufferedReader(new FileReader(analysisFileString));
            while( (line = readerAF.readLine()) != null)
            {
                linesRead++;
                
                if(!PSTBUtil.checkIfLineIgnorable(line))
                {
                    String[] splitLine = line.split(PSTBUtil.COLUMN_SEPARATOR);
                    
                    if(checkLineLength(splitLine.length))
                    {
                        HashMap<AnalysisInput, String> requested = developRequestMatrix(splitLine);
                        if(!requested.isEmpty())
                        {
                            log.trace(logHeader + "Line " + linesRead + "'s syntax checks out.");
                            requestedAnalysis.add(requested);
                        }
                        else
                        {
                            isParseSuccessful = false;
                            log.error(logHeader + "Line " + linesRead + " - Request isn't correct!");
                        }
                    }
                    else
                    {
                        isParseSuccessful = false;
                        log.error(logHeader + "Line " + linesRead + " - Length isn't correct!");
                    }
                }
            }
            readerAF.close();
        } 
        catch (IOException e) 
        {
            isParseSuccessful = false;
            log.error(logHeader + "Cannot find file!", e);
        }
        return isParseSuccessful;
    }
    
    /**
     * Checks if the user input for the given line is proper. 
     * If it is, returns a HashMap containing all of these requests.
     * 
     * @param splitLine - the line from the analysis file broken apart 
     * @return null on error; a HashMap of the different requests otherwise
     */
    private HashMap<AnalysisInput, String> developRequestMatrix(String[] splitLine)
    {
        HashMap<AnalysisInput, String> retVal = new HashMap<AnalysisInput, String>();
                
        String givenBN = splitLine[LOC_BENCHMARK_NUMBER];
        String givenT = splitLine[LOC_TOPO_FILE_PATH];
        String givenDF = splitLine[LOC_DISTRIBUTED_FLAG];
        String givenP = splitLine[LOC_PROTOCOL];
        String givenPL = splitLine[LOC_PERIOD_LENGTH];
        String givenMS = splitLine[LOC_MESSAGE_SIZE];
        String givenNA = splitLine[LOC_NUM_ATTRIBUTES];
        String givenAR = splitLine[LOC_ATTRIBUTE_RATIO];
        
        boolean error = false;
        
        // Benchmark Start Times
        if(!givenBN.equals("null"))
        {
            Pattern bstTest = Pattern.compile(PSTBUtil.BENCHMARK_NUMBER_REGEX);
            if(!bstTest.matcher(givenBN).matches())
            {
                log.error(logHeader + "Given BenchmarkNumber " + givenBN + " is not valid!");
                error = true;
            }
        }
        
        // Topologies
        // Currently not applicable
        
        // Distributed
        if(!givenDF.equals("null"))
        {
            try
            {
                DistributedFlagValue.valueOf(givenDF);
            }
            catch(Exception e)
            {
                log.error(logHeader + "Given string " + givenDF + " isn't a DistributedFlagValue!");
                error = true;
            }
        }
        
        // Protocols
        if(!givenP.equals("null"))
        {
            try
            {
                NetworkProtocol.valueOf(givenP);
            }
            catch(Exception e)
            {
                log.error(logHeader + "Given string " + givenP + " isn't a NetworkProtocol!");
                error = true;
            }
        }
        
        // PeriodLengths
        if(!givenPL.equals("null"))
        {
            Long temp = PSTBUtil.checkIfLong(givenPL, false, null);
            if(temp == null)
            {
                log.error(logHeader + "Given string " + givenPL + " isn't a Long!");
                error = true;
                
                // Should also be compared to BenchmarkConfig
            }
            else if(temp.compareTo(0L) <= 0)
            {
                log.error(logHeader + "Given string " + givenPL + " is a Long less than 1!");
                error = true;
            }
        }
        
        // MessageSize
        if(!givenMS.equals("null"))
        {
            try
            {
                MessageSize.valueOf(givenMS);
            }
            catch(Exception e)
            {
                log.error(logHeader + "Given string " + givenMS + " isn't a MessageSize!");
                error = true;
            }
        }
        
        // NumAttribute
        if(!givenNA.equals("null"))
        {
            try
            {
                NumAttribute.valueOf(givenNA);
            }
            catch(Exception e)
            {
                log.error(logHeader + "Given string " + givenNA + " isn't a NumAttribute!");
                error = true;
            }
        }
        
        // AttributesRatio
        if(!givenAR.equals("null"))
        {
            try
            {
                AttributeRatio.valueOf(givenAR);
            }
            catch(Exception e)
            {
                log.error(logHeader + "Given string " + givenAR + " isn't an AttributeRatio!");
                error = true;
            }
        }
        
        if(!error)
        {
            retVal.put(AnalysisInput.BenchmarkNumber, givenBN);
            String cleanT = PSTBUtil.cleanTFS(givenT);
            retVal.put(AnalysisInput.TopologyFilePath, cleanT);
            retVal.put(AnalysisInput.DistributedFlag, givenDF);
            retVal.put(AnalysisInput.Protocol, givenP);
            retVal.put(AnalysisInput.PeriodLength, givenPL);
            retVal.put(AnalysisInput.MessageSize, givenMS);
            retVal.put(AnalysisInput.NumAttribute, givenNA);
            retVal.put(AnalysisInput.AttributeRatio, givenAR);
        }
        
        return retVal;
    }
    
    private boolean checkLineLength(int lineLength)
    {
        return lineLength == NUM_SEGMENTS_THROUGHPUT;
    }
}
