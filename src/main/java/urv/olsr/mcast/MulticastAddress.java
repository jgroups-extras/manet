package urv.olsr.mcast;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.jgroups.Address;
import org.jgroups.stack.IpAddress;

/**
 * Class that represents a multicast address of a group
 * 
 * @author Gerard Paris Aixala
 *
 */
public class MulticastAddress extends IpAddress implements Externalizable{

	//	CLASS FIELDS --

	//	CONSTRUCTORS --
	
	public MulticastAddress(){}

	//	OVERRIDDEN METHODS --
	
	@Override
	public Object clone(){
		MulticastAddress newAddr = new MulticastAddress();
		newAddr.setValue(this.ip_addr);
		return newAddr;
	}
	
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		byte[] b = new byte[4];
		in.read(b, 0, 4);
		this.ip_addr=InetAddress.getByAddress(b);
	}
	
	@Override
	public void readFrom(DataInput in) throws IOException, IllegalAccessException, InstantiationException {
		 byte[] a = new byte[4]; // 4 bytes (IPv4)
	     in.readFully(a);
	     this.ip_addr=InetAddress.getByAddress(a);
	}
	
	public InetAddress toInetAddress(){
		return ip_addr;
	}
	
	@Override
	public String toString(){
		return ip_addr.toString();
	}
	
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.write(ip_addr.getAddress());
	}
	
	@Override
	public void writeTo(DataOutput out) throws IOException {
		byte[] a = ip_addr.getAddress();  // 4 bytes (IPv4)
        out.write(a, 0, a.length);
	}
	
	//	ACCESS METHODS --
	
	public void setValue(Address multicastAddress){
		this.ip_addr = ((IpAddress)multicastAddress).getIpAddress();
	}
	
	public InetAddress getMcastAddress() {
		return ip_addr;
	}
	
	public void setValue(InetAddress multicastAddress){
		this.ip_addr = multicastAddress;
	}
	
	public void setValue(String multicastAddress){
		try {
			this.ip_addr = InetAddress.getByName(multicastAddress);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
}