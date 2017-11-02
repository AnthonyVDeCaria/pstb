/**
 * 
 */
package pstb.startup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pstb.util.PSTBUtil;

/**
 * @author padres-dev-4187
 *
 */
public class DistributedFileParser {
	String disFileString;
	HashMap<String, ArrayList<Integer>> hostsAndPorts;
	
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
		hostsAndPorts = new HashMap<String, ArrayList<Integer>>();
    }
	
	public HashMap<String, ArrayList<Integer>> getHostsAndPorts()
	{
		return hostsAndPorts;
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
					String[] splitLine = line.split("	");
					
					if(splitLine.length == NUM_ELEMENTS)
					{
						String[] splitPorts = splitLine[LOC_PORTS].split(PSTBUtil.COMMA);
						int numPorts = splitPorts.length;
						
						for(int i = 0 ; i < numPorts ; i++)
						{
							Integer portI = PSTBUtil.checkIfInteger(splitPorts[i], false, null);
							if(portI != null)
							{
								lineIsPorts.add(portI);
							}
							else
							{
								isParseSuccessful = false;
								hostsAndPorts.clear();
								numPortsRead = 0;
								log.error(logHeader + "Error in Line " + linesRead + " - Port " + i + " is not an Integer!");
							}
						}
						
						if(isParseSuccessful)
						{
							hostsAndPorts.put(splitLine[LOC_HOST], lineIsPorts);
							numPortsRead += numPorts;
						}
						
					}
					else
					{
						isParseSuccessful = false;
						log.error(logHeader + "Error in Line " + linesRead + " - Length isn't correct!");
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
		
		if(isParseSuccessful)
		{
			if(numPortsRead < numPortsNeeded)
			{
				log.error(logHeader + "Not enough ports were found!");
				hostsAndPorts.clear();
				isParseSuccessful = false;
			}
		}
		
		return isParseSuccessful;
	}

}
