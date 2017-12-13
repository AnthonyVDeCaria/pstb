/**
 * 
 */
package pstb.benchmark;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.Socket;

import org.apache.logging.log4j.Logger;

import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 *
 */
public class Physical {
	private String nodeName;
	private String context;
	private String masterIPAddress;
	private Integer portNumber;
	private Boolean distributed;
	private String user;
	
	private Socket masterConnection;
	
	private Logger log;
	private String logHeader;
	
	public Physical(Logger givenLog, String givenLogHeader)
	{
		nodeName = new String();
		context = new String();
		masterIPAddress = new String();
		portNumber = new Integer(-1);
		distributed = null;
		user = new String();
		
		masterConnection = null;
		
		log = givenLog;
		logHeader = givenLogHeader;
	}
	
	public boolean parseArguments(String[] args)
	{
		String portNumString = new String();
		String disString = new String();
		
		for (int i = 0; i < args.length; i++) 
		{
			if (args[i].equals("-n")) 
			{
				nodeName = args[i + 1];
			}
			if (args[i].equals("-c")) 
			{
				context = args[i + 1];
			}
			if (args[i].equals("-m")) 
			{
				masterIPAddress = args[i + 1];
			}
			if (args[i].equals("-p")) 
			{
				portNumString = args[i + 1];
			}
			if (args[i].equals("-d")) 
			{
				disString = args[i + 1];
			}
			if (args[i].equals("-u")) 
			{
				user = args[i + 1];
			}
		}
		
		boolean allArgsExist = true;
		if(nodeName.isEmpty())
		{
			log.error(logHeader + "Missing name!");
			allArgsExist = false;
		}
		if(context.isEmpty())
		{
			log.error(logHeader + "Missing context!");
			allArgsExist = false;
		}
		if(masterIPAddress.isEmpty())
		{
			log.error(logHeader + "Missing ip address!");
			allArgsExist = false;
		}
		if(portNumString.isEmpty())
		{
			log.error(logHeader + "Missing port!");
			allArgsExist = false;
		}
		if(disString.isEmpty())
		{
			log.error(logHeader + "Missing distributed boolean!");
			allArgsExist = false;
		}
		if(user.isEmpty())
		{
			log.error(logHeader + "Missing user!");
			allArgsExist = false;
		}
		
		if(!allArgsExist)
		{
			return false;
		}
		
		portNumber = PSTBUtil.checkIfInteger(portNumString, false, null);
		if(portNumber == null)
		{
			log.error(logHeader + "The given port " + portNumString + " isn't an Integer!");
			return false;
		}
		
		if(disString.equals("true"))
		{
			distributed = true;
		}
		else if(disString.equals("false"))
		{
			distributed = false;
		}
		else
		{
			log.error(logHeader + "The given distributed value " + disString + " isn't a boolean!");
			return false;
		}
		
		return true;
	}
	
	public String getName()
	{
		return nodeName;
	}
	
	public String getContext()
	{
		return context;
	}
	
	public String getMasterIPAddress()
	{
		return masterIPAddress;
	}
	
	public Integer getPortNumber()
	{
		return portNumber;
	}
	
	/**
	 * @return the masterConnection
	 */
	public Socket getMasterConnection() 
	{
		return masterConnection;
	}
	
	public Boolean areWeDistributed()
	{
		return distributed;
	}
	
	public String getUser()
	{
		return user;
	}
	
	public boolean createSocket()
	{		
		try
		{
			masterConnection = new Socket(masterIPAddress, portNumber);
		}
		catch (IOException e) 
		{
			log.error(logHeader + "error creating a new Socket: ", e);
			return false;
		}
		
		return true;
	}
	
	public String readConnection()
	{
		String inputLine = new String();
		try 
		{
			BufferedReader bufferedIn = new BufferedReader(new InputStreamReader(masterConnection.getInputStream()));
			
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
	
	public Object getObjectFromMaster()
	{
		Object retVal = null;
		try
		{
			ObjectInputStream oISIn = new ObjectInputStream(masterConnection.getInputStream());
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
}
