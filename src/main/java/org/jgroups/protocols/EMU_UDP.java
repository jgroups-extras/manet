package org.jgroups.protocols;

import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Properties;

import org.jgroups.Address;
import org.jgroups.Global;
import org.jgroups.Message;
import org.jgroups.stack.IpAddress;
import org.jgroups.util.Util;

import urv.emulator.VirtualDatagramSocket;
import urv.emulator.VirtualNetworkInformation;

/**
 * This protocol is an adaptation of the original UDP from JGroups
 * in order to control the transport layer in emulated environments
 * 
 * @author Marcel Arrufat
 * @author Gerard Paris
 * @author Raul Gracia
 * 
 */
public class EMU_UDP extends TP {

    private final static String MCAST_RECEIVER_THREAD_NAME = "UDP mcast receiver";

    private VirtualNetworkInformation vni = VirtualNetworkInformation.getInstance();

    /** Socket used for
     * <ol>
     * <li>sending unicast packets and
     * <li>receiving unicast packets
     * </ol>
     * The address of this socket will be our local address (<tt>local_addr</tt>) */
    VirtualDatagramSocket  sock=null;


    /** Maintain a list of local ports opened by DatagramSocket. If this is 0, this option is turned off.
     * If bind_port is > 0, then this option will be ignored */
    int             num_last_ports=100;

    /**
     * Traffic class for sending unicast and multicast datagrams.
     * Valid values are (check {@link DatagramSocket#setTrafficClass(int)} );  for details):
     * <UL>
     * <LI><CODE>IPTOS_LOWCOST (0x02)</CODE>, <b>decimal 2</b></LI>
     * <LI><CODE>IPTOS_RELIABILITY (0x04)</CODE><, <b>decimal 4</b>/LI>
     * <LI><CODE>IPTOS_THROUGHPUT (0x08)</CODE>, <b>decimal 8</b></LI>
     * <LI><CODE>IPTOS_LOWDELAY (0x10)</CODE>, <b>decimal</b> 16</LI>
     * </UL>
     */
    int             tos=8; // valid values: 2, 4, 8 (default), 16

    /** The multicast address (mcast address and port) this member uses */
    IpAddress       mcast_addr=null;

    /** The multicast address used for sending and receiving packets */
    String          mcast_addr_name="228.8.8.8";

    /** The multicast port used for sending and receiving packets */
    int             mcast_port=7600;

    /** The multicast receiver thread */
    Thread          mcast_receiver=null;

    /** The unicast receiver thread */
    UcastReceiver   ucast_receiver=null;

    /** Whether to enable IP multicasting. If false, multiple unicast datagram
     * packets are sent rather than one multicast packet */
    boolean         ip_mcast=true;

    /** The time-to-live (TTL) for multicast datagram packets */
    int             ip_ttl=8;

    /** Send buffer size of the multicast datagram socket */
    int             mcast_send_buf_size=32000;

    /** Receive buffer size of the multicast datagram socket */
    int             mcast_recv_buf_size=64000;

    /** Send buffer size of the unicast datagram socket */
    int             ucast_send_buf_size=32000;
    
    /** Receive buffer size of the unicast datagram socket */
    int             ucast_recv_buf_size=64000;
    
    /** Number of the emulated node */
    int				emu_node_id=0;

    /** Port number of the emulated node */
    int				emu_port=0;
    
    /**
     * Creates the EMU_UDP protocol, and initializes the
     * state variables, does however not start any sockets or threads.
     */
    public EMU_UDP() {
    }

    public String getInfo() {
        StringBuilder sb=new StringBuilder();
        sb.append("group_addr=").append(mcast_addr_name).append(':').append(mcast_port).append("\n");
        return sb.toString();
    }
    
    /* ----------------------- Receiving of MCAST UDP packets ------------------------ */

    public String getName() {
        return "EMU_UDP";
    }

    public void postUnmarshalling(Message msg, Address dest, Address src, boolean multicast) {
        msg.setDest(dest);
    }

    public void postUnmarshallingList(Message msg, Address dest, boolean multicast) {
        msg.setDest(dest);
    }
    public void sendToAllMembers(byte[] data, int offset, int length) throws Exception {}

    public void sendToSingleMember(Address dest, byte[] data, int offset, int length) throws Exception {
        _send(((IpAddress)dest).getIpAddress(), ((IpAddress)dest).getPort(), false, data, offset, length);
    }

    /**
     * Setup the Protocol instance acording to the configuration string.
     * The following properties are read by the EMU_UDP protocol:
     * <ul>
     * <li> param mcast_addr - the multicast address to use default is 228.8.8.8
     * <li> param mcast_port - (int) the port that the multicast is sent on default is 7600
     * <li> param ip_mcast - (boolean) flag whether to use IP multicast - default is true
     * <li> param ip_ttl - Set the default time-to-live for multicast packets sent out on this socket. default is 32
     * </ul>
     * @return true if no other properties are left.
     *         false if the properties still have data in them, ie ,
     *         properties are left over and not handled by the protocol stack
     */
    public boolean setProperties(Properties props) {
        String str;

        super.setProperties(props);

        str=props.getProperty("num_last_ports");
        if(str != null) {
            num_last_ports=Integer.parseInt(str);
            props.remove("num_last_ports");
        }

        str=Util.getProperty(new String[]{Global.UDP_MCAST_ADDR, "jboss.partition.udpGroup"}, props,
                             "mcast_addr", false, "228.8.8.8");
        if(str != null)
            mcast_addr_name=str;

        str=Util.getProperty(new String[]{Global.UDP_MCAST_PORT, "jboss.partition.udpPort"},
                             props, "mcast_port", false, "7600");
        if(str != null)
            mcast_port=Integer.parseInt(str);

        str=props.getProperty("ip_mcast");
        if(str != null) {
            ip_mcast=Boolean.valueOf(str).booleanValue();
            props.remove("ip_mcast");
        }

        str=Util.getProperty(new String[]{Global.UDP_IP_TTL}, props, "ip_ttl", false, "64");
        if(str != null) {
            ip_ttl=Integer.parseInt(str);
            props.remove("ip_ttl");
        }

        str=props.getProperty("tos");
        if(str != null) {
            tos=Integer.parseInt(str);
            props.remove("tos");
        }

        str=props.getProperty("mcast_send_buf_size");
        if(str != null) {
            mcast_send_buf_size=Integer.parseInt(str);
            props.remove("mcast_send_buf_size");
        }

        str=props.getProperty("mcast_recv_buf_size");
        if(str != null) {
            mcast_recv_buf_size=Integer.parseInt(str);
            props.remove("mcast_recv_buf_size");
        }

        str=props.getProperty("ucast_send_buf_size");
        if(str != null) {
            ucast_send_buf_size=Integer.parseInt(str);
            props.remove("ucast_send_buf_size");
        }

        str=props.getProperty("ucast_recv_buf_size");
        if(str != null) {
            ucast_recv_buf_size=Integer.parseInt(str);
            props.remove("ucast_recv_buf_size");
        }

        str=props.getProperty("null_src_addresses");
        if(str != null) {
            props.remove("null_src_addresses");
            log.error("null_src_addresses has been deprecated, property will be ignored");
        }        
        str=props.getProperty("emu_node_id");
        if(str != null) {
        	emu_node_id=Integer.parseInt(str);
            props.remove("emu_node_id");
        }

        str=props.getProperty("emu_port");
        if(str != null) {
        	emu_port=Integer.parseInt(str);
            props.remove("emu_port");
        }
        Util.checkBufferSize("EMU_UDP.mcast_send_buf_size", mcast_send_buf_size);
        Util.checkBufferSize("EMU_UDP.mcast_recv_buf_size", mcast_recv_buf_size);
        Util.checkBufferSize("EMU_UDP.ucast_send_buf_size", ucast_send_buf_size);
        Util.checkBufferSize("EMU_UDP.ucast_recv_buf_size", ucast_recv_buf_size);
        return props.isEmpty();
    }


    /* ------------------------------------------------------------------------------- */



    /*------------------------------ Protocol interface ------------------------------ */

    /**
     * Creates the unicast and multicast sockets and starts the unicast and multicast receiver threads
     */
    public void start() throws Exception {    		
	        if(log.isDebugEnabled()) log.debug("creating sockets and starting threads");
	        try {	        	
	        	createSockets();
	        }
	        catch(Exception ex) {
	            String tmp="problem creating sockets (bind_addr=" + bind_addr + ", mcast_addr=" + mcast_addr + ")";
	            throw new Exception(tmp, ex);
	        }
	        super.start();
	        
	        if (sock.isEnabled()){
	        	startThreads();
	        }
    	
    }
    public void stop() {
        if(log.isDebugEnabled()) log.debug("closing sockets and stopping threads");
        stopThreads();  // will close sockets, closeSockets() is not really needed anymore, but...
        closeSockets(); // ... we'll leave it in there for now (doesn't do anything if already closed)
        super.stop();
    }
    private void _send(InetAddress dest, int port, boolean mcast, byte[] data, int offset, int length) throws Exception {
        DatagramPacket packet=new DatagramPacket(data, offset, length, dest, port);
        try {
            if(mcast) {
            }else {
                if(sock != null)
                    sock.send(packet);
            }
        }
        catch(Exception ex) {
            throw new Exception("dest=" + dest + ":" + port + " (" + length + " bytes)", ex);
        }
    }
    /*--------------------------- End of Protocol interface -------------------------- */


    /* ------------------------------ Private Methods -------------------------------- */

    private void closeSocket() {
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
    private void createSockets() throws Exception {
        if(bind_addr != null)
            if(log.isDebugEnabled()) log.debug("sockets will use interface " + bind_addr.getHostAddress());            
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
        if(additional_data != null)
            ((IpAddress)local_addr).setAdditionalData(additional_data);
        if(log.isDebugEnabled()) log.debug("socket information:\n" + dumpSocketInfo());
    }
    private String dumpSocketInfo() throws Exception {
        StringBuilder sb=new StringBuilder(128);
        sb.append("local_addr=").append(local_addr);
        sb.append(", mcast_addr=").append(mcast_addr);
        sb.append(", bind_addr=").append(bind_addr);
        sb.append(", ttl=").append(ip_ttl);

        if(sock != null) {
            sb.append("\nsock: bound to ");
            sb.append(sock.getLocalAddress().getHostAddress()).append(':').append(sock.getLocalPort());
            sb.append(", receive buffer size=").append(sock.getReceiveBufferSize());
            sb.append(", send buffer size=").append(sock.getSendBufferSize());
        }
        return sb.toString();
    }    
    protected VirtualDatagramSocket createVirtualDatagramSocket() throws SocketException {
    	VirtualDatagramSocket tmp = null;
            try {
                tmp=new VirtualDatagramSocket(emu_port, vni.getEmuNodeAddress(emu_node_id)); // first time localPort is 0
            }
            catch(SocketException bindException) {
                // Vladimir May 30th 2007
                // special handling for Linux 2.6 kernel which sometimes throws BindException while we probe for a random port
                //localPort++;
            	bindException.printStackTrace();
            }
        return tmp;
    }
    protected void handleConfigEvent(HashMap map) {
        super.handleConfigEvent(map);
        if(map == null) return;

        if(map.containsKey("send_buf_size")) {
            mcast_send_buf_size=((Integer)map.get("send_buf_size")).intValue();
            ucast_send_buf_size=mcast_send_buf_size;
        }
        if(map.containsKey("recv_buf_size")) {
            mcast_recv_buf_size=((Integer)map.get("recv_buf_size")).intValue();
            ucast_recv_buf_size=mcast_recv_buf_size;
        }
    }
    protected void setThreadNames() {
        super.setThreadNames();        
        if(thread_naming_pattern != null) {
        	thread_naming_pattern.renameThread(MCAST_RECEIVER_THREAD_NAME, mcast_receiver);
        	if(ucast_receiver != null)
        		thread_naming_pattern.renameThread(UcastReceiver.UCAST_RECEIVER_THREAD_NAME, ucast_receiver.getThread());
        }        
    }
    
    protected void unsetThreadNames() {
        super.unsetThreadNames();       
        if(ucast_receiver != null && ucast_receiver.getThread() != null)
        	ucast_receiver.getThread().setName(UcastReceiver.UCAST_RECEIVER_THREAD_NAME);        	                      
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
    void startThreads() throws Exception {
        if(ucast_receiver == null) {
            //start the listener thread of the ucast_recv_sock
            ucast_receiver=new UcastReceiver();
            ucast_receiver.start();
            if(thread_naming_pattern != null)
                thread_naming_pattern.renameThread(UcastReceiver.UCAST_RECEIVER_THREAD_NAME, ucast_receiver.getThread());
            if(log.isDebugEnabled())
                log.debug("created unicast receiver thread " + ucast_receiver.getThread());
        }
    }


    /**
     * Stops unicast and multicast receiver threads
     */
    void stopThreads() {
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
                    receive(local_addr, sender, data, offset, len);
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
                thread=new Thread(Util.getGlobalThreadGroup(), this, UCAST_RECEIVER_THREAD_NAME);
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