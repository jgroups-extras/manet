package urv.emulator;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.jgroups.Address;
import org.jgroups.stack.IpAddress;

import urv.conf.ApplicationConfig;
import urv.conf.PropertiesLoader;
import urv.emulator.topology.AddressDoubleMap;
import urv.util.graph.Node;

/**
 * This class generates Addresses for the simulation
 * 
 * @author Marcel Arrufat Arias
 */
public class VirtualAddressGenerator {

	public static final int UNICAST_PORT = PropertiesLoader.getUnicastPort();

	//	CLASS FIELDS --
	
	private byte[] baseIPs;
	private AddressDoubleMap<Address,Node> addressMap;
	private AddressDoubleMap<InetAddress,Node> inetAddressMap;

	//	CONSTRUCTORS --
	
	public VirtualAddressGenerator() {
		baseIPs=ApplicationConfig.emulatedIPs;
		addressMap = new AddressDoubleMap<>();
		inetAddressMap = new AddressDoubleMap<>();
	}
	
	//	PUBLIC METHODS --

	/**
	 * Provides a new emulated address to a new node in the simulation
	 * InetAddress and hostname are created
	 * @param nodeNumber The number of the node in the simulation
	 * @return
	 */
	public Address createEmuAddress(int nodeNumber){
		String hostname = "host"+nodeNumber;
		Address addr = null;
		if (nodeNumber>254){
			System.err.println("Fatal error: Too many nodes in the simulation!");
			System.exit(-1);
		}
		//Create InetAddress
		try {
			InetAddress inetAddress = InetAddress.getByAddress(hostname,new byte[]{baseIPs[0],baseIPs[1],baseIPs[2],(byte)(baseIPs[3]+nodeNumber-1)});
			addr = new IpAddress(inetAddress, UNICAST_PORT);
			//Store mapping
			addressMap.addDoubleMap(addr, new Node(nodeNumber));
			inetAddressMap.addDoubleMap(inetAddress, new Node(nodeNumber));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}		
		return addr;
	}
	
	/**
	 * Returns an already created Emulated Address
	 */
	public Address getEmuAddress(int nodeNumber){
		return addressMap.getAddress(new Node(nodeNumber));
	}
	
	/**
	 * Returns the Address of the node from the given 
	 * node number
	 * @param nodeNumber
	 * @return
	 */
	public Address getAddress(Node nodeNumber){
		return addressMap.getAddress(nodeNumber);
	}
	
	/**
	 * Returns the InetAddress of the node from the given 
	 * node number
	 * @param nodeNumber
	 * @return
	 */
	public InetAddress getInetAddress(Node nodeNumber){
		return inetAddressMap.getAddress(nodeNumber);
	}

	/**
	 * Returns the node number of the node from the given 
	 * InetAddress
	 * @param addr
	 * @return
	 */
	public Node getNodeNumber(Address addr){
		return addressMap.getNodeNumber(addr);
	}

	/**
	 * Returns the node number of the node from the given 
	 * InetAddress
	 * @param addr
	 * @return
	 */
	public Node getNodeNumber(InetAddress addr) {
		return addressMap.getNodeNumber(getAddress(addr));
	}
	

	/**
	 * @param inetAddress
	 * @return
	 */
	static Address getAddress(InetAddress inetAddress) {
		IpAddress ipAddress = new IpAddress(inetAddress, UNICAST_PORT);
		return ipAddress;
	}

}