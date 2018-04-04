/**
 * 
 */
package pstb.startup.distributed;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 *
 */
public class DistributedFileParser {
	String disFileString;
	ArrayList<Machine> machines;
	
	private final int NUM_ELEMENTS = 2;
	private final int LOC_HOST = 0;
	private final int LOC_PORTS = 1;
	
	Logger log = LogManager.getRootLogger();
	String logHeader = "Distributed Parser: ";
	
	/**
	 * FilePath Constructor
	 * 
	 * @param givenDFS - the Distributed File String to parse
	 * @param log - the Logger we have to use
	 */
	public DistributedFileParser(String givenDFS)
	{
		disFileString = givenDFS;
		machines = new ArrayList<Machine>();
    }
	
	public ArrayList<Machine> getHostsAndPorts()
	{
		return machines;
	}
	
	public boolean parse(int numPortsNeeded)
	{
		boolean isParseSuccessful = true;
		String line = null;
		int linesRead = 0;
		int numPortsRead = 0;
		
		try 
		{
			BufferedReader tFReader = new BufferedReader(new FileReader(disFileString));
			while( (line = tFReader.readLine()) != null)
			{
				linesRead++;
				ArrayList<Integer> lineIsPorts = new ArrayList<Integer>();
				
				if(!PSTBUtil.checkIfLineIgnorable(line))
				{
					String[] splitLine = line.split(PSTBUtil.COLUMN_SEPARATOR);
					
					if(splitLine.length != NUM_ELEMENTS)
					{
						isParseSuccessful = false;
						log.error(logHeader + "Error in Line " + linesRead + " - Length isn't correct!");
					}
					else
					{
						String[] splitPorts = splitLine[LOC_PORTS].split(PSTBUtil.ITEM_SEPARATOR);
						int numSegments = splitPorts.length;
						
						for(int i = 0 ; i < numSegments ; i++)
						{
							String portIString = splitPorts[i];
							
							if(portIString.contains("-"))
							{
								String[] rangeI = portIString.split("-");
								
								if(rangeI.length != 2)
								{
									isParseSuccessful = false;
									log.error(logHeader + "Error in Line " + linesRead + " - Port range " + portIString + " is improper!");
								}
								else
								{
									Integer lowerBound = PSTBUtil.checkIfInteger(rangeI[0], false, null);
									Integer upperBound = PSTBUtil.checkIfInteger(rangeI[1], false, null);
									
									if(lowerBound == null || upperBound == null)
									{
										isParseSuccessful = false;
										log.error(logHeader + "Error in Line " + linesRead + " - Port range " + portIString 
													+ " contains a port that isn't an Integer!");
									}
									else
									{
										if(lowerBound > upperBound)
										{
											log.warn(logHeader + "Line " + linesRead + " has a port bound that goes from upper to lower, "
														+ "instead of lower to upper.");
											for(int j = upperBound.intValue() ; j < lowerBound.intValue(); j++)
											{
												lineIsPorts.add(j);
												numPortsRead++;
											}
										}
										else if(lowerBound == upperBound)
										{
											log.warn(logHeader + "Line " + linesRead + " has a port range that only contains one number.");
											lineIsPorts.add(lowerBound);
											numPortsRead++;
										}
										else
										{
											for(int j = lowerBound.intValue() ; j < upperBound.intValue(); j++)
											{
												lineIsPorts.add(j);
												numPortsRead++;
											}
										}
									}
								}
							}
							else
							{
								Integer portI = PSTBUtil.checkIfInteger(portIString, false, null);
								if(portI != null)
								{
									lineIsPorts.add(portI);
									numPortsRead++;
								}
								else
								{
									isParseSuccessful = false;
									log.error(logHeader + "Error in Line " + linesRead + " - Port " + i + " is not an Integer!");
								}
							}
						}
						
						if(isParseSuccessful)
						{
							Machine machineI = new Machine(splitLine[LOC_HOST], lineIsPorts);
							machines.add(machineI);
						}
					}
				}
			}
			tFReader.close();
		} 
		catch (IOException e) 
		{
			isParseSuccessful = false;
			log.error(logHeader + "Cannot find file: ", e);
		}
		
		if(!isParseSuccessful)
		{
			machines.clear();
		}
		else if(numPortsRead < numPortsNeeded)
		{
			machines.clear();
			log.error(logHeader + "Not enough ports were found!");
			isParseSuccessful = false;
		}
		
		return isParseSuccessful;
	}

}
