/**
 * 
 */
package pstb.util;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author adecaria
 *
 */
public class FlowingQueue {
	ArrayBlockingQueue<Object> queue;
	int qSize;
	
	public FlowingQueue(int queueSize)
	{
		qSize = queueSize;
		queue = new ArrayBlockingQueue<Object>(queueSize);
	}
	
	public void add(Object newObj)
	{
		if(queue.size() == qSize)
		{
			queue.remove();
		}
		
		queue.add(newObj);
	}
	
	public Object[] getAllElements()
	{
		return queue.toArray();
	}
}
