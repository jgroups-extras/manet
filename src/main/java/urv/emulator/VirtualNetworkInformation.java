package urv.emulator;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jgroups.Address;

import urv.conf.PropertiesLoader;
import urv.emulator.topology.graph.GraphChanges;
import urv.emulator.topology.graph.GraphInformation;
import urv.emulator.topology.parser.DynamicPajekParserImpl;
import urv.emulator.topology.parser.PajekParserImpl;
import urv.emulator.topology.parser.Parser;
import urv.util.graph.NetworkGraph;
import urv.util.graph.Node;
import urv.util.graph.Weight;

/**
 * This class offers information to perform the simulation.
 * Offers information about the network topology (neighbour information)
 * and also about new Virtual IP generation
 * 
 * @author Marcel Arrufat Arias
 */
public class VirtualNetworkInformation {

	//	CLASS FIELDS --
	
	private final static VirtualNetworkInformation instance = new VirtualNetworkInformation();
		private VirtualAddressGenerator addressGenerator;
	private GraphInformation graphInformation;
	//Network graph info is contained inside the graph information
	private NetworkGraph<Node,Weight> networkGraph;
	private GraphChanges graphChanges;	
	private String graphFile;
	
	//	CONSTRUCTORS --
	
	// Private constructor suppresses generation of a (public) default constructor
	private VirtualNetworkInformation() {
		graphFile = PropertiesLoader.getGraphFile();		
		//load file
		Parser ppi = loadParser();		
		graphInformation = ppi.loadNetwork(graphFile);		
		//Get the two fields
		networkGraph = graphInformation.getGraph();
		graphChanges = graphInformation.getChanges();
		//For dynamic files we must also initialize the network
		//Static files perform this transparently (just do nothing)		
		performInitializationChanges();
		System.out.println(networkGraph.toString());		
		//Create Virtual Address Generator
		addressGenerator = new VirtualAddressGenerator();
		createEmuAddresses();
	}

	//	STATIC METHODS --
	
	public static VirtualNetworkInformation getInstance(){
		return instance;
	}
	
	//	PUBLIC METHODS --
	
	/**
	 * Checks if the two nodes are neighbours (are in radio range) in the current
	 * network topology
	 * @param addr1
	 * @param addr2
	 * @return
	 */
	public boolean areNeighbours(Address addr1, Address addr2){		
		//Perform translation to nodes
		Node node1 = addressGenerator.getNodeNumber(addr1);
		Node node2 = addressGenerator.getNodeNumber(addr2);		
		//Return neighbour relationship
		return networkGraph.areNeighbours(node1,node2);
	}
	/**
	 * Returns a new Address for an existing node in the simulation
	 * @param nodeId id of the node in the simulation
	 * @return
	 */
	public Address getEmuNodeAddress(int nodeId){		
		//Create a hostname and IpAddress
		return addressGenerator.getEmuAddress(nodeId);
	}
	/**
	 * Returns a list with all the neighbours of the given address
	 * If there are no neighbours, means that the node is isolated and
	 * is not currently connected to the others
	 * @param addr
	 * @return
	 */
	public List<Address> getNeighbours(Address addr){
		//Perform translation to nodes
		Node node1 = addressGenerator.getNodeNumber(addr);
		Set<Node> nodeList = networkGraph.getNeighbours(node1);
		List<Address> inetList = new LinkedList<>();
		for(Node n:nodeList){
			inetList.add(addressGenerator.getAddress(n));
		}		
		return inetList;
	}
	/**
	 * Returns the total number of nodes in the network
	 * @return
	 */
	public int getNetworkSize(){
		return networkGraph.getNetworkSize();
	}
	public String getStringGraph(){
		return networkGraph.toString();
	}
	public boolean performTopologyChangesUntilSecond(int second){
		return graphChanges.performChangesUntilTimeInterval(second,networkGraph);
	}
	
	//	PRIVATE METHODS --
	
	/**
	 * Creates all the addresses that will be used in the simulation
	 *
	 */
	private void createEmuAddresses() {
		int numNodes = getNetworkSize();
		for (int i=0;i<numNodes;i++){
			createEmuNodeAddress(i+1);
		}		
	}	
	/**
	 * Returns a new Address for a new node in the simulation
	 * Also the mapping between the address and the node number 
	 * is stored in the address generator
	 * Provides a virtual IP and hostname
	 * The node number must be greater than zero
	 * @param nodeId id of the node in the simulation
	 * @return
	 */
	private Address createEmuNodeAddress(int nodeId){		
		//Create a hostname and IpAddress
		Address addr = addressGenerator.createEmuAddress(nodeId);		
		return addr;
	}	
	/**
	 * Loads a parser depending on the type of file we are loading
	 * @return
	 */
	private Parser loadParser() {				
		if (graphFile.endsWith(".tim")){
			return new DynamicPajekParserImpl();		
		}else if (graphFile.endsWith(".net")){			
			return new PajekParserImpl();
		}else{
			System.err.println("Unknown file extension for file: "+graphFile);
			System.err.println("Should end with .net or .tim");
			System.exit(-1);
			return null;
		}		
	}	
	private void performInitializationChanges() {
		networkGraph = graphInformation.getGraph();
		graphChanges.performChangesUntilTimeInterval(1, networkGraph);		
	}

	public boolean areNeighbours(InetAddress addr1, InetAddress addr2) {
		//Perform translation to nodes
		Node node1 = addressGenerator.getNodeNumber(addr1);
		Node node2 = addressGenerator.getNodeNumber(addr2);		
		//Return neighbour relationship
		return networkGraph.areNeighbours(node1,node2);
	}	
}