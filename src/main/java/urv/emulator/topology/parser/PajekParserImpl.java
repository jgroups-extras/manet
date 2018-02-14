package urv.emulator.topology.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import urv.emulator.topology.graph.GraphChanges;
import urv.emulator.topology.graph.GraphInformation;
import urv.util.graph.NetworkGraph;
import urv.util.graph.Node;
import urv.util.graph.Weight;

/**
 * This class retrieves the topology information stored in a Pajek
 * file
 * 
 * @author Marcel Arrufat
 *
 */
public class PajekParserImpl implements Parser {

	//	OVERRIDDEN METHODS --

	public GraphInformation loadNetwork(String file) {
		//Open and parse file		
		NetworkGraph<Node,Weight> netGraph = new NetworkGraph<Node,Weight>();
		String line;
		int numNodes;
		String temp;		
		try {
			String resolvedFilePath = file;
	        if (!(new File(file).exists())) {
	        	resolvedFilePath = ClassLoader.getSystemClassLoader().getResource(file).getPath();
	        }

			FileReader netFile = new FileReader(resolvedFilePath);
			BufferedReader reader = new BufferedReader(netFile);
			//First of all, read number of nodes
			line = reader.readLine();
			StringTokenizer firstLine = new StringTokenizer(line);
			firstLine.nextToken();
			numNodes = Integer.parseInt(firstLine.nextToken());			
			//Read nodes
			//line format: id label			
			for (int i=0;i<numNodes;i++){
				line = reader.readLine();
				StringTokenizer nodeLine = new StringTokenizer(line);
				temp=nodeLine.nextToken();
				netGraph.addNode(new Node(Integer.parseInt(temp)));				
			}			
			//Skip "*Edges" line
			reader.readLine(); 			
			//Finally, read edges
			//line format: source target weight
			Float weight;
			int source,target;			
			while ((line = reader.readLine())!=null){
				StringTokenizer edgeLine = new StringTokenizer(line);
				source = new Integer(Integer.parseInt(edgeLine.nextToken()));
				target = new Integer(Integer.parseInt(edgeLine.nextToken()));
				weight = new Float(Float.parseFloat(edgeLine.nextToken()));
				//This network is represented by directed graph
				netGraph.addEdge(new Node(source),new Node(target),new Weight().setValue(weight));
				netGraph.addEdge(new Node(target),new Node(source),new Weight().setValue(weight));				
			}						
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		//Since it is a static graph, we will return an empty set of changes
		return new GraphInformation(netGraph,new GraphChanges());
	}
}