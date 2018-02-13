package urv.emulator.tasks.stats;

import java.io.Serializable;

/**
 * @author Marcel Arrufat Arias
 */
public class SequenceNumberMessageWrapper implements Serializable{

	//	CLASS FIELDS --
	
	private int seqNumber;
	private Serializable content;
		
	//	CONSTRUCTORS --
	
	public SequenceNumberMessageWrapper(int seqNumber, Serializable content) {
		//super(new IpAddress(dest,ApplicationConfig.UNICAST_PORT), null,content);
		this.content = content;
		this.seqNumber = seqNumber;		
	}

	//	OVERRIDDEN METHODS --
	
	public String toString(){
		return "SeqNum:"+seqNumber+";content:"+content.toString();
	}
	
	//	ACCESS METHODS --
	
	/**
	 * @return Returns the content.
	 */
	public Serializable getContent() {
		return content;
	}
	/**
	 * @return Returns the seqNumber.
	 */
	public int getSeqNumber() {
		return seqNumber;
	}
}