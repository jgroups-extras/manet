package urv.emulator.topology;

import java.util.Hashtable;

/**
 * 
 * @author Marcel Arrufat Arias
 */
public class AddressDoubleMap<K1,K2> {
	
	//	CLASS FIELDS --

	private Hashtable<K1,K2> inetToIntMap;
	private Hashtable<K2,K1> intToInetMap;
	
	//	CONSTRUCTORS --
	
	public AddressDoubleMap() {
		inetToIntMap=new Hashtable<K1,K2>();
		intToInetMap= new Hashtable<K2,K1>();
	}
	
	//	PUBLIC METHODS --
	
	/**
	 * Creates a double relationship between this InetAddress
	 * and node number
	 */
	public void addDoubleMap(K1 key1,K2 key2){
		inetToIntMap.put(key1,key2);
		intToInetMap.put(key2,key1);
	}
	
	//	ACCESS METHODS --
	
	/**
	 * Returns the InetAddress of the node from the given 
	 * node number
	 * @param nodeNumber
	 * @return
	 */
	public K1 getInetAddress(K2 key2){
		return intToInetMap.get(key2);
	}	
	/**
	 * Returns the node number of the node from the given 
	 * InetAddress
	 * @param addr
	 * @return
	 */
	public K2 getNodeNumber(K1 key1){
		return inetToIntMap.get(key1);
	}
}