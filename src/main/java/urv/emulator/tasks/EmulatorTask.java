package urv.emulator.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import urv.emulator.core.EmulationController;
import urv.util.date.DateUtils;

/**
 * Each class that wants to be run in the simulator must extends this class
 * 
 * @author Marcel Arrufat Arias
 */
public abstract class EmulatorTask extends Thread  {

	//	CLASS FIELDS --
	
	private static BufferedWriter fTasks;
	private EmulationController emulationController;
	private String className;
	private BufferedWriter f;
	private long initialTime;

	//	CONSTRUCTORS --
	
	public EmulatorTask() {
		super();
		String[] fullname = this.getClass().getName().split("\\.");
		className = fullname[fullname.length-1];
		try {
			String dir = "tasksResults" + File.separator;
			File baseDir = new File(dir);
			//Create log directory
			if (!baseDir.exists()) baseDir.mkdir();
			String dateStr=DateUtils.getTimeFormatString();
			f = new BufferedWriter(new FileWriter(new File(dir+dateStr+" "+className+".txt")));
			fTasks = new BufferedWriter(new FileWriter(new File(dir+dateStr+" "+"AllTasks.txt")));
			initialTime = System.currentTimeMillis();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//	ABSTRACT METHODS --
	
	public abstract void doSomething();

	//	OVERRIDDEN METHODS --
	
	@Override
	public void run(){
		doSomething();
	}
	
	//	PUBLIC METHODS --
	
	public void print(String str,boolean outputToFile){
		String out = ("["+getClassName()+":"+getElapsedMsecs()+"]\n"+str+"\n\n");
		if (outputToFile){
			try {
				//Write to the task file
				f.write(out);
				f.flush();
				//Write to the common file
				synchronized (fTasks){
					fTasks.write(out);
					fTasks.flush();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println(out);
	}
	
	//	ACCESS METHODS --
	
	/**
	 * @return Returns the className.
	 */
	public String getClassName() {
		return className;
	}
	/**
	 * @return Returns the emulationController.
	 */
	public EmulationController getEmulationController() {
		return emulationController;
	}
	public void setEmulationController(EmulationController emulationController){
		if (this.emulationController==null)
			this.emulationController = emulationController;
	}

	//	PRIVATE METHODS --
	
	private long getElapsedMsecs(){
		return System.currentTimeMillis()-initialTime;
	}
}