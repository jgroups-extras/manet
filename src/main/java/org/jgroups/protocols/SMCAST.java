package org.jgroups.protocols;

import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.Global;
import org.jgroups.Message;
import org.jgroups.annotations.MBean;
import org.jgroups.annotations.Property;
import org.jgroups.stack.IpAddress;
import org.jgroups.stack.Protocol;
import urv.olsr.data.OLSRNode;
import urv.olsr.mcast.TopologyEvent;
import urv.omolsr.data.OMOLSRNetworkGraph;

import java.io.Serializable;
import java.util.Set;


/**
 * Simple Multicast Protocol.
 * This protocol provides multicast communication by means of a simple
 * multiple unicast scheme.
 * 
 * @author Gerard Paris Aixala
 */
@MBean(description="Simple Multicast Protocol. This protocol provides multicast communication by means of a simple " +
  "multiple unicast scheme.")
public class SMCAST extends Protocol {

	private OLSRNode localNode;
	private OMOLSRNetworkGraph omolsrNetworkGraph;


    @Property(name="mcast_addr", description="The multicast address used for sending and receiving packets",
      defaultValueIPv4="224.0.0.66", defaultValueIPv6="ff0e::0:0:66",
      systemProperty=Global.UDP_MCAST_ADDR,writable=false)
    private String mcast_addr_name;
	
	public SMCAST(){}		
		

	@Override
	public Object down(Message msg) {

        Address dest = msg.getDest();
        boolean multicast = dest == null;
        if (multicast) {
            // The message is multicast

            // TODO Obtain the list of nodes that are in the multicast group
            if (omolsrNetworkGraph!=null){
                Set<OLSRNode> groupMembers =  omolsrNetworkGraph.getGroupNodes(); // CHANGED TO OMOLSRNetworkGraph
						
                // TODO Send a unicast message to each member of the group
                for (OLSRNode n: groupMembers){
                    // TODO Here, we use the destination multicast port as the port of the unicast destination...
                    // TODO or port=0 if dest==null
							
                    Address ucastDest = new IpAddress(n.getAddress(),dest==null ? 0 : ((IpAddress)dest).getPort());
                    Message ucastMsg = new Message(ucastDest, (Serializable)msg.getObject());
							
                    // TODO Do we need a SMCAST header?? Probably not now, because upper protocols that handle multicast
                    // messages will put their own headers (NAKACK,...)
                    ucastMsg.putHeader(id, new SMCASTHeader());
                    down_prot.down(ucastMsg);
                }
            }
            return null;
					
					
            //OLSRNode destNode = new OLSRNode();
            //destNode.setValue(((IpAddress) dest).getIpAddress());
					
					
            //We add the mcast addr of the group (stack) in this originating message
            //return controller.handleOutgoingDataMessage(msg,destNode,mcast_addr_name);
					
        }
        return down_prot.down(msg);
    }




	@Override
	public void start() throws Exception{
		super.start();
	}
	
	 @Override
	public void stop() {
	}
	
	/**
     * An event was received from the layer below. Usually the current layer will want to examine
     * the event type and - depending on its type - perform some computation
     * (e.g. removing headers from a MSG event type, or updating the internal membership list
     * when receiving a VIEW_CHANGE event).
     * Finally the event is either a) discarded, or b) an event is sent down
     * the stack using {@code down_prot.down()} or c) the event (or another event) is sent up
     * the stack using {@code up_prot.up()}.
     */
	@Override
	public Object up(Event evt) {
		Message msg, rsp_msg;
		Object obj;
		Address coord;
		SMCASTHeader hdr;

		//log.debug("INI- Up - New event!");
		
		switch (evt.getType()) {

		case Event.SET_LOCAL_ADDRESS:

			localNode = new OLSRNode();
			localNode.setValue(((IpAddress) evt.getArg()).getIpAddress());
			//System.out.println("SMCAST: Event.SET_LOCAL_ADDRESS: "+ localNode.toString());
			
			//startController();
	
			return up_prot.up(evt);
		case Event.CONFIG:
			//System.out.println("SMCAST: Event.CONFIG"+ evt.getArg());
			
            Object ret=up_prot.up(evt);
            if(log.isDebugEnabled()) log.debug("received CONFIG event: " + evt.getArg());
            //handleConfigEvent((HashMap)evt.getArg());
            return ret;

			
		case Event.USER_DEFINED:
			Object objArg = evt.getArg();
			if (objArg instanceof TopologyEvent){
				TopologyEvent updateEvt = (TopologyEvent)objArg;

				omolsrNetworkGraph  = updateEvt.getOMOLSRNetworkGraph();
				
			}
			return up_prot.up(evt);
		default:
			log.debug("An event (not a MSG or SET_LOCAL_ADDRESS) has been received");
		
			return up_prot.up(evt); // Pass up to the layer above us
		}
	}

    @Override
	public Object up(Message msg) {

        // getHeader from protocol name
        Object obj=msg.getHeader(id);

        if (obj == null || !(obj instanceof SMCASTHeader)) {
            return up_prot.up(msg);
        }
        SMCASTHeader hdr=msg.getHeader(id);

        // If there is an SMCAST header, the message destination address is changed
        // TODO Is it useful?? Is the destination address used in the upper protocols?? Probably not!
        /*InetAddress mcastAddr = null;
        			try {
        				mcastAddr = InetAddress.getByName(mcast_addr_name);
        			} catch (UnknownHostException e) {

        				e.printStackTrace();
        			}
        			//IpAddress oldDest = ((IpAddress)msg.getDest());

        			Address mcastDest = new IpAddress(mcastAddr,0);
        			msg.setDest(mcastDest);*/




        return up_prot.up(msg);
    }
}
