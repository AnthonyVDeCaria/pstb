package pstb.analysis.analysisobjects;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class PSTBAnalysisObject {
    protected String name;
    
    // Logger
    protected String logHeader;
    protected final Logger log = LogManager.getRootLogger();
    
    public PSTBAnalysisObject()
    {
        name = null;
    }
    
    /**
     * Sets the name
     * 
     * @param givenName - the new name
     */
    public void setName(String givenName) {
        name = givenName;
    }
        
    /**
     * Gets the name
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }
    
    public boolean recordAO(Path givenFilePath)
    {
        try
        {
            Files.deleteIfExists(givenFilePath);
            Files.createFile(givenFilePath);
        }
        catch(Exception e)
        {
            log.error(logHeader + "Couldn't recreate file " + givenFilePath + ": ", e);
            return false;
        }
        
        boolean recordCheck = completeRecord(givenFilePath);
        if(!recordCheck)
        {
            log.error("Couldn't complete record!");
            return false;
        }
        
        return true;
    }
    
    /**
     * Finishes the printing
     * 
     * @param givenFilePath - the location to print to
     * @return false on failure; true otherwise
     */
    public abstract boolean completeRecord(Path givenFilePath);
}
