/**
 * 
 */
package pstb.benchmark;

/**
 * @author padres-dev-4187
 *
 */
public class ServerPacket {
	private Object node;
	private boolean client;
	
	public ServerPacket(Object accessedNode, boolean isClient)
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
