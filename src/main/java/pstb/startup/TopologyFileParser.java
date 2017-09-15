/**
 * @author padres-dev-4187
 * 
 * Handles the Topology File parsing.
 * 
 */
package pstb.startup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.util.LogicalTopology;
import pstb.util.NodeRole;
import pstb.util.PSTBUtil;

public class TopologyFileParser {	
	private LogicalTopology logicalTopo;
	
	private static final Logger logger = LogManager.getRootLogger();
	
	private final int SEGMENTSNUM = 3;
	private final int NODE_NAME_LOCATION = 0;
	private final int NODE_ROLE_LOCATION = 1;
	private final int NODE_CONN_LOCATION = 2;
	
	/**
	 * Empty Constructor
	 */
	public TopologyFileParser()
    {
		logicalTopo = new LogicalTopology();
    }
	
	/**
	 * Gets the logical topology contained in this parser
	 * @return the Logical Topology
	 */
	public LogicalTopology getLogicalTopo()
	{
		return logicalTopo;
	}
	
	/**
	 * Determines if the given line can be ignored
	 * - i.e. its blank, or starts with a #
	 * @param fileline - the line from the file
	 * @return true if it can be ignored; false if it can't
	 */
	private boolean checkIfLineIgnorable(String fileline)
	{
		boolean isLineIgnorable = false;
		if(fileline.length() == 0 || fileline.startsWith("#"))
		{
			isLineIgnorable = true;
		}
		return isLineIgnorable;
	}
	
	/**
	 * Determines if the given line has been properly written
	 * - i.e. contains a node name, node types and node connections - 
	 * by seeing if the split line has the right number of segments
	 * @param splitFileline - the line from the file that has already be properly split
	 * @return true if it is; false if it isn't
	 */
	private boolean checkProperSpacing(String[] splitFileline)
	{
		boolean isLengthProper = true;
		if(splitFileline.length != SEGMENTSNUM)
		{
			isLengthProper = false;
		}
		return isLengthProper;
	}
	
	/**
	 * Determines if the roles listed in the file line are proper PubSub roles
	 * @param roles - the unsplit String of roles from the file
	 * @return true if they are; false if they aren't
	 */
	private boolean checkProperRoles(String roles)
	{		
		String[] brokenTypes = roles.split(PSTBUtil.COMMA);
		List<NodeRole> nodeTypeLedger = new ArrayList<NodeRole>();
		
		for(int i = 0 ; i < brokenTypes.length ; i++)
		{
			NodeRole brokenTypesI = null;
			try
			{
				brokenTypesI = NodeRole.valueOf(brokenTypes[i]);
			}
			catch(IllegalArgumentException e)
			{
				logger.error("Parser: " + brokenTypes[i] + " is not a valid type.", e);
				return false;
			}
			
			if(nodeTypeLedger.contains(brokenTypesI))
			{
				logger.error("Parser: More than one of the same type, " + brokenTypesI + ", requested.");
				return false;
			}
			
			if( (brokenTypesI.equals(NodeRole.P) || brokenTypesI.equals(NodeRole.S)) 
					&& nodeTypeLedger.contains(NodeRole.B))
			{
				logger.error("Parser: A broker cannot be a client.");
				return false;
			}
			
			if ( (brokenTypesI.equals(NodeRole.B)) 
					&& (nodeTypeLedger.contains(NodeRole.P) || nodeTypeLedger.contains(NodeRole.S) ) )
			{
				logger.error("Parser: A client cannot be a broker.");
				return false;
			}
			
			nodeTypeLedger.add(brokenTypesI);
		}
		
		return true;
	}
	
	/**
	 * Determines if the given name is unique
	 * I.e. doesn't already exist in any of the groups.
	 * @param name - the name to check
	 * @return true if the name is unique; false if it isn't
	 */
	private boolean checkUniqueName(String name)
	{	
		try
		{
			logicalTopo.forEach((role, group)->{
				if(group.checkNodeIsPresent(name))
				{
					throw new IllegalArgumentException();
				}
			});
		}
		catch(IllegalArgumentException e)
		{
			logger.error("Parser: Node " + name + " already exists!");
			return false;
		}
		
		return true;
	}
	
	/**
	 * Adds the line from the file to the topology
	 * @param the line from the file
	 */
	private void addLineToTopo(String[] splitLine)
	{
		String[] nodeRoles = splitLine[NODE_ROLE_LOCATION].split(PSTBUtil.COMMA);
		String name = splitLine[NODE_NAME_LOCATION];
		String[] connections = splitLine[NODE_CONN_LOCATION].split(PSTBUtil.COMMA);
		ArrayList<String> aLConnections = PSTBUtil.turnStringArrayIntoArrayListString(connections);
		
		logicalTopo.addNewNodeToTopo(nodeRoles, name, aLConnections);
	}
	
	/**
	 * @author padres-dev-4187
	 * Parses the file in reader
	 * @returns false if there is an error; true if the parse is successful
	 */
	public boolean parse(String fileName)
	{
		boolean isParseSuccessful = true;
		String line;
		int linesRead = 0;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			while( (line = reader.readLine()) != null)
			{
				linesRead++;
				if(!checkIfLineIgnorable(line))
				{
					String[] splitLine = line.split(PSTBUtil.SPACE);
					
					if(checkProperSpacing(splitLine))
					{
						if(checkProperRoles(splitLine[NODE_ROLE_LOCATION]))
						{
							if(checkUniqueName(splitLine[NODE_NAME_LOCATION]))
							{
								logger.trace("Parser: Line " + linesRead + "'s syntax checks out.");
								addLineToTopo(splitLine);
							}
							else
							{
								isParseSuccessful = false;
								logger.error("Parser: Error in Line " + linesRead + " - Duplicate names");
							}
						}
						else
						{
							isParseSuccessful = false;
							logger.error("Parser: Error in Line " + linesRead + " - Error with Types");
						}
					}
					else
					{
						isParseSuccessful = false;
						logger.error("Parser: Error in Line " + linesRead + " - Length isn't correct");
					}
				}
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			isParseSuccessful = false;
			logger.error("Parser: Cannot find file", e);
		}
		return isParseSuccessful;
	}
}
