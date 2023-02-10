/**
 * @author padres-dev-4187
 *
 */
package pstb.startup.workload;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.startup.config.SupportedEngines;
import pstb.startup.config.SupportedEngines.PSEngine;
import pstb.util.PSTBUtil;

public class WorkloadFileParser {
    // Constants
    private final int MINSEGMENTSNUM = 3;
    private final int MAXSEGMENTSNUM = 4;
    private final int LOC_ACTION_DELAY = 0;
    private final int LOC_CLIENT_ACTION = 1;
    private final int LOC_ATTRIBUTES = 2;
    private final int LOC_PAYLOAD_TIME_ACTIVE = 3;
    
    private final int NO_PAYLOAD = 0;
    
    // Given
    private String workloadFileString;
    private ArrayList<PSEngine> requestedEngines;
    
    // Produced
    private ArrayList<PSAction> workloadP;
    private ArrayList<PSAction> workloadS;
    private Boolean throttle;
    
    private final String logHeader = "Workload Parser: ";
    private Logger logger = LogManager.getRootLogger();
    
    /**
     * Constructor
     * 
     * @param log - the Logger that is requested we log to
     */
    public WorkloadFileParser()
    {
        workloadFileString = new String();
        requestedEngines = new ArrayList<PSEngine>();
        
        workloadP = new ArrayList<PSAction>();
        workloadS = new ArrayList<PSAction>();
        
        throttle = null;
    }
    
    public WorkloadFileParser(String givenWorkloadFileString, ArrayList<PSEngine> givenEngines)
    {
        workloadFileString = givenWorkloadFileString;
        requestedEngines = givenEngines;
        
        workloadP = new ArrayList<PSAction>();
        workloadS = new ArrayList<PSAction>();
        
        throttle = null;
    }
    
    /**
     * Sets the WorkloadFileString
     * 
     * @param givenWFS - a String representation of the workload file's Path
     */
    public void setWorkloadFileString(String givenWFS)
    {
        workloadFileString = givenWFS;
    }
    
    /**
     * Sets the Requested Engines
     * 
     * @param givenRE - a ArrayList<> of the engines the user wishes to test
     */
    public void setRequestedEngines(ArrayList<PSEngine> givenRE)
    {
        requestedEngines = givenRE;
    }
    
    /**
     * Gets the workload as detailed by PADRES
     * 
     * @return the PADRESAction ArrayList
     */
    public ArrayList<PSAction> getPADRESWorkload()
    {
        return workloadP;
    }
    
    /**
     * Gets the workload as detailed by SIENA
     * 
     * @return theSIENAAction ArrayList
     */
    public ArrayList<PSAction> getSIENAWorkload()
    {
        return workloadS;
    }
    
    public Boolean isThrottle()
    {
        return throttle;
    }
    
    /**
     * Parses the Workload file as stated by the requestedWF
     * 
     * @param requestedWF - the type of Workload file that is to be parsed
     * @return false on failure; true otherwise
     */
    public boolean parse()
    {
        if(workloadFileString.isEmpty())
        {
            logger.error(logHeader + "No workload file was given!");
            return false;
        }
        if(requestedEngines.isEmpty())
        {
            logger.error(logHeader + "No engine information was given!");
            return false;
        }
        
        if(!SupportedEngines.checkProperWorkloadFileEndings(workloadFileString))
        {
            logger.error(logHeader + "Improper file extension on this workload file!");
            return false;
        }
        
        BufferedReader readerWF = null;
        String line = null;
        int linesRead = 0;
        try
        {
            readerWF = new BufferedReader(new FileReader(workloadFileString));
            
            while( (line = readerWF.readLine()) != null)
            {
                linesRead++;
                String[] splitLine = line.split("    ");
                if(!checkProperLength(splitLine))
                {
                    logger.error(logHeader + "line " + linesRead + " is not the proper length");
                    readerWF.close();
                    return false;
                }
                
                String actionDelayString = splitLine[LOC_ACTION_DELAY];
                Long actionDelay = PSTBUtil.checkIfLong(actionDelayString, false, null);
                if(actionDelay == null)
                {
                    logger.error(logHeader + "line " + linesRead + " has an incorrect action delay!");
                    readerWF.close();
                    return false;
                }
                
                PSActionType lineIsActionType = checkProperClientAction(splitLine[LOC_CLIENT_ACTION].toUpperCase());
                if(lineIsActionType == null)
                {
                    logger.error(logHeader + "line " + linesRead + " has an incorrect Client Action!");
                    readerWF.close();
                    return false;
                }
                
                if(splitLine.length != MAXSEGMENTSNUM)
                {
                    if(!addActionToWorkload(actionDelay, lineIsActionType, splitLine[LOC_ATTRIBUTES], 
                            Long.MAX_VALUE, NO_PAYLOAD))
                    {
                        logger.error(logHeader + "error adding " + linesRead + " to the workload!");
                        readerWF.close();
                        return false;
                    }
                }
                
                Long timeActive = null; 
                Integer payloadSize = null;
                if(lineIsActionType.equals(PSActionType.P))
                {
                    payloadSize = PSTBUtil.checkIfInteger(splitLine[LOC_PAYLOAD_TIME_ACTIVE], false, null);
                    if(payloadSize == null)
                    {
                        logger.error(logHeader + "line " + linesRead + " has an incorrect payload size!");
                        readerWF.close();
                        return false;
                    }
                }
                else if(lineIsActionType.equals(PSActionType.A) || lineIsActionType.equals(PSActionType.S))
                {
                    timeActive = PSTBUtil.checkIfLong(splitLine[LOC_PAYLOAD_TIME_ACTIVE], false, null);
                    if(timeActive == null)
                    {
                        logger.error(logHeader + "line " + linesRead + " has an incorrect time active value!");
                        readerWF.close();
                        return false;
                    }
                }
                else
                {
                    logger.warn(logHeader + "line " + linesRead + " shouldn't have a third column - ignoring it.");
                }
                
                if(!addActionToWorkload(actionDelay, lineIsActionType, splitLine[LOC_ATTRIBUTES], 
                        timeActive, payloadSize))
                {
                    logger.error(logHeader + "error adding " + linesRead + " to the workload!");
                    readerWF.close();
                    return false;
                }
            }
            readerWF.close();
        }
        catch (IOException e) 
        {
            logger.error(logHeader + "Cannot find file: ", e);
            return false;
        }
        
        return true;
    }
    
    /**
     * Determines if the given line has been properly written
     * - i.e. contains a client action, attributes 
     * and either a payload size or a time active - 
     * by seeing if the split line has the right number of segments
     * 
     * @param splitFileLine - the split line
     * @return true if it does; false otherwise
     */
    private boolean checkProperLength(String[] splitFileline)
    {
        return (splitFileline.length == MINSEGMENTSNUM) || (splitFileline.length == MAXSEGMENTSNUM);
    }
    
    /**
     * Determines if the given client action is a both client action
     * and a client action that makes sense for the Client
     * i.e. a publisher doesn't have any subscribe requests
     * 
     * @param supposedClientAction - the Client Action being tested
     * @param fileType - the type of file
     * @return null on failure; the given Client Action otherwise
     */
    private PSActionType checkProperClientAction(String supposedClientAction)
    {
        PSActionType test = null;
        try 
        {
            test = PSActionType.valueOf(supposedClientAction);
        }
        catch(IllegalArgumentException e)
        {
            logger.error(logHeader + supposedClientAction + " is not a valid Client Action.", e);
            return null;
        }
        
        if(test.equals(PSActionType.R))
        {
            logger.error(logHeader + supposedClientAction + " is a Client Action that should not be "
                    + "submitted by the user.");
            return null;
        }
        
        return test;
    }
    
    /**
     * Adds and action to the Workload
     * 
     * @param actionDelay - the desired delay
     * @param actionType - the desired PSActionType
     * @param attributes - the desired attributes
     * @param payloadOrTA - the desired Payload/TimeActive
     * @return false on error; true otherwise
     */
    private boolean addActionToWorkload(Long actionDelay, PSActionType actionType, String attributes, Long timeActive, Integer payloadSize)
    {
        PSAction newAction = new PSAction();
        
        newAction.setActionDelay(actionDelay);
        newAction.setActionType(actionType);
        
        if(actionType.equals(PSActionType.A) || actionType.equals(PSActionType.S))
        {
            if(timeActive != null)
            {
                if(timeActive < (Long.MAX_VALUE / PSTBUtil.MILLISEC_TO_NANOSEC))
                {
                    timeActive *= PSTBUtil.MILLISEC_TO_NANOSEC;
                }
                newAction.setTimeActive(timeActive);
            }
            else
            {
                logger.error(logHeader + "given Time Active value is null when it shouldn't be!");
                return false;
            }
        }
        else if(actionType.equals(PSActionType.P))
        {
            if(payloadSize != null)
            {
                newAction.setPayloadSize(payloadSize);
            }
            else
            {
                logger.error(logHeader + "given Payload Size value is null when it shouldn't be!");
                return false;
            }
        }
        
        String fileType = PSTBUtil.getFileExtension(workloadFileString, false);
        if(requestedEngines.contains(PSEngine.PADRES))
        {
            if(fileType.equals(SupportedEngines.WORKLOAD_FILE_TYPE_PADRES))
            {
                newAction.setAttributes(attributes);
            }
            else if(fileType.equals(SupportedEngines.WORKLOAD_FILE_TYPE_SIENA))
            {
                String convertedAttributes = SupportedEngines.convertSIENAAttributesToPADRES(attributes);
                if(convertedAttributes == null)
                {
                    logger.error(logHeader + "This SIENA file contains an operator not supported by PADRES!");
                    return false;
                }
                newAction.setAttributes(convertedAttributes);
                
            }
            else
            {
                // ignore for now
            }
            
            workloadP.add(newAction);
        }
        
        if(requestedEngines.contains(PSEngine.SIENA))
        {
            if(fileType.equals(SupportedEngines.WORKLOAD_FILE_TYPE_PADRES))
            {
                String properAttributes = SupportedEngines.convertPADRESAttributesToSIENA(attributes);
                newAction.setAttributes(properAttributes);
            }
            else if(fileType.equals(SupportedEngines.WORKLOAD_FILE_TYPE_SIENA))
            {
                newAction.setAttributes(attributes);
            }
            else
            {
                // ignore for now
            }
            
            workloadS.add(newAction);
        }
        
        return true;
    }
    
}
