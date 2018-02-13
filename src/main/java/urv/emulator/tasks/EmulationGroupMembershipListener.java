package urv.emulator.tasks;

import java.net.InetAddress;

import urv.machannel.MChannel;

/**
 * @author Marcel Arrufat Arias
 */
public interface EmulationGroupMembershipListener {
	
	public void onGroupCreated(InetAddress multicastAddress,InetAddress localAddress, MChannel mChannel);

}