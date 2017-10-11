/**
 * @author padres-dev-4187
 *
 */
package pstb.util;

public class PSAction implements java.io.Serializable
{
	private static final long serialVersionUID = 1L;
	
	private Long actionDelay;	// nanoseconds
	private String attributes;
	private Long payloadSize;	// bytes
	private Long timeActive;	// nanoseconds
	
	/**
	 * Empty Constructor
	 */
	public PSAction()
	{
		actionDelay = new Long(-1);
		attributes = new String();
		payloadSize = new Long(-1);
		timeActive = new Long(0);
	}
	
	/**
	 * Gets the action delay (nanoseconds)
	 * @return the action delay
	 */
	public Long getActionDelay()
	{
		return actionDelay;
	}
	
	/**
	 * Gets the attributes
	 * @return the attributes
	 */
	public String getAttributes() 
	{
		return attributes;
	}
	
	/**
	 * Gets the payload size (bytes)
	 * @return the payload size
	 */
	public Long getPayloadSize() 
	{
		return payloadSize;
	}
	
	/**
	 * Gets the time active
	 * @return the time active (nanoseconds)
	 */
	public Long getTimeActive() 
	{
		return timeActive;
	}
	
	/**
	 * Sets the delay between actions
	 * @param nAD - the given delay
	 */
	public void setActionDelay(Long nAD)
	{
		this.actionDelay = nAD;
	}
	
	/**
	 * Sets the attributes
	 * @param nAttri - the attributes to be set
	 */
	public void setAttributes(String nAttri) 
	{
		this.attributes = nAttri;
	}
	
	/**
	 * Sets the payload size
	 * @param nPS - the new payload size
	 */
	public void setPayloadSize(Long nPS) 
	{
		this.payloadSize = nPS;
	}
	
	/**
	 * Sets the time active
	 * @param nTA - the new time active
	 */
	public void setTimeActive(Long nTA) 
	{
		this.timeActive = nTA;
	}
}
