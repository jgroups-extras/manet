package urv.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.View;

import urv.emulator.tasks.EmulationMessageListener;
import urv.util.date.DateUtils;

/**
 * @author Gerard Paris Aixala
 *
 */
public class CommunicationLog implements EmulationMessageListener{

	//	CLASS FIELDS --
	
	private BufferedWriter f;
	private long initialTime;

	//	CONSTRUCTORS --
	
	public CommunicationLog(){
		try {
			String dateStr=DateUtils.getTimeFormatString();
			String dir = "runResults" + File.separator;
			File baseDir = new File(dir);
			//Create log directory
			if (!baseDir.exists())
				baseDir.mkdir();

			f = new BufferedWriter(new FileWriter(new File(dir+dateStr+"_comm.log")));
			initialTime = System.currentTimeMillis();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//	OVERRIDDEN METHODS --
	
	@Override
	public void onMessageReceived(Message msg, Address src, Address mainDst, Address realDst, int seqNumber) {
		StringBuffer out = new StringBuffer();
		out.append(getElapsedMsecs()+" ");
		out.append("RCVD ");
		out.append(src + " > ");
		out.append(mainDst + " ");
		out.append("at " + realDst + " ");
		out.append("#"+seqNumber);
		out.append("\n");
		try {
			//Write to the task file
			f.write(out.toString());
			f.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onMessageSent(Message msg, Address src, Address dst, int seqNumber, View view) {
		StringBuffer out = new StringBuffer();
		out.append(getElapsedMsecs()+" ");
		out.append("SENT ");
		out.append(src.toString() + " > ");
		out.append(dst.toString() + " ");
		out.append("#"+seqNumber);
		out.append("\n");
		try {
			//Write to the task file
			f.write(out.toString());
			f.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//	PRIVATE METHODS --
	
	private long getElapsedMsecs(){
		return System.currentTimeMillis()-initialTime;
	}
}