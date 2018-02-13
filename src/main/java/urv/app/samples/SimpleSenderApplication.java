package urv.app.samples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.jgroups.Address;
import org.jgroups.MembershipListener;
import org.jgroups.Message;
import org.jgroups.MessageListener;
import org.jgroups.View;
import org.jgroups.blocks.PullPushAdapter;
import org.jgroups.stack.IpAddress;

import urv.app.Application;
import urv.machannel.MChannel;
import urv.olsr.mcast.MulticastAddress;
import urv.util.network.NetworkUtils;

/**
 * @author Gerard Paris Aixala
 * @author Marcel Arrufat Arias */
public class SimpleSenderApplication extends Application implements MessageListener,MembershipListener{

	//	CLASS FIELDS --
	
	private static final int NUM_MESSAGES = 10;
	private Set<Address> receivedMessages = new HashSet<Address>();
	private BufferedWriter f;
	private String name = "SSA";
	private MulticastAddress mcastAddr = null;
	private MChannel mChannel = null;

	//	CONSTRUCTORS --
	
	public SimpleSenderApplication(){}

	//	OVERRIDDEN METHODS --
	
	public void block() {}

	public byte[] getState() {
		return null;
	}
	public void receive(Message msg) {
		receivedMessages.add(msg.getSrc());
		try {
			f.write("RECEIVED: " + msg.getObject().toString()+"\n");
			System.out.println("["+((IpAddress)mChannel.getLocalAddress()).getIpAddress().getHostName()+
				"] RECEIVED: " + msg.getObject().toString()+"\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setState(byte[] state) {}
	
	@Override
	public void start() {
		createMChannel();
		waitUntilInformationIsSpread();
		sendMulticastMessages();
		//Block forever to avoid finishing the application
		while(true){
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void suspect(Address suspected_mbr) {}
	
	public void viewAccepted(View new_view) {
		System.out.println("---------> New view for node "+
			mChannel.getLocalAddress()+":"+new_view);
	}
	
	//	PRIVATE METHODS --
	
	private void createMChannel(){
		mcastAddr = new MulticastAddress();
		mcastAddr.setValue("224.0.0.10");
		mChannel = super.createMChannel(mcastAddr);
		mChannel.registerListener(name,this);
		((PullPushAdapter)mChannel).addMembershipListener(this);
		try {
			f = new BufferedWriter(new FileWriter(new File(((IpAddress)mChannel.getLocalAddress()).getIpAddress().getHostName()+".txt")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void sendMulticastMessages(){
		Random r = new Random(System.currentTimeMillis());
		int i=0;
		while (i<NUM_MESSAGES){
			mChannel.send(NetworkUtils.getJGroupsAddresFor(mcastAddr.toInetAddress()),
					mChannel.getLocalAddress(), "MSG "+i
				+" from "+mChannel.getLocalAddress()+" to "+mcastAddr);
			String str=("Nodes for "+mChannel.getLocalAddress() +" = ("+mChannel.getInetAddressesOfGroupMebers().size()+") ");
			for(InetAddress addr:mChannel.getInetAddressesOfGroupMebers()){
				str+=(addr+":");
			}
			System.out.println("-- " + str);
			try {
				Thread.sleep(2500+r.nextInt(10000));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			i++;
		}
		//Wait for the other and receive all messages
		while(true){
			try {
				f.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	private void waitUntilInformationIsSpread() {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}