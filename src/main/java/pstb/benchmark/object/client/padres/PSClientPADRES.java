package pstb.benchmark.object.client.padres;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.logging.log4j.ThreadContext;

import ca.utoronto.msrg.padres.client.BrokerState;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import pstb.analysis.diary.DiaryEntry;
import pstb.benchmark.object.client.PSClient;
import pstb.startup.config.AttributeRatio;
import pstb.startup.config.NumAttribute;
import pstb.startup.workload.PSActionType;

/**
 * @author padres-dev-4187
 * 
 * The Client Object
 * 
 * Handles all of the client actions: 
 * initializing it, shutting it down, 
 * connecting and disconnecting it to a broker,
 * starting it (i.e. handle advertisements, publications and subscriptions)
 * and message processing.
 */
public class PSClientPADRES extends PSClient
{
	// Constants
	private static final long serialVersionUID = 1L;
	
	// PADRES Client Variables
	private PADRESClientExtension actualClient;
	private ArrayList<BrokerState> connectedBrokers;
	private ClientConfig cConfig;
	
	/**
	 * Empty Constructor
	 */
	public PSClientPADRES()
	{
		super();
		
		logHeader = "PClient: ";
	}

	/**
	 * Sets some of the variables and creates a new Client
	 * (The idea being you would initialize a general Client first before giving it it's tasks
	 * 
	 * @param connectAsWell - connects this client to the network
	 * @return false if there's a failure; true otherwise
	 */
	public boolean initialize(boolean connectAsWell) 
	{
		// Check that the name exists
		if(nodeName.isEmpty())
		{
			nodeLog.error(logHeader + "Attempted to initialize a client with no name");
			return false;
		}
		
		nodeLog.debug(logHeader + "Attempting to create a new ClientConfig" + nodeName + "...");
		// Attempt to create a new config file
		try 
		{
			cConfig = new ClientConfig();
		} 
		catch (ClientException e) 
		{
			nodeLog.error(logHeader + "Error creating new Config for Client " + nodeName, e);
			return false;
		}
		nodeLog.debug(logHeader + "New ClientConfig created.");
		
		cConfig.clientID = nodeName;
		
		if(connectAsWell)
		{
			// Check that there are brokerURIs to connect to
			if(brokersURIs.isEmpty())
			{
				nodeLog.error(logHeader + "Attempted to connect client " + nodeName + " that had no brokerURIs");
				return false;
			}
			// There are
			
			nodeLog.debug(logHeader + "Adding brokerlist to ClientConfig...");
			cConfig.connectBrokerList = (String[]) brokersURIs.toArray(new String[brokersURIs.size()]);
		}
		
		nodeLog.debug(logHeader + "Creating PADRES client object...");
		try 
		{
			actualClient = new PADRESClientExtension(cConfig, this);
		}
		catch (Exception e)
		{
			nodeLog.error(logHeader + "Cannot initialize new client " + nodeName, e);
			return false;
		}
		// If connecting was requested, that set would have also connected the client
		
		nodeLog.debug(logHeader + "Initialized client " + nodeName);
		return true;
	}

	/**
	 * Attempts to shutdown the Client
	 * 
	 * @return false on error, true if successful
	 */
	public boolean shutdown() 
	{
		nodeLog.debug(logHeader + "Attempting to shutdown client " + nodeName);
		
		try 
		{
			actualClient.shutdown();
		}
		catch (ClientException e) 
		{
			nodeLog.error(logHeader + "Cannot shutdown client " + nodeName, e);
			return false;
		}
		
		nodeLog.debug(logHeader + "Client " + nodeName + " shutdown");
		return true;
	}

	/**
	 * Connects this client to the network
	 * NOTE: NOT CURRENTLY WORKING! 
	 * Please use initialize(true)
	 * 
	 * @return false on error; true if successful
	 */
	public boolean connect() 
	{
		nodeLog.info(logHeader + "Attempting to connect client " + nodeName + " to network.");
		
		for(int i = 0 ; i < brokersURIs.size() ; i++)
		{
			String iTHURI = brokersURIs.get(i);
			BrokerState brokerI = null;
			
			try
			{
				brokerI = actualClient.connect(iTHURI);
			}
			catch (ClientException e)
			{
				nodeLog.error(logHeader + "Cannot connect client " + nodeName + " to broker " + iTHURI + ": ", e);
				this.shutdown();
				return false;
			}
			
			connectedBrokers.add(brokerI);
		}
		
		nodeLog.debug(logHeader + "Added client " + nodeName + " to network.");
		return true;
	}

	/**
	 * Disconnects the client from the network
	 */
	public void disconnect() 
	{
		nodeLog.info(logHeader + "Disconnecting client " + nodeName + " from network.");
		actualClient.disconnectAll();
	}
	
	/**
	 * Stores the given Message
	 * Assuming it's a Publication
	 * @param msg - the given Message
	 */
	public void storePublication(Message msg) 
	{
		try
		{
			ThreadContext.put("client", generateNodeContext());
			
			Boolean areWeRunning = getCurrentlyRunning();
			if(areWeRunning == null)
			{
				nodeLog.error(logHeader + "CurrentlyRunning is null!");
			}
			else if(areWeRunning)
			{
				if(msg instanceof PublicationMessage)
				{
					Long currentTime = System.currentTimeMillis();
					DiaryEntry receivedMsg = new DiaryEntry();
					
					Publication pub = ((PublicationMessage) msg).getPublication();
					
					Long timePubCreated = pub.getTimeStamp().getTime();
					
					receivedMsg.setPSActionType(PSActionType.R);
					receivedMsg.addMessageID(pub.getPubID());
					receivedMsg.addTimeCreated(timePubCreated);
					receivedMsg.addTimeReceived(currentTime);
					receivedMsg.addTimeDifference(currentTime - timePubCreated);
					receivedMsg.addAttributes(pub.toString());

					diaryLock.lock();
					try
					{
						diary.addDiaryEntryToDiary(receivedMsg);
					}
					finally
					{
						diaryLock.unlock();
					}
					
					nodeLog.debug(logHeader + "new publication received " + pub.toString());
				}
			}
		}
		catch(Exception e)
		{
			nodeLog.error(logHeader + "FUBAR: ", e);
		}
	}
	
	@Override
	protected void advertise(String givenAttributes, DiaryEntry resultingEntry) throws Exception
	{
		Message result = actualClient.advertise(givenAttributes, brokersURIs.get(0));
		resultingEntry.addMessageID(result.getMessageID());
	}
	
	@Override
	protected void unadvertise(String givenAttributes, DiaryEntry resultingEntry) throws Exception
	{
		DiaryEntry originalAd = diary.getDiaryEntryGivenActionTypeNAttributes(PSActionType.A, givenAttributes, nodeLog);
		if(originalAd == null)
		{
			throw new Exception("Couldn't find original advertisement!");
		}
		
		String originalAdID = originalAd.getMessageID();
		Message result = actualClient.unAdvertise(originalAdID);
		resultingEntry.addMessageID(result.getMessageID());
	}
	
	@Override
	protected void subscribe(String givenAttributes, DiaryEntry resultingEntry) throws Exception
	{
		Message result = actualClient.subscribe(givenAttributes, brokersURIs.get(0));
		resultingEntry.addMessageID(result.getMessageID());
	}
	
	@Override
	protected void unsubscribe(String givenAttributes, DiaryEntry resultingEntry) throws Exception
	{
		nodeLog.debug(logHeader + "Attempting to find original subscription...");
		DiaryEntry originalSub = diary.getDiaryEntryGivenActionTypeNAttributes(PSActionType.S, givenAttributes, nodeLog);
		if(originalSub == null)
		{
			throw new Exception("Couldn't find original subscription!");
		}
		nodeLog.info(logHeader + "Found original subscription.");
		
		nodeLog.debug(logHeader + "Accessing original sub message id...");
		String originalSubID = originalSub.getMessageID();
		if(originalSubID == null)
		{
			throw new Exception("Original sub's message id was never set!");
		}
		nodeLog.info(logHeader + "Original sub id = " + originalSubID + ".");
		
		nodeLog.debug(logHeader + "Attempting to actually unsubscribe...");
		Message result = actualClient.unSubscribe(originalSubID);
		nodeLog.info(logHeader + "Unsubscribe successful.");
		
		nodeLog.debug(logHeader + "Attempting to add unsubscribe ID to diary...");
		resultingEntry.addMessageID(result.getMessageID());
		nodeLog.info(logHeader + "Unsub ID added.");
	}
	
	@Override
	protected void publish(String givenAttributes, DiaryEntry resultingEntry, Integer givenPayloadSize) throws Exception
	{
		Message result = null;
		
		if(givenPayloadSize < 0)
		{
			throw new Exception("Payload size is less than 0!");
		}
		else if(givenPayloadSize == 0)
		{
			result = actualClient.publish(givenAttributes, brokersURIs.get(0));
			
		}
		else
		{
			byte[] payload = new byte[givenPayloadSize];
			Arrays.fill( payload, (byte) 1 );
			
			Publication pubI = MessageFactory.createPublicationFromString(givenAttributes);
			
			pubI.setPayload(payload);
			
			result = actualClient.publish(pubI, brokersURIs.get(0));
			
		}
		
		resultingEntry.addMessageID(result.getMessageID());
	}

	@Override
	protected String generateThroughputAttributes(PSActionType givenPSAT, int messageNumber) 
	{
		if(givenPSAT == null)
		{
			return null;
		}
		
		String retVal = null;
		
		if(givenPSAT.equals(PSActionType.P) || givenPSAT.equals(PSActionType.R))
		{
			//	Pub
			retVal = "[class,\"oneITS\"]";
		}
		else
		{
			// Ad
			retVal = "[class,eq,\"oneITS\"]";
		}
		
		if(na.equals(NumAttribute.Twelve))
		{
			// Eleven Doubles, One String
			if(ar.equals(AttributeRatio.String0P))
			{
				if(givenPSAT.equals(PSActionType.P) || givenPSAT.equals(PSActionType.R))
				{
					// Pub
					retVal = retVal + ","
							+"[Number," + messageNumber + "],"
							+ "[Beetles,4],"
							+ "[LeafCups,13],"
							+ "[Temp,32],"
							+ "[Tones,12],"
							+ "[Guessings,2],"
							+ "[Masters,2],"
							+ "[NumBatmanMovies,13],"
							+ "[Nice,69],"
							+ "[Money,12345678],"
							+ "[ChristmasDate,25]";
				}
				else
				{
					// Ad
					retVal = retVal + ","
							+ "[Number,>,0],"
							+ "[Beetles,>,0],"
							+ "[LeafCups,>,0],"
							+ "[Temp,>,0],"
							+ "[Tones,>,0],"
							+ "[Guessings,>,0],"
							+ "[Masters,>,0],"
							+ "[NumBatmanMovies,>,0],"
							+ "[Nice,>,0],"
							+ "[Money,>,0],"
							+ "[ChristmasDate,>,0]";
				}
			}
			// Six Doubles, Six Strings
			else if(ar.equals(AttributeRatio.String50P))
			{
				if(givenPSAT.equals(PSActionType.P) || givenPSAT.equals(PSActionType.R))
				{
					// Pub
					retVal = retVal + ","
							+ "[Number," + messageNumber + "],"
							+ "[Band,'Coheed&Cambria'],"
							+ "[HabCups,24],"
							+ "[WillAnyOneKnow,'No...'],"
							+ "[Tones,12],"
							+ "[Location,'Hell'],"
							+ "[NumBatmanMovies,13],"
							+ "[WhatMatters,'Nothing'],"
							+ "[Money,12345678],"
							+ "[Time,'SpentWell'],"
							+ "[ChristmasDate,25]";
				}
				else
				{
					// Ad
					retVal = retVal + ","
							+ "[Number,>,-1],"
							+ "[Band,eq,'Coheed&Cambria'],"
							+ "[HabCups,>,0],"
							+ "[WillAnyOneKnow,eq,'No...'],"
							+ "[Tones,>,0],"
							+ "[Location,eq,'Hell'],"
							+ "[NumBatmanMovies,>,0],"
							+ "[WhatMatters,eq,'Nothing'],"
							+ "[Money,>,0],"
							+ "[Time,eq,'SpentWell'],"
							+ "[ChristmasDate,>,0]";
				}
			}
			// Zero Doubles, 12 Strings
			else
			{
				if(givenPSAT.equals(PSActionType.P) || givenPSAT.equals(PSActionType.R))
				{
					// Pub
					retVal = retVal + ","
							+ "[Band,'Coheed&Cambria'],"
							+ "[Album,'GoodApolloImBurningStarIV'],"
							+ "[WillAnyOneKnow,'No...'],"
							+ "[SongName,'MotherSuperior'],"
							+ "[Location,'Hell'],"
							+ "[FirstLyric,'Here-sleep...atTheBottomOfHell...'],"
							+ "[WhatMatters,'Nothing'],"
							+ "[GrowsInto,'Man'],"
							+ "[Time,'SpentWell'],"
							+ "[Cost,'Life']";
				}
				else
				{
					// Ad 
					retVal = retVal + ","
							+ "[Band,eq,'Coheed&Cambria'],"
							+ "[Album,eq,'GoodApolloImBurningStarIV'],"
							+ "[WillAnyOneKnow,eq,'No...'],"
							+ "[SongName,eq,'MotherSuperior'],"
							+ "[Location,eq,'Hell'],"
							+ "[FirstLyric,eq,'Here-sleep...atTheBottomOfHell...'],"
							+ "[WhatMatters,eq,'Nothing'],"
							+ "[GrowsInto,eq,'Man'],"
							+ "[Time,eq,'SpentWell'],"
							+ "[Cost,eq,'Life']";
				}
			}
		}
		else if(na.equals(NumAttribute.TwentyFour))
		{
			// 23 Doubles, One String
			if(ar.equals(AttributeRatio.String0P))
			{
				if(givenPSAT.equals(PSActionType.P) || givenPSAT.equals(PSActionType.R))
				{
					//  Pub
					retVal = retVal + ","
							+ "[Number," + messageNumber + "],"
							+ "[Beetles,4],"
							+ "[LeafCups,13],"
							+ "[Temp,32],"
							+ "[Tones,12],"
							+ "[Guessings,2],"
							+ "[Masters,2],"
							+ "[NumBatmanMovies,13],"
							+ "[Nice,69],"
							+ "[Money,12345678],"
							+ "[ChristmasDate,25],"
							+ "[NumAttributes,24],"
							+ "[Partridges,1],"
							+ "[TurtleDoves,2],"
							+ "[FrenchHens,3],"
							+ "[CallingBirds,4],"
							+ "[GoldenRings,5],"
							+ "[LayingGeese,6],"
							+ "[SwimmingSwans,7],"
							+ "[MilkingMaids,8],"
							+ "[DancingLadies,9],"
							+ "[LeapingLords,10],"
							+ "[PipingPipers,11],"
							+ "[DrummingDrummers,12]";
					
				}
				else
				{
					// Ad
					retVal = retVal + ","
							+ "[Number,>,-1],"
							+ "[Beetles,>,0],"
							+ "[LeafCups,>,0],"
							+ "[Temp,>,0],"
							+ "[Tones,>,0],"
							+ "[Guessings,>,0],"
							+ "[Masters,>,0],"
							+ "[NumBatmanMovies,>,0],"
							+ "[Nice,>,0],"
							+ "[Money,>,0],"
							+ "[ChristmasDate,>,0],"
							+ "[NumAttributes,>,0],"
							+ "[Partridges,>,0],"
							+ "[TurtleDoves,>,0],"
							+ "[FrenchHens,>,0],"
							+ "[CallingBirds,>,0],"
							+ "[GoldenRings,>,0],"
							+ "[LayingGeese,>,0],"
							+ "[SwimmingSwans,>,0],"
							+ "[MilkingMaids,>,0],"
							+ "[DancingLadies,>,0],"
							+ "[LeapingLords,>,0],"
							+ "[PipingPipers,>,0],"
							+ "[DrummingDrummers,>,0]";
				}
			}
			// Twelve Doubles, Twelve Strings
			else if(ar.equals(AttributeRatio.String50P))
			{
				if(givenPSAT.equals(PSActionType.P) || givenPSAT.equals(PSActionType.R))
				{
					// Pub
					retVal = retVal + "," 
							+ "[Number," + messageNumber + "],"
							+ "[Time,'LongGone'],"
							+ "[Partridges,1],"
							+ "[Answer,'Barrel'],"
							+ "[TurtleDoves,2],"
							+ "[Murder,'Just'],"
							+ "[FrenchHens,3],"
							+ "[Hurts,'NoMore'],"
							+ "[CallingBirds,4],"
							+ "[World,'Deserted'],"
							+ "[GoldenRings,5],"
							+ "[Calling,'Mercy'],"
							+ "[LayingGeese,6],"
							+ "[PrayFor,'Me'],"
							+ "[SwimmingSwans,7],"
							+ "[Goodbye,'Tomorrow'],"
							+ "[MilkingMaids,8],"
							+ "[CurseOf,'RadioByeBye'],"
							+ "[DancingLadies,9],"
							+ "[YouAre,'Unforgettable'],"
							+ "[LeapingLords,10],"
							+ "[TheEnd,'Complete'],"
							+ "[PipingPipers,11]";					
				}
				else
				{
					// Ad
					retVal = retVal + ","
							+ "[Number,>,-1],"
							+ "[Time,eq,'LongGone'],"
							+ "[Partridges,>,0],"
							+ "[Answer,eq,'Barrel'],"
							+ "[TurtleDoves,>,0],"
							+ "[Murder,eq,'Just'],"
							+ "[FrenchHens,>,0],"
							+ "[Hurts,eq,'NoMore'],"
							+ "[CallingBirds,>,0],"
							+ "[World,eq,'Deserted'],"
							+ "[GoldenRings,>,0],"
							+ "[Calling,eq,'Mercy'],"
							+ "[LayingGeese,>,0],"
							+ "[PrayFor,eq,'Me'],"
							+ "[SwimmingSwans,>,0],"
							+ "[Goodbye,eq,'Tomorrow'],"
							+ "[MilkingMaids,>,0],"
							+ "[CurseOf,eq,'RadioByeBye'],"
							+ "[DancingLadies,>,0],"
							+ "[YouAre,eq,'Unforgettable'],"
							+ "[LeapingLords,>,0],"
							+ "[TheEnd,eq,'Complete'],"
							+ "[PipingPipers,>,0]";
				}
			}
			// Zero Doubles, Twenty Four Strings
			else
			{
				if(givenPSAT.equals(PSActionType.P) || givenPSAT.equals(PSActionType.R))
				{
					// Pub
					retVal = retVal + ","
							+ "[Done,'TheMath'],"
							+ "[HowMuch,'Enough'],"
							+ "[Why,'ToKnowTheDangers'],"
							+ "[Of,'OurSecondGuessing'],"
							+ "[IKnow,'ThePiecesFit'],"
							+ "[FireHas,'BurnedAHoleBetweenUs'],"
							+ "[ThePieces,'TumbledDown'],"
							+ "[Fault,'None'],"
							+ "[Watched,'TheTempleToppleOver'],"
							+ "[Poetry,'ComesFromTheSquaring'],"
							+ "[BeautyIs,'InTheDissonance'],"
							+ "[Doomed,'ToCrumble'],"
							+ "[Unless,'WeGrow'],"
							+ "[Strengthen,'Communication'],"
							+ "[ColdSilence,'HasATendancyToAtrophy'],"
							+ "[What,'AnySenseofCompassion'],"
							+ "[Between,'SupposedLovers'],"
							+ "[And,'SupposedBrothers'],"
							+ "[TimeSequence,'LOL'],"
							+ "[SongIs,'Schism'],"
							+ "[Length,'SevenMin'],"
							+ "[Desire,'PointTheFinger'],"
							+ "[Circling,'WorthIt']";
				}
				else
				{
					// Ad
					retVal = retVal + ","
							+ "[Done,eq,'TheMath'],"
							+ "[HowMuch,eq,'Enough'],"
							+ "[Why,eq,'ToKnowTheDangers'],"
							+ "[Of,eq,'OurSecondGuessing'],"
							+ "[IKnow,eq,'ThePiecesFit'],"
							+ "[FireHas,eq,'BurnedAHoleBetweenUs'],"
							+ "[ThePieces,eq,'TumbledDown'],"
							+ "[Fault,eq,'None'],"
							+ "[Watched,eq,'TheTempleToppleOver'],"
							+ "[Poetry,eq,'ComesFromTheSquaring'],"
							+ "[BeautyIs,eq,'InTheDissonance'],"
							+ "[Doomed,eq,'ToCrumble'],"
							+ "[Unless,eq,'WeGrow'],"
							+ "[Strengthen,eq,'Communication'],"
							+ "[ColdSilence,eq,'HasATendancyToAtrophy'],"
							+ "[What,eq,'AnySenseofCompassion'],"
							+ "[Between,eq,'SupposedLovers'],"
							+ "[And,eq,'SupposedBrothers'],"
							+ "[TimeSequence,eq,'LOL'],"
							+ "[SongIs,eq,'Schism'],"
							+ "[Length,eq,'SevenMin'],"
							+ "[Desire,eq,'PointTheFinger'],"
							+ "[Circling,eq,'WorthIt']";
				}
			}
		}
		
		return retVal;
	}
}
