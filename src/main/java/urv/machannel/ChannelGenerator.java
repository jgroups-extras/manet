package urv.machannel;

import java.net.InetAddress;
import java.util.Hashtable;

import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.JChannel;

import urv.app.Application;
import urv.conf.ApplicationConfig;
import urv.conf.PropertiesLoader;
import urv.emulator.core.EmulationController;
import urv.olsr.mcast.MulticastAddress;

/**
 * This class is responsible of create MChannel instances.
 * In Emulation mode also generates and registrates some test applications
 * in the same JVM, emulating a network where the peer share the channel
 * and are running the test application.
 *  
 * @author Gerard Paris Aixala
 * @author Raul Gracia Tinedo
 *
 */
public class ChannelGenerator {

	//	CLASS FIELDS --
	
	private boolean emulated = false;
	private String channelName = PropertiesLoader.getChannelId();	
	private Hashtable<Application,Integer> applicationIds = new Hashtable<Application,Integer>();
	
	//	CONSTRUCTORS --
	
	public ChannelGenerator(boolean emulated){
		this.emulated = emulated;
	}
	
	//	PUBLIC METHODS --
	
	/**
	 * This method returns and initiates a MChannel instance. It has a default group name.
	 * 
	 * @param mcastAddr
	 * @param application
	 * @param notif 
	 * @return MChannel
	 */
	public MChannel createMChannel(MulticastAddress mcastAddr, Application application, EmulationController controller) {	
		String props = null;
		InetAddress mcastInetAddr = mcastAddr.toInetAddress();		
		if (emulated){
			// Search the application id
			Integer id = applicationIds.get(application);
			props = ApplicationConfig.getProtocolStackConfig(id.intValue()+1,PropertiesLoader.getUnicastPort(),mcastInetAddr);		
		}else {
			props = ApplicationConfig.getProtocolStackConfig(0,PropertiesLoader.getUnicastPort(),mcastInetAddr);
		}		
		print(props);
		MChannel mChannel = new MChannelImpl(createJChannel(props),mcastAddr,channelName,controller);
		return mChannel;
	}
	/**
	 * This method returns and initiates a MChannel instance when the channel is used in a not emulated environment.
	 * You only need to know the mcastAddress to start up the group.
	 * 
	 * @param mcastAddr
	 * @param application
	 * @param notif 
	 * @return MChannel
	 */
	public MChannel createMChannel(MulticastAddress mcastAddr) {	
		String props = null;
		InetAddress mcastInetAddr = mcastAddr.toInetAddress();		
		props = ApplicationConfig.getProtocolStackConfig(0,PropertiesLoader.getUnicastPort(),mcastInetAddr);
		print(props);
		MChannel mChannel = new MChannelImpl(createJChannel(props),mcastAddr,channelName,null);
		return mChannel;
	}
	/**
	 * This method returns and initiates a MChannel instance when the channel is used in a not emulated environment.
	 * You need to set the mcastAddress (MulticastAddress) and the group ID to start up the group.
	 * 
	 * @param mcastAddr
	 * @param application
	 * @param notif 
	 * @return MChannel
	 */
	public MChannel createMChannel(MulticastAddress mcastAddr, String groupId) {	
		String props = null;
		InetAddress mcastInetAddr = mcastAddr.toInetAddress();		
		props = ApplicationConfig.getProtocolStackConfig(0,PropertiesLoader.getUnicastPort(),mcastInetAddr);
		print(props);
		MChannel mChannel = new MChannelImpl(createJChannel(props),mcastAddr,
				(groupId==null) ? channelName : groupId, null);
		return mChannel;
	}
	/**
	 * This method returns and initiates a MChannel instance when the channel is used in a not emulated environment.
	 * You need to set the mcastAddress (String) and the group ID to start up the group.
	 * 
	 * @param mcastAddr
	 * @param application
	 * @param notif 
	 * @return MChannel
	 */
	public MChannel createMChannel(String mcastAddr, String groupId) {	
		MulticastAddress multicastRealAddress = new MulticastAddress();
		multicastRealAddress.setValue(mcastAddr);
		return createMChannel(multicastRealAddress, groupId);
	}
	/**
	 * Inserts into the applicationIds attribute the new application
	 * instance created 
	 */
	public void registerApplicationId(Application app, Integer id){
		applicationIds.put(app, id);
	}
	
	//	PRIVATE METHODS --
	
	/**
	 * Creates a Channel. If emulated mode, notifies the group membership
	 * notifier that a new node has joined a group
	 * 
	 * @param props
	 * @return Channel
	 */
	private Channel createJChannel(String props) {		
		Channel c = null;
		try{
			c = new JChannel(props);
		    c.setOpt(Channel.AUTO_RECONNECT, Boolean.TRUE);
		    c.connect(channelName);
		} catch (ChannelException e) {
			e.printStackTrace();
		}
		return c;
	}		
	/**
	 * This method is used to print the properties loaded on the channel
	 * 
	 * @param txt
	 */
	private void print(String txt){
		while (true){
			if (txt.length()>80){
				String substring = txt.substring(0, 80);
				System.out.println(substring);
				txt = txt.substring(80, txt.length());
			}else{
				System.out.println(txt);
				break;
			}
		}
	}	
}