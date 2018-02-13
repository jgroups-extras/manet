package urv.emulator.core;

import urv.conf.PropertiesLoader;
import urv.emulator.VirtualNetworkInformation;
import urv.emulator.tasks.GroupMembershipNotifier;
import urv.emulator.tasks.MessageNotifier;

/**
 * @author Marcel Arrufat Arias
 */
public class EmulationController {

	//	CLASS FIELDS --
	
	private VirtualNetworkInformation virtualNetworkInformation = null;
	private MessageNotifier messageNotifier;
	private GroupMembershipNotifier groupMembershipNotifier;
	
	//	CONSTRUCTORS --
	
	public EmulationController() {
		if (PropertiesLoader.isEmulated()){
			virtualNetworkInformation = VirtualNetworkInformation.getInstance();
		}				
		this.messageNotifier = new MessageNotifier();
		this.groupMembershipNotifier = new GroupMembershipNotifier();
	}
	
	//	ACCESS METHODS --
	
	/**
	 * @return Returns the groupMembershipNotifier.
	 */
	public GroupMembershipNotifier getGroupMembershipNotifier() {
		return groupMembershipNotifier;
	}
	/**
	 * @return Returns the messageNotifier.
	 */
	public MessageNotifier getMessageNotifier() {
		return messageNotifier;
	}
	/**
	 * @return Returns the virtualNetworkInformation.
	 */
	public VirtualNetworkInformation getVirtualNetworkInformation() {
		return virtualNetworkInformation;
	}
}