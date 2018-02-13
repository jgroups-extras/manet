package urv.emulator.topology.parser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import urv.emulator.topology.graph.GraphChanges;
import urv.emulator.topology.graph.GraphEvent;
import urv.emulator.topology.graph.GraphInformation;
import urv.util.graph.NetworkGraph;
import urv.util.graph.Node;
import urv.util.graph.Weight;

/**
 * This class allows loading Pajek Dynamic network files (.tim)
 * No order is considered when executing graph changes in a selected
 * time interval (i.e. commands with in the same time interval can be 
 * run in no defined order 
 * 
 * @author Marcel Arrufat Arias
 */
public class DynamicPajekParserImpl implements Parser {

	//	CONSTANTS --
	
	protected static String PARSE_REAL_TIME_INTERVAL = "TI";
	protected static String PARSE_ADD_VERTEX = "AV";
	protected static String PARSE_ADD_EDGE = "AE";
	protected static String PARSE_HIDE_EDGE = "HE";
	protected static String PARSE_SHOW_EDGE = "SE";
	
	//	OVERRIDDEN METHODS --
	
	public GraphInformation loadNetwork(String file) {
		//Open and parse file		
		NetworkGraph<Node,Weight> netGraph = new NetworkGraph<Node,Weight>();
		GraphChanges graphChanges = new GraphChanges();
		String line;
		String temp;
		try {			
			FileReader netFile = new FileReader(file);
			BufferedReader reader = new BufferedReader(netFile);
			//First of all, read number of nodes
			line = reader.readLine();
			StringTokenizer firstLine = new StringTokenizer(line);
			firstLine.nextToken();
			//Skip events line
			reader.readLine();
			//Next one should be a TI tag
			//Initialize time interval
			int timeInterval= -1;			
			while((line=reader.readLine())!=null){				
				StringTokenizer nodeLine = new StringTokenizer(line);
				//Get type
				temp=nodeLine.nextToken();
				//if we found a new TimeInstant Tag, create a new entry for storing all changes
				if (temp.equals(PARSE_REAL_TIME_INTERVAL)){					
					//Read time interval
					timeInterval=Integer.parseInt(nodeLine.nextToken());					
				}else{
					GraphEvent graphEvent = null;
					if (temp.equals(PARSE_ADD_VERTEX)){
						String vertex = nodeLine.nextToken();
						//Skip hostname (maybe is not necessary)
						if (nodeLine.hasMoreTokens())
							nodeLine.nextToken();
						graphEvent = new GraphEvent(GraphEvent.ADD_VERTEX,vertex);
					}
					else if (temp.equals(PARSE_ADD_EDGE)){
						String src = nodeLine.nextToken();
						String dst = nodeLine.nextToken();
						String weight = nodeLine.nextToken();
						//Add a new edge to the graph
						graphEvent = new GraphEvent(GraphEvent.ADD_EDGE,src,dst,weight);
					}
					else if (temp.equals(PARSE_HIDE_EDGE)){
						String src = nodeLine.nextToken();
						String dst = nodeLine.nextToken();
						graphEvent = new GraphEvent(GraphEvent.HIDE_EDGE,src,dst);
					}
					else if (temp.equals(PARSE_SHOW_EDGE)){
						String src = nodeLine.nextToken();
						String dst = nodeLine.nextToken();
						graphEvent = new GraphEvent(GraphEvent.SHOW_EDGE,src,dst);						
					}  
					//Add all the changes with this timeInterval
					graphChanges.addGraphEvent(timeInterval,graphEvent);
				}				
			}			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		//Since it is a dynamic graph, we will return the set of changes
		return new GraphInformation(netGraph,graphChanges);
	}
}