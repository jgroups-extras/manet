package urv.emulator.topology.graph;

/**
 * This class contains information about events that will generate a graph
 * from a Pajek dynamic file
 * 
 * @author Marcel Arrufat Arias
 */
public class GraphEvent {
	
	//	CONSTANTS --

	public static String ADD_VERTEX = "AV";
	public static String ADD_EDGE 	= "AE";
	public static String HIDE_EDGE 	= "HE";
	public static String SHOW_EDGE 	= "SE";
	
	//	CLASS FIELDS --
	
	private String type;
	private String srcNode;
	private String dstNode;
	private String weight;
	
	//	CONSTRUCTORS --
	
	/**
	 * @param type
	 * @param srcNode
	 */
	public GraphEvent(String type, String srcNode) {
		this(type,srcNode,"-1","1");
	}
	public GraphEvent(String type, String src, String dst) {
		this(type,src,dst,"1");
	}
	/**
	 * @param type
	 * @param srcNode
	 * @param dstNode
	 * @param weight
	 */
	public GraphEvent(String type, String srcNode, String dstNode, String weight) {
		this.type = type;
		this.srcNode = srcNode;
		this.dstNode = dstNode;
		this.weight = weight;
	}
	
	//	OVERRIDDEN METHODS --
	
	public String toString(){
		return "Type="+type+";src="+srcNode+";dst="+dstNode+"weight="+weight;
	}
	
	//	ACCESS METHODS --
	
	/**
	 * @return Returns the dstNode.
	 */
	public String getDstNode() {
		return dstNode;
	}
	/**
	 * @return Returns the srcNode.
	 */
	public String getSrcNode() {
		return srcNode;
	}
	/**
	 * @return Returns the type.
	 */
	public String getType() {
		return type;
	}
	/**
	 * @return Returns the weight.
	 */
	public String getWeight() {
		return weight;
	}
}