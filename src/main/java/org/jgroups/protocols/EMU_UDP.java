package org.jgroups.protocols;

import org.jgroups.Address;
import org.jgroups.Global;
import org.jgroups.Message;
import org.jgroups.annotations.Property;
import org.jgroups.stack.IpAddress;
import urv.emulator.VirtualDatagramSocket;
import urv.emulator.VirtualNetworkInformation;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Collection;

/**
 * This protocol is an adaptation of the original UDP from JGroups
 * in order to control the transport layer in emulated environments
 * 
 * @author Marcel Arrufat
 * @author Gerard Paris
 * @author Raul Gracia
 * 
 */
public class EMU_UDP extends UDP {

    private final static String MCAST_RECEIVER_THREAD_NAME = "UDP mcast receiver";

    private final VirtualNetworkInformation vni = VirtualNetworkInformation.getInstance();

    /** The multicast address used for sending and receiving packets */
    String          mcast_addr_name="228.8.8.8";

    /** The multicast receiver thread */
    Thread          mcast_receiver;

    /** The unicast receiver thread */
    UcastReceiver   ucast_receiver;
  
    /** Number of the emulated node */
    @Property(description="Number of the emulated node.")
    protected int				emu_node_id;

    @Property(description="Port number of the emulated node")
    protected int				emu_port;


    /**
     * Creates the EMU_UDP protocol, and initializes the
     * state variables, does however not start any sockets or threads.
     */
    public EMU_UDP() {
    }

    @Override
	public String getInfo() {
        StringBuilder sb=new StringBuilder();
        sb.append("group_addr=").append(mcast_addr_name).append(':').append(mcast_port).append("\n");
        return sb.toString();
    }
    
    /* ----------------------- Receiving of MCAST UDP packets ------------------------ */

    @Override
	public String getName() {
        return "EMU_UDP";
    }

    public static void postUnmarshalling(Message msg, Address dest, Address src, boolean multicast) {
        msg.setDest(dest);
    }

    public static void postUnmarshallingList(Message msg, Address dest, boolean multicast) {
        msg.setDest(dest);
    }
    
    @Override
	protected void sendToMembers(Collection<Address> mbrs, byte[] buf, int offset, int length) throws Exception {
    	// do nothing
    }

    @Override
	public void sendToSingleMember(Address dest, byte[] data, int offset, int length) throws Exception {
        _send(((IpAddress)dest).getIpAddress(), ((IpAddress)dest).getPort(), data, offset, length);
    }



    /* ------------------------------ Private Methods -------------------------------- */

    void closeSocket() {
        if(sock != null) {
            sock.close();
            sock=null;
            if(log.isDebugEnabled()) log.debug("socket closed");
        }
    }
    
    /**
     * Create EMU_UDP sender and receiver sockets. Currently there are 2 sockets
     * (sending and receiving). This is due to Linux's non-BSD compatibility
     * in the JDK port (see DESIGN).
     */
    @Override
	protected void createSockets() throws Exception {
        if(bind_addr != null)
            log.debug("sockets will use interface " + bind_addr.getHostAddress());
        sock = createVirtualDatagramSocket();        	
        if(tos > 0) {
            try {
                sock.setTrafficClass(tos);
            }
            catch(SocketException e) {
                log.warn("traffic class of " + tos + " could not be set, will be ignored");
                if(log.isDebugEnabled())
                    log.debug("Cause of failure to set traffic class:", e);
            }
        }
        if(sock == null)
            throw new Exception("EMU_UDP.createSocket(): sock is null");

        local_addr=new IpAddress(sock.getLocalAddress(), sock.getLocalPort());
        log.debug("socket information:\n" + dumpSocketInfo());
    }
    
    protected VirtualDatagramSocket createVirtualDatagramSocket() {
    	VirtualDatagramSocket tmp = null;
            try {
                tmp=new VirtualDatagramSocket(emu_port, vni.getEmuNodeAddress(emu_node_id)); // first time localPort is 0
            }
            catch(IOException bindException) {
                // Vladimir May 30th 2007
                // special handling for Linux 2.6 kernel which sometimes throws BindException while we probe for a random port
                //localPort++;
            	bindException.printStackTrace();
            }
        return tmp;
    }
    
    @Override
	protected void setThreadNames() {
        super.setThreadNames();        
        if(mcast_receiver != null) {
        	mcast_receiver.setName(MCAST_RECEIVER_THREAD_NAME);
        	if(ucast_receiver != null)
        		ucast_receiver.getThread().setName(UcastReceiver.UCAST_RECEIVER_THREAD_NAME);
        		// TODO implement intended Thread name pattern
        }        
    }
    
    @Override
	protected void unsetThreadNames() {
        super.unsetThreadNames();       
        if(ucast_receiver != null && ucast_receiver.getThread() != null)
        	ucast_receiver.getThread().setName(UcastReceiver.UCAST_RECEIVER_THREAD_NAME);        	                      
    		// TODO implement intended Thread name pattern
    }

    /**
     * Closed UDP unicast and multicast sockets
     */
    void closeSockets() {
        // 2. Close socket
        closeSocket();
    }

    /**
     * Starts the unicast and multicast receiver threads
     */
    @Override
	protected void startThreads() throws Exception {
        if(ucast_receiver == null) {
            //start the listener thread of the ucast_recv_sock
            ucast_receiver=new UcastReceiver();
            ucast_receiver.start();
            if(thread_naming_pattern != null)
            	ucast_receiver.getThread().setName(UcastReceiver.UCAST_RECEIVER_THREAD_NAME);
            	// TODO implement intended Thread name pattern
            if(log.isDebugEnabled())
                log.debug("created unicast receiver thread " + ucast_receiver.getThread());
        }
    }


    /**
     * Stops unicast and multicast receiver threads
     */
    @Override
	protected void stopThreads() {
        // 2. Stop the unicast receiver thread
        if(ucast_receiver != null) {
            ucast_receiver.stop();
            ucast_receiver=null;
        }
    }

    /* ----------------------------- End of Private Methods ---------------------------------------- */

    /* ----------------------------- Inner Classes ---------------------------------------- */

    public class UcastReceiver implements Runnable {
    	
    	public static final String UCAST_RECEIVER_THREAD_NAME = "EMU_UDP ucast receiver";
        boolean running=true;
        Thread thread=null;

        public Thread getThread(){
        	return thread;
        }
        

        @Override
		public void run() {
            DatagramPacket  packet;
            byte            receive_buf[]=new byte[65535];
            int             offset, len;
            byte[]          data;
            InetAddress     sender_addr;
            int             sender_port;
            Address         sender;

            // moved out of loop to avoid excessive object creations (bela March 8 2001)
            packet=new DatagramPacket(receive_buf, receive_buf.length);

            while(running && thread != null && sock != null) {
                try {
                    packet.setData(receive_buf, 0, receive_buf.length);
                    sock.receive(packet);
                    sender_addr=packet.getAddress();
                    sender_port=packet.getPort();
                    offset=packet.getOffset();
                    len=packet.getLength();
                    data=packet.getData();
                    sender=new IpAddress(sender_addr, sender_port);
                    
                    if(len > receive_buf.length) {
                        if(log.isErrorEnabled())
                            log.error("size of the received packet (" + len + ") is bigger than allocated buffer (" +
                                      receive_buf.length + "): will not be able to handle packet. " +
                                      "Use the FRAG protocol and make its frag_size lower than " + receive_buf.length);
                    }
                    receive(sender, data, offset, len);
                }
                catch(SocketException sock_ex) {
                    if(log.isDebugEnabled()) log.debug("unicast receiver socket is closed, exception=" + sock_ex);
                    break;
                }
                catch(InterruptedIOException io_ex) { // thread was interrupted
                }
                catch(Throwable ex) {
                    if(log.isErrorEnabled())
                        log.error("[" + local_addr + "] failed receiving unicast packet", ex);
                }
            }
            if(log.isDebugEnabled()) log.debug("unicast receiver thread terminated");
        }
        public void start() {
            if(thread == null) {
                thread=new Thread(this, UCAST_RECEIVER_THREAD_NAME);
                // thread.setDaemon(true);
                running=true;
                thread.start();
            }
        }
        public void stop() {
            Thread tmp;
            if(thread != null && thread.isAlive()) {
                running=false;
                tmp=thread;
                thread=null;
                closeSocket(); // this will cause the thread to break out of its loop
                tmp.interrupt();
                try {
                    tmp.join(Global.THREAD_SHUTDOWN_WAIT_TIME);
                }
                catch(InterruptedException e) {
                    Thread.currentThread().interrupt(); // set interrupt flag again
                }
                tmp=null;
            }
            thread=null;
        }
    }
}