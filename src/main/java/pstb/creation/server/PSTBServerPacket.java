/**
 * 
 */
package pstb.creation.server;

/**
 * @author padres-dev-4187
 *
 */
public class PSTBServerPacket {
	private Object node;
	private boolean client;
	
	public PSTBServerPacket(Object accessedNode, boolean isClient)
	{
		node = accessedNode;
		client = isClient;
	}
	
	public Object getNode()
	{
		return node;
	}
	
	public boolean isClient()
	{
		return client;
	}
}
