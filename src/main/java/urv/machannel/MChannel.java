package urv.machannel;

import org.jgroups.*;
import urv.olsr.data.OLSRNode;
import urv.util.graph.NetworkGraph;
import urv.util.graph.Weight;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.List;

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
	 * Return the InetAddresses of the group members
	 * 
	 * @return addressesOfGroupMembers
	 */
    List<InetAddress> getInetAddressesOfGroupMebers();
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