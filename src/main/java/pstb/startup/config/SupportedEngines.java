/**
 * 
 */
package pstb.startup.config;

import pstb.util.PSTBUtil;
import siena.Op;

/**
 * @author padres-dev-4187
 *
 */
public class SupportedEngines {
	public enum SupportedEngine {
		PADRES, SIENA
	}
	
	public static final String WORKLOAD_FILE_TYPE_PADRES = ".pdrs";
	public static final String WORKLOAD_FILE_TYPE_SIENA = ".cna";
	
	public static boolean checkProperWorkloadFileEndings(String givenWFS)
	{
		String fileExtension = PSTBUtil.getFileExtension(givenWFS);
		
		return fileExtension.equals(WORKLOAD_FILE_TYPE_PADRES) || fileExtension.equals(WORKLOAD_FILE_TYPE_SIENA);
	}
	
	/**
	 * 
	 * 
	 * @param givenAttributes
	 * @return the converted attributes
	 */
	public static String convertPADRESAttributesToSIENA(String givenAttributes)
	{
		String retVal = new String();
		
		String[] brokenAttributes = givenAttributes.split("],");
		for(int i = 0 ; i < brokenAttributes.length ; i++)
		{
			String cleanedSegmentI = brokenAttributes[i].replace("[", "").replace("]", "").replace("'", "").replaceAll("isPresent", "any");
			
			retVal += cleanedSegmentI;
			retVal += "|";
		}
		
		return retVal;
	}
	
	/**
	 * 
	 * @param givenAttributes
	 * @return null if there is an error; the converted string otherwise
	 */
	public static String convertSIENAAttributesToPADRES(String givenAttributes)
	{
		String retVal = new String();
		
		String[] brokenAttributes = givenAttributes.split("\\|");
		int i = 0;
		while(true)
		{
			String segmentI = brokenAttributes[i];
			
			String[] segmentIComponets = segmentI.split(",");
			int numComponents = segmentIComponets.length;
			String key = segmentIComponets[0];
			String operator = new String();
			String givenValue = segmentIComponets[numComponents - 1];
			String value = new String();
			
			retVal += "[" + key;
			
			if(numComponents == 3)
			{
				operator = segmentIComponets[1];
				int operatorCheck = Op.op(operator);
				if(operatorCheck == Op.NE || operatorCheck == Op.PF || operatorCheck == Op.SF || operatorCheck == Op.SS)
				{
					return null;
				}
				else if(operatorCheck == Op.ANY)
				{
					operator = "isPresent";
				}
				else if(operatorCheck == Op.EQ)
				{
					operator = "eq";
				}
				
				retVal += "," + operator;
			}
			
			Double doubleCheck = PSTBUtil.checkIfDouble(givenValue, false, null);
			if(doubleCheck == null)
			{
				if(givenValue.indexOf('\"') < 0) // " isn't present
				{
					value += "'";
					value += segmentIComponets[numComponents - 1];
					value += "'";
				}
				else
				{
					value = givenValue;
				}
			}
			else
			{
				value = doubleCheck.toString();
			}
			
			retVal += "," + value + "]";
			
			i++;
			if(i < brokenAttributes.length)
			{
				retVal += ",";
			}
			else
			{
				break;
			}
		}
		
		return retVal;
	}
}

