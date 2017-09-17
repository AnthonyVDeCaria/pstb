/**
 * @author padres-dev-4187
 *
 */
package pstb.util;

public class Workload {
	private String attributes;
	private Integer payloadSize;
	private Integer timeActive;
	
	/**
	 * Empty Constructor
	 * Both Integers are set to unrealistic numbers
	 * i.e. the payload can't be -1 bytes,
	 * nor can the time active be 0 TK(units)
	 */
	public Workload()
	{
		attributes = new String();
		payloadSize = new Integer(-1);
		timeActive = new Integer(0);
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
	public Integer getTimeActive() {
		return timeActive;
	}
	
	public void setAttributes(String attributes) {
		this.attributes = attributes;
	}
	
	public void setPayloadSize(Integer payloadSize) {
		this.payloadSize = payloadSize;
	}

	public void setTimeActive(Integer timeActive) {
		this.timeActive = timeActive;
	}

}
