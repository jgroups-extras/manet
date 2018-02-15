package org.jgroups.protocols;

import org.jgroups.Global;
import org.jgroups.Header;
import urv.olsr.data.OLSRNode;

import java.io.*;
import java.net.InetAddress;
import java.util.function.Supplier;

public class OLSRHeader extends Header {

	public static final int CONTROL = 0;
	public static final int DATA    = 1;
	
	public int      type;
	public OLSRNode dest;
	public String   mcastAddress;
	
	public OLSRHeader() {
		
	}

    @Override
	public short getMagicId() {
        return Constants.OLSR_ID;
    }

    /**
	 * @return the dest
	 */
	public OLSRNode getDest() {
		return dest;
	}

	/**
	 * @return Returns the mcastAddress.
	 */
	public String getMcastAddress() {
		return mcastAddress;
	}
	
	/**
	 * @return the type
	 */
	public int getType() {
		return type;
	}


    @Override
	public Supplier<? extends Header> create() {
        return OLSRHeader::new;
    }

    @Override
	public int serializedSize() {
        int retval=Global.BYTE_SIZE; // type;
        if(type == DATA)
            retval+=dest.serializedSize() + Global.INT_SIZE;
        return retval;
    }

	@Override
	public void readFrom(DataInput in) throws Exception {
        type = in.readByte();
        if (type==DATA){
            dest = new OLSRNode();
            dest.readFrom(in);
            byte[] addr = new byte[4];
            for(int i=0;i<4;i++){
                addr[i]=in.readByte();
            }
            mcastAddress = InetAddress.getByAddress(addr).getHostAddress();
		}
	}

    @Override
	public void writeTo(DataOutput out) throws Exception {
        out.writeByte(type);
        if (type==DATA){
            dest.writeTo(out);
            byte[] addr = InetAddress.getByName(mcastAddress).getAddress();
            for(int i=0;i<addr.length;i++){
                out.writeByte(addr[i]);
            }
        }

    }

	
    /**
     * @param dest the dest to set
	 */
	public void setDest(OLSRNode dest) {
		this.dest = dest;
	}

	/**
	 * @param mcastAddress The mcastAddress to set.
	 */
	public void setMcastAddress(String mcastAddress) {
		this.mcastAddress = mcastAddress;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(int type) {
		this.type = type;
	}

	@Override
	public String toString() {
        return "[OLSR: <variables> ]";
    }


}
