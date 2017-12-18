package pstb.benchmark.client.padres;

import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Message;

/**
 * @author padres-dev-4187
 * 
 * This code extends the PADRES client.
 * Allowing us to overwrite its processMessage() with storePublication()
 */
public class PADRESClientExtension extends Client  
{
	PSTBClientPADRES hiddenClient;
	
	/**
	 * Constructor
	 */
	public PADRESClientExtension(ClientConfig givenCConfig, PSTBClientPADRES givenClient) throws ClientException
	{
		super(givenCConfig);
		hiddenClient = givenClient;
	}
	
	/**
	 * A processMessage Override
	 * Allowing us to call our storePublication function
	 * @see storePublication()
	 */
	@Override
	public void processMessage(Message msg) 
	{
		hiddenClient.storePublication(msg);
	}
}
