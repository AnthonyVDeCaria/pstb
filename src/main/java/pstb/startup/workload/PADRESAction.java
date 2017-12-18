/**
 * 
 */
package pstb.startup.workload;

/**
 * @author padres-dev-4187
 *
 */
public class PADRESAction extends PSAction {
	private static final long serialVersionUID = 1L;
	private String attributes;
	
	public PADRESAction()
	{
		super();
		attributes = new String();
	}
	
	public PADRESAction(PSActionType givenAT)
	{
		super(givenAT);
		attributes = new String();
	}
	
	public PADRESAction(PSActionType givenAT, Long givenAD)
	{
		super(givenAT, givenAD);
		attributes = new String();
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
	 * Gets the attributes
	 * @return the attributes
	 */
	public String getAttributes() 
	{
		return attributes;
	}
}
