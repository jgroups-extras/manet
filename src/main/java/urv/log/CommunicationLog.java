package urv.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;

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
		super();
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
	
	public void onMessageReceived(Message msg, InetAddress src, InetAddress mainDst, InetAddress realDst, int seqNumber) {
		StringBuffer out = new StringBuffer();
		out.append(getElapsedMsecs()+" ");
		out.append("RCVD ");
		out.append(src.getHostAddress()+" > ");
		out.append(mainDst.getHostAddress()+ " ");
		out.append("at " + realDst.getHostAddress()+ " ");
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
	public void onMessageSent(Message msg, InetAddress src, InetAddress dst, int seqNumber, View view) {
		StringBuffer out = new StringBuffer();
		out.append(getElapsedMsecs()+" ");
		out.append("SENT ");
		out.append(src.getHostAddress()+" > ");
		out.append(dst.getHostAddress()+ " ");
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