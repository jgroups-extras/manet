package urv.emulator.tasks.stats;

import java.io.Serializable;
import java.net.InetAddress;

import org.jgroups.Message;
import org.jgroups.stack.IpAddress;

import urv.conf.PropertiesLoader;

/**
 * @author Marcel Arrufat Arias
 */
public class SequenceNumberMessage extends Message {

	//	CLASS FIELDS --
	
	private int seqNumber;
		
	// CONSTRUCTORS --
	
	public SequenceNumberMessage(int seqNumber, InetAddress dest, Serializable content) {
		super(new IpAddress(dest,PropertiesLoader.getUnicastPort()), content);
		this.seqNumber = seqNumber;		
	}

	//	ACCESS METHODS --
	
	/**
	 * @return Returns the seqNumber.
	 */
	public int getSeqNumber() {
		return seqNumber;
	}
}