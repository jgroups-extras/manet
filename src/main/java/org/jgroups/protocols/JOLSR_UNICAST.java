package org.jgroups.protocols;


import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.annotations.MBean;

/**
 * OLSR adapted version of {@link UNICAST3}, with small changes
 * @author Bela Ban
 * @since  3.3
 */
@MBean(description="Reliable unicast layer")
public class JOLSR_UNICAST extends UNICAST3 {

    public Object down(Message msg) {
        Address dst=msg.getDest();

        // Marc added: if the destination is broadcast, do not add it to retransmit, just pass it down
        // todo (bela): this won't work with IPv6 addresses, or types of addresses
        if(dst != null && dst.toString().split(":")[0].equals("255.255.255.255"))
            return down_prot.down(msg);
        return super.down(msg);
    }

    public String getMembers() {return members != null? members.toString() : "[]";}
}
