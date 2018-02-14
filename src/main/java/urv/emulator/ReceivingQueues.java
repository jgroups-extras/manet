package urv.emulator;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;


/**
 * @author Gerard Paris Aixala
 *
 */
public class ReceivingQueues {
	
	//	CLASS FIELDS --
	
	private final Map<InetAddress,Queue> queues = new HashMap<>();
	
	//	CONSTRUCTORS --
	
	private ReceivingQueues(){}
	
	//	STATIC METHODS --
	
	public static ReceivingQueues getInstance() {
		return SingletonHolder.INSTANCE;
	}
	
	//	PUBLIC METHODS --
	
	/**
	 * Registers a receiving queue given the InetAddress of the host 
	 * @param id
	 * @param q
	 */
	public void registerQueue(InetAddress id, Queue q){
		queues.put(id, q);
	}
	
	//	ACCESS METHODS --
	
	public Queue getQueue(InetAddress id){
		return queues.get(id);
	}
	
	//	PRIVATE METHODS --
	
	private static class SingletonHolder {
		private final static ReceivingQueues INSTANCE = new ReceivingQueues();
	}	
}