/**
 * 
 */
package pstb.benchmark.object.client;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;

import pstb.analysis.diary.ClientDiary;
import pstb.analysis.diary.DiaryEntry;
import pstb.benchmark.object.PSNode;
import pstb.benchmark.process.client.PSTBClientProcess;
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
	
	// Pre-set variables
	protected ReentrantLock diaryLock = new ReentrantLock();
	
	// Variables needed by user to run experiment
	protected ArrayList<String> brokersURIs;
	protected ArrayList<PSAction> workload;
	
	// Variables set on creation
	private long defaultDelay;
	
	// Output Variables
	protected ClientDiary diary;
	
	/**
	 * Empty Constructor
	 */
	public PSClient()
	{
		super();
		
		brokersURIs = new ArrayList<String>();
		workload = new ArrayList<PSAction>();
		
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
		return diary;
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
		
		defaultDelay = runLength / 10 / PSTBUtil.MILLISEC_TO_NANOSEC;
		ArrayList<PSAction> activeList = new ArrayList<PSAction>();
		
		PSTBUtil.synchronizeRun();
		
		nodeLog.info(logHeader + "Starting run...");
		int i = 0;
		Long runStart = System.nanoTime();
		Long currentTime = System.nanoTime();
		while( (currentTime - runStart) < runLength)
		{
			Long delayValue = new Long(defaultDelay);
			
			nodeLog.info(logHeader + "Updating active lists...");
			updateActiveList(activeList);
			nodeLog.info(logHeader + "Updated active lists.");
			
			if(i < workload.size())
			{
				PSAction actionI = workload.get(i);
				PSActionType actionIsActionType = actionI.getActionType();
				delayValue = actionI.getActionDelay();
				
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
				
				i++;
			}
			
			// Sleep for some time
			try 
			{				
				nodeLog.debug(logHeader + "pausing for " + delayValue.toString() + ".");
				Thread.sleep(delayValue);
			} 
			catch (InterruptedException e) 
			{
				nodeLog.error(logHeader + "error sleeping in client " + nodeName + ": ", e);
				return false;
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
		 * 	Get the iTH one - itself, its PSActionType and its Attributes
		 * 	Get the currentTime using System.nanoTime()
		 * 	Get the Diary Entry associated with this Action - the one that was created when it was launched
		 * 		Can't find it?
		 * 			Error
		 * 		Found it?
		 * 			Get the System.nanoTime() value associated with starting it (Currently that's getStartedAction())
		 * 			If the Difference between the currentTime and the "start Time" is >= the time it should have been active
		 * 				Launch the associated undo action - U or V (assuming that the offending Action is a S or A)
		 * 					Undo Successful?
		 * 						Add i to an ArrayList<Integer>
		 * 					Undo Unsuccessful?
		 * 						Error
		 * 
		 * After this is complete
		 * 	Loop through the new ArrayList<Integer>
		 * 		Remove the jTH Action from givenActiveList
		 */
		for(int i = 0 ; i < numActiveActions ; i++)
		{
			PSAction activeActionI = givenActiveList.get(i);
			PSActionType aAIActionType = activeActionI.getActionType();
			String aAIAttributes = activeActionI.getAttributes();
			nodeLog.trace(logHeader + "Accessing entry " + aAIAttributes + ".");
			
			Long currentTime = System.nanoTime();
			
			DiaryEntry activeActionIEntry = diary.getDiaryEntryGivenActionTypeNAttributes(aAIActionType, aAIAttributes);
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
					nodeLog.error(logHeader + "improper active list given");
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
							+ " from ActiveList");
			givenActiveList.remove(j);
			nodeLog.debug(logHeader + "Remove successful");
		}
		
		nodeLog.trace(logHeader + "Update complete");
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
		nodeLog.debug(logHeader + "Undoing " + sizeAL + " 'infinite' actions."); 
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
	
	protected boolean variableCheck()
	{
		boolean everythingPresent = true;
		
		if(brokersURIs.isEmpty())
		{
			nodeLog.error("No broker URI was given!");
			everythingPresent = false;
		}
		if(workload.isEmpty())
		{
			nodeLog.error("No workload was given!");
			everythingPresent = false;
		}
		
		everythingPresent = contextVariableCheck();
		
		return everythingPresent;
	}
	
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
		// The only time the ActionTypes should differ is if we want to Unad / Unsub
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
		// This function should be used for the R(eceived) ActionType
		// That should be limited to storePublication
		if(selectedAction.equals(PSActionType.R))
		{
			nodeLog.error(logHeader + "launchAction doesn't handle Received publications!");
			return false;
		}
		
		// Variable initialization - DiaryEntry
		DiaryEntry thisEntry = new DiaryEntry();
		
		// Execute the action... after getting some number's first
		Long startTime = System.currentTimeMillis();
		Long startAction = System.nanoTime();
		boolean actionSuccessful = executeAction(selectedAction, givenAction, thisEntry);
		Long endAction = System.nanoTime();
		Long brokerFinished = System.currentTimeMillis();
		
		if(actionSuccessful)
		{
			// Get a few missing recordings
			Long timeDiff = endAction - startAction;
			String attributes = givenAction.getAttributes();
			
			// Add all recordings to DiaryEntry
			thisEntry.addPSActionType(selectedAction);
			thisEntry.addStartedAction(startAction);
			thisEntry.addEndedAction(endAction);
			thisEntry.addActionDelay(timeDiff);
			thisEntry.addTimeActionStarted(startTime);
			thisEntry.addTimeBrokerFinished(brokerFinished);
			thisEntry.addAttributes(attributes);
			
			if(selectedAction.equals(PSActionType.U) || selectedAction.equals(PSActionType.V))
			{
				// If we're unsubscribing / unadvertising, we need to determine how long they were active for
				// So we need to find the associated subscribe / advertise
				DiaryEntry ascAction = null;
				
				if(selectedAction.equals(PSActionType.U))
				{
					ascAction = diary.getDiaryEntryGivenActionTypeNAttributes(PSActionType.S, attributes);
				}
				else // I can do this cause I limited the options above
				{
					ascAction = diary.getDiaryEntryGivenActionTypeNAttributes(PSActionType.A, attributes);
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
			try
			{
				diaryLock.lock();
				diary.addDiaryEntryToDiary(thisEntry);
			}
			finally
			{
				diaryLock.unlock();
			}
			nodeLog.info(logHeader + selectedAction + " " + attributes + " recorded");
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
		boolean successfulExecution = false;
		
		switch(selectedAction)
		{
			case A:
			{
				nodeLog.debug(logHeader + generalLog + "advertize " + givenAttributes);
				successfulExecution = advertise(givenAttributes, givenEntry);
				break;
			}
			case V:
			{
				nodeLog.debug(logHeader + generalLog + "unadvertize " + givenAttributes + ".");
				successfulExecution = unadvertise(givenAttributes, givenEntry);
				break;
			}
			case S:
			{
				nodeLog.debug(logHeader + generalLog + "subscribe to " + givenAttributes + ".");
				successfulExecution = subscribe(givenAttributes, givenEntry);
				break;
			}
			case U:
			{
				nodeLog.debug(logHeader + generalLog + "unsubscribe from " + givenAttributes + ".");
				successfulExecution = unsubscribe( givenAttributes, givenEntry);
				break;
			}
			case P:
			{
				nodeLog.debug(logHeader + generalLog + "publish " + givenAttributes);
				successfulExecution = publish(givenAttributes, givenEntry, givenAction.getPayloadSize());
				break;
			}
			default:
				break;
		}
		
		return successfulExecution;
	}
	
	protected abstract boolean advertise(String givenAttributes, DiaryEntry resultingEntry);
	
	protected abstract boolean unadvertise(String givenAttributes, DiaryEntry resultingEntry);
	
	protected abstract boolean subscribe(String givenAttributes, DiaryEntry resultingEntry);
	
	protected abstract boolean unsubscribe(String givenAttributes, DiaryEntry resultingEntry);
	
	protected abstract boolean publish(String givenAttributes, DiaryEntry resultingEntry, Integer givenPayLoadSize);

}
