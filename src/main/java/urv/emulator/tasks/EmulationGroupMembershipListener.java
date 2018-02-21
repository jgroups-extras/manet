package urv.emulator.tasks;

import org.jgroups.Address;

import urv.machannel.MChannel;

/**
 * @author Marcel Arrufat Arias
 */
public interface EmulationGroupMembershipListener {
	
	void onGroupCreated(Address multicastAddress, Address localAddress, MChannel mChannel);

}