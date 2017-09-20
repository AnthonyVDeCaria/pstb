/**
 * 
 */
package pstb.benchmark;

import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Message;

/**
 * @author padres-dev-4187
 *
 */
public class PADRESClientExtension extends Client 
{
	PSClientPADRES hiddenClient;
	
	public PADRESClientExtension(ClientConfig givenCConfig, PSClientPADRES givenClient) throws ClientException
	{
		super(givenCConfig);
		hiddenClient = givenClient;
	}
	
	public PADRESClientExtension(String clientName, PSClientPADRES givenClient) throws ClientException
	{
		super(clientName);
		hiddenClient = givenClient;
	}
	
	@Override
	public void processMessage(Message msg) 
	{
		hiddenClient.storePublication(msg);
	}
}
