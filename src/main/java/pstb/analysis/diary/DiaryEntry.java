package pstb.analysis.diary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import pstb.startup.workload.PSActionType;
import pstb.util.PSTBUtil;
import pstb.util.PSTBUtil.TimeType;

/**
 * @author padres-dev-4187
 * 
 * A record of what happened with regards to a particular PSAction: 
 * When did we start this PSAction? How long did it take to finish? What are its attributes?
 */
public class DiaryEntry  implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;
    HashMap<DiaryHeader, Object> page;
    
    private final String logHeader = "DiaryEntry: ";
    
    /**
     * Empty Constructor
     */
    public DiaryEntry()
    {
        page = new HashMap<DiaryHeader, Object>();
    }
    
    /**
     * PSActionType setter
     * 
     * @param givenPSAT - the PSActionType to add
     */
    public void setPSActionType(PSActionType givenPSAT)
    {
        page.put(DiaryHeader.PSActionType, givenPSAT);
    }
    
    /**
     * TimeActionStarted setter
     * 
     * @param givenTAS - the time the action started
     */
    public void setTimeActionStarted(Long givenTAS)
    {
        page.put(DiaryHeader.TimeActionStarted, givenTAS);
    }
    
    /**
     * TimeFunctionReturned setter
     * 
     * @param givenTFR - the time the broker finished handling the action
     */
    public void setTimeFunctionReturned(Long givenTFR)
    {
        page.put(DiaryHeader.TimeFunctionReturned, givenTFR);
    }
    
    /**
     * Started Action setter
     * 
     * @param givenSA - the Time Started Action to add
     */
    public void addStartedAction(Long givenSA)
    {
        page.put(DiaryHeader.StartedAction, givenSA);
    }
    
    /**
     * Action Ended setter 
     * 
     * @param givenEA - the Action Ended to add
     */
    public void addEndedAction(Long givenEA)
    {
        page.put(DiaryHeader.EndedAction, givenEA);
    }
    
    /**
     * Action Delay setter
     * @param givenAD - the Action Delay to add
     */
    public void addActionDelay(Long givenAD)
    {
        page.put(DiaryHeader.ActionDelay, givenAD);
    }
    
    public void addMessageID(String givenMID)
    {
        page.put(DiaryHeader.MessageID, givenMID);
    }
    
    public void addAttributes(String givenA)
    {
        page.put(DiaryHeader.Attributes, givenA);
    }
    
    public void addPayloadSize(Integer givenPS)
    {
        page.put(DiaryHeader.PayloadSize, givenPS);
    }
    
    public void addTimeActiveStarted(Long givenTAS)
    {
        page.put(DiaryHeader.TimeActiveStarted, givenTAS);
    }
    
    public void addTimeActiveAck(Long givenTAA)
    {
        page.put(DiaryHeader.TimeActiveEnded, givenTAA);
    }
    
    public void addTimeCreated(Long givenTC)
    {
        page.put(DiaryHeader.TimeMessageCreated, givenTC);
    }
    
    public void addTimeReceived(Long givenTR)
    {
        page.put(DiaryHeader.TimeMessageReceived, givenTR);
    }
    
    public void addTimeDifference(Long givenTD)
    {
        page.put(DiaryHeader.MessageDelay, givenTD);
    }
    
    public void setRound(Integer givenRound)
    {
        page.put(DiaryHeader.Round, givenRound);
    }
    
    public void setMessageRate(Double givenMR) 
    {
        page.put(DiaryHeader.MessageRate, givenMR);
    }
    
    public void setRoundLatency(Double givenLatency)
    {    
        page.put(DiaryHeader.RoundLatency, givenLatency);
    }
    
    public void setMessagesReceievedRound(Integer givenMRR)
    {    
        page.put(DiaryHeader.MessagesReceievedRound, givenMRR);
    }
    
    public void setMessagesReceievedTotal(Integer givenMRT)
    {    
        page.put(DiaryHeader.MessagesReceievedTotal, givenMRT);
    }

    public void setCurrentThroughput(Double givenCT) {
        page.put(DiaryHeader.CurrentThroughput, givenCT);
    }
    
    public void setSecant(Double givenMT)
    {
        page.put(DiaryHeader.Secant, givenMT);
    }
    
    public void setAverageThroughput(Double givenAT)
    {
        page.put(DiaryHeader.AverageThroughput, givenAT);
    }
    
    public void setFinalThroughput(Double givenFT)
    {
        page.put(DiaryHeader.FinalThroughput, givenFT);
    }
    
    public void setY0(Double y0)
    {
        page.put(DiaryHeader.Y0, y0);
    }
    
    public void setY1(Double y1)
    {
        page.put(DiaryHeader.Y1, y1);
    }
    
    public void setX0(Double x0)
    {
        page.put(DiaryHeader.X0, x0);
    }
    
    public void setX1(Double x1)
    {
        page.put(DiaryHeader.X1, x1);
    }
    
    public void setCurrentRatio(Double givenCR)
    {
        page.put(DiaryHeader.CurrentRatio, givenCR);
    }
    
    public PSActionType getPSActionType()
    {
        return (PSActionType) page.get(DiaryHeader.PSActionType);
    }
    
    public Long getStartedAction()
    {
        return (Long) page.get(DiaryHeader.StartedAction);
    }
    
    public Long getEndedAction()
    {
        return (Long) page.get(DiaryHeader.EndedAction);
    }
    
    public Long getActionDelay()
    {
        return (Long) page.get(DiaryHeader.ActionDelay);
    }
    
    public String getMessageID()
    {
        return (String) page.get(DiaryHeader.MessageID);
    }
    
    public String getAttributes()
    {
        return (String) page.get(DiaryHeader.Attributes);
    }
    
    public Integer getPayloadSize()
    {
        return (Integer) page.get(DiaryHeader.PayloadSize);
    }
    
    public Long getTimeActiveStarted()
    {
        return (Long) page.get(DiaryHeader.TimeActiveStarted);
    }
    
    public Long getTimeActiveEnded()
    {
        return (Long) page.get(DiaryHeader.TimeActiveEnded);
    }
    
    public Long getTimeCreated()
    {
        return (Long) page.get(DiaryHeader.TimeMessageCreated);
    }
    
    public Long getTimeReceived()
    {
        return (Long) page.get(DiaryHeader.TimeMessageReceived);
    }
    
    public Long getMessageDelay()
    {
        return (Long) page.get(DiaryHeader.MessageDelay);
    }
    
    public Double getMessageRate()
    {
        return (Double) page.get(DiaryHeader.MessageRate);
    }
    
    public Double getRoundLatency()
    {    
        return (Double) page.get(DiaryHeader.RoundLatency);
    }
    
    public Double getCurrentThroughput()
    {
        return (Double) page.get(DiaryHeader.CurrentThroughput);
    }
    
    public Double getAverageThroughput()
    {
        return (Double) page.get(DiaryHeader.AverageThroughput);
    }
    
    public Double getSecant()
    {
        return (Double) page.get(DiaryHeader.Secant);
    }
    
    public Double getFinalThroughput()
    {
        return (Double) page.get(DiaryHeader.FinalThroughput);
    }
    
    public Double getY0()
    {
        return (Double) page.get(DiaryHeader.Y0);
    }
    
    public Double getY1()
    {
        return (Double) page.get(DiaryHeader.Y1);
    }
    
    public Double getX0()
    {
        return (Double) page.get(DiaryHeader.X0);
    }
    
    public Double getX1()
    {
        return (Double) page.get(DiaryHeader.X1);
    }
    
    public Double getCurrentRatio()
    {
        return (Double) page.get(DiaryHeader.CurrentRatio);
    }
    
    public boolean containsKey(Object value) 
    {
        return page.containsKey(value);
    }
    
    /**
     * Combs the Entry looking for a value
     * (Basically an extension of the HashMap<> containsValue()
     * @param value
     * @return true if it's in the Entry; false otherwise
     */
    public boolean containsValue(Object value) 
    {
        return page.containsValue(value);
    }
    
    public boolean recordPage(Path givenFilePath, Logger log)
    {
        if(!Files.exists(givenFilePath))
        {
            try 
            {
                Files.createFile(givenFilePath);
            } 
            catch (IOException e) 
            {
                log.error(logHeader + "Couldn't create a new file for this DiaryEntry: ", e);
                return false;
            }
        }
        
        try
        {
            page.forEach((header, data)->{
                /*
                 *  If we're looking at TimeActionStarted or TimeBrokerFinished, don't record the number value
                 *  Instead, convert that number into a date
                 */
                if(header.equals(DiaryHeader.TimeActionStarted) || header.equals(DiaryHeader.TimeFunctionReturned))
                {
                    Long convertedData = (Long) data;
                    if(convertedData != null)
                    {
                        data = PSTBUtil.DATE_FORMAT.format(convertedData);
                    }
                    else
                    {
                        throw new IllegalArgumentException("Converted data null for header" + header 
                                                                + " and data " + data);
                    }
                }
                
                String line = header + ": " + data;
                try
                {
                    Files.write(givenFilePath, line.getBytes(), StandardOpenOption.APPEND);
                }
                catch(IOException e)
                {
                    throw new IllegalArgumentException("IO failed at line " + line);
                }
                
                /*
                 * If we're looking at a delay value or a time started/ended value,
                 * Then let's convert it from a long number into something meaningful.
                 * Except for TimeStartedAction or TimeEndedAction - these values don't mean anything.
                 * 
                 * Why have both the original value and the converted value?
                 * That way a user can double check that the values are accurate, if they wish.
                 */
                if(header.equals(DiaryHeader.ActionDelay) 
                    || header.equals(DiaryHeader.MessageDelay)
                    || header.equals(DiaryHeader.TimeActiveStarted)
                    || header.equals(DiaryHeader.TimeActiveEnded)
                    || header.equals(DiaryHeader.TimeMessageCreated) 
                    || header.equals(DiaryHeader.TimeMessageReceived)
                    )
                {
                    Long convertedData = (Long) data;
                    if(convertedData != null)
                    {
                        String formatted = null;
                        
                        // All of these are time stamps in nanoseconds
                        if(header.equals(DiaryHeader.ActionDelay) 
                                || header.equals(DiaryHeader.TimeActiveStarted)
                                || header.equals(DiaryHeader.TimeActiveEnded)
                                )
                        {
                            formatted = PSTBUtil.createTimeString(convertedData, TimeType.Nano, TimeUnit.MILLISECONDS);
                        }
                        // This time stamp is in milliseconds
                        else if(header.equals(DiaryHeader.MessageDelay))
                        {
                            formatted = PSTBUtil.createTimeString(convertedData, TimeType.Milli, TimeUnit.MILLISECONDS);
                        }
                        // The other 2 (for now) are dates 
                        else
                        {
                            formatted = PSTBUtil.DATE_FORMAT.format(convertedData);
                        }
                        
                        line = " -> " + formatted;
                        try
                        {
                            Files.write(givenFilePath, line.getBytes(), StandardOpenOption.APPEND);
                        }
                        catch(IOException e)
                        {
                            throw new IllegalArgumentException("IO failed to add time data at line " + line);
                        }
                    }
                    else
                    {
                        throw new IllegalArgumentException("Converted data null at line " + line);
                    }
                }
                else if(header.equals(DiaryHeader.RoundLatency))
                {
                    Double convertedData = (Double) data;
                    if(convertedData != null)
                    {
                        convertedData *= PSTBUtil.MILLISEC_TO_NANOSEC;
                        String formatted = PSTBUtil.createTimeString(convertedData.longValue(), TimeType.Nano, TimeUnit.SECONDS);
                        
                        line = " -> " + formatted;
                        try
                        {
                            Files.write(givenFilePath, line.getBytes(), StandardOpenOption.APPEND);
                        }
                        catch(IOException e)
                        {
                            throw new IllegalArgumentException("IO failed to add time data at line " + line);
                        }
                    }
                    else
                    {
                        throw new IllegalArgumentException("Converted data null at line " + line);
                    }
                }
                
                line = "\n";
                try
                {
                    Files.write(givenFilePath, line.getBytes(), StandardOpenOption.APPEND);
                }
                catch(IOException e)
                {
                    throw new IllegalArgumentException("IO failed to add a newline at line " + line);
                }
            });
        }
        catch(IllegalArgumentException e)
        {
            log.error(logHeader + "Error writing to file: ", e);
            return false;
        }
        
        return true;
    }
}
