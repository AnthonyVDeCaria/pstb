/**
 * 
 */
package pstb.startup.distributed;

import java.util.ArrayList;

import pstb.util.PSTBUtil;

/**
 * @author adecaria
 *
 */
public class Machine
{
    public final int INVALID_ACTIVE_PORT = -1;
    public final Integer LOCAL_BROKER_PORT_START = 1100;
    
    private String machineName;
    private ArrayList<Integer> ports;
    private int i;
    
    /**
     * Local 
     * 
     * @param numBrokersNeeded
     */
    public Machine(int numBrokersNeeded)
    {
        machineName = PSTBUtil.LOCAL;
        
        ports = new ArrayList<Integer>();
        Integer localPortNum = LOCAL_BROKER_PORT_START;
        for(int i = 0 ; i < numBrokersNeeded ; i++)
        {
            ports.add(localPortNum);
            localPortNum++;
        }
        
        i = 0;
    }
    
    public Machine(String givenName, ArrayList<Integer> givenPorts)
    {
        machineName = givenName;
        ports = givenPorts;
        i = 0;
    }

    public void setMachineName(String givenName) {
        machineName = givenName;
    }
    
    public void setPorts(ArrayList<Integer> givenPorts)
    {
        ports = givenPorts;
    }
    
    public void addNewPort(Integer newPort)
    {
        ports.add(newPort);
    }
    
    public void updateActivePort()
    {
        i++;
        
        if(i >= ports.size())
        {
            i = INVALID_ACTIVE_PORT;
        }
    }
    
    public String getMachineName() {
        return machineName;
    }
    
    public ArrayList<Integer> getPorts()
    {
        return ports;
    }
    
    public Integer getActivePort()
    {
        Integer retVal = null;
        
        if(i != INVALID_ACTIVE_PORT)
        {
            retVal = ports.get(i);
        }
        
        return retVal;
    }
}
