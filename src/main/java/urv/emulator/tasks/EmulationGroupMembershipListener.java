package urv.emulator.tasks;

import urv.machannel.MChannel;

import java.net.InetAddress;

/**
 * @author Marcel Arrufat Arias
 */
public interface EmulationGroupMembershipListener {
	
	void onGroupCreated(InetAddress multicastAddress, InetAddress localAddress, MChannel mChannel);

}