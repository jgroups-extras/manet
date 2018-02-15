package org.jgroups.protocols;

import java.util.Iterator;

import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.Message;
import org.jgroups.annotations.MBean;
import org.jgroups.annotations.ManagedAttribute;
import org.jgroups.annotations.Property;
import org.jgroups.util.BoundedList;


/**
 * Reliable unicast protocol using a combination of positive and negative acks. See docs/design/UNICAST3.txt for details.
 * @author Bela Ban
 * @since  3.3
 */
@MBean(description="Reliable unicast layer")
public class JOLSR_UNICAST extends UNICAST3 {

    /* ------------------------------------------ Properties  ------------------------------------------ */


	// if JOLSR_UNICAST is used without GMS, don't consult the membership on retransmit() if use_gms=false
    // default is true
    @Property(description="Whether to consult the membership on retransmit()")
    protected boolean          use_gms=true;

    /* --------------------------------------------- JMX  ---------------------------------------------- */


    /* --------------------------------------------- Fields ------------------------------------------------ */

    /** A list of members who left, used to determine when to prevent sending messages to left mbrs */
    protected final BoundedList<Address> previous_members=new BoundedList<>(50);

    @ManagedAttribute
    public String getMembers() {return members != null? members.toString() : "[]";}

    @Override
	public void init() throws Exception {
        super.init();
    }
    
	/*
	 * @see org.jgroups.protocols.UNICAST3#down(org.jgroups.Event)
	 */
	@Override
	public Object down(Event evt) {
		final Object result = super.down(evt);
        // code by Matthias Weber May 23 2006
        for(Iterator<Address> i=previous_members.iterator(); i.hasNext();) {
            Address mbr=i.next();
            if(members.contains(mbr)) {
                i.remove();
                if(log.isTraceEnabled())
                    log.trace("removed " + mbr + " from previous_members as result of VIEW_CHANGE event, " +
                            "previous_members=" + previous_members);
            
            }
        }
        return result;
	}

    /*
	 * @see org.jgroups.protocols.UNICAST3#removeSendConnection(org.jgroups.Address)
	 */
	@Override
	protected void removeSendConnection(Address mbr) {
		super.removeSendConnection(mbr);
        if(use_gms && !previous_members.contains(mbr))
            previous_members.add(mbr);

	}

	/*
	 * @see org.jgroups.protocols.UNICAST3#removeReceiveConnection(org.jgroups.Address)
	 */
	@Override
	protected void removeReceiveConnection(Address mbr) {
		super.removeReceiveConnection(mbr);
        if(use_gms && !previous_members.contains(mbr))
            previous_members.add(mbr);
	}

	/*
	 * @see org.jgroups.protocols.UNICAST3#handleDataReceived(org.jgroups.Address, long, short, boolean, org.jgroups.Message)
	 */
	@Override
	protected void handleDataReceived(Address sender, long seqno, short conn_id, boolean first, Message msg) {
        if(previous_members.contains(sender)) {
            // we don't want to see messages from departed members
            if(seqno > DEFAULT_FIRST_SEQNO) {
                if(log.isTraceEnabled())
                    log.trace("discarding message " + seqno + " from previous member " + sender);
                return; // don't ack this message so the sender keeps resending it !
            }
            if(log.isTraceEnabled())
                log.trace("removed " + sender + " from previous_members as we received a message from it");
            previous_members.remove(sender);
        }
		super.handleDataReceived(sender, seqno, conn_id, first, msg);
	}

}
