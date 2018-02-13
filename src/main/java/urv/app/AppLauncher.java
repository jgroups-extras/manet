package urv.app;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import urv.conf.PropertiesLoader;
import urv.emulator.VirtualNetworkInformation;
import urv.emulator.core.EmulationController;
import urv.emulator.tasks.EmulatorTask;
import urv.emulator.tasks.MessageNotifier;
import urv.log.CommunicationLog;
import urv.log.Log;
import urv.machannel.ChannelGenerator;
import urv.olsr.data.mpr.MprSet;
import urv.olsr.data.neighbour.NeighborTable;
import urv.olsr.data.routing.RoutingTable;
import urv.olsr.data.topology.TopologyInformationBaseTable;

/**
 * This class is responsible of run the application defined in the 
 * omolsr.properties file as APPLICATION property. This execution could be real
 * execution or emulated.
 * 
 * @author Gerard Paris Aixala
 * @author Marcel Arrufat Arias *
 */
public class AppLauncher {

	//	CLASS FIELDS --
	
	private final static boolean emulated = PropertiesLoader.isEmulated();
	private ChannelGenerator channelGenerator = null;
	private EmulationController emulationController = null;

	//	CONSTRUCTORS --
	
	public AppLauncher(){
		Log.getInstance().setCurrentLevel(Log.INFO);
		activateLog4J();
		registerDumpingClasses();
		channelGenerator = new ChannelGenerator(emulated);
		emulationController = new EmulationController();
		if (emulated) {
			//Start vni, in case it is not started
			emulationController.getVirtualNetworkInformation();
			// Starting emulation tasks
			startEmulationTasks();
			// Start emulated applications
			startEmulation();
		} else {
			if (PropertiesLoader.isCommunicationLog()){
				MessageNotifier messageNotifier = emulationController.getMessageNotifier();
				messageNotifier.addMessageListener( new CommunicationLog() );
			}
			startRealApplication();
		}
	}

	//	MAIN --
	
	public static void main(String[] args) {
		try {
			new AppLauncher();
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	//	ACCESS METHODS --
	
	/**
	 * @return the channelGenerator
	 */
	public ChannelGenerator getChannelGenerator() {
		return channelGenerator;
	}

	public EmulationController getEmulationController() {
		return emulationController;
	}
	
	//	PRIVATE METHODS --
	
	private void activateLog4J() {
		String log4jPropertiesFile = "./conf/log4j.properties";
		File f = new File(log4jPropertiesFile);
		if (f.exists()){
			PropertyConfigurator.configure(log4jPropertiesFile);
		} else {
			BasicConfigurator.configure();
		}
	}
	private Application newApplicationInstance(){
		Application app = null;
		try {
			String applicationClass = PropertiesLoader.getApplication();
			Object newObj = Class.forName(applicationClass).newInstance();
			if (newObj instanceof Application){
				app = (Application)newObj;
			}
			app.registerAppLauncher(this);
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return app;
	}
	private void registerDumpingClasses() {
		Log log = Log.getInstance();

		log.registerDumpingClass(NeighborTable.class.getName());
		log.registerDumpingClass(RoutingTable.class.getName());
		log.registerDumpingClass(TopologyInformationBaseTable.class.getName());
		log.registerDumpingClass(MprSet.class.getName());
	}
	private void startEmulation() {
		VirtualNetworkInformation vni = emulationController.getVirtualNetworkInformation();
		int networkSize = vni.getNetworkSize();
		for (int i=0;i<networkSize;i++){
			final Application app = newApplicationInstance();
			channelGenerator.registerApplicationId(app, new Integer(i));
			//Launch each application in a new Thread
			new Thread(){
				public void run(){
					app.start();
				}
			}.start();
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//Wait for the application to initialize (not necessary, just to clarify output on console)
		}
	}	
	private void startEmulationTasks(){
		String[] tasks = PropertiesLoader.getEmulationTasks();
		for (int i=0;i<tasks.length;i++){
			try {
				Class taskClass = Class.forName(tasks[i]);
				Object taskObj = taskClass.newInstance();
				if (taskObj instanceof EmulatorTask){
					((EmulatorTask)taskObj).setEmulationController(emulationController);
					((EmulatorTask)taskObj).start();
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}

		}
	}
	private void startRealApplication() {
		Application app = newApplicationInstance();
		app.start();
	}
}