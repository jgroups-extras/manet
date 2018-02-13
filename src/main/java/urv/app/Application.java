package urv.app;

import urv.emulator.core.EmulationController;
import urv.machannel.MChannel;
import urv.olsr.mcast.MulticastAddress;

/**
 * @author Gerard Paris Aixala
 *
 */
public abstract class Application {

	//	CLASS FIELDS --
	
	private AppLauncher appLauncher = null; 
	
	//	PUBLIC METHODS --
	
	public void registerAppLauncher(AppLauncher appLauncher){
		this.appLauncher = appLauncher;
	}
	
	//	ABSTRACT METHODS --
	
	public abstract void start();

	//	PROTECTED METHODS --
	
	protected MChannel createMChannel(MulticastAddress mcastAddr){		
		EmulationController controller = appLauncher.getEmulationController();		
		MChannel mChannel = appLauncher.getChannelGenerator().createMChannel(mcastAddr,this,controller);
		return mChannel;			
	}
}