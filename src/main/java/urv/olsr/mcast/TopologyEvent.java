package urv.olsr.mcast;

import org.jgroups.Address;
import org.jgroups.View;
import org.jgroups.ViewId;
import org.jgroups.stack.IpAddress;
import urv.conf.PropertiesLoader;
import urv.olsr.data.OLSRNode;
import urv.olsr.data.routing.RoutingTable;
import urv.omolsr.data.OMOLSRNetworkGraph;
import urv.util.graph.NetworkGraph;
import urv.util.graph.Weight;

import java.io.Serializable;
import java.util.Set;

/**
 * Event passed to the above layer for giving some information. In
 * this protocol is used for passing information (omolsrNetworkGraph)
 * from the OLSR protocol to the multicast protocol (OMOLSR). We also
 * will use this event to notify the higher layer protocols about the
 * membership, in the way that JGroups usually do it (VIEW_CHANGE Event). 
 *
 * @see View
 * 
 * @author Gerard Paris Aixala
 * @author Raul Gracia Tinedo
 *
 */
public class TopologyEvent extends View implements Serializable{

	//	CLASS FIELDS --
	
	private OMOLSRNetworkGraph omolsrNetworkGraph;
	private RoutingTable routingTable;
	private OLSRNode localNode;
	
	//	CONSTRUCTORS --
	
	public TopologyEvent(NetworkGraph<OLSRNode,Weight> networkGraph, RoutingTable routingTable, OLSRNode localNode){
		this.omolsrNetworkGraph = new OMOLSRNetworkGraph(networkGraph);
		this.routingTable = routingTable;
		this.localNode = localNode;
		// Create the membership for the jGroups protocols
        Set<OLSRNode> nodeList=networkGraph.getNodeList();
		this.members = new Address[nodeList.size()];
		int index=0;
    	for (OLSRNode neighbor: nodeList){
    		//At the moment (it should be different) all nodes use the same port, defined in the omolsr.properties
    		IpAddress ipAddress = new IpAddress(neighbor.getAddress(), PropertiesLoader.getUnicastPort());
    		members[index++]=ipAddress;
    	}
    	this.view_id = new ViewId(this.localNode.getJGroupsAddress());
	}	
	
	//	PUBLIC METHODS --
	
	/**
	 * Returns the the bandwidth capacity of a specific node
	 */
	public long getBandwidthCapacityInBytesOf (OLSRNode node){
		return (routingTable.getRoutingTableEntry(node)==null)? ((node.equals(localNode))? localNode.getBwBytesCapacity() : -1) :
			routingTable.getRoutingTableEntry(node).getDestAddr().getBwBytesCapacity();
	}
	public long getBandwidthCapacityInMessagesOf (OLSRNode node){
		return (routingTable.getRoutingTableEntry(node)==null)? localNode.getBwMessagesCapacity() :
			routingTable.getRoutingTableEntry(node).getDestAddr().getBwMessagesCapacity();
	}
	public int getHopCountTo (OLSRNode olsrNode){
		//When the result is null we are searching our own node where hops = 0
		return (routingTable.getRoutingTableEntry(olsrNode) == null) ?
				0 : routingTable.getRoutingTableEntry(olsrNode).getHops();
	}
	public OLSRNode getLocalNode (){
		return localNode;
	}
	public long getLowestBandwidthCapacityInBytesOfRouteTo (OLSRNode node){
		System.out.print(omolsrNetworkGraph.toString());
		if (routingTable.getRoutingTableEntry(node)==null) return -1;
		return omolsrNetworkGraph.getLowestBandwithInBytesOfRouteTo(localNode, 
					routingTable.getRoutingTableEntry(node).getDestAddr());	
	}
	public long getMyBandwidthCapacityInBytes (){
		return localNode.getBwBytesCapacity();
	}
	public long getMyBandwidthCapacityInMessages (){
		return localNode.getBwMessagesCapacity();
	}
	public OLSRNode getNextHopTo (OLSRNode node){
		return (routingTable.getRoutingTableEntry(node)==null)? ((node.equals(localNode))? localNode : null) :
			routingTable.getRoutingTableEntry(node).getNextAddr();
	}
	public OMOLSRNetworkGraph getOMOLSRNetworkGraph () {
		return omolsrNetworkGraph;
	}
}