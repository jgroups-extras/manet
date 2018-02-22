package org.jgroups.protocols;

import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.Message;
import org.jgroups.annotations.MBean;
import org.jgroups.annotations.Property;
import org.jgroups.protocols.pbcast.NakAckHeader2;
import org.jgroups.stack.IpAddress;
import org.jgroups.stack.Protocol;
import urv.olsr.data.OLSRNode;
import urv.olsr.mcast.TopologyEvent;
import urv.omolsr.core.OMOLSRController;
import urv.omolsr.core.UnicastHandlerListener;
import urv.omolsr.data.OMOLSRNetworkGraph;

import java.net.UnknownHostException;

/**
 * An Overlay Multicast protocol. Multicast messages are intercepted by this protocol and are sent by means of several
 * unicast messages. This protocol constructs a multicast mesh in order to send multicast messages in an efficient manner.
 * @author Marcel Arrufat Arias
 * @author Gerard Paris Aixala
 */
@MBean(description="An Overlay Multicast protocol. Multicast messages are intercepted by this protocol and are sent by " +
  "means of several unicast messages. This protocol constructs a multicast mesh in order to send multicast messages in " +
  "an efficient manner")
public class OMOLSR extends Protocol {

    @Property(description="Multicast address for this stack")
	private String                    mcast_addr;

    @Property(description="Multicast port for this stack")
	private int                       mcast_port;

    @Property(description="Minimum number of neighbors to perform broadcast instead of unicast")
	private int                       bcast_min_neigh;

	private UnicastHandlerListener    unicastHandlerListener;
    private Address                   mcastAddr;
	private Address                   localAddress;

	//TODO OMOLSR: Now, it is not a singleton
	private volatile OMOLSRController controller;
	private boolean                   recomputeMstFlag;

    // New vars 01-04-2008
    private OLSRNode                  localNode;


	/**
	 * An event is to be sent down the stack. The layer may want to examine its
	 * type and perform some action on it, depending on the event's type. If the
	 * event is a message MSG, then the layer may need to add a header to it (or
	 * do nothing at all) before sending it down the stack using
	 * {@code PassDown}. In case of a GET_ADDRESS event (which tries to
	 * retrieve the stack's address from one of the bottom layers), the layer
	 * may need to send a new response event back up the stack using
	 * {@code passUp()}.
	 */
	@Override
	public Object down(Message msg) {
        Address dest = msg.getDest();
        boolean multicast=dest == null;
        if (multicast){
            //In first place, check if MST must be recomputed
            if (isRecomputeMstFlag()){
                setRecomputeMstFlag(false);
                controller.computeMST();
            }
            // The message is addressed to a multicast group
            OMOLSRHeader header = new OMOLSRHeader().setType(OMOLSRHeader.DATA).setGroupId(mcastAddr)
              //Since OLSR changes src address we must recover this information at omolsr level
              .setSrcAddress(localAddress);
            msg.putHeader(getId(),header);
            handleOutgoingDataMessage(msg);
            return null;
        }
		// Count message retransmission sent
		NakAckHeader2 nakackHeader = msg.getHeader(Constants.NAKACK2_ID);
		if (nakackHeader!=null) {
		    String headerStr = (nakackHeader).toString();
		    if (headerStr.contains("XMIT_RSP")) {
		        // Obtain received retransmited messages
		        //obtainRetransmissionListStatistics(headerStr);
		    }
		}

        return down_prot.down(msg);
    }



    public Object eventDown(Event evt){
        Message msg =evt.getArg();
        System.err.println("OMOLSR: sending message from "+localNode+" to "+msg.getDest());
        return down_prot.down(evt);
    }
	public Object eventUp(Event evt){
		return up_prot.up(evt);
	}	

	/**
	 * 
	 * @param msg
	 */
	public void sendUnicastDataMessage(Message msg){		
		down_prot.down(msg);
	}

	public void setUnicastHandlerListener(UnicastHandlerListener listener){
		this.unicastHandlerListener = listener;	
	}
	
	/**
	 * Starts the protocol, sets the port where broadcast messages are received 
	 */
	@Override
	public void start() throws Exception{
		super.start();
		log.debug("OMOLSR: start!! mcast="+ mcast_addr +" local="+localAddress);
	}
	
	/**
	 * Stops the protocol, by unregistering itself from the OMcastHandler
	 */
	@Override
	public void stop() {
		//controller.unregisterOmcastProtocol(multicastAddress);
	}
	
	/**
	 * An event was received from the layer below. Usually the current layer will want to examine
	 * the event type and - depending on its type - perform some computation
	 * (e.g. removing headers from a MSG event type, or updating the internal membership list
	 * when receiving a VIEW_CHANGE event).
	 * Finally the event is either a) discarded, or b) an event is sent down
	 * the stack using {@code PassDown} or c) the event (or another event) is sent up
	 * the stack using {@code PassUp}.
	 * <p/>
	 *
	 * @param evt - the event that has been sent from the layer below
	 */
    @Override
	public Object up(Event evt) {
        switch (evt.getType()) {
            case Event.SET_LOCAL_ADDRESS:
                log.debug("Received local address in OMOLSR");
                localAddress =evt.getArg();
                localNode = new OLSRNode();
                localNode.setValue(((IpAddress)localAddress).getIpAddress());
                System.err.println("Received local node in OMOLSR:"+localNode);

                // Setting local address for this group
                try {
                    mcastAddr = new IpAddress(mcast_addr, mcast_port);
                    //Needed when sending messages by local broadcast
                    //Dst address must be multicast address
                } catch (UnknownHostException e1) {
                    e1.printStackTrace();
                }
                onStartOK();
                return up_prot.up(evt);

            case Event.VIEW_CHANGE:
                Object objArg = evt.getArg();
                if (objArg instanceof TopologyEvent){
                    TopologyEvent updateEvt = (TopologyEvent)objArg;
                    OMOLSRNetworkGraph omolsrNetworkGraph  = updateEvt.getOMOLSRNetworkGraph();
                    localNode = updateEvt.getLocalNode().copy();
                    getController().updateMulticastNetworkGraph(omolsrNetworkGraph);
                    setRecomputeMstFlag(true);
                    return up_prot.up(new Event(Event.VIEW_CHANGE, updateEvt));
                }
                return up_prot.up(evt);

            default:
                log.debug("An event (not a MSG or SET_LOCAL_ADDRESS) has been received");
                return up_prot.up(evt); // Pass up to the layer above us
        }
    }

    @Override
	public Object up(Message msg) {

        // retrieve header and check if message contains data or control information
        // getHeader from protocol name
        Object obj = msg.getHeader(id);

        if (!(obj instanceof OMOLSRHeader))
            return up_prot.up(msg);

        OMOLSRHeader hdr=(OMOLSRHeader)obj;
        //Set back src and dst Addresses before getting the message copy

        //Set back the src address, since OLSR changes src address when
        //routing messages
        Address srcAddr = hdr.getSrcAddress();
        System.err.println("["+localNode.getAddress().getHostAddress()+"] Message src was "+msg.getSrc()+ " and now changing src to "+srcAddr);
        msg.setSrc(srcAddr);
        //Set destination multicast address
        msg.setDest(mcastAddr);

        // Check message type
        switch (hdr.type) {

            case OMOLSRHeader.CONTROL:
                //OMOLSR: No control messages anymore
                return null;

            case OMOLSRHeader.DATA:
                try{
                    //In first place, check if MST must be recomputed
                    if (isRecomputeMstFlag()){
                        setRecomputeMstFlag(false);
                        getController().computeMST();
                    }
                    handleIncomingDataMessage(msg.copy()); // TODO There is no header!!
                    //Not needed, it's done out of the case statement
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            default:
                System.err.println("Received a message without type!");
                log.debug("got OMcast header with unknown type ("
                            + hdr.type
                            + ')');
                break;
        }
        return up_prot.up(msg);
    }

    private void createController() {
        controller = new OMOLSRController(this,localNode);
	}
    
	/**
	 * @return Returns the controller.
	 */
	private OMOLSRController getController() {
		//Wait for the event that creates the controller
        while (controller==null) {
        	 // FIXME busy wait
        }
        return controller;
	}
	
	private boolean handleIncomingDataMessage(Message msg){
		return unicastHandlerListener.handleIncomingDataMessage(msg);
	}
	
	/**
	 * 
	 * @param msg
	 */
	private void handleOutgoingDataMessage(Message msg){
		unicastHandlerListener.handleOutgoingDataMessage(msg);
	}
	
	/**
	 * Returns the value of the flag
	 * @return
	 */
	private boolean isRecomputeMstFlag() {
		return recomputeMstFlag;
	}
	
	/**
	 * Launches bootstrapping process
	 *
	 */
	private void onStartOK() {
		// Creates the controller
		createController();
		System.err.println("OMOLSR: start!! mcast="+ mcast_addr +" local="+localAddress+" localNode="+localNode);
		getController().registerOmolsrProtocol(this);
	}

	/**
	 * Indicates whether the MST must be recomputed before sending 
	 * a new message
	 * @param b
	 */
	private synchronized void setRecomputeMstFlag(boolean b) {
		this.recomputeMstFlag = b;
	}
}