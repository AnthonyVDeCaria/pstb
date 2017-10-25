package pstb.util;

import java.io.File;
import java.util.Scanner;

/**
 * @author padres-dev-4187
 * 
 * This class handles PSTB's User Interface
 * allowing it to collect input from the user
 */
public class UI {
	
	/**
	 * Gets an input from the user
	 * 
	 * @param prompt - A prompt for the user
	 * @param userInterface - the scanner the user is utilizing to give their response
	 * @return their input
	 */
	public static String getInputFromUser(String prompt, Scanner userInterface)
	{
		System.out.println(prompt);
		
		return userInterface.next();
	}
	
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
		
		String userAns = getInputFromUser(prompt, userInterface);
		
		while(!userAns.equalsIgnoreCase("Y") && !userAns.equalsIgnoreCase("N"))
		{
			userAns = getInputFromUser("Incorrect response\n" + prompt, userInterface);
		}
		
		if(userAns.equalsIgnoreCase("Y"))
		{
			userResponse = true;
		}
		
		return userResponse;
	}
	
	/**
	 * Gets a file path from the user.
	 * This file path will be confirmed as a file on disc.
	 * 
	 * @param prompt - A prompt for the user
	 * @param userInterface - the scanner the user is utilizing to give their response
	 * @return a path to a file (as a String)
	 */
	public static String getAndCheckFilePathFromUser(String prompt, Scanner userInterface)
	{
		String userAns = getInputFromUser(prompt, userInterface);
		
		while(!new File(userAns).isFile())
		{
			userAns = getInputFromUser("Given File does not exist\n" + prompt, userInterface);
		}
		
		return userAns;
	}

}
