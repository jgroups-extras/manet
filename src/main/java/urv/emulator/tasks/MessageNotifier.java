package urv.emulator.tasks;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

import org.jgroups.Message;
import org.jgroups.View;

/**
 * This class registers listeners that will receive all the messages
 * sent by MChannels 
 * 
 * @author Marcel Arrufat Arias
 */
public class MessageNotifier {
	
	//	CLASS FIELDS --

	private List<EmulationMessageListener> listenerList = new LinkedList<EmulationMessageListener>();
	
	//	CONSTRUCTORS --
	
	public MessageNotifier() {
		super();
	}
	
	//	PUBLIC METHODS --

	public synchronized void addMessageListener(EmulationMessageListener listener){	
		listenerList.add(listener);
	}
	public synchronized void newMessageReceived(Message msg, InetAddress src, InetAddress mainDst, InetAddress realDst, int seqNumber){
		for (EmulationMessageListener listener:listenerList){
			listener.onMessageReceived(msg,src,mainDst,realDst,seqNumber);
		}
	}
	public synchronized void newMessageSent(Message msg, InetAddress src, InetAddress dst, int seqNumber, View view){
		for (EmulationMessageListener listener:listenerList){
			listener.onMessageSent(msg,src,dst,seqNumber,view);
		}
	}
}