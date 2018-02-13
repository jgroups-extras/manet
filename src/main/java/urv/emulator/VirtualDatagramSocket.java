package urv.emulator;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgroups.util.Queue;
import org.jgroups.util.QueueClosedException;

import urv.conf.PropertiesLoader;

/**
 * @author Gerard Paris Aixala
 *
 */
public class VirtualDatagramSocket {
	
	//	CLASS FIELDS --
	
	private Queue myReceivingQueue;
	private ReceivingQueues receivingQueues = ReceivingQueues.getInstance();
	private VirtualNetworkInformation vni = VirtualNetworkInformation.getInstance();	
	private int localPort = 0;
	private InetAddress localAddr = null;
	private boolean enabled = false;
    protected final Log log=LogFactory.getLog(this.getClass());
    
    //	CONSTRUCTORS --
    
	public VirtualDatagramSocket(int port,InetAddress addr) throws SocketException{		
		this.localPort = port;
		this.localAddr = addr;		
		if (receivingQueues.getQueue(addr)==null){
			// Only a receivingQueue per host			
			enabled = true;			
			myReceivingQueue = new Queue();
			receivingQueues.registerQueue(addr, myReceivingQueue);			
			log.info("VirtualDatagramSocket created. Delivery probability: "+PropertiesLoader.getSendingProb());
		}		
	}	
	
	//	PUBLIC METHODS --
	
	/**
	 * Closes this virtual datagram socket.
	 *
	 */
	public void close(){}	
	/**
	 * Receives a datagram packet from this socket.
	 * @param p the DatagramPacket into which to place the incoming data.
	 */
	public void receive(DatagramPacket p) throws IOException{
		if (myReceivingQueue!=null){
			try {
				// blocks until a packet is available
				DatagramPacket packet = (DatagramPacket)myReceivingQueue.remove();				
				// Copying the received packet to the referenced packet
				p.setAddress(packet.getAddress());
				p.setData(packet.getData(),packet.getOffset(),packet.getLength());
				p.setLength(packet.getLength());
				p.setPort(packet.getPort());
				p.setSocketAddress(packet.getSocketAddress());
			} catch (QueueClosedException e) {
				e.printStackTrace();
			}
		}
	}	
	/**
	 * Sends a datagram packet from this socket.
	 * @param p
	 */
	public void send(DatagramPacket p) throws IOException{
		InetAddress addr = p.getAddress();		
		simulateRandomPropagationDelay();
		if (isBroadcastAddress(addr)){
			sendBroadcast(p);
		} else {
			if (vni.areNeighbours(getLocalAddress(), addr)){
				sendUnicast(p,addr);		
			}
		}
	}	
	/**
	 * Not implemented
	 * @param 
	 * @throws SocketException
	 */
	public void setReceiveBufferSize(int size) throws SocketException{}
	
	/**
	 * Not implemented
	 * @param 
	 * @throws SocketException
	 */
	public void setSendBufferSize(int size) throws SocketException{}
	
	/**
	 * Not implemented
	 * @param tc
	 * @throws SocketException
	 */
	public void setTrafficClass(int tc) throws SocketException{}
	
	//	ACCESS METHODS --
	
	/**
	 * Gets the local address to which the socket is bound.
	 * @return the local address to which the socket is bound
	 */
	public InetAddress getLocalAddress(){
		return localAddr;
	}	
	/**
	 * Returns the port number on the local host to which this socket is bound. 
	 * @return he port number on the local host to which this socket is bound.
	 */	
	public int getLocalPort(){
		return localPort;
	}	
	public int getReceiveBufferSize() throws SocketException{
		return 0;
	}
	public int getSendBufferSize() throws SocketException{
		return 0;
	}	
	public boolean isEnabled() {
		return enabled;
	}
	
	//	PRIVATE METHODS --
	
	private boolean isBroadcastAddress(InetAddress addr){
		return addr.getAddress()[3] == (byte)255; 
	}	
	private void sendBroadcast(DatagramPacket p){
		// Obtain the list of receivers
		List<InetAddress> neighbours = vni.getNeighbours(getLocalAddress());
		for (InetAddress addr : neighbours){
			if (addr!=null){ // Modified 31-04-2008
				Queue queue = receivingQueues.getQueue(addr);
				sendToQueueProb(p,queue);
			}			
		}
	}	
	private void sendToQueue(DatagramPacket p,Queue q){
		try {
			//IF the queue has not been created yet, since the other
			//application is not created, discard paquet
			if (q==null){
				log.debug("Queue not created, message discarded:"+p);
				return;
			}
			q.add(p);
		} catch (QueueClosedException e) {
			e.printStackTrace();
		}
	}	
	/**
	 * Sends a message to a queue with the probability specified in SENDING_PROB
	 * @param p
	 * @param queue
	 */
	private void sendToQueueProb(DatagramPacket p, Queue queue) {
		Random rand = new Random();
		double randomDouble = rand.nextDouble();
		if (randomDouble<PropertiesLoader.getSendingProb()){
			sendToQueue(p,queue);
		}		
	}	
	private void sendUnicast(DatagramPacket p, InetAddress dest){
		Queue queue = receivingQueues.getQueue(dest);
		sendToQueue(p,queue);
	}
	private void simulateRandomPropagationDelay() {		
		//Thread sleep provides 1ms resolution
		//will generate a delay between 10 and 20 ms
		Random r = new Random(System.currentTimeMillis());		
		try {
			Thread.sleep(r.nextInt(10)+11);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}
}