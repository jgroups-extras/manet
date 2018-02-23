package org.jgroups.protocols;


import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.jgroups.Address;
import org.jgroups.Global;
import org.jgroups.Message;
import org.jgroups.logging.Log;
import org.jgroups.logging.LogFactory;
import org.jgroups.stack.IpAddress;
import org.jgroups.util.BoundedList;
import org.jgroups.util.Util;




/**
 * IP multicast transport based on UDP. Messages to the group (msg.dest == null) will
 * be multicast (to all group members), whereas point-to-point messages
 * (msg.dest != null) will be unicast to a single member. Uses a multicast and
 * a unicast socket.<p>
 * The following properties are read by the UDP protocol:
 * <ul>
 * <li> param mcast_addr - the multicast address to use; default is 228.8.8.8.
 * <li> param mcast_port - (int) the port that the multicast is sent on; default is 7600
 * <li> param ip_mcast - (boolean) flag whether to use IP multicast; default is true.
 * <li> param ip_ttl - the default time-to-live for multicast packets sent out on this
 * socket; default is 32.
 * <li> param use_packet_handler - boolean, defaults to false.
 * If set, the mcast and ucast receiver threads just put
 * the datagram's payload (a byte buffer) into a queue, from where a separate thread
 * will dequeue and handle them (unmarshal and pass up). This frees the receiver
 * threads from having to do message unmarshalling; this time can now be spent
 * receiving packets. If you have lots of retransmissions because of network
 * input buffer overflow, consider setting this property to true.
 * </ul>
 * @author Bela Ban
 * @version $Id: JOLSR_UDP.java,v 1.144.2.1 2007/09/17 07:41:03 belaban Exp $
 */
public class JOLSR_UDP extends UDP implements Runnable {

    /**
     * BoundedList<Integer> of the last 100 ports used. This is to avoid reusing a port for DatagramSocket
     */
    private static volatile BoundedList<Integer> last_ports_used=null;

    private final static String MCAST_RECEIVER_THREAD_NAME = "UDP mcast receiver";

    protected final Log        log=LogFactory.getLog(this.getClass());

    /** Socket used for
     * <ol>
     * <li>sending unicast packets and
     * <li>receiving unicast packets
     * </ol>
     * The address of this socket will be our local address (<tt>local_addr</tt>) */
    DatagramSocket  sock=null;

    /** Maintain a list of local ports opened by DatagramSocket. If this is 0, this option is turned off.
     * If bind_port is > 0, then this option will be ignored */
    int             num_last_ports=100;


    /** If we have multiple mcast send sockets, e.g. send_interfaces or send_on_all_interfaces enabled */
    MulticastSocket[] mcast_send_sockets=null;

    /** The multicast receiver thread */
    Thread          mcast_receiver=null;

    /** The unicast receiver thread */
    UcastReceiver   ucast_receiver=null;


    /** Usually, src addresses are nulled, and the receiver simply sets them to the address of the sender. However,
     * for multiple addresses on a Windows loopback device, this doesn't work
     * (see http://jira.jboss.com/jira/browse/JGRP-79 and the JGroups wiki for details). This must be the same
     * value for all members of the same group. Default is true, for performance reasons */
    // private boolean null_src_addresses=true;



    /**
     * Creates the JOLSR_UDP protocol, and initializes the
     * state variables, does however not start any sockets or threads.
     */
    public JOLSR_UDP() {
    }



    @Override
	public String getInfo() {
        StringBuilder sb=new StringBuilder();
        sb.append("group_addr=").append(mcast_addr).append(':').append(mcast_port).append("\n");
        return sb.toString();
    }

    /* ----------------------- Receiving of MCAST UDP packets ------------------------ */

    @Override
	public String getName() {
        return "JOLSR_UDP";
    }

    public static void postUnmarshalling(Message msg, Address dest, Address src, boolean multicast) {
        msg.setDest(dest);
    }

    public static void postUnmarshallingList(Message msg, Address dest, boolean multicast) {
        msg.setDest(dest);
    }

    @Override
	public void run() {
        DatagramPacket  packet;
        byte            receive_buf[]=new byte[65535];
        int             offset, len, sender_port;
        byte[]          data;
        InetAddress     sender_addr;
        Address         sender;


        // moved out of loop to avoid excessive object creations (bela March 8 2001)
        packet=new DatagramPacket(receive_buf, receive_buf.length);

        while(mcast_receiver != null && mcast_sock != null) {
            try {
                packet.setData(receive_buf, 0, receive_buf.length);
                mcast_sock.receive(packet);
                sender_addr=packet.getAddress();
                sender_port=packet.getPort();
                offset=packet.getOffset();
                len=packet.getLength();
                data=packet.getData();
                sender=new IpAddress(sender_addr, sender_port);

                if(len > receive_buf.length) {
                    if(log.isErrorEnabled())
                        log.error("size of the received packet (" + len + ") is bigger than " +
                                  "allocated buffer (" + receive_buf.length + "): will not be able to handle packet. " +
                                  "Use the FRAG protocol and make its frag_size lower than " + receive_buf.length);
                }

                receive(sender, data, offset, len);
            }
            catch(SocketException sock_ex) {
                 if(log.isTraceEnabled()) log.trace("multicast socket is closed, exception=" + sock_ex);
                break;
            }
            catch(InterruptedIOException io_ex) { // thread was interrupted
            }
            catch(Throwable ex) {
                if(log.isErrorEnabled())
                    log.error("failure in multicast receive()", ex);
                // Util.sleep(100); // so we don't get into 100% cpu spinning (should NEVER happen !)
            }
        }
        if(log.isDebugEnabled()) log.debug("multicast thread terminated");
    }


    public void sendToAllMembers(byte[] data, int offset, int length) throws Exception {
        if(ip_mcast && mcast_addr != null) {
            _send(mcast_addr.getIpAddress(), mcast_addr.getPort(), true, data, offset, length);
        }
        else {
            List<Address> mbrs=new ArrayList<>(members);
            for(Address mbr: mbrs) {
                _send(((IpAddress)mbr).getIpAddress(), ((IpAddress)mbr).getPort(), false, data, offset, length);
            }
        }
    }

    @Override
	public void sendToSingleMember(Address dest, byte[] data, int offset, int length) throws Exception {
        _send(((IpAddress)dest).getIpAddress(), ((IpAddress)dest).getPort(), false, data, offset, length);
    }


    /* ------------------------------------------------------------------------------- */



    /*------------------------------ Protocol interface ------------------------------ */

    /**
     * Creates the unicast and multicast sockets and starts the unicast and multicast receiver threads
     */
    @Override
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
        startThreads();
    }

    @Override
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
                if(mcast_send_sockets != null) {
                    MulticastSocket s;
                    for(int i=0; i < mcast_send_sockets.length; i++) {
                        s=mcast_send_sockets[i];
                        try {
                            s.send(packet);
                        }
                        catch(Exception e) {
                            log.error("failed sending packet on socket " + s);
                        }
                    }
                }
                else { // DEFAULT path
                    if(mcast_sock != null)
                        mcast_sock.send(packet);
                }
            }
            else {
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





    /**
     *
     * @param interfaces List<NetworkInterface>. Guaranteed to have no duplicates
     * @param s
     * @param mcastAddr
     */
    @Override
	protected void bindToInterfaces(List<NetworkInterface> interfaces, MulticastSocket s, InetAddress mcastAddr) {
        SocketAddress tmp_mcast_addr=new InetSocketAddress(mcastAddr, mcast_port);
        for(NetworkInterface iface : interfaces) {
            for(Enumeration<InetAddress> en2 = iface.getInetAddresses(); en2.hasMoreElements();) {
            	InetAddress addr = en2.nextElement();
                try {
					s.joinGroup(tmp_mcast_addr, iface);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                if(log.isTraceEnabled())
                    log.trace("joined " + tmp_mcast_addr + " on " + iface.getName() + " (" + addr + ")");
                break;
            }
        }
    }

    void closeSocket() {
        if(sock != null) {
            sock.close();
            sock=null;
            if(log.isDebugEnabled()) log.debug("socket closed");
        }
    }



    /**
     * Create JOLSR_UDP sender and receiver sockets. Currently there are 2 sockets
     * (sending and receiving). This is due to Linux's non-BSD compatibility
     * in the JDK port (see DESIGN).
     */
    @Override
	protected void createSockets() throws Exception {
        // bind_addr not set, try to assign one by default. This is needed on Windows

        // changed by bela Feb 12 2003: by default multicast sockets will be bound to all network interfaces

        // CHANGED *BACK* by bela March 13 2003: binding to all interfaces did not result in a correct
        // local_addr. As a matter of fact, comparison between e.g. 0.0.0.0:1234 (on hostA) and
        // 0.0.0.0:1.2.3.4 (on hostB) would fail !
//        if(bind_addr == null) {
//            InetAddress[] interfaces=InetAddress.getAllByName(InetAddress.getLocalHost().getHostAddress());
//            if(interfaces != null && interfaces.length > 0)
//                bind_addr=interfaces[0];
//        }

        if(bind_addr == null /* && !use_local_host */) {
            bind_addr=Util.getNonLoopbackAddress();
        }
        if(bind_addr == null)
            bind_addr=InetAddress.getLocalHost();

        if(bind_addr != null)
            if(log.isDebugEnabled()) log.debug("sockets will use interface " + bind_addr.getHostAddress());


        // 2. Create socket for receiving unicast UDP packets. The address and port
        //    of this socket will be our local address (local_addr)
        if(bind_port > 0) {
            sock=createDatagramSocketWithBindPort();
        }
        else {
            sock=createEphemeralDatagramSocket();
        }
        sock.setBroadcast(true);
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
            throw new Exception("JOLSR_UDP.createSocket(): sock is null");

        //local_addr=new IpAddress(sock.getLocalAddress(), sock.getLocalPort());
        //OMOLSR CHANGE
        local_addr=new IpAddress(bind_addr,bind_port);

        // 3. Create socket for receiving IP multicast packets
        if(ip_mcast) {
            // 3a. Create mcast receiver socket
        	InetAddress mcast_ipAddress = getMulticastAddress();
            mcast_sock=new MulticastSocket(mcast_port);
            mcast_sock.setTimeToLive(ip_ttl);
            if(tos > 0) {
                try {
                    mcast_sock.setTrafficClass(tos);
                }
                catch(SocketException e) {
                    log.warn("traffic class of " + tos + " could not be set, will be ignored", e);
                }
            }

            if(receive_on_all_interfaces || (receive_interfaces != null && !receive_interfaces.isEmpty())) {
            	List<NetworkInterface> interfaces;
                if(receive_interfaces != null)
                    interfaces=receive_interfaces;
                else
                    interfaces=Util.getAllAvailableInterfaces();
                bindToInterfaces(interfaces, mcast_sock, mcast_ipAddress);
            }
            else {
                if(bind_addr != null)
                    mcast_sock.setInterface(bind_addr);
                 mcast_sock.joinGroup(mcast_ipAddress);
            }

            // 3b. Create mcast sender socket
//            if(send_on_all_interfaces || (send_interfaces != null && !send_interfaces.isEmpty())) {
                List<NetworkInterface> interfaces=Util.getAllAvailableInterfaces();
                
//                if(send_interfaces != null)
//                    interfaces=send_interfaces;
//                else
//                    interfaces=Util.getAllAvailableInterfaces();
                mcast_send_sockets=new MulticastSocket[interfaces.size()];
                int index=0;
                for(NetworkInterface intf : interfaces) {
                	Enumeration<InetAddress> inetAddresses = intf.getInetAddresses();
                	if (!inetAddresses.hasMoreElements()) {
                		// No addresses bound to interface. Skip!
                		log.warn("Skipping interface with no address bound: " + intf.getDisplayName());
                		continue;
                	}
                    mcast_send_sockets[index]=new MulticastSocket();
                    mcast_send_sockets[index].setNetworkInterface(intf);
                    mcast_send_sockets[index].setTimeToLive(ip_ttl);
                    if(tos > 0) {
                        try {
                            mcast_send_sockets[index].setTrafficClass(tos);
                        }
                        catch(SocketException e) {
                            log.warn("traffic class of " + tos + " could not be set, will be ignored", e);
                        }
                    }
                    index++;
                }
//            }
        }

        setBufferSizes();
        if(log.isDebugEnabled()) log.debug("socket information:\n" + dumpSocketInfo());
    }


    /**
     * Creates a DatagramSocket when bind_port > 0. Attempts to allocate the socket with port == bind_port, and
     * increments until it finds a valid port, or until port_range has been exceeded
     * @return DatagramSocket The newly created socket
     * @throws Exception
     */
    protected DatagramSocket createDatagramSocketWithBindPort() throws Exception {
        DatagramSocket tmp=null;
        // 27-6-2003 bgooren, find available port in range (start_port, start_port+port_range)
        int rcv_port=bind_port, max_port=bind_port + port_range;
        while(rcv_port <= max_port) {
            try {
                tmp=new DatagramSocket(rcv_port);//, bind_addr);
                break;
            }
            catch(SocketException bind_ex) {	// Cannot listen on this port
                rcv_port++;
            }
            catch(SecurityException sec_ex) { // Not allowed to listen on this port
                rcv_port++;
            }

            // Cannot listen at all, throw an Exception
            if(rcv_port >= max_port + 1) { // +1 due to the increment above
                throw new Exception("cannot create a socket on any port in range " +
                        bind_port + '-' + (bind_port + port_range));
            }
        }
        return tmp;
    }

    /** Creates a DatagramSocket with a random port. Because in certain operating systems, ports are reused,
     * we keep a list of the n last used ports, and avoid port reuse */
    protected DatagramSocket createEphemeralDatagramSocket() {
        DatagramSocket tmp;
        int localPort=0;
        while(true) {
            try {
                tmp=new DatagramSocket(localPort, bind_addr); // first time localPort is 0
            }
            catch(SocketException bindException) {
                // Vladimir May 30th 2007
                // special handling for Linux 2.6 kernel which sometimes throws BindException while we probe for a random port
                localPort++;
                continue;
            }

            if(num_last_ports <= 0)
                break;
            localPort=tmp.getLocalPort();
            if(last_ports_used == null)
                last_ports_used=new BoundedList<>(num_last_ports);
            if(last_ports_used.contains(new Integer(localPort))) {
                if(log.isDebugEnabled())
                    log.debug("local port " + localPort + " already seen in this session; will try to get other port");
                try {tmp.close();} catch(Throwable e) {}
                localPort++;
            }
            else {
                last_ports_used.add(new Integer(localPort));
                break;
            }
        }
        return tmp;
    }

    @Override
	protected void setThreadNames() {
        super.setThreadNames();

        if(thread_naming_pattern != null) {
//        	thread_naming_pattern.renameThread(MCAST_RECEIVER_THREAD_NAME, mcast_receiver); // FIXME
        	mcast_receiver.setName(MCAST_RECEIVER_THREAD_NAME);
        	if(ucast_receiver != null)
//        		thread_naming_pattern.renameThread(UcastReceiver.UCAST_RECEIVER_THREAD_NAME, ucast_receiver.getThread());
        		ucast_receiver.getThread().setName(UcastReceiver.UCAST_RECEIVER_THREAD_NAME);
        }
    }


    @Override
	protected void unsetThreadNames() {
        super.unsetThreadNames();
        if(mcast_receiver != null)
        	mcast_receiver.setName(MCAST_RECEIVER_THREAD_NAME);

        if(ucast_receiver != null && ucast_receiver.getThread() != null)
        	ucast_receiver.getThread().setName(UcastReceiver.UCAST_RECEIVER_THREAD_NAME);
    }




    @Override
	void closeMulticastSocket() {
        if(mcast_sock != null) {
            try {
                if(mcast_addr != null) {
                    mcast_sock.leaveGroup(mcast_addr.getIpAddress());
                }
                mcast_sock.close(); // this will cause the mcast receiver thread to break out of its loop
                mcast_sock=null;
                if(log.isDebugEnabled()) log.debug("multicast socket closed");
            }
            catch(IOException ex) {
            	// ignore
            }
            mcast_addr=null;
        }

        if(mcast_send_sockets != null) {
            MulticastSocket s;
            for(int i=0; i < mcast_send_sockets.length; i++) {
                s=mcast_send_sockets[i];
                s.close();
                if(log.isDebugEnabled()) log.debug("multicast send socket " + s + " closed");
            }
            mcast_send_sockets=null;
        }
    }


    /**
     * Closed UDP unicast and multicast sockets
     */
    void closeSockets() {
        // 1. Close multicast socket
        closeMulticastSocket();

        // 2. Close socket
        closeSocket();
    }


    @Override
	void setBufferSizes() {
        if(sock != null)
            setBufferSize(sock, ucast_send_buf_size, ucast_recv_buf_size);

        if(mcast_sock != null)
            setBufferSize(mcast_sock, mcast_send_buf_size, mcast_recv_buf_size);

        if(mcast_send_sockets != null) {
            for(int i=0; i < mcast_send_sockets.length; i++) {
                setBufferSize(mcast_send_sockets[i], mcast_send_buf_size, mcast_recv_buf_size);
            }
        }
    }

    /**
     * Starts the unicast and multicast receiver threads
     */
    @Override
	protected
	void startThreads() throws Exception {
        if(ucast_receiver == null) {
            //start the listener thread of the ucast_recv_sock
            ucast_receiver=new UcastReceiver();
            ucast_receiver.start();
            if(thread_naming_pattern != null)
//                thread_naming_pattern.renameThread(UcastReceiver.UCAST_RECEIVER_THREAD_NAME, ucast_receiver.getThread());
        		ucast_receiver.getThread().setName(UcastReceiver.UCAST_RECEIVER_THREAD_NAME);
            if(log.isDebugEnabled())
                log.debug("created unicast receiver thread " + ucast_receiver.getThread());
        }

        if(ip_mcast) {
            if(mcast_receiver != null) {
                if(mcast_receiver.isAlive()) {
                    if(log.isDebugEnabled()) log.debug("did not create new multicastreceiver thread as existing " +
                                                       "multicast receiver thread is still running");
                }
                else
                    mcast_receiver=null; // will be created just below...
            }

            if(mcast_receiver == null) {
                mcast_receiver=new Thread(this, MCAST_RECEIVER_THREAD_NAME);
                mcast_receiver.setPriority(Thread.MAX_PRIORITY); // needed ????
                if(thread_naming_pattern != null)
//                    thread_naming_pattern.renameThread(MCAST_RECEIVER_THREAD_NAME, mcast_receiver);
                	mcast_receiver.setName(MCAST_RECEIVER_THREAD_NAME);
                // mcast_receiver.setDaemon(true);
                mcast_receiver.start();
                if(log.isDebugEnabled())
                log.debug("created multicast receiver thread " + mcast_receiver);
            }
        }
    }


    /**
     * Stops unicast and multicast receiver threads
     */
    @Override
	protected
	void stopThreads() {
        Thread tmp;

        // 1. Stop the multicast receiver thread
        if(mcast_receiver != null) {
            if(mcast_receiver.isAlive()) {
                tmp=mcast_receiver;
                mcast_receiver=null;
                closeMulticastSocket();  // will cause the multicast thread to terminate
                tmp.interrupt();
                try {
                    tmp.join(Global.THREAD_SHUTDOWN_WAIT_TIME);
                }
                catch(InterruptedException e) {
                    Thread.currentThread().interrupt(); // set interrupt flag again
                }
                tmp=null;
            }
            mcast_receiver=null;
        }

        // 2. Stop the unicast receiver thread
        if(ucast_receiver != null) {
            ucast_receiver.stop();
            ucast_receiver=null;
        }
    }



    /* ----------------------------- End of Private Methods ---------------------------------------- */

    /* ----------------------------- Inner Classes ---------------------------------------- */




    public class UcastReceiver implements Runnable {

    	public static final String UCAST_RECEIVER_THREAD_NAME = "JOLSR_UDP ucast receiver";
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
            try {
				sock.setBroadcast(true);
				System.err.println("Broadcast enabled = "+sock.getBroadcast()+" on port "+sock.getPort());
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            while(running && thread != null && sock != null) {
                try {
                    packet.setData(receive_buf, 0, receive_buf.length);
                    sock.receive(packet);
                    sender_addr=packet.getAddress();
                    System.err.println("Received message from "+sender_addr);
                    sender_port=packet.getPort();
                    offset=packet.getOffset();
                    len=packet.getLength();
                    data=packet.getData();
                    sender=new IpAddress(sender_addr, sender_port);

                    if(len > receive_buf.length) {
                        if(log.isErrorEnabled())
                            System.out.println("size of the received packet (" + len + ") is bigger than allocated buffer (" +
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
                    // Util.sleep(100); // so we don't get into 100% cpu spinning (should NEVER happen !)
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
