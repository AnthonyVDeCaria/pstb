package pstb.benchmark.object.client;

import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;

import pstb.analysis.diary.ClientDiary;
import pstb.analysis.diary.DiaryEntry;
import pstb.benchmark.object.PSNode;
import pstb.benchmark.process.client.PSTBClientProcess;
import pstb.startup.config.ExperimentType;
import pstb.startup.config.MessageSize;
import pstb.startup.workload.PSAction;
import pstb.startup.workload.PSActionType;
import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 * 
 * The Base Client Class. 
 * It contains all the functionality all clients need. 
 * Such as - handling the broker URIs, detailing how a run is executed, ...
 * except how Advertising, Unadvertising, Subscribing, Unsubscribing, Publishing are actually implemented. 
 * That is handled by child classes, as they all have their own functions to handle these actions.
 */
public abstract class PSClient extends PSNode
{
    // Constants
    protected static final long serialVersionUID = 1L;
    protected final long MIN_RUNLENGTH = 1;
    protected final long TP_RUN_COMPLETE = -1;
    
    // Pre-set variables
    protected ReentrantLock diaryLock = new ReentrantLock();
    protected ReentrantLock runningLock = new ReentrantLock();
    
    // Variables needed by user to run experiment
    protected ArrayList<String> brokersURIs;
    protected ArrayList<PSAction> workload;
    protected PSClientMode cMode;
    protected Integer numPubs;
    
    // Varaibles needed to run Throughput experiment
    private String masterIPAddress;
    private Integer portNumber;
    private Long messageDelay;
    private Integer messagesReceived;
    private Double roundLatency;
    
    // Variables set during experiment
    protected Boolean currentlyRunning;
    
    // Output Variables
    protected ClientDiary diary;
    
    /**
     * Empty Constructor
     */
    public PSClient()
    {
        super();
        
        brokersURIs = null;
        workload = null;
        cMode = null;
        
        numPubs = null;
        masterIPAddress = null;
        portNumber = null;
        messageDelay = null;
        messagesReceived = new Integer(0);
        roundLatency = new Double(0.0);
        
        currentlyRunning = null;
        
        diary = new ClientDiary();
        
        nodeLog = LogManager.getLogger(PSTBClientProcess.class);
    }
    
    /**
     * Sets a list of the Brokers this client is connected to
     * (By which I mean their URIs)
     * 
     * @param givenBrokersURIs
     */
    public void setBrokersURIs(ArrayList<String> givenBrokersURIs)
    {
        brokersURIs = givenBrokersURIs;
    }
    
    /**
     * Sets the Client workload
     * 
     * @param clientIWorkload - the given Workload
     */
    public void setWorkload(ArrayList<PSAction> clientIWorkload)
    {
        workload = clientIWorkload;
    }
    
    public void setClientMode(PSClientMode givenMode)
    {
        cMode = givenMode;
    }
    
    public void setNumPubs(Integer givenNP)
    {
        numPubs = givenNP;
    }
    
    public void setMIP(String givenIP)
    {
        masterIPAddress = givenIP;
    }
    
    public void setMasterPort(Integer givenPort)
    {
        portNumber = givenPort;
    }
    
    public void startCR()
    {
        runningLock.lock();
        try
        {
            currentlyRunning = new Boolean(true);
        }
        finally
        {
            runningLock.unlock();
        }
    }
    
    public void stopCR()
    {
        runningLock.lock();
        try
        {
            currentlyRunning = new Boolean(false);
        }
        finally
        {
            runningLock.unlock();
        }
    }
    
    public Boolean getCurrentlyRunning()
    {
        Boolean retVal = null;
        runningLock.lock();
        try
        {
            retVal = currentlyRunning;
        }
        finally
        {
            runningLock.unlock();
        }
        
        return retVal; 
    }
    
    /**
     * Gets the URIs of the Broker(s) this Client is connected to 
     * 
     * @return a list of the URIs
     */
    public ArrayList<String> getBrokersURIs()
    {
        return brokersURIs;
    }
    
    /**
     * Get this Client's diary
     * AKA the notes its made of all PSActions
     * 
     * @return its Diary
     */
    public ClientDiary getDiary()
    {
        ClientDiary retVal = null;
        diaryLock.lock();
        try
        {
            retVal = diary;
        }
        finally
        {
            diaryLock.unlock();
        }
        
        return retVal;
    }
    
    public PSClientMode getClientMode()
    {
        return cMode;
    }
    
    /**
     * Begins the run for the client
     * @return false if there are any errors; true otherwise
     */
    public boolean startRun()
    {
        // Check if we have everything
        if(!variableCheck())
        {
            nodeLog.error(logHeader + "Missing some components!");
            return false;
        }
        // We do
        
        PSTBUtil.synchronizeRun();
        
        startCR();
        
        boolean runCheck = false;
        if(cMode.equals(PSClientMode.Scenario))
        {
            nodeLog.info(logHeader + "Starting scenario run...");
            runCheck = normalRun();
        }
        else
        {
            nodeLog.info(logHeader + "Starting throughput run...");
            runCheck = throughputRun();
        }
        
        stopCR();
        
        return runCheck;
    }
    
    protected boolean variableCheck()
    {
        boolean everythingPresent = true;
        
        everythingPresent = contextVariableCheck();
        if(!everythingPresent)
        {
            return false;
        }
        
        if(brokersURIs == null)
        {
            nodeLog.error("No broker URI was given!");
            everythingPresent = false;
        }
        
        if(cMode == null)
        {
            nodeLog.error("No PSClientMode was was given!");
            everythingPresent = false;
        }
        else if(cMode.equals(PSClientMode.Scenario))
        {
            if(!mode.equals(ExperimentType.Scenario))
            {
                nodeLog.error("Mode mismatch - Client Mode is Scenario but Benchmark Mode isn't!");
                everythingPresent = false;
            }
            
            if(workload == null)
            {
                nodeLog.error("No workload was given!");
                everythingPresent = false;
            }
        }
        else if((cMode.equals(PSClientMode.TPPub) || (cMode.equals(PSClientMode.TPSub))) && !mode.equals(ExperimentType.Throughput) )
        {
            nodeLog.error("Mode mismatch!");
            everythingPresent = false;
        }
        
        return everythingPresent;
    }
    
    private boolean normalRun() 
    {
        ArrayList<PSAction> activeList = new ArrayList<PSAction>();
        int numActions = workload.size();
        
        Long pauseTime = runLength / 10;
        
        int i = 0;
        Long runStart = System.nanoTime();
        Long currentTime = System.nanoTime();
        while( (currentTime - runStart) < runLength)
        {
            nodeLog.info(logHeader + "Updating active lists...");
            updateActiveList(activeList);
            nodeLog.info(logHeader + "Updated active lists.");
            
            if(i >= numActions)
            {
                PSTBUtil.waitAPeriod(pauseTime, nodeLog, (logHeader + "resting for " + pauseTime + "..."));
            }
            else
            {
                PSAction actionI = workload.get(i);
                PSActionType actionIsActionType = actionI.getActionType();
                
                nodeLog.debug(logHeader + "Attempting to send " + actionIsActionType + " " + actionI.getAttributes() + ".");
                boolean checkPublication = launchAction(actionIsActionType, actionI);
                if(!checkPublication)
                {
                    nodeLog.error(logHeader + "launch failed!");
                    cleanup(activeList);
                    return false;
                }
                
                if(actionIsActionType.equals(PSActionType.A) || actionIsActionType.equals(PSActionType.S))
                {
                    activeList.add(actionI);
                }
                else if(actionIsActionType.equals(PSActionType.V) || actionIsActionType.equals(PSActionType.U))
                {
                    String actionIAttr = actionI.getAttributes();
                    int j = 0, numActiveActions = activeList.size();
                    
                    while(j < numActiveActions)
                    {
                        PSAction actionJ = activeList.get(j);
                        String actionJAttr = actionJ.getAttributes();
                        PSActionType actionJType = actionJ.getActionType();
                        if(actionJAttr.equals(actionIAttr))
                        {
                            if(
                                    (actionJType.equals(PSActionType.A) && actionIsActionType.equals(PSActionType.V))
                                    || (actionJType.equals(PSActionType.S) && actionIsActionType.equals(PSActionType.U))
                                )
                            {
                                activeList.remove(j);
                                break;
                            }
                        }
                        j++;
                    }
                }
                
                i++;
            }
            
            currentTime = System.nanoTime();
        }

        boolean cleanupCheck = cleanup(activeList);
        if(!cleanupCheck)
        {
            nodeLog.error(logHeader + "Couldn't end run cleanly!");
            return false;
        }
        else
        {
            nodeLog.info(logHeader + "Run complete.");
            return true;
        }
    }
    
    /**
     * Updates the givenActiveList
     * I.e. checks every node and sees if it's still active or not
     * by looking at its start and exist time
     * 
     * @param givenActiveList - the ActiveList to look over
     * @return false on any error; true if successful
     */
    private boolean updateActiveList(ArrayList<PSAction> givenActiveList)
    {    
        int numActiveActions = givenActiveList.size();
        ArrayList<Integer> nodesToRemove = new ArrayList<Integer>();
        
        /*
         * updateActiveList Pseudocode
         * 
         * For each member of the givenActiveList
         *     Get the iTH one - itself, its PSActionType and its Attributes
         *     Get the currentTime using System.nanoTime()
         *     Get the Diary Entry associated with this Action - the one that was created when it was launched
         *         Can't find it?
         *             Error
         *         Found it?
         *             Get the System.nanoTime() value associated with starting it (Currently that's getStartedAction())
         *             If the Difference between the currentTime and the "start Time" is >= the time it should have been active
         *                 Launch the associated undo action - U or V (assuming that the offending Action is a S or A)
         *                     Undo Successful?
         *                         Add i to an ArrayList<Integer>
         *                     Undo Unsuccessful?
         *                         Error
         * 
         * After this is complete
         *     Loop through the new ArrayList<Integer>
         *         Remove the jTH Action from givenActiveList
         */
        for(int i = 0 ; i < numActiveActions ; i++)
        {
            PSAction activeActionI = givenActiveList.get(i);
            PSActionType aAIActionType = activeActionI.getActionType();
            String aAIAttributes = activeActionI.getAttributes();
            nodeLog.trace(logHeader + "Accessing entry " + aAIAttributes + ".");
            
            Long currentTime = System.nanoTime();
            
            DiaryEntry activeActionIEntry = diary.getDiaryEntryGivenActionTypeNAttributes(aAIActionType, aAIAttributes, nodeLog);
            if(activeActionIEntry == null)
            {
                nodeLog.error(logHeader + "Couldn't find " + activeActionI.getActionType() + " " + activeActionI.getAttributes() 
                                        + "'s diary entry!");
                return false;
            }
            
            Long activeActionIStartTime = activeActionIEntry.getStartedAction();
            
            nodeLog.trace(logHeader + "Delay is = " + (currentTime - activeActionIStartTime) 
                            + " TA is = " + activeActionI.getTimeActive());
            
            if((currentTime - activeActionIStartTime) >= activeActionI.getTimeActive())
            {
                boolean check = true;
                if(activeActionI.getActionType() == PSActionType.S)
                {
                    check = launchAction(PSActionType.U, activeActionI);
                }
                else if(activeActionI.getActionType() == PSActionType.A)
                {
                    check = launchAction(PSActionType.V, activeActionI);
                }
                else
                {
                    nodeLog.error(logHeader + "improper active list given!");
                    return false;
                }
                
                if(!check)
                {
                    nodeLog.error(logHeader + "Error ending " + activeActionI.getActionType() + " " + activeActionI.getAttributes());
                    return false;
                }
                else
                {
                    nodesToRemove.add(i);
                }
            }
        }
        
        for(int i = 0 ; i < nodesToRemove.size() ; i++)
        {
            int j = nodesToRemove.get(i);
            PSAction inactionActionJ = givenActiveList.get(j);
            
            nodeLog.debug(logHeader + "Removing " + inactionActionJ.getActionType().toString() + " " + inactionActionJ.getAttributes()
                            + " from ActiveList...");
            givenActiveList.remove(j);
            nodeLog.debug(logHeader + "Remove successful.");
        }
        
        nodeLog.trace(logHeader + "Update complete.");
        return true;
    }
    
    /**
     * Unadvertises and Unsubscribes any active Ads or Subs.
     * 
     * @param activeList - the list of all remaining Ads and Subs
     * @return false on failure; true otherwise
     */
    private boolean cleanup(ArrayList<PSAction> activeList)
    {
        int sizeAL = activeList.size();
        nodeLog.debug(logHeader + "Undoing " + sizeAL + " 'infinite' actions..."); 
        for(int i = 0 ; i < sizeAL ; i++)
        {
            PSAction activeActionI = activeList.get(i);
            
            if(activeActionI.getActionType().equals(PSActionType.A))
            {
                boolean check = launchAction(PSActionType.V, activeActionI);
                if(!check)
                {
                    nodeLog.error(logHeader + "Couldn't unadvertize " + activeActionI.getAttributes() + "!");
                    return false;
                }
            }
            else if(activeActionI.getActionType().equals(PSActionType.S))
            {
                boolean check = launchAction(PSActionType.U, activeActionI);
                if(!check)
                {
                    nodeLog.error(logHeader + "Couldn't unsubscribe " + activeActionI.getAttributes() + "!");
                    return false;
                }
            }
            else
            {
                nodeLog.error(logHeader + "ActiveList contains a improper PSAction!");
                return false;
            }
        }
        return true;
    }
    
    private boolean throughputRun()
    {
        if(masterIPAddress == null || portNumber == null)
        {
            nodeLog.error(logHeader + "Missing the components needed to make a Socket!");
            return false;
        }
        
        PSActionType firstActionType = null;
        Long delay = 1000L;
        if(cMode.equals(PSClientMode.TPPub))
        {
            PSTBUtil.waitAPeriod((long)(0.5 * PSTBUtil.SEC_TO_NANOSEC), nodeLog, logHeader);
            firstActionType = PSActionType.A;
        }
        else
        {
            firstActionType = PSActionType.S;
        }
        String attri = generateThroughputAttributes(firstActionType, 0);
        PSAction firstAction = new PSAction(firstActionType, delay, attri, 0, Long.MAX_VALUE);
        boolean firstActionCheck = launchAction(firstActionType, firstAction);
        if(!firstActionCheck)
        {
            nodeLog.error(logHeader + "Couldn't send the first action!");
            return false;
        }
        
        int i = 0, periodNum = 0;
        Boolean stillRunning = true;
        while(stillRunning)
        {
            stillRunning = handleMaster(messagesReceived, roundLatency);
            if(stillRunning == null)
            {
                nodeLog.error(logHeader + "Couldn't get new pubDelay!");
                return false;
            }
            else if(stillRunning)
            {
                Long roundStart = System.nanoTime();
                Long currentTime = System.nanoTime();
                while((currentTime - roundStart) < periodLength)
                {
                    boolean checkDPA = duringPeriodAction(i);
                    if(!checkDPA)
                    {
                        return false;
                    }
                    i++;
                    currentTime = System.nanoTime();
                }
                nodeLog.info(logHeader + "Round " + periodNum + " complete.");
                boolean checkAPA = afterPeriodAction(periodNum);
                if(!checkAPA)
                {
                    return false;
                }
                
                periodNum++;
            }
            else
            {    
//                PSActionType undoActionType = null;
//                if(cMode.equals(PSClientMode.TPPub))
//                {
//                    undoActionType = PSActionType.V;
//                    boolean undoFirstActionCheck = launchAction(undoActionType, firstAction);
//                    if(!undoFirstActionCheck)
//                    {
//                        nodeLog.error(logHeader + "Couldn't undo the first action!");
//                        return false;
//                    }
//                    else
//                    {
//                        nodeLog.info(logHeader + "Undo complete.");
//                    }                    
//                }
//                else if(cMode.equals(PSClientMode.TPSub))
//                {
//                    undoActionType = PSActionType.U;
//                }
//                boolean undoFirstActionCheck = launchAction(undoActionType, firstAction);
//                if(!undoFirstActionCheck)
//                {
//                    nodeLog.error(logHeader + "Couldn't undo the first action!");
//                    return false;
//                }
//                else
//                {
//                    nodeLog.info(logHeader + "Undo complete.");
//                }
                
                nodeLog.debug(logHeader + "Throughput experiment complete.");
            }
        }
        
        return true;
    }
    
    private boolean duringPeriodAction(int i)
    {
        if(cMode.equals(PSClientMode.TPPub))
        {
            String pubAttribute = generateThroughputAttributes(PSActionType.P, i);
            
            Integer payloadSize = 0;
            if(ms.equals(MessageSize.TenKilobytes))
            {
                payloadSize = 1000;
            }
            else if(ms.equals(MessageSize.OneHundredKilobytes))
            {
                payloadSize = 100000;
            }
            
            PSAction pubI = new PSAction(PSActionType.P, messageDelay, pubAttribute, payloadSize, 0L);
            
            boolean pubISendCheck = launchAction(PSActionType.P, pubI);
            if(!pubISendCheck)
            {
                nodeLog.error(logHeader + "Couldn't send pub " + i + "!");
                return false;
            }
        }
        
        return true;
    }
    
    private boolean afterPeriodAction(int givenPN)
    {
        if(cMode.equals(PSClientMode.TPSub))
        {
            int counter = 0;
            double delay = 0.0;
            
            ClientDiary currentDiary = null;
            diaryLock.lock();
            try
            {
                currentDiary = diary;
            }
            finally
            {
                diaryLock.unlock();
            }
            
            for(int j = 0 ; j < currentDiary.size() ; j++)
            {
                DiaryEntry pageI = currentDiary.getDiaryEntryI(j);
                PSActionType pageIsPSAT = pageI.getPSActionType();
                if(pageIsPSAT != null && pageIsPSAT.equals(PSActionType.R))
                {
                    Double delayI = pageI.getMessageDelay().doubleValue();
                    nodeLog.debug(logHeader + "delayI = " + delayI + " | counter = " + counter + ".");
                    delay += delayI;
                    counter++;
                }
            }
            
            if(counter == 0)
            {
                nodeLog.warn(logHeader + "No messages received.");
            }
            else
            {
                roundLatency = delay / counter;
                messagesReceived += counter;
                nodeLog.info(logHeader + "RoundLatency = " + roundLatency + " | MessagesReceived = " + messagesReceived + ".");
            }
            
            Double secondsPubPerMessage = messageDelay.doubleValue() / PSTBUtil.SEC_TO_NANOSEC;
            Double messagesPerSecondPub = 1 / secondsPubPerMessage;
            Double messageRate = messagesPerSecondPub * numPubs;

            diaryLock.lock();
            try
            {
                diary.removeDiaryEntiresWithGivenPSActionType(PSActionType.R);
                DiaryEntry delayEntry = new DiaryEntry();
                delayEntry.setRound(givenPN);
                delayEntry.setMessageRate(messageRate);
                delayEntry.setRoundLatency(roundLatency);
                delayEntry.setMessagesReceievedRound(counter);
                delayEntry.setMessagesReceievedTotal(messagesReceived);
                diary.addDiaryEntryToDiary(delayEntry);
            }
            finally
            {
                diaryLock.unlock();
            }
        }
        
        return true;
    }
    
    private Boolean handleMaster(Integer givenMR, Double givenDelay)
    {
        nodeLog.debug(logHeader + "Waiting for master...");
        Long masterValue = connectToMaster(givenMR, givenDelay);
        nodeLog.debug(logHeader + "Wait complete.");
        
        if(masterValue == null)
        {
            nodeLog.error(logHeader + "major error waiting for master!");
            return null;
        }
        else if(masterValue == TP_RUN_COMPLETE)
        {
            nodeLog.info(logHeader + "Run complete.");
            return false;
        }
        else
        {
            messageDelay = masterValue;
            nodeLog.info(logHeader + "Continuing...");
            return true;
        }
    }
    
    private Long connectToMaster(Integer givenMR, Double givenDelay)
    {
        Long retVal = null;
        
        nodeLog.debug(logHeader + "Attempting to connect to Throughput Master...");
        Socket throughputMasterConnection = null;
        try
        {
            throughputMasterConnection = new Socket(masterIPAddress, portNumber);
        }
        catch (Exception e) 
        {
            nodeLog.error(logHeader + "error creating a new Socket: ", e);
            return null;
        }
        nodeLog.debug(logHeader + "Connected to Throughput Master.");
        
        OutputStream toMaster = null;
        try
        {
            toMaster = throughputMasterConnection.getOutputStream();
        }
        catch (Exception e) 
        {
            nodeLog.error(logHeader + "error creating a new OutputStream: ", e);
        }
        
        if(toMaster != null)
        {    
            String delay = nodeName + "_" + givenMR + "_" + givenDelay; 
            
            nodeLog.debug(logHeader + "Attempting to send delay to master...");
            PSTBUtil.sendStringAcrossSocket(toMaster, delay);
            nodeLog.debug(logHeader + "Delay sent.");
            
            String receivedMessage = PSTBUtil.readConnection(throughputMasterConnection, nodeLog, logHeader);
            nodeLog.info(logHeader + receivedMessage + " received.");
            
            if(receivedMessage == null)
            {
                nodeLog.error(logHeader + "Something happened with master - got null!");
            }
            else
            {
                retVal = PSTBUtil.checkIfLong(receivedMessage, false, null);
                if(retVal == null)
                {
                    if(receivedMessage.equals(PSTBUtil.STOP))
                    {
                        retVal = new Long(TP_RUN_COMPLETE);
                    }
                }
            }
        }

        try 
        {
            throughputMasterConnection.close();
        } 
        catch (Exception e) 
        {
            nodeLog.error(logHeader + "couldn't close " + nodeName + "'s socket: ", e);
            return null;
        }
        nodeLog.debug(logHeader + "Connection to master closed.");
        
        return retVal;
    }
    
    protected abstract String generateThroughputAttributes(PSActionType givenPSAT, int messageNumber);
    
    /**
     * Create a new diary entry and record all the information associated with it
     * e.g. TimeStartedAction, AckDelay, TimeActiveAck, ...
     * assuming executeAction runs successfully.
     * @see executeAction
     * 
     * @param selectedAction - the type of action we're doing
     * @param givenAction - the given Action
     * @return true if the action was recorded; false on error
     */
    public boolean launchAction(PSActionType selectedAction, PSAction givenAction)
    {
        nodeLog.debug(logHeader + "Preparing to record " + selectedAction + " " + givenAction.getAttributes() + ".");
        
        /*
         *  Make sure our inputs are proper
         */
        // The only time the ActionTypes should differ is if we want to Unad / Unsub and have the original action
        if(selectedAction != givenAction.getActionType())
        {
            if(selectedAction == PSActionType.V && givenAction.getActionType() == PSActionType.A)
            {
                nodeLog.trace(logHeader + "Attempting to unadvertise.");
            }
            else if(selectedAction == PSActionType.U && givenAction.getActionType() == PSActionType.S)
            {
                nodeLog.trace(logHeader + "Attempting to unsubscribe.");
            }
            else
            {
                nodeLog.error(logHeader + "givenAction and selectedAction are different!");
                return false;
            }
        }
        // This function should not be used for the R(eceived) ActionType
        // That should be limited to storePublication
        if(selectedAction.equals(PSActionType.R))
        {
            nodeLog.error(logHeader + "launchAction doesn't handle Received publications!");
            return false;
        }
        
        Long delayValue = givenAction.getActionDelay();
        if(delayValue == null)
        {
            nodeLog.error(logHeader + "givenAction has a null delay!");
            return false;
        }
        
        // Variable initialization - DiaryEntry
        DiaryEntry thisEntry = new DiaryEntry();
        
        // Execute the action... after getting some number's first
        Long startTime = System.currentTimeMillis();
        Long startAction = System.nanoTime();
        boolean actionSuccessful = executeAction(selectedAction, givenAction, thisEntry);
        Long brokerFinished = System.currentTimeMillis();
        Long endAction = System.nanoTime();
        
        if(actionSuccessful)
        {
            // Get a few missing recordings
            Long timeDiff = endAction - startAction;
            String attributes = givenAction.getAttributes();
            
            // Add all recordings to DiaryEntry
            thisEntry.setPSActionType(selectedAction);
            thisEntry.addStartedAction(startAction);
            thisEntry.addEndedAction(endAction);
            thisEntry.addActionDelay(timeDiff);
            thisEntry.setTimeActionStarted(startTime);
            thisEntry.setTimeFunctionReturned(brokerFinished);
            thisEntry.addAttributes(attributes);
            
            if(selectedAction.equals(PSActionType.U) || selectedAction.equals(PSActionType.V))
            {
                // If we're unsubscribing / unadvertising, we need to determine how long they were active for
                // So we need to find the associated subscribe / advertise
                DiaryEntry ascAction = null;
                
                if(selectedAction.equals(PSActionType.U))
                {
                    ascAction = diary.getDiaryEntryGivenActionTypeNAttributes(PSActionType.S, attributes, nodeLog);
                }
                else // I can do this cause I limited the options above
                {
                    ascAction = diary.getDiaryEntryGivenActionTypeNAttributes(PSActionType.A, attributes, nodeLog);
                }
                
                if (ascAction == null)
                {
                    nodeLog.error(logHeader + "Couldn't find associated action to " + selectedAction + " " + attributes);
                    return false;
                }
                else
                {
                    Long aAStarted = ascAction.getStartedAction();
                    Long aAAcked = ascAction.getEndedAction();
                    
                    Long startedDif = startAction - aAStarted;
                    Long ackedDif = endAction - aAAcked;
                    
                    thisEntry.addTimeActiveStarted(startedDif);
                    thisEntry.addTimeActiveAck(ackedDif);
                }
            }
            
            // Add the entry to the diary
            // However, since the diary is being used by both the sending thread and the listening thread, we need a lock
            diaryLock.lock();
            try
            {
                diary.addDiaryEntryToDiary(thisEntry);
            }
            finally
            {
                diaryLock.unlock();
            }
            nodeLog.debug(logHeader + selectedAction + " " + attributes + " recorded.");
            
            PSTBUtil.waitAPeriod(delayValue, nodeLog, logHeader);
        }
        
        return actionSuccessful;
    }
    
    /**
     * Handles calling the particular PADRES function associated with a given action.
     * I.e. if the action is an advertisement, it would call the advertise function
     * 
     * @param givenAction - the Action itself
     * @return the proper message on success; null on failure
     */
    private boolean executeAction(PSActionType selectedAction, PSAction givenAction, DiaryEntry givenEntry) 
    {
        String generalLog = "Attempting to ";
        String givenAttributes = givenAction.getAttributes();
        boolean successfulExecution = true;
        
        try
        {
            switch(selectedAction)
            {
                case A:
                {
                    nodeLog.debug(logHeader + generalLog + "advertize " + givenAttributes);
                    advertise(givenAttributes, givenEntry);
                    break;
                }
                case V:
                {
                    nodeLog.debug(logHeader + generalLog + "unadvertize " + givenAttributes + ".");
                    unadvertise(givenAttributes, givenEntry);
                    break;
                }
                case S:
                {
                    nodeLog.debug(logHeader + generalLog + "subscribe to " + givenAttributes + ".");
                    subscribe(givenAttributes, givenEntry);
                    break;
                }
                case U:
                {
                    nodeLog.debug(logHeader + generalLog + "unsubscribe from " + givenAttributes + ".");
                    unsubscribe(givenAttributes, givenEntry);
                    break;
                }
                case P:
                {
                    nodeLog.debug(logHeader + generalLog + "publish " + givenAttributes);
                    publish(givenAttributes, givenEntry, givenAction.getPayloadSize());
                    break;
                }
                default:
                    break;
            }
        }
        catch(Exception e)
        {
            nodeLog.error(logHeader + "Issue executing action " + selectedAction + " " + givenAttributes + ": ");
            successfulExecution = false;
        }
        
        return successfulExecution;
    }
    
    protected abstract void advertise(String givenAttributes, DiaryEntry resultingEntry) throws Exception;
    
    protected abstract void unadvertise(String givenAttributes, DiaryEntry resultingEntry) throws Exception;

    protected abstract void subscribe(String givenAttributes, DiaryEntry resultingEntry) throws Exception;
                      
    protected abstract void unsubscribe(String givenAttributes, DiaryEntry resultingEntry) throws Exception;
                       
    protected abstract void publish(String givenAttributes, DiaryEntry resultingEntry, Integer givenPayLoadSize) throws Exception;

}
