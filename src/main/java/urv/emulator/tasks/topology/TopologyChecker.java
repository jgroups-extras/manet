package urv.emulator.tasks.topology;

import java.net.InetAddress;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.jgroups.Address;
import org.jgroups.stack.IpAddress;

import urv.conf.PropertiesLoader;
import urv.emulator.EmulationNeighborData;
import urv.emulator.VirtualNetworkInformation;
import urv.olsr.data.OLSRNode;
import urv.olsr.data.mpr.OLSRSet;
import urv.olsr.data.neighbour.NeighborTable;
import urv.olsr.util.Util;

/**
 * @author Marcel Arrufat Arias
 */
public class TopologyChecker {

	public static final int UNICAST_PORT = PropertiesLoader.getUnicastPort();
	
	//	CLASS FIELDS --
	
	private VirtualNetworkInformation vni;
	private Hashtable<Address,OLSRNode> transTable;

	//	CONSTRUCTORS --
	
	public TopologyChecker(VirtualNetworkInformation vni) {
		this.vni = vni;
		transTable = new Hashtable<>();
		for (int i=0;i<vni.getNetworkSize();i++){
			OLSRNode node = new OLSRNode();
			node.setValue(vni.getEmuNodeAddress(i+1));
			transTable.put(vni.getEmuNodeAddress(i+1),node);
		}
	}
	
	//	PUBLIC METHODS --

	/**
	 * Compares neighbors stored in the neighbor table with information
	 * from the emulation (topology graph)
	 * return true if both neighbors set match 
	 * @param emuNodeAddress
	 * @return
	 */
	public boolean checkOneHopNeighbors(Address emuNodeAddress) {
		OLSRNode node = new OLSRNode();
		//We just need the inetAddress to create an OLSRNOde and get the neighborTable
		node.setValue(emuNodeAddress);
		NeighborTable neighborTable = EmulationNeighborData.getInstance().getNeighbortable(node);		
		///Check whether the information is available (applications are launched after the tasks)
		if (neighborTable != null) {
			OLSRSet realNeighbors = neighborTable.getCopyOfNeighbors();
			// Get the list of neighbors in the graph
			List<Address> emulatedAddresses = vni.getNeighbours(emuNodeAddress);
			// Get the "real list" from the neighbor table, and change it to
			// Addresses
			List<Address> realAddresses = Util.getAddressList(realNeighbors);
			// We should have the same number of neighbors
			//And all nodes shoud be connected
			if (emulatedAddresses.size() != realAddresses.size() && emulatedAddresses.size()!=0) return false;
			// For each address in the graph, verify it exists in the
			// realNeighbors
			// that is, in the neighbor table
			for (Address emulatedAddr : emulatedAddresses) {
				Iterator<Address> it = realAddresses.iterator();
				while (it.hasNext()) {
					Address realAddress = it.next();
					if (realAddress.equals(emulatedAddr)) {
						// Empty the real Addressess list
						it.remove();
						continue;
					}
				}
			}
			// If the set is empty, we have looked all the neighbors on the real
			// addressess
			return realAddresses.isEmpty();
		}
		return false;
	}
	
	public boolean checkTwoHopNeighbors(Address emuNodeAddress) {
		OLSRNode node = new OLSRNode();
		//We just need the inetAddress to create an OLSRNOde and get the neighborTable
		node.setValue(emuNodeAddress);
		NeighborTable neighborTable = EmulationNeighborData.getInstance().getNeighbortable(node);
		///Check whether the information is available (applications are launched after the tasks)
		if (neighborTable != null) {
			// Get the list of neighbors in the graph
			List<Address> emulatedAddresses = vni.getNeighbours(emuNodeAddress);
			// For each emulated neighbour, get the address of emulated
			// neighbors
			for (Address emulatedAddr : emulatedAddresses) {
				// List of NoNs for this node
				// real NoNs
				OLSRSet nons = (OLSRSet) neighborTable.getEntry(
						transTable.get(emulatedAddr)).getNeighborsOfNeighbors().clone();
				// Emulated (from the graph) NoNs
				List<Address> emulatedNeighbors = vni.getNeighbours(emulatedAddr);
				// Remove local node from the NoN (a node is not NoN of himself
				emulatedNeighbors.remove(emuNodeAddress);
				// The amount of NoNs should be the sime, otherwise return false
				if (nons.size() != emulatedNeighbors.size()) return false;

				for (Address neighbor : emulatedNeighbors) {
					OLSRNode translatedNode = transTable.get(neighbor);
					if (nons.contains(translatedNode))
						nons.remove(translatedNode);
				}
				if (!nons.isEmpty()) return false;
			}
			return true;
		}
		return false;
	}
	
	static Address getAddress(InetAddress inetAddress) {
		IpAddress ipAddress = new IpAddress(inetAddress, UNICAST_PORT);
		return ipAddress;
	}
}