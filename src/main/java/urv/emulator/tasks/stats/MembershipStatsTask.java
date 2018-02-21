package urv.emulator.tasks.stats;

import java.util.HashSet;
import java.util.Set;

import org.jgroups.Address;
import org.jgroups.View;

import urv.conf.PropertiesLoader;
import urv.emulator.core.EmulationController;
import urv.emulator.tasks.EmulationGroupMembershipListener;
import urv.emulator.tasks.EmulatorTask;
import urv.emulator.tasks.GroupMembershipNotifier;
import urv.machannel.MChannel;
import urv.util.graph.HashMapSet;


/**
 * This task gathers information about the groups created in the applications
 * and the nodes that joined these groups
 * This information is checked with the view of each MChannel, in order to 
 * verify the correct behaviour of getView() method in the channel 
 * 
 * @author Marcel Arrufat Arias
 */
public class MembershipStatsTask extends EmulatorTask implements EmulationGroupMembershipListener{

	//	CLASS FIELDS --
	
	private HashMapSet<Address,Address> registeredMembership = new HashMapSet<>();
	//Keeps all the created channels created in a group (with the same multicast address) so we can check
	//the view against the one in registeredMembership
	private HashMapSet<Address,MChannel> channelMembership = new HashMapSet<>();	
	private Object lock = new Object();
	
	//	CONSTRUCTORS --
	
	/**
	 * @param emulationController
	 */
	public MembershipStatsTask() {
		super();
		
	}

	//	OVERRIDDEN METHODS --

	/**
	 * Add the code that should be launched in the run method
	 */
	@Override
	public void doSomething() {
		
		//In first place, register the class as listener in order to intercept
		//network messages
		EmulationController controller = super.getEmulationController();
		GroupMembershipNotifier membershipNotifier = controller.getGroupMembershipNotifier();
		membershipNotifier.addGroupMembershipListener(this);		

		while (true){
			//Rest for a little while
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (PropertiesLoader.isReliabilityEnabled()){
				//Check all send messages have been received correctly
				checkMembership();
			}
		}
	}
	
	@Override
	public void onGroupCreated(Address multicastAddress, Address localAddress, MChannel mChannel) {
		System.out.println("*** Group for channel "+multicastAddress+" on "+mChannel.getLocalAddress()+" has been created");
		synchronized (lock) {
			//Store info
			registeredMembership.addToSet(multicastAddress,localAddress);
			channelMembership.addToSet(multicastAddress,mChannel);
		}		
	}
	
	//	PRIVATE METHODS --
	
	/**
	 * For each node that joined a group, check its current view and verify that view
	 * information matches joined groups list
	 *
	 */
	private void checkMembership() {
		synchronized (lock) {
			Set<Address> mcastAddresses = registeredMembership.keySet();			
			String output ="";
			//Get all groups
			for (Address mcastAddr:mcastAddresses){
				//For each group, get all the nodes that have registered a channel
				HashSet<Address> nodeList = registeredMembership.get(mcastAddr);
				String str = ("Registered view for all nodes should be ("+nodeList.size()+")");
				for(Address node:nodeList){
					str += " "+node;
				}				
				//And all the channels registered, in order to get the view
				HashSet<MChannel> channelList = channelMembership.get(mcastAddr);				
				//For each channel, check that all registered nodes are in the channel view
				str +="\n";
				int wrongMembership=0;
				for(MChannel nodeMChannel:channelList){
					View view = nodeMChannel.getView();					
					//If the size is not the same, we do not have the same view
					//Now we are assuming that if sizes are equal,  the addresses are the same
					if (view.size()!=nodeList.size()) wrongMembership++;
					
					str += ("View node "+nodeMChannel.getLocalAddress()+ " is ("+view.size()+") "+view+"\n");
				}
				int registeredMembers=nodeList.size();
				int rightMbshp=registeredMembers-wrongMembership;
				float rightPerCent = 100*(float)(rightMbshp)/registeredMembers;
				output="MC@:"+mcastAddr+" MBSHP ok="+rightMbshp+"("+rightPerCent+"%)";				
				print(str,true);
				print(output,true);
				//Now, all the channels in the list should have the same view, which should be a 
				//list of the same nodes that are found in the nodeList				
			}
		}
	}
}