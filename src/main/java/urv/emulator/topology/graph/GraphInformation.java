package urv.emulator.topology.graph;

import urv.util.graph.NetworkGraph;
import urv.util.graph.Node;
import urv.util.graph.Weight;

/**
 * This class stores information about a network graph
 * and the changes to be performed on the graph
 * 
 * @author Marcel Arrufat Arias
 */
public class GraphInformation {

	//	CLASS FIELDS --
	
	private NetworkGraph<Node,Weight> graph;
	private GraphChanges changes;
	
	//	CONSTRUCTORS --
	
	public GraphInformation() {}
	
	/**
	 * @param graph
	 * @param changes
	 */
	public GraphInformation(NetworkGraph<Node,Weight> graph, GraphChanges changes) {
		this.graph = graph;
		this.changes = changes;
	}
	
	//	ACCESS METHODS --
	
	/**
	 * @return Returns the changes.
	 */
	public GraphChanges getChanges() {
		return changes;
	}
	/**
	 * @return Returns the graph.
	 */
	public NetworkGraph<Node,Weight> getGraph() {
		return graph;
	}
}