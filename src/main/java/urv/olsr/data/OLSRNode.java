package urv.olsr.data;

import java.io.DataInput;
import java.io.DataOutput;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.jgroups.Address;
import org.jgroups.Global;
import org.jgroups.stack.IpAddress;
import org.jgroups.util.SizeStreamable;

import urv.conf.PropertiesLoader;

/**
 * Object that represents a node in a network based in OLSR protocol.
 * OLSR does not make any assumption about node addresses, other than
 * that each node is assumed to have a unique IP address.
 * 
 * @author Marcel Arrufat Arias
 * @author Raul Gracia Tinedo
 */
public class OLSRNode implements SizeStreamable {

	//	CLASS FIELDS --
	
	private Address address;
	//Bandwidth information about each node
	private float bandwithCoefficient;
	private long bwBytesCapacity;
	private long bwMessagesCapacity;
	
	//	CONSTRUCTORS --
	
	public OLSRNode() {}

    //	ACCESS METHODS --

    /**
     * @return Returns the address.
     */
    public Address getAddress() {
        return address;
    }

    public Address getJGroupsAddress(){
        try {
			return new IpAddress(address.toString(), PropertiesLoader.getUnicastPort());
		} catch (UnknownHostException e) {
			// FIXME
			e.printStackTrace();
			return null;
		}
    }

    public synchronized float getBandwithCoefficient() {
        return bandwithCoefficient;
    }
    public synchronized long getBwBytesCapacity() {
        return bwBytesCapacity;
    }
    public synchronized long getBwMessagesCapacity() {
        return bwMessagesCapacity;
    }
    public synchronized OLSRNode setBandwithCoefficient(float bandwithCoefficient) {
        this.bandwithCoefficient = bandwithCoefficient;
        return this;
    }
    public synchronized OLSRNode setBwBytesCapacity(long bwBytesCapacity) {
        this.bwBytesCapacity = bwBytesCapacity;
        return this;
    }
    public synchronized OLSRNode setBwMessagesCapacity(long bwMessagesCapacity) {
        this.bwMessagesCapacity = bwMessagesCapacity;
        return this;
    }
    public OLSRNode setValue(Address address2) {
        this.address = address2;
        return this;
    }


    public void updateBandwidth (OLSRNode updatedNode){
        this.bandwithCoefficient = updatedNode.getBandwithCoefficient();
        this.bwBytesCapacity = updatedNode.getBwBytesCapacity();
        this.bwMessagesCapacity = updatedNode.getBwMessagesCapacity();
    }


    public OLSRNode copy(){
        OLSRNode node = new OLSRNode();
        node.setValue(this.address);
        node.setBandwithCoefficient(this.bandwithCoefficient);
        node.setBwBytesCapacity(this.bwBytesCapacity);
        node.setBwMessagesCapacity(this.bwMessagesCapacity);
        return node;
    }
	@Override
	public boolean equals(Object obj){
		OLSRNode node = (OLSRNode)obj;
		return address.equals(node.address);
	}


	@Override
	public int hashCode(){
		return address.hashCode();
	}

    @Override
	public int serializedSize() {
    	return address.serializedSize() + Global.FLOAT_SIZE + Global.LONG_SIZE * 2;
    }

    @Override
	public void readFrom(DataInput in) throws Exception {
        IpAddress ipAddress = new IpAddress();
        ipAddress.readFrom(in); // Length byte followed by 4 (IPv4) or 6 (IPv6) data bytes
        this.address = ipAddress;
        byte[] a = new byte[4];
        in.readFully(a, 0, 4);	//read the bandwidth coefficient, 4 bytes
        this.bandwithCoefficient = ByteBuffer.wrap(a).getFloat();
        a = new byte[8];
        in.readFully(a, 0, 8);	//read the bytes capacity, 8 bytes
        this.bwBytesCapacity = ByteBuffer.wrap(a).getLong();
        in.readFully(a, 0, 8);	//read the messages capacity, 8 bytes
        this.bwMessagesCapacity = ByteBuffer.wrap(a).getLong();
	}

    @Override
	public void writeTo(DataOutput out) throws Exception {
    	address.writeTo(out);
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byte[] b = byteBuffer.putFloat(bandwithCoefficient).array();
        out.write(b);
        byteBuffer = ByteBuffer.allocate(8);
        b = byteBuffer.putLong(bwBytesCapacity).array();
        out.write(b);
        byteBuffer = ByteBuffer.allocate(8);
        b = byteBuffer.putLong(bwMessagesCapacity).array();
        out.write(b);
    }

    @Override
	public String toString(){
        return String.format("%s bw_bytes: %d bw_messages: %d bw_coefficient: %.2f",
                             address.toString(), bwBytesCapacity, bwMessagesCapacity, bandwithCoefficient);
    }


}