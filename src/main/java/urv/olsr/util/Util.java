package urv.olsr.util;

import java.util.LinkedList;
import java.util.List;

import org.jgroups.Address;

import urv.olsr.data.OLSRNode;
import urv.olsr.data.mpr.OLSRSet;

/**
 * @author Marcel Arrufat Arias
 */
public class Util {
	
	//	STATIC METHODS --

	/**
	 * Returns a copy of the list of nodes
	 */
	public static OLSRSet copyNodeSet(OLSRSet originalSet){		
		OLSRSet clonedSet = new OLSRSet();		
		for (OLSRNode node:originalSet){
			clonedSet.add(node.copy());
		}
		return clonedSet;
	}
	
	/**
	 * This method will return a list of the addresses of the neighbors
	 * in the group (virtual neighbors)
	 * 
	 * @param realNeighbors
	 * @return group neighbors
	 */
	public static List<Address> getAddressList(OLSRSet realNeighbors) {
		List<Address> list = new LinkedList<>();
		for (OLSRNode node:realNeighbors){
			list.add(node.getAddress());			
		}
		return list;
	}
}