package urv.emulator;

import java.net.InetAddress;
import java.net.UnknownHostException;

import urv.conf.ApplicationConfig;
import urv.emulator.topology.AddressDoubleMap;
import urv.util.graph.Node;

/**
 * This class generates InetAddresses for the simulation
 * 
 * @author Marcel Arrufat Arias
 */
public class VirtualAddressGenerator {

	//	CLASS FIELDS --
	
	private byte[] baseIPs;
	private AddressDoubleMap<InetAddress,Node> addressMap;
		
	//	CONSTRUCTORS --
	
	public VirtualAddressGenerator() {
		super();
		baseIPs=ApplicationConfig.emulatedIPs;
		addressMap = new AddressDoubleMap<InetAddress,Node>();
	}
	
	//	PUBLIC METHODS --

	/**
	 * Provides a new emulated address to a new node in the simulation
	 * InetAddress and hostname are created
	 * @param nodeNumber The number of the node in the simulation
	 * @return
	 */
	public InetAddress createEmuInetAddress(int nodeNumber){
		String hostname = "host"+nodeNumber;
		InetAddress addr = null;
		if (nodeNumber>254){
			System.err.println("Fatal error: Too many nodes in the simulation!");
			System.exit(-1);
		}
		//Create InetAddress
		try {
			addr = InetAddress.getByAddress(hostname,new byte[]{baseIPs[0],baseIPs[1],baseIPs[2],(byte)(baseIPs[3]+nodeNumber-1)});
			//Store mapping
			addressMap.addDoubleMap(addr,new Node(nodeNumber));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}		
		return addr;
	}
	/**
	 * Returns an already created Emulated InetAddres
	 */
	public InetAddress getEmuInetAddress(int nodeNumber){
		return addressMap.getInetAddress(new Node(nodeNumber));
	}
	/**
	 * Returns the InetAddress of the node from the given 
	 * node number
	 * @param nodeNumber
	 * @return
	 */
	public InetAddress getInetAddress(Node nodeNumber){
		return addressMap.getInetAddress(nodeNumber);
	}	
	/**
	 * Returns the node number of the node from the given 
	 * InetAddress
	 * @param addr
	 * @return
	 */
	public Node getNodeNumber(InetAddress addr){
		return addressMap.getNodeNumber(addr);
	}
}