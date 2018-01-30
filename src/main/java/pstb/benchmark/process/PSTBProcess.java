/**
 * 
 */
package pstb.benchmark.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import pstb.benchmark.object.PSNode;
import pstb.benchmark.process.broker.PADRESBrokerProcess;
import pstb.benchmark.process.broker.PSTBBrokerProcess;
import pstb.benchmark.process.broker.SIENABrokerProcess;
import pstb.benchmark.process.client.PADRESClientProcess;
import pstb.benchmark.process.client.PSTBClientProcess;
import pstb.benchmark.process.client.SIENAClientProcess;
import pstb.startup.config.SupportedEngines.PSEngine;
import pstb.startup.topology.NodeRole;
import pstb.util.PSTBError;
import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 *
 */
public abstract class PSTBProcess {
	// Constants
	private static final int MIN_ARGS = 6;
	private static final int MAX_ARGS = 7;
	private static final int LOC_NAME = 0;
	private static final int LOC_CNXT = 1;
	private static final int LOC_IPAD = 2;
	private static final int LOC_PORT = 3;
	private static final int LOC_ENGN = 4;
	private static final int LOC_ROLE = 5;
	private static final int LOC_USER = 6;
	
	// Standard Variables
	protected static String nodeName;
	protected static String context;
	protected static String masterIPAddress;
	protected static Integer portNumber;
	protected static PSEngine engine;
	protected static NodeRole role;
	
	// Occasional Variables
	protected static boolean distributed;
	protected static String username;
	
	// Socket
	protected static Socket connection;
	protected static OutputStream connOut;
	
	// Logger
	protected static Logger log = LogManager.getLogger(PSTBProcess.class);
	protected static String logHeader = "PhyNode: ";
	protected static String threadContextString = "node";
	
	public PSTBProcess(String givenName, String givenContext, String givenIPAddress, Integer givenPort, PSEngine givenEngine, 
			NodeRole givenRole, boolean areWeDistributed, String givenUsername, Socket givenConnection, OutputStream givenOut, 
			Logger givenLog, String givenLogHeader, String givenThreadContextString)
	{
		nodeName = givenName;
		context = givenContext;
		masterIPAddress = givenIPAddress;
		portNumber = givenPort;
		engine = givenEngine;
		role = givenRole;
		
		distributed = areWeDistributed;
		username = givenUsername;
		
		connection = givenConnection;
		connOut = givenOut;
		
		log = givenLog;
		logHeader = givenLogHeader;
	}
	
	public static void main(String[] args)
	{		
		Long currentTime = System.currentTimeMillis();
		String formattedTime = PSTBUtil.DATE_FORMAT.format(currentTime);
		ThreadContext.put("node", formattedTime);
		
		PSTBProcess fleshedNode = parseArguments(args);
		if(fleshedNode == null)
		{
			log.error(logHeader + "Not given proper arguments");
			System.exit(PSTBError.N_ARGS);
		}
		
		ThreadContext.put(threadContextString, context);
		Thread.currentThread().setName(context);
		
		log.debug(logHeader + "Attempting to send name to master...");
		PSTBUtil.sendStringAcrossSocket(connOut, nodeName, log, logHeader);
		log.info(logHeader + "Name sent.");
		
		log.debug(logHeader + "Attempting to retrieve " + nodeName + "'s Object from master...");
		Object check = getObjectFromMaster();
		if(check == null)
		{
			log.error(logHeader + "Didn't get object " + nodeName + "'s Object from master!");
			System.exit(PSTBError.N_OBJECT);
		}
		log.info(logHeader + nodeName + "'s Object received.");
		
		fleshedNode.complete((PSNode) check);
	}
	
	private static PSTBProcess parseArguments(String[] args)
	{
		PSTBProcess retVal = null;
		
		String portNumString = null;
		String engineString = null;
		String nodeRoleString = null;
		
		int numArgs = args.length;
		if(numArgs < MIN_ARGS || numArgs > MAX_ARGS)
		{
			log.error(numArgs + " is an improper number of arguments!");
			return null;
		}
		else if(numArgs == MAX_ARGS)
		{
			distributed = new Boolean(true);
			username = args[LOC_USER];
		}
		else
		{
			distributed = new Boolean(false);
		}
		
		nodeName = args[LOC_NAME];
		context = args[LOC_CNXT];
		masterIPAddress = args[LOC_IPAD];
		portNumString = args[LOC_PORT];
		engineString = args[LOC_ENGN];
		nodeRoleString = args[LOC_ROLE];
		
		portNumber = PSTBUtil.checkIfInteger(portNumString, false, null);
		if(portNumber == null)
		{
			log.error(logHeader + "The given port " + portNumString + " isn't an Integer!");
			return null;
		}
		
		try
		{
			role = NodeRole.valueOf(nodeRoleString);
		}
		catch(Exception e)
		{
			log.error(logHeader + "The given role " + engineString + " isn't a NodeRole!");
			return null;
		}
		
		try
		{
			engine = PSEngine.valueOf(engineString);
		}
		catch(Exception e)
		{
			log.error(logHeader + "The given engine " + engineString + " isn't a PSEngine!");
			return null;
		}
		
		try
		{
			connection = new Socket(masterIPAddress, portNumber);
		}
		catch (IOException e) 
		{
			log.error(logHeader + "error creating a new Socket: ", e);
			return null;
		}
		
		try
		{
			connOut = connection.getOutputStream();
		}
		catch(IOException e)
		{
			log.error(logHeader + "Couldn't create an OutputStream from connection!");
			return null;
		}
		
		if(role.equals(NodeRole.B))
		{
			log = LogManager.getLogger(PSTBBrokerProcess.class);
			threadContextString = "broker";
			
			if (engine.equals(PSEngine.PADRES))
			{
				logHeader = "PBP: ";
				retVal = new PADRESBrokerProcess(nodeName, context, masterIPAddress, portNumber, engine, role, distributed, username,
						connection, connOut, log, logHeader, threadContextString);
			} 
			else if (engine.equals(PSEngine.SIENA))
			{
				logHeader = "SBP: ";
				retVal = new SIENABrokerProcess(nodeName, context, masterIPAddress, portNumber, engine, role, distributed, username,
						connection, connOut, log, logHeader, threadContextString);
			}
		}
		else
		{
			log = LogManager.getLogger(PSTBClientProcess.class);
			threadContextString = "client";
			
			if (engine.equals(PSEngine.PADRES))
			{
				logHeader = "PCP: ";
				retVal = new PADRESClientProcess(nodeName, context, masterIPAddress, portNumber, engine, role, distributed, username,
						connection, connOut, log, logHeader, threadContextString);
			} 
			else if (engine.equals(PSEngine.SIENA))
			{
				logHeader = "SCP: ";
				retVal = new SIENAClientProcess(nodeName, context, masterIPAddress, portNumber, engine, role, distributed, username,
						connection, connOut, log, logHeader, threadContextString);
			}
		}
		
		return retVal;
	}
	
	protected static String readConnection()
	{
		String inputLine = new String();
		try 
		{
			BufferedReader bufferedIn = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			
			if ((inputLine = bufferedIn.readLine()) != null) 
			{
				return inputLine;
            }
		}
		catch (IOException e) 
		{
			log.error(logHeader + "Couldn't get a BufferedReader/InputStreamReader onto the masterConnection: ", e);
		}
		
		return null; 
	}
	
	protected static Object getObjectFromMaster()
	{
		Object retVal = null;
		try
		{
			ObjectInputStream oISIn = new ObjectInputStream(connection.getInputStream());
			retVal = oISIn.readObject();
		}
		catch (IOException e)
		{
			log.error(logHeader + "error accessing ObjectInputStream: ", e);
			return null;
		}
		catch(ClassNotFoundException e)
		{
			log.error(logHeader + "can't find class: ", e);
			return null;
		}
		
		return retVal;
	}
	
	protected void initalized()
	{
		log.debug(logHeader + "Letting master know we've initialized...");
		PSTBUtil.sendStringAcrossSocket(connOut, PSTBUtil.INIT, log, logHeader);
		log.info(logHeader + "Master should know.");
	}
	
	protected abstract void complete(PSNode givenNode);
	
	protected abstract boolean setup(PSNode givenNode);
	
	protected abstract boolean run();
}
