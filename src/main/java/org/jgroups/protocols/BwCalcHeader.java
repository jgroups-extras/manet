package org.jgroups.protocols;

import org.jgroups.Global;
import org.jgroups.Header;

import java.io.*;
import java.util.function.Supplier;

/**
 * Bandwidth Calculator protocol Header.
 * The communication interface between the protocol and the other layers.
 * 
 * @author Marc Espelt
 */
public class BwCalcHeader extends Header {

	//	CONSTANTS --
	public static final int MAX_INCOMING_BANDWIDTH = 1;
	public static final int MAX_INCOMING_MESSAGES  = 2;
	
	//	CLASS FIELDS --
	
	private int type = MAX_INCOMING_BANDWIDTH | MAX_INCOMING_MESSAGES;
	
	//	CONSTRUCTORS --
	
	public BwCalcHeader(){
		super();
		this.type = MAX_INCOMING_BANDWIDTH | MAX_INCOMING_MESSAGES;
	}

    public short                      getMagicId()                             {return Constants.BW_CALC_ID;}
    public int                        getType()                                {return type;}
    public void                       setType(int type)                        {this.type = type;}
    public Supplier<? extends Header> create()                                 {return BwCalcHeader::new;}
    public int                        serializedSize()                         {return Global.BYTE_SIZE;}
    public void                       writeTo(DataOutput out) throws Exception {out.writeByte(type);}
    public void                       readFrom(DataInput in) throws Exception  {type=in.readByte();}


    public String toString() {return String.format("%s", getClass().getSimpleName());}

}