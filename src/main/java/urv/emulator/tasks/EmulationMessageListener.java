package urv.emulator.tasks;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.View;

/**
 * This listener is used to check whether all the messages sent into an emulation are properly
 * received by end-to-end application, and thus verify the correct behaviour of all layers
 * 
 * @author Marcel Arrufat Arias
 */
public interface EmulationMessageListener {

	public void onMessageReceived(Message msg, Address src, Address mainDst, Address realDst, int seqNumber);
	
	public void onMessageSent(Message msg, Address src, Address dst, int seqNumber,View view);
	
}