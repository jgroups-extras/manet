package urv.util.graph;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jgroups.Address;
import org.jgroups.util.ByteArrayDataOutputStream;
import org.jgroups.util.Streamable;

import urv.olsr.data.BandwidthUpdatable;
import urv.olsr.data.OLSRNode;

/**
 * This class represents file network in edge-list terms
 * A node list is also stored, necessary for mapping from file to net ids 
 * 
 * @author Marcel Arrufat
 *
 */
public class NetworkGraph<N,W> implements Serializable, BandwidthUpdatable{

	//	CLASS FIELDS --
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private LinkedList<Edge<N>> edges = null;
	private Set<N> nodes = null;
	//Map which contains a list of nodes (neighbours) for each node in the network
	//This is an static approach, since we cannot remove edges from the graph, and hence we
	//cannot remove neighbours
	private HashMapSet<N,N> neighbourList = new HashMapSet<>();
	private LockObject lock = new LockObject();
	
	//	CONSTRUCTORS --
	
	public NetworkGraph(){	
		nodes = new HashSet<>();
		edges = new LinkedList<>();
	}
	
	//	OVERRIDDEN METHODS --
	
	@Override
	public Object clone(){
		NetworkGraph<N, W> newGraph = new NetworkGraph<>();	
		synchronized (lock) {	
			for (Edge<N> e:this.edges){
				newGraph.addEdge(e.getSource(),e.getTarget(),e.getWeight());
				// Nodes and neighbours automatically added by addEdge
			}	
		}
		return newGraph;
	}	
	/**
	 * Prints the list of nodes and edges
	 */
	@Override
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		synchronized (lock) {
		buffer.append("Nodes:"+"\n");
			for (N n:nodes){
				buffer.append("\t"+n.toString()+"\n");
			}	
			buffer.append("Edges:"+"\n");
			for (Edge<N> e: edges){
				buffer.append("\t"+e.toString()+"\n");
			}			
			buffer.append("Neighbours:"+"\n");
			for (N n:neighbourList.keySet()){
				buffer.append("\t"+n.toString()+": ");
				for (N noN:neighbourList.getSet(n)){
					buffer.append(noN+" ");
				}
				buffer.append("\n");
			}			
			return buffer.toString();
		}
	}	
	@Override
	public void updateBwOf(OLSRNode node) {
		synchronized (lock) {
			for (N currentNode : getNodeList()){
				if (currentNode instanceof OLSRNode && currentNode.equals(node)){
					((OLSRNode) currentNode).updateBandwidth(node);
					break;
				}
			}	
			for (Edge<N> edge : getEdges()){
				N source = edge.getSource();
				N target = edge.getTarget();
				if (source instanceof OLSRNode && source.equals(node)) {
					((OLSRNode) source).updateBandwidth(node);
				}
				if (target instanceof OLSRNode && target.equals(node)) {
					((OLSRNode) target).updateBandwidth(node);
				}
			}
			for (N currentNode : neighbourList.keySet()){
				if (currentNode instanceof OLSRNode && currentNode.equals(node)){
					((OLSRNode) currentNode).updateBandwidth(node);
					for (N noN : neighbourList.getSet((N) node)){
						if (noN instanceof OLSRNode && noN.equals(node)) {
							((OLSRNode) noN).updateBandwidth(node);
						}
					}
				}
			}	
		}		
	}
	
	//	PUBLIC METHODS --
	
	/**
	 * Add a new (directed) Edge to the network structure
	 * @param source
	 * @param target
	 * @param weight
	 */
	public void addEdge(N source, N target, Weight weight){
		synchronized (lock) {
			edges.add(new Edge<>(source,target,weight));			
			neighbourList.addToSet(source,target);
			if (!nodes.contains(source))
				nodes.add(source);
			if (!nodes.contains(target))
				nodes.add(target);
		}
	}	
	/**
	 * Add a new node to network structure
	 * This structure will be needed when mapping fileIds into netIds
	 * @param node
	 */
	public void addNode(N node){
		synchronized (lock) {
			if (!nodes.contains(node))
				nodes.add(node);
			if (!neighbourList.containsKey(node))
				neighbourList.putSet(node,new HashSet<N>());
		}		
	}	
	/**
	 * Checks is node1 has node2 as neighbour or node2 has node1 as neighbour
	 * @param node1
	 * @param node2
	 * @return
	 */
	public boolean areLinked(N node1, N node2){
		synchronized (lock) {
			return neighbourList.existsInList(node1,node2) ||
				neighbourList.existsInList(node2,node1);
		}		
	}
	/**
	 * Checks is node1 has node2 has neighbour
	 * @param node1
	 * @param node2
	 * @return
	 */
	public boolean areNeighbours(N node1, N node2) {		
		synchronized (lock) {
			return neighbourList.existsInList(node1,node2);
		}
	}
	public void assignWeightToAllEdges(Float float1) {
		for (Edge<?> e:this.edges){
			e.getWeight().setValue(float1);
		}
	}
	/**
	 * Empties the graph, nodes, edges and neighbours
	 *
	 */
	public void clear(){		
		synchronized (lock) {
			nodes.clear();
			edges.clear();
			neighbourList.clear();
		}		
	}	
	public Edge<N> getEdgeBetween(N node1, N node2) {
		synchronized (lock) {
			for(Edge<N> e:edges){
				if (e.getSource().equals(node1) && e.getTarget().equals(node2)){
					return e;
				}
				else if (e.getSource().equals(node2) && e.getTarget().equals(node1)){
					return e;
				}
			}
		}
		return null;
	}	
	/**
	 * Return edge list from actual network graph
	 * @return
	 */
	public LinkedList<Edge<N>> getEdges(){		
		synchronized (lock) {
			return this.edges;
		}
	}	
	/**
	 * Return edge list where the node n is the source or the target
	 * @return
	 */
	public LinkedList<Edge<N>> getEdges(N node){
		LinkedList<Edge<N>> edgeList = new LinkedList<>(); 
		synchronized (lock) {
			for(Edge<N> e:edges){
				if (e.getSource().equals(node) || e.getTarget().equals(node)){
					edgeList.add(e);
				}
			}			
		}
		return edgeList;
	}	
	/**
	 * Returns a list of nodes that are in the neighbor list of node1 or 
	 * that have node1 in their own neighbor list
	 * @param node1
	 * @return
	 */
	public Set<N> getLinkedNodes(N node1) {
		Set<N> linkedNodes = new HashSet<>();		
		synchronized (lock) {
			for (N n:this.nodes){
				if (neighbourList.existsInList(n,node1) ||
						neighbourList.existsInList(node1,n)){
					linkedNodes.add(n);
				}
			}
			return linkedNodes;
		}		
	}	
	/**
	 * Returns a list of neighbours of the current node
	 * @param node1
	 * @return
	 */
	public Set<N> getNeighbours(N node1) {
		synchronized (lock) {
			Set<N> nodeList = neighbourList.getSet(node1);
			return nodeList;
		}		
	}	
	/**
	 * Returns the total number of nodes in the network
	 * 
	 * @return nodes in the network
	 */
	public int getNetworkSize(){
		synchronized (lock) {
			return this.nodes.size();
		}
	}
	/**
	 * Return node list from actual network graph
	 * @return
	 */
	public Set<N> getNodeList(){
		synchronized (lock) {
			return this.nodes;
		}
	}	
	/**
	 * Returns all the nodes under the current node (in a directed graph)
	 * without the current node
	 * @param node1
	 * @return
	 */
	public HashSet<N> getSubtree(N node1) {
		synchronized (lock) {
			HashSet<N> set = getNeighborsRecursively(node1,0);
			//The head of the tree was already added, we must skip it
			set.remove(node1);
			return set;
		} 
	}
	/**
	 * Removes an edge from the graph
	 * @param src
	 * @param dst
	 * @param w
	 */	
	public void removeEdge(N src, N dst, Weight w) {
		Edge<N> edge = new Edge<>(src,dst,w);
		synchronized (lock) {
			if (edges.contains(edge)){
				//remove the edge
				System.out.println("\t\tRemoving from graph");
				edges.remove(edge);
				//They are not neighbors anymore
				neighbourList.removeFromBothSets(src,dst);
			}
		}
	}	
	/**
	 * Removes edges where the node n is the source or the target.
	 * It also removes the node
	 * @return
	 */
	public void removeEdges(N node){		
		synchronized (lock) {
			Iterator<Edge<N>> it = edges.iterator();
			while(it.hasNext()){
				Edge<N> e = it.next();
				if (e.getSource().equals(node)){ 
					it.remove();
					neighbourList.removeFromSet(node,e.getTarget());
				}
				else if (e.getTarget().equals(node)){
					it.remove();
					neighbourList.removeFromSet(e.getSource(),node);
				}
			}
			nodes.remove(node);			
		}
	}
	/**
	 * Removes edges that link either node1 to node2 or node2 to node1
	 * @return
	 */
	public void removeEdges(N node1, N node2){		
		synchronized (lock) {
			Iterator<Edge<N>> it = edges.iterator();
			while(it.hasNext()){
				Edge<N> e = it.next();
				if (e.getSource().equals(node1) && e.getTarget().equals(node2)){
					it.remove();
					neighbourList.removeFromSet(node1,node2);
				}
				else if (e.getSource().equals(node2) && e.getTarget().equals(node1)){
					it.remove();
					neighbourList.removeFromSet(node2,node1);
				}
			}			
		}
	}	
	/**
	 * Removes edges that have node as target
	 * @param node
	 */
	public void removeEdgesWithTarget(N node){
		synchronized (lock) {
			Iterator<Edge<N>> it = edges.iterator();
			while(it.hasNext()){
				Edge<N> e = it.next();
				if (e.getTarget().equals(node)){
					it.remove();
					neighbourList.removeFromSet(e.getSource(),node);
				}
			}
		}
	}
	
	public void removeIsolatedNodes() {
		synchronized(lock){
			Iterator<N> it = nodes.iterator();
			while (it.hasNext()){
				N n = it.next();
				boolean isolated = true;
				for (Edge<?> e:this.edges){
					if (e.getSource().equals(n) || e.getTarget().equals(n)){
						isolated = false;
					}
				}
				if (isolated){
					it.remove();
				}
			}
		}		
	}
	
	/**
	 * Method that allows to print the network graph into a pajek file
	 */
	public String  toPajek(String fileName, boolean writeFile){
		StringBuffer buffer = new StringBuffer();
		int idNode = 0;
		HashMap<String,Edge<N>> edgesStored = new HashMap<>();
		Edge<N> eTmp = null;
		HashMap<Integer, Integer> idNodetoPajekNodes = new HashMap<>();
		
		synchronized (lock) {
			buffer.append("*Vertices "+nodes.size()+"\n");
			//Print of the nodes IDs
			int contNodes=0;
			for (N n:nodes){
				contNodes++;
				idNode = getIdNode(n);
				buffer.append(contNodes+" \"node_"+getTwoLastBytes(n)+"\"\n");
				idNodetoPajekNodes.put(idNode,contNodes);
			}	
			buffer.append("*Edges"+"\n");
			//Print the edge
			for (Edge<N> e: edges){
				//Avoid printing double edges, if we have already print 1-2, we won't print 2-1, 
				//they are bidirectional but we don't need to print both edge in the pajek file
				if(edgesStored.get(e.toString())==null){
					eTmp = new Edge<>(e.getTarget(),e.getSource(),e.getWeight());//insert the complementary edge of the bidirection
					edgesStored.put(eTmp.toString(), eTmp);
					buffer.append(idNodetoPajekNodes.get(getIdNode(e.getSource()))+" "+idNodetoPajekNodes.get(getIdNode(e.getTarget()))+" "+e.getWeight().getValue().intValue()+"\n");
				}else{
					edgesStored.remove(e.toString());
				}
			}			
			if(writeFile){
				try (FileWriter fw = new FileWriter(fileName)) {
					fw.write(buffer.toString());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}			
			return buffer.toString();
		}
	}
	
	/**
	 * Method that returns the id of the node.
	 * When the node is an instance of Node, the id is itself
	 * If the node is an instance of OLSRNode, the id is the composition of the 2 last bytes
	 */
	private static int getIdNode(Object n){		
		int idNode = 0; 
		if(n instanceof urv.olsr.data.OLSRNode){
			byte addr[];
			try {
				addr = getBytes(((OLSRNode)n).getAddress());
			} catch (Exception e) {
				e.printStackTrace();
				return -1;
			}
			for ( int cnt=addr.length - 2;cnt<addr.length;cnt++ )  {  
			   idNode = (idNode*1000)+(addr[cnt]<0 ? addr[cnt]+256 : addr[cnt]) ;  
			}
		}else if(n instanceof urv.util.graph.Node){
			idNode =  Integer.parseInt(n.toString());
		}else{
			idNode = -1;
		}		
		return idNode; 		
	}
	
	//	PRIVATE METHODS --
	
	/**
	 * Returns a set with all the child nodes of a given node
	 * @param node
	 * @return Neighbors
	 */
	private HashSet<N> getNeighborsRecursively(N node,int level) {		
		Set<N> nodeSet = neighbourList.getSet(node);
		List<N> newSet = new LinkedList<>();		
		if (nodeSet != null) {
			for (N nodeInSet : nodeSet) {
				// Recursivity, end nodes will not enter here
				newSet.addAll(getNeighborsRecursively(nodeInSet, level));
				if (level <= 10){
					level++;
				}
			}
		}		
		//End case
		newSet.add(node);
		if (newSet.size()>nodes.size())
			System.err.println("ERROR: There are "+newSet.size() +" nodes in the set!!");
		return new HashSet<>(newSet);		
	}
	
	/**
	 * Serializes a Streamable object to a byte array
	 * @param s object to serialize into bytes
	 * @return serialized bytes
	 * @throws Exception
	 */
	private static byte[] getBytes(Streamable s) throws Exception {
		ByteArrayDataOutputStream baos = new ByteArrayDataOutputStream(255, true);
		s.writeTo(baos);
		return baos.buffer();
	}
	
	/**
	 * Method that returns the last two bytes of the node address
	 */
	private static String getTwoLastBytes(Object n){		
		String lastTwoBytes = ""; 
		if(n instanceof urv.olsr.data.OLSRNode){
			Address address = ((OLSRNode)n).getAddress();
			byte[] addr;
			try {
				addr = getBytes(address);
			} catch (Exception e) {
				e.printStackTrace();
				return "-1";
			}
			for ( int cnt=addr.length - 2;cnt<addr.length;cnt++ )  {  
				lastTwoBytes = lastTwoBytes+(addr[cnt]<0 ? addr[cnt]+256 : addr[cnt]);
				if(cnt==2) lastTwoBytes+=".";
			}
		}else if(n instanceof urv.util.graph.Node){
			lastTwoBytes =  n.toString();
		}else{
			lastTwoBytes = "-1";
		}
		return lastTwoBytes; 		
	}	
	
	//	PRIVATE CLASSES --
	
	/**
	 * To serialize the whole object we need that the
	 * lock object implements Serializable
	 */
	class LockObject implements Serializable{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		//Nothing
	}
}