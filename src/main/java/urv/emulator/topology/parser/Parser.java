package urv.emulator.topology.parser;

import urv.emulator.topology.graph.GraphInformation;

/**
 * This interface must be implemented in order to create
 * file parser classes.
 * 
 * @author Marcel Arrufat
 *
 */
public interface Parser {
	/**
	 * This method must load network file and create a new NetworkGraph.
	 * NetworkGraph must contain node and edges information (both stored in 
	 * LinkedList). This information can be stored by using addNode and addEdge
	 * methods
	 * @param file local file from which NetworkGraph will be load
	 * @return
	 */
	public GraphInformation loadNetwork(String file);
}