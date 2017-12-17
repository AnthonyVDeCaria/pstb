package pstb.startup.topology;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	private String topoFileString;
	private Set<String> submittedWorkloadFilesStrings;
	private LogicalTopology logicalTopo;
	
	private final int MIN_SEGMENTS = 3;
	private final int MAX_SEGMENTS = 4;
	private final int LOC_NAME = 0;
	private final int LOC_ROLE = 1;
	private final int LOC_CONN = 2;
	private final int LOC_WORKLOAD_FILE = 3;
	
	private final String logHeader = "Topology Parser: ";
	private Logger logger = LogManager.getRootLogger();
	
	/**
	 * FilePath Constructor
	 * 
	 * @param givenTFS - the Topology File String to parse
	 * @param log - the Logger we have to use
	 */
	public TopologyFileParser(String givenTFS, Set<String> givenWFS)
    {
		topoFileString = givenTFS;
		submittedWorkloadFilesStrings = givenWFS;
		logicalTopo = new LogicalTopology();
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
		try 
		{
			BufferedReader tFReader = new BufferedReader(new FileReader(topoFileString));
			while( (line = tFReader.readLine()) != null)
			{
				linesRead++;
				if(!PSTBUtil.checkIfLineIgnorable(line))
				{
					String[] splitLine = line.split(PSTBUtil.COLUMN_SEPARATOR);
					int lineIsLength = splitLine.length;
					if(lineIsLength < MIN_SEGMENTS || lineIsLength > MAX_SEGMENTS)
					{
						isParseSuccessful = false;
						logger.error(logHeader + "Line " + linesRead + "'s length isn't correct!");
					}
					else
					{
						String name = splitLine[LOC_NAME];
						String role = splitLine[LOC_ROLE];
						String connections = splitLine[LOC_CONN];
						String workloadFileString = new String();
						
						NodeRole lineIsRole = checkProperRoles(role);
						if(lineIsRole == null)
						{
							isParseSuccessful = false;
							logger.error(logHeader + "Line " + linesRead + "'s references a role that doesn't exist!");
						}
						else
						{
							if(!checkProperLineLength(lineIsRole, splitLine.length))
							{
								isParseSuccessful = false;
								logger.error(logHeader + "Line " + linesRead + "'s length isn't correct for its role!");
							}
							else
							{
								if(!checkUniqueName(name))
								{
									isParseSuccessful = false;
									logger.error(logHeader + "Line " + linesRead + " conatins a previously used name!");
								}
								else
								{
									if(lineIsRole.equals(NodeRole.C))
									{
										workloadFileString = splitLine[LOC_WORKLOAD_FILE];
										
										if(!submittedWorkloadFilesStrings.contains(workloadFileString))
										{
											isParseSuccessful = false;
											logger.error(logHeader + "Line " + linesRead + " conatins a unsubmitted workload!");
										}
									}
									
									if(isParseSuccessful)
									{
										logger.trace(logHeader + "Line " + linesRead + "'s syntax checks out.");
										
										ArrayList<String> splitConnections = PSTBUtil.turnStringArrayIntoArrayListString(
																					connections.split(PSTBUtil.ITEM_SEPARATOR)
																				);
										
										isParseSuccessful = logicalTopo.addNewNodeToTopo(lineIsRole, name, splitConnections, 
																								workloadFileString);
										if(!isParseSuccessful)
										{
											logger.error(logHeader + "Couldn't add line " + linesRead + " to the logical topology!");
										}
									}
								}
							}
						}
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
	 * Determines if the role listed in the file line are proper PubSub roles
	 * 
	 * @param role - String of the role from the file
	 * @return true if they are; false if they aren't
	 */
	private NodeRole checkProperRoles(String role)
	{		
		NodeRole properRole = null;
		try
		{
			properRole = NodeRole.valueOf(role);
		}
		catch(IllegalArgumentException e)
		{
			logger.error(logHeader + role + " is not a valid!", e);
			return null;
		}
		return properRole;
	}
	
	/**
	 * Checks if a given line has the proper number of columns
	 * 
	 * @param givenRole - the role for this line
	 * @param lineLength - the length of this line
	 * @return false if it's not; true if it is
	 */
	private boolean checkProperLineLength(NodeRole givenRole, int lineLength)
	{
		switch(givenRole)
		{
			case B:
			{
				return (lineLength == MIN_SEGMENTS); 
			}
			case C:
			{
				return (lineLength == MAX_SEGMENTS);
			}
			default:
			{
				return false;
			}
		}
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
