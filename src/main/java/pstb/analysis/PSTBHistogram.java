/**
 * 
 */
package pstb.analysis;

/**
 * @author padres-dev-4187
 * 
 * Extends the histogram class to allow the inclusion of a name
 * @see Histogram
 */
public class PSTBHistogram extends Histogram {
	private String histogramName;
	
	public PSTBHistogram()
	{
		super();
		setHistogramName(new String());
	}

	/**
	 * Sets the histogramName
	 * 
	 * @param givenHN the histogramName to set
	 */
	public void setHistogramName(String givenHN) {
		this.histogramName = givenHN;
	}
	
	/**
	 * Gets the histogramName
	 * 
	 * @return the histogramName
	 */
	public String getHistogramName() {
		return histogramName;
	}

}
