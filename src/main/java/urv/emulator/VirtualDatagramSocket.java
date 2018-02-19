package urv.emulator;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

import org.jgroups.logging.Log;
import org.jgroups.logging.LogFactory;
import org.jgroups.util.ConcurrentLinkedBlockingQueue;

import urv.conf.PropertiesLoader;

/**
 * @author Gerard Paris Aixala
 *
 */
public class VirtualDatagramSocket extends MulticastSocket {
	
	//	CLASS FIELDS --
	
	private BlockingQueue<DatagramPacket> myReceivingQueue;
	private final ReceivingQueues receivingQueues = ReceivingQueues.getInstance();
	private final VirtualNetworkInformation vni = VirtualNetworkInformation.getInstance();
	private int localPort = 0;
	private InetAddress localAddr = null;
	private boolean enabled = false;
    protected final Log log=LogFactory.getLog(this.getClass());
    
    //	CONSTRUCTORS --
    
	public VirtualDatagramSocket(int port, InetAddress addr) throws IOException {
		super(null);
		this.localPort = port;
		this.localAddr = addr;		
		if (receivingQueues.getQueue(addr)==null){
			// Only a receivingQueue per host			
			enabled = true;			
			myReceivingQueue = new ConcurrentLinkedBlockingQueue<>(500);
			receivingQueues.registerQueue(addr, myReceivingQueue);			
			log.info("VirtualDatagramSocket created. Delivery probability: "+PropertiesLoader.getSendingProb());
		}		
	}	
	
	//	PUBLIC METHODS --
	
	/**
	 * Closes this virtual datagram socket.
	 *
	 */
	@Override
	public void close(){
		// do nothing
	}
	
	/**
	 * Receives a datagram packet from this socket.
	 * @param p the DatagramPacket into which to place the incoming data.
	 */
	@Override
	public synchronized void receive(DatagramPacket p) throws IOException{
		if (myReceivingQueue!=null){
			// blocks until a packet is available
			DatagramPacket packet;
			try {
				packet = myReceivingQueue.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
				// write dummy data into the DatagramPacket
				p.setAddress(InetAddress.getLoopbackAddress());
				p.setData(new byte[0]);
				p.setLength(0);
				p.setPort(0);
				p.setSocketAddress(new InetSocketAddress(0));
				return;
			}
			// Copying the received packet to the referenced packet
			p.setAddress(packet.getAddress());
			p.setData(packet.getData(),packet.getOffset(),packet.getLength());
			p.setLength(packet.getLength());
			p.setPort(packet.getPort());
			p.setSocketAddress(packet.getSocketAddress());
		}
	}
	
	/**
	 * Sends a datagram packet from this socket.
	 * @param p
	 */
	@Override
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
	@Override
	public synchronized void setReceiveBufferSize(int size) throws SocketException{
		// do nothing
	}
	
	/**
	 * Not implemented
	 * @param 
	 * @throws SocketException
	 */
	@Override
	public synchronized void setSendBufferSize(int size) throws SocketException{
		// do nothing
	}
	
	/**
	 * Not implemented
	 * @param tc
	 * @throws SocketException
	 */
	@Override
	public synchronized void setTrafficClass(int tc) throws SocketException{
		// do nothing
	}
	
	//	ACCESS METHODS --
	
	/**
	 * Gets the local address to which the socket is bound.
	 * @return the local address to which the socket is bound
	 */
	@Override
	public InetAddress getLocalAddress(){
		return localAddr;
	}
	
	/**
	 * Returns the port number on the local host to which this socket is bound. 
	 * @return he port number on the local host to which this socket is bound.
	 */	
	@Override
	public int getLocalPort(){
		return localPort;
	}
	
	@Override
	public synchronized int getReceiveBufferSize() throws SocketException{
		return 0;
	}
	
	@Override
	public synchronized int getSendBufferSize() throws SocketException{
		return 0;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	//	PRIVATE METHODS --
	
	private static boolean isBroadcastAddress(InetAddress addr){
		return addr.getAddress()[3] == (byte)255; 
	}
	
	private void sendBroadcast(DatagramPacket p){
		// Obtain the list of receivers
		List<InetAddress> neighbours = vni.getNeighbours(getLocalAddress());
		for (InetAddress addr : neighbours){
			if (addr!=null){ // Modified 31-04-2008
				Queue<DatagramPacket> queue = receivingQueues.getQueue(addr);
				sendToQueueProb(p,queue);
			}			
		}
	}
	
	private void sendToQueue(DatagramPacket p,Queue<DatagramPacket> q){
		//IF the queue has not been created yet, since the other
		//application is not created, discard paquet
		if (q==null){
			log.debug("Queue not created, message discarded:"+p);
			return;
		}
		q.add(p);
	}
	
	/**
	 * Sends a message to a queue with the probability specified in SENDING_PROB
	 * @param p
	 * @param queue
	 */
	private void sendToQueueProb(DatagramPacket p, Queue<DatagramPacket> queue) {
		Random rand = new Random();
		double randomDouble = rand.nextDouble();
		if (randomDouble<PropertiesLoader.getSendingProb()){
			sendToQueue(p,queue);
		}		
	}	
	private void sendUnicast(DatagramPacket p, InetAddress dest){
		Queue<DatagramPacket> queue = receivingQueues.getQueue(dest);
		sendToQueue(p,queue);
	}
	
	private static void simulateRandomPropagationDelay() {		
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