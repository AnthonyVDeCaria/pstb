package pstb.startup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.util.LogicalTopology;
import pstb.util.NodeRole;
import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 * 
 * Handles the Topology File parsing.
 * 
 * This code will take a given Topology File
 * and convert it into a Logical Topology 
 * 
 */
public class TopologyFileParser {	
	private LogicalTopology logicalTopo;
	private String topoFileString;
	
	private final int NUM_SEGMENTS = 3;
	private final int LOC_NAME = 0;
	private final int LOC_ROLE = 1;
	private final int LOC_CONN = 2;
	
	private final String logHeader = "Topology Parser: ";
	private Logger logger = LogManager.getRootLogger();
	
	/**
	 * FilePath Constructor
	 * 
	 * @param givenTFS - the Topology File String to parse
	 * @param log - the Logger we have to use
	 */
	public TopologyFileParser(String givenTFS)
    {
		logicalTopo = new LogicalTopology();
		topoFileString = givenTFS;
    }
	
	/**
	 * Gets the logical topology contained in this parser
	 * 
	 * @return the Logical Topology
	 */
	public LogicalTopology getLogicalTopo()
	{
		return logicalTopo;
	}
	
	/**
	 * Parses the given Topology File
	 * 
	 * @returns false if there is an error; true if the parse is successful
	 */
	public boolean parse()
	{
		boolean isParseSuccessful = true;
		String line = null;
		int linesRead = 0;
		try {
			BufferedReader tFReader = new BufferedReader(new FileReader(topoFileString));
			while( (line = tFReader.readLine()) != null)
			{
				linesRead++;
				if(!PSTBUtil.checkIfLineIgnorable(line))
				{
					String[] splitLine = line.split("	");
					String name = splitLine[LOC_NAME];
					String roles = splitLine[LOC_ROLE];
					String connections = splitLine[LOC_CONN];
					
					if(splitLine.length == NUM_SEGMENTS)
					{
						ArrayList<NodeRole> lineIsRoles = checkProperRoles(roles);
						if(lineIsRoles != null)
						{
							if(checkUniqueName(name))
							{
								logger.trace(logHeader + "Line " + linesRead + "'s syntax checks out.");
								
								ArrayList<String> splitConnections = PSTBUtil.turnStringArrayIntoArrayListString(
																			connections.split(",")
																		);
								
								logicalTopo.addNewNodeToTopo(lineIsRoles, name, splitConnections);
							}
							else
							{
								isParseSuccessful = false;
								logger.error(logHeader + "Error in Line " + linesRead + " - Duplicate names!");
							}
						}
						else
						{
							isParseSuccessful = false;
							logger.error(logHeader + "Error in Line " + linesRead + " - Roles are improper!");
						}
					}
					else
					{
						isParseSuccessful = false;
						logger.error(logHeader + "Error in Line " + linesRead + " - Length isn't correct!");
					}
				}
			}
			tFReader.close();
		} 
		catch (IOException e) 
		{
			isParseSuccessful = false;
			logger.error(logHeader + "Cannot find file: ", e);
		}
		return isParseSuccessful;
	}
	
	/**
	 * Determines if the roles listed in the file line are proper PubSub roles
	 * 
	 * @param roles - the unsplit String of roles from the file
	 * @return true if they are; false if they aren't
	 */
	private ArrayList<NodeRole> checkProperRoles(String roles)
	{		
		String[] brokenRoles = roles.split(PSTBUtil.COMMA);
		ArrayList<NodeRole> roleLedger = new ArrayList<NodeRole>();
		
		for(int i = 0 ; i < brokenRoles.length ; i++)
		{
			String brokenRoleI = brokenRoles[i];
			NodeRole roleI = null;
			try
			{
				roleI = NodeRole.valueOf(brokenRoleI);
			}
			catch(IllegalArgumentException e)
			{
				logger.error(logHeader + brokenRoleI + " is not a valid type!", e);
				return null;
			}
			
			if(roleLedger.contains(roleI))
			{
				logger.error(logHeader + "More than one of the same type, " + roleI + ", requested!");
				return null;
			}
			
			if( (roleI.equals(NodeRole.P) || roleI.equals(NodeRole.S)) && roleLedger.contains(NodeRole.B))
			{
				logger.error(logHeader + "A broker cannot be a client!");
				return null;
			}
			
			if ( (roleI.equals(NodeRole.B)) && (roleLedger.contains(NodeRole.P) || roleLedger.contains(NodeRole.S) ) )
			{
				logger.error(logHeader + "A client cannot be a broker!");
				return null;
			}
			
			roleLedger.add(roleI);
		}
		
		return roleLedger;
	}
	
	/**
	 * Determines if the given name is unique
	 * I.e. doesn't already exist in any of the groups.
	 * 
	 * @param name - the name to check
	 * @return true if the name is unique; false if it isn't
	 */
	private boolean checkUniqueName(String name)
	{	
		return !logicalTopo.getBrokers().containsKey(name) && !logicalTopo.getClients().containsKey(name) ;
	}
}
