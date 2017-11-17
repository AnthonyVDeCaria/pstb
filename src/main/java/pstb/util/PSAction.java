/**
 * @author padres-dev-4187
 *
 */
package pstb.util;

public class PSAction implements java.io.Serializable
{
	private static final long serialVersionUID = 1L;
	
	private PSActionType actionType;
	private Long actionDelay;	// nanoseconds
	private String attributes;
	private Integer payloadSize;	// bytes
	private Long timeActive;	// nanoseconds
	
	/**
	 * Empty Constructor
	 */
	public PSAction()
	{
		actionType = null;
		actionDelay = new Long(-1);
		attributes = new String();
		payloadSize = new Integer(-1);
		timeActive = new Long(0);
	}
	
	/**
	 * ActionType Constructor
	 */
	public PSAction(PSActionType givenAT)
	{
		actionType = givenAT;
		actionDelay = new Long(-1);
		attributes = new String();
		payloadSize = new Integer(-1);
		timeActive = new Long(0);
	}
	
	/**
	 * Gets the actionType of this action
	 * 
	 * @return the action type
	 */
	public PSActionType getActionType()
	{
		return actionType;
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
	public Integer getPayloadSize() 
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
	 * Sets the action Type of this action
	 * @param nPSAT - the given action type
	 */
	public void setActionType(PSActionType nPSAT)
	{
		this.actionType = nPSAT;
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
	public void setPayloadSize(Integer nPS) 
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
