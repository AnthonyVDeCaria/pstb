/**
 * @author padres-dev-4187
 *
 * A collection of useful functions.
 */
package pstb.util;

import java.util.ArrayList;
import java.util.Arrays;

public class PSTBUtil {
	
	public static boolean isInteger(String s) 
	{
		boolean isValidInteger = false;
		try
		{
			Integer.parseInt(s);
			// s is a valid integer
			isValidInteger = true;
		}
		catch (NumberFormatException ex)
		{
			// s is not an integer
		}
		return isValidInteger;
	}
	
	public static ArrayList<String> turnStringArrayIntoArrayListString(String[] input)
	{
		return new ArrayList<String>(Arrays.asList(input));
	}

}
