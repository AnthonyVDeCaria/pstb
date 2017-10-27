/**
 * 
 */
package pstb.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import pstb.util.PSTBUtil;
import pstb.util.PSTBUtil.TimeType;

/**
 * @author padres-dev-4187
 * 
 * A file that allows us to store an average delay
 * and a name associated with it 
 * @see Analyzer
 */
public class PSTBDelay {
	private String name;
	private Long value;

	/**
	 * Constructor
	 */
	public PSTBDelay() {
		setName(new String());
		setValue(new Long(0));
	}

	/**
	 * Sets the name
	 * 
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Sets the value
	 * 
	 * @param value the value to set
	 */
	public void setValue(Long value) {
		this.value = value;
	}
	
	/**
	 * Gets the name of this Delay
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the value
	 * 
	 * @return the value
	 */
	public Long getValue() {
		return value;
	}
	
	/**
	 * Prints the delay value
	 * 
	 * @param givenFilePath - the location to print to
	 * @return false on failure; true otherwise
	 */
	public boolean printDelay(Path givenFilePath)
	{
		String line = PSTBUtil.createTimeString(value, TimeType.Nano);
		
		try
		{
			if(Files.exists(givenFilePath))
			{
				Files.write(givenFilePath, line.getBytes(), StandardOpenOption.APPEND);
			}
			else
			{
				Files.write(givenFilePath, line.getBytes());
			}
		}
		catch(IOException e)
		{
			return false;
		}
		
		return true;
	}

}
