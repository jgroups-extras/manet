package urv.emulator.tasks;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

import urv.machannel.MChannel;


/**
 * @author Marcel Arrufat Arias
 */
public class GroupMembershipNotifier {

	List<EmulationGroupMembershipListener> listenerList = new LinkedList<EmulationGroupMembershipListener>();
	
	public GroupMembershipNotifier() {}

	public synchronized void addGroupMembershipListener(EmulationGroupMembershipListener listener){	
		listenerList.add(listener);
	}
	
	/**
	 * Notify that a new group has been joined by the localNode
	 * @param multicastAddress
	 * @param localAddress
	 * @param channel
	 */
	public synchronized void newGroupJoined(InetAddress multicastAddress, InetAddress localAddress,MChannel mChannel){
		for (EmulationGroupMembershipListener listener:listenerList){
			listener.onGroupCreated(multicastAddress,localAddress,mChannel);
		}
	}
}