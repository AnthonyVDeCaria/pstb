/**
 * 
 */
package pstb.util;

import java.util.Scanner;

/**
 * @author padres-dev-4187
 *
 */
public class UI {
	
	/**
	 * Gets a Y or N answer from a user for a given prompt
	 * And returns it as a boolean.
	 * 
	 * @param prompt - A prompt for the user
	 * @param userInterface - the scanner the user is utilizing to give their response
	 * @return true if the user typed y or Y; false if they typed n or N
	 */
	public static boolean getYNAnswerFromUser(String prompt, Scanner userInterface)
	{
		boolean userResponse = false;
		
		System.out.println(prompt);
		
		String userAns = userInterface.next();
		
		while(!userAns.equalsIgnoreCase("Y") && !userAns.equalsIgnoreCase("N"))
		{
			userAns = userInterface.next();
		}
		
		if(userAns.equalsIgnoreCase("Y"))
		{
			userResponse = true;
		}
		return userResponse;
	}

}
