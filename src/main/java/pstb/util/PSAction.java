/**
 * @author padres-dev-4187
 *
 */
package pstb.util;

public class PSAction implements java.io.Serializable
{
	private Long actionDelay;
	private String attributes;
	private Long payloadSize;
	private Long timeActive;
	
	/**
	 * Empty Constructor
	 * Both Integers are set to unrealistic numbers
	 * i.e. the delay between actions can't be -1 TK(UNITS)
	 * nor can the payload/time active be -1 bytes/minutes,
	 * nor can the time active be 0 minutes
	 */
	public PSAction()
	{
		actionDelay = new Long(-1);
		attributes = new String();
		payloadSize = new Long(-1);
		timeActive = new Long(0);
	}
	
	/**
	 * Gets the action delay TK(UNITS)
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
	public String getAttributes() {
		return attributes;
	}
	
	/**
	 * Gets the payload size (bytes)
	 * @return the payload size
	 */
	public Long getPayloadSize() {
		return payloadSize;
	}
	
	/**
	 * Gets the time active
	 * @return the time active (minutes)
	 */
	public Long getTimeActive() {
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
	public void setAttributes(String nAttri) {
		this.attributes = nAttri;
	}
	
	/**
	 * Sets the payload size
	 * @param nPS - the new payload size
	 */
	public void setPayloadSize(Long nPS) {
		this.payloadSize = nPS;
	}
	
	/**
	 * Sets the time active
	 * @param nTA - the new time active
	 */
	public void setTimeActive(Long nTA) {
		this.timeActive = nTA;
	}
}
