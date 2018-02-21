package urv.machannel;

import java.io.Serializable;
import java.util.List;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;

import urv.olsr.data.OLSRNode;
import urv.util.graph.NetworkGraph;
import urv.util.graph.Weight;

/**
 * This interface defines the methods that provides
 * our Topology Aware Multicast Channel
 * 
 * @author Marcel Arrufat Arias
 * @author Raúl Gracia Tinedo
 */
public interface MChannel {
	/**
	 * Returns the Address of the Local Node
	 * 
	 * @return local Address
	 */
    Address getLocalAddress();
    
	/**
	 * This method retrieves the name of the channel
	 * 
	 * @return MChannelName
	 */
    String getChannelName();
    
	/**
	 * Return the Addresses of the group members
	 * 
	 * @return addressesOfGroupMembers
	 */
    List<Address> getAddressesOfGroupMebers();
    
	/**
	 * Retrieves the NetworkGraph with the topology below us
	 * 
	 * @return Topology Graph
	 */
    NetworkGraph<OLSRNode,Weight> getNetworkGraph();
    
	/**
	 * Returns a view with the members of the current group
	 * 
	 * @see View
	 * @return Members
	 */
    View getView();
    
	/**
	 * Stops the channel
	 */
    void close();
    
	/**
	 * Sends the message to the destination of the messages. As the normal Channel,
	 * if the destination is a multicast address, the message is sent to all members
	 * of this group.
	 */
    void send(Message msg);
    
	/**
	 * Sends an unicast message to the selected member.
	 */
    void send(Address dst, Address src, Serializable content);
    
	/**
	 * Send a message to all neighbours (1 hop) of the current group
	 */
    void sendToNeighbors(Serializable content);

    void setReceiver(Receiver r);
}