package urv.emulator.topology.graph;

import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import urv.util.graph.HashMapSet;
import urv.util.graph.NetworkGraph;
import urv.util.graph.Node;
import urv.util.graph.Weight;

/**
 * This class keeps all the changes that must be performed
 * on a graph for a certain instant
 * 
 * @author Marcel Arrufat Arias
 */
public class GraphChanges {

	//	CLASS FIELDS --
	
	private HashMapSet<Integer,GraphEvent> changesSet;

	//	CONSTRUCTORS --
	
	public GraphChanges() {
		changesSet = new HashMapSet<Integer,GraphEvent>(); 
	}

	//	OVERRIDDEN METHODS --
	
	public String toString(){
		StringBuffer buff = new StringBuffer();		
		for (Integer key:changesSet.keySet()){
			buff.append("\tTime = "+key+"\n\t"+changesSet.get(key).toString());
			buff.append("\n");
		}
		return buff.toString();
	}
	
	//	PUBLIC METHODS --
	
	public void addGraphEvent(int timeInterval, GraphEvent information) {
		changesSet.addToSet(new Integer(timeInterval),information);		
	}	
	/**
	 * Performs the changes stored whose timeInterval is less than 
	 * currentTimeInterval
	 * 
	 * @param currentTimeInterval
	 * @param networkGraph
	 */
	public boolean performChangesUntilTimeInterval(Integer currentTimeInterval, NetworkGraph<Node,Weight> networkGraph) {
		TreeSet<Integer> orderedSet = new TreeSet<Integer>(); 
		boolean changes=false;
		//Order changes
		for (Integer interval:changesSet.keySet()){			
			if (currentTimeInterval>=interval){
				orderedSet.add(interval);
				System.out.println("*************************** There will be topology changes*********************************!!!!");
				changes=true;
			}
		}
		Iterator<Integer> it = orderedSet.iterator();
		while (it.hasNext()){
			Integer currentTime = it.next();
			HashSet<GraphEvent> events = changesSet.get(currentTime);
			performEvents(events,networkGraph);
			//These events should not be performed again
			changesSet.remove(currentTime);
		}		
		return changes;		
	}

	//	PRIVATE METHODS --
	
	private void performEvents(HashSet<GraphEvent> events, NetworkGraph<Node,Weight> networkGraph) {		
		for (GraphEvent event:events){			
			//Get info from the event
			Node src = new Node(Integer.parseInt(event.getSrcNode()));
			Node dst = new Node(Integer.parseInt(event.getDstNode()));
			Weight w = new Weight();
			w.setValue(new Float(event.getWeight()));			
			//Add to the network graph
			if (event.getType().equals(GraphEvent.ADD_EDGE)||event.getType().equals(GraphEvent.SHOW_EDGE)){
				//System.err.println("\t\tAdding to graph:"+new Edge(src,dst,w));
				networkGraph.addEdge(src,dst,w);
				networkGraph.addEdge(dst,src,w);
			}else if(event.getType().equals(GraphEvent.HIDE_EDGE)){
				networkGraph.removeEdge(src,dst,w);
				networkGraph.removeEdge(dst,src,w);
			}else if(event.getType().equals(GraphEvent.ADD_VERTEX)){
				networkGraph.addNode(src);
			}
		}		
	}
}