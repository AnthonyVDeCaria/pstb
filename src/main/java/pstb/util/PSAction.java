/**
 * @author padres-dev-4187
 *
 */
package pstb.util;

public class PSAction {
	private String attributes;
	private Integer payloadSize;
	private Long timeActive;
	
	/**
	 * Empty Constructor
	 * Both Integers are set to unrealistic numbers
	 * i.e. the payload can't be -1 bytes,
	 * nor can the time active be 0 minutes
	 */
	public PSAction()
	{
		attributes = new String();
		payloadSize = new Integer(-1);
		timeActive = new Long(0);
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
	public Integer getPayloadSize() {
		return payloadSize;
	}
	
	/**
	 * Gets the time active
	 * @return the time active ()
	 */
	public Long getTimeActive() {
		return timeActive;
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
	public void setPayloadSize(Integer nPS) {
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
