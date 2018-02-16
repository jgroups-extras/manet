package org.jgroups.protocols;

import org.jgroups.Address;
import org.jgroups.Global;
import org.jgroups.Header;
import org.jgroups.stack.IpAddress;
import urv.olsr.data.OLSRNode;
import urv.util.graph.HashMapSet;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Header for unicast messages sent in OMOLSR:
  * @author Marcel Arrufat Arias
 */
public class OMOLSRHeader extends Header {

	public static final byte DATA=1;   // arg = null
    public static final byte CONTROL=2;   // arg = PingRsp(local_addr, coord_addr)
    
    public byte type;
    
    //TODO: check if it is needed in OMOLSR
    public IpAddress groupId;
    
    public IpAddress srcAddress;
    
	/**
     * A list of following destinations 
     * <Address>
     */
    private HashMapSet<OLSRNode,OLSRNode> forwardingTable = new HashMapSet<>();
    
	/**
	 * @param type
	 */
	public OMOLSRHeader() {
		
	}
	
	public static String type2Str(byte t) {
		switch (t) {
			case DATA :
				return "DATA";
			case CONTROL :
				return "CONTROL";
			default :
				return "<undefined>";
		}
	}

	/**
	 * Gets the list of all nodes that must receive the 
	 * packet (from all nodes)
	 * @return list
	 */
	public HashMapSet<OLSRNode,OLSRNode> getForwardingTable(){
		return this.forwardingTable;
	}
	
	/**
	 * Gets the list of all nodes that must receive the 
	 * packet (from one node)
	 * @param node
	 * @return
	 */
	public HashSet<OLSRNode> getForwardingTableEntry(OLSRNode node){
		return this.forwardingTable.get(node);
	}
	
	/**
	 * @return Returns the srcAddress.
	 */
	public Address getSrcAddress() {
		return srcAddress;
	}


	/**
	 * Sets the list of all nodes that must receive the
	 * packet
	 * @param list
	 */
	public void setForwardingTable(HashMapSet<OLSRNode,OLSRNode> forwardingTable){
		this.forwardingTable = forwardingTable;
	}

	/**
	 * @param groupId the groupId to set
	 */
	public void setGroupId(Address groupId) {
		this.groupId = (IpAddress)groupId;
	}

	
	/**
	 * @param srcAddress The srcAddress to set.
	 */
	public void setSrcAddress(Address srcAddress) {
		this.srcAddress = (IpAddress)srcAddress;
	}
	
	public void setType(byte type){
		this.type = type;
	}

    @Override
	public short getMagicId() {
        return Constants.OMOLSR_ID;
    }

    @Override
	public Supplier<? extends Header> create() {
        return OMOLSRHeader::new;
    }

    @Override
	public int serializedSize() {
        int retval=Global.BYTE_SIZE + sizeOf(groupId) + sizeOf(srcAddress) + Global.INT_SIZE;
        if(forwardingTable != null) {
            for(Map.Entry<OLSRNode,HashSet<OLSRNode>> entry: forwardingTable.entrySet()) {
                OLSRNode key=entry.getKey();
                HashSet<OLSRNode> val=entry.getValue();
                retval+=key.serializedSize();
                int len=val != null && !val.isEmpty()? val.size() : 0;
                retval+=Global.INT_SIZE;
                if(len > 0) {
                    for(OLSRNode n: val)
                        retval+=n.serializedSize();
                }
            }
        }
        return retval;
    }

    @Override
	public void writeTo(DataOutput out) throws Exception {
        out.writeByte(type);
        writeIpAddress(groupId, out);
        writeIpAddress(srcAddress, out);
        out.writeInt(forwardingTable == null? 0 : forwardingTable.size());
        if(forwardingTable != null) {
            for(Map.Entry<OLSRNode,HashSet<OLSRNode>> entry: forwardingTable.entrySet()) {
                OLSRNode key=entry.getKey();
                HashSet<OLSRNode> val=entry.getValue();
                key.writeTo(out);
                int len=val != null && !val.isEmpty()? val.size() : 0;
                out.writeInt(len);
                if(len > 0) {
                    for(OLSRNode n: val)
                        n.writeTo(out);
                }
            }
        }

    }

    @Override
	public void readFrom(DataInput in) throws Exception {
        type=in.readByte();
        groupId=readIpAddress(in);
        srcAddress=readIpAddress(in);
        int len=in.readInt();
        for(int i=0; i < len; i++) {
            OLSRNode key=new OLSRNode();
            key.readFrom(in);
            HashSet<OLSRNode> vals=new HashSet<>();
            for(int j=0; j < in.readInt(); j++) {
                OLSRNode n=new OLSRNode();
                n.readFrom(in);
                vals.add(n);
            }
            if(!vals.isEmpty())
                forwardingTable.put(key, vals);
        }
    }

    @Override
	public String toString() {
		StringBuffer ret = new StringBuffer();
		ret.append("["
				+ type2Str(type));
		if (groupId!=null){
			ret.append(", mcast=" + groupId);
		}
		if (srcAddress!=null){
			ret.append(", src=" + srcAddress);
		}
		if (type == DATA){
			// TODO show the list of successive destinations
			//ret.append(", something=" + something);
			ret.append("\n");
			for(OLSRNode node:forwardingTable.keySet()){
				Set<OLSRNode> nodeSet = forwardingTable.get(node);
				ret.append("+ VN-Node:"+node+" is responsible for: \n");
				for(OLSRNode nodeInSet:nodeSet){
					ret.append("\t - Node "+nodeInSet+"\n");
					
				}
				
			}
		}
		if (type == CONTROL){
			//ret.append(", something=" + something);
		}
		ret.append(']');
		return ret.toString();
	}


    protected static void writeIpAddress(IpAddress addr, DataOutput out) throws Exception {
	    out.writeByte(addr == null? 0 : 1);
	    if(addr != null)
	        addr.writeTo(out);
    }

    protected static IpAddress readIpAddress(DataInput in) throws Exception {
	    IpAddress retval=null;
	    if(in.readByte() == 1) {
	        retval=new IpAddress();
	        retval.readFrom(in);
        }
        return retval;
    }

    protected static int sizeOf(IpAddress addr) {
	    return Global.BYTE_SIZE + (addr ==null? 0 : addr.serializedSize());
    }

}
