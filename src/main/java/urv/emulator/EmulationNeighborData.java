package urv.emulator;

import java.net.InetAddress;
import java.util.Hashtable;

import urv.olsr.data.OLSRNode;
import urv.olsr.data.neighbour.NeighborTable;

/**
 * This class is used to verify the correct topology view of
 * a node. Concretely, it helps to compare the two-hop neighborhood
 * of a node with the information contained in the emulation graph
 * 
 * @author Marcel Arrufat Arias
 */
public class EmulationNeighborData {

	//	CLASS FIELDS --
	
	private static EmulationNeighborData instance = new EmulationNeighborData();
	private Hashtable<InetAddress,NeighborTable> table;
	
	//	CONSTRUCTORS --
	
	private EmulationNeighborData() {		
		this.table = new Hashtable<InetAddress,NeighborTable>();
	}
	
	//	STATIC METHODS --
	
	/**
	 * Returns an instance of the EmulationNeighborData (Singleton)
	 * @return
	 */
	public static EmulationNeighborData getInstance(){
		return instance;
		
	}	
	
	//	PUBLIC METHODS --
	
	/**
	 * Adds a new table to the structure
	 * @param node
	 * @param neighborTable
	 */
	public void registerNeighborTable(OLSRNode node, NeighborTable neighborTable){
		InetAddress address = node.getAddress();
		table.put(address,neighborTable);
	}
	
	//	ACCESS METHODS --
	
	/**
	 * Returns a previous registered neighbor table
	 * @param node
	 * @return
	 */
	public NeighborTable getNeighbortable(OLSRNode node){
		InetAddress address = node.getAddress();
		return table.get(address);
	}
}