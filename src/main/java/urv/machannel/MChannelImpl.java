package urv.machannel;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelClosedException;
import org.jgroups.ChannelNotConnectedException;
import org.jgroups.Message;
import org.jgroups.MessageListener;
import org.jgroups.Transport;
import org.jgroups.View;
import org.jgroups.blocks.PullPushAdapter;
import org.jgroups.stack.IpAddress;
import org.jgroups.util.Util;

import urv.conf.PropertiesLoader;
import urv.emulator.core.EmulationController;
import urv.emulator.tasks.GroupMembershipNotifier;
import urv.emulator.tasks.MessageNotifier;
import urv.emulator.tasks.stats.SequenceNumberMessageWrapper;
import urv.olsr.data.OLSRNode;
import urv.olsr.mcast.MulticastAddress;
import urv.olsr.mcast.TopologyEvent;
import urv.util.graph.NetworkGraph;
import urv.util.graph.Weight;

/**
 * This class provides an implementation of the MChannel interface
 * in order to group communication with topology awareness.
 * 
 * @author Marcel Arrufat
 * @author Gerard París
 * @author Raúl Gracia
 * 
 * @version $Revision

 */
public class MChannelImpl extends PullPushAdapter implements MChannel {
		
	//	CLASS FIELDS --

	//Multicast address for this MChannel instance
	private MulticastAddress mcastAddr;
	//Channel Name
	private String channelId;
	//Graph that represents the underlying topology
	private NetworkGraph<OLSRNode,Weight> graph = new NetworkGraph<OLSRNode, Weight>();
    private View view;
	private EmulationController controller;
	private MessageNotifier notifier;	
	private GroupMembershipNotifier groupMembershipInformation;
	private int seqNumber=0;
	
    //	CONSTRUCTORS --
		
	public MChannelImpl(Transport channel, MulticastAddress mcastAddr, String channelName, EmulationController controller){
		//In the super constructor is started the channel, because is called the start() method
		super(channel);
		this.channelId = channelName;
		this.mcastAddr = mcastAddr;
    	if (PropertiesLoader.isEmulated()){
    		this.controller = controller;
    		this.notifier = this.controller.getMessageNotifier();
    		this.groupMembershipInformation = this.controller.getGroupMembershipNotifier();
	    	groupMembershipInformation.newGroupJoined(
	    		mcastAddr.toInetAddress(),getInetAddress(((Channel) transport).getLocalAddress()),this);
	    }
	}

	//	OVERRIDDEN METHODS --	
	
	@Override
	public Address getLocalAddress(){
		return ((Channel) transport).getLocalAddress();
	}
	@Override
	public NetworkGraph<OLSRNode,Weight> getNetworkGraph(){
		return graph;
	}
	@Override
	public synchronized View getView() {
		return (view == null) ? null : (View) view.clone();
	}
	@Override
	public String getChannelName() {
		return channelId;
	}
	
    /**
     * Reentrant run(): message reception is serialized, then the listener is notified of the
     * message reception
     */
	@Override
    public void run() {
        Object obj;
        while(receiver_thread != null && Thread.currentThread().equals(receiver_thread)) {
            try {
                obj=transport.receive(0);
                if(obj == null)
                    continue;
                //If we receive information about the current topology
                //store this info
                if(obj instanceof Message) {
                	//Change, we intercept seq Numbers
                    super.handleMessage(getReceivedMessage((Message)obj));
	            } else if(obj instanceof View) {
                    notifyViewChange((View)obj);
                }
            }catch(ChannelNotConnectedException conn) {
                Address local_addr=((Channel)transport).getLocalAddress();
                if(log.isTraceEnabled()) log.trace('[' + (local_addr == null ? "<null>" : local_addr.toString()) +
                        "] channel not connected, exception is " + conn);
                Util.sleep(1000);
                receiver_thread=null;
                break;
            }catch(ChannelClosedException closed_ex) {
                Address local_addr=((Channel)transport).getLocalAddress();
                if(log.isTraceEnabled()) log.trace('[' + (local_addr == null ? "<null>" : local_addr.toString()) +
                        "] channel closed, exception is " + closed_ex);
                receiver_thread=null;
                break;
            }
            catch(Throwable e) {}
        }
    }	
	
    /* **********************************************
     * 				 MESSAGE DELIVERY
     * ********************************************/
	
	/**
	 * Sends a message to all peers in a group
	 */
	@Override
	public void send(Message msg) {
		try {
			send(channelId,msg);
		} catch (Exception e) {
			System.err.println("Could not send message "+msg);
			e.printStackTrace();
		}
	}
    /**
     * Sends a message to a selected peer
     */
	@Override
	public void send(Address dst, Address src, Serializable content) {
		Message msg=createMessage(dst,content);
		try {
			send(channelId,msg);
		} catch (Exception e) {
			System.err.println("Could not send message "+msg);
			e.printStackTrace();
		}
	}	
	/**
	 * Sends a message to all the neighbors of the localNode in this group
	 */
	@Override
	public void sendToNeighbors(Serializable content) {
		//If we have neighbors, send the message to them
		if (graph!=null){
			OLSRNode localNode = new OLSRNode();
			localNode.setValue(getInetAddress(getLocalAddress()));
			for (OLSRNode node : graph.getNeighbours(localNode)){
				try {
					send(createMessage(node.getJGroupsAddress(),content));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}		
		}
	}
	@Override
	public synchronized List<InetAddress> getInetAddressesOfGroupMebers () {
		List<InetAddress> addresses = new ArrayList<InetAddress>();
		if (getView()==null) return addresses;
		for (Address jGroupsAddress : getView().getMembers()){
			addresses.add(getInetAddress(jGroupsAddress));
		}
		return addresses;
	}
	@Override
	public void close() {
        super.stop();		
		((Channel)transport).close();
	}
	@Override
	protected void notifyViewChange(View topologyEvent) {
		this.view = (View)((View)topologyEvent).clone();
		super.notifyViewChange(view);
		System.out.println("New membership received!!! ["+ view.size() + "]" );		
		// The obtained graph is only a view of the network that the local node has.
		// Moreover, the graph only includes the members of the multicast group
		if (topologyEvent instanceof TopologyEvent){
			graph = ((TopologyEvent)topologyEvent).getOMOLSRNetworkGraph().getNetworkGraphCopy();
		}
	}
	@Override
	public void registerListener(Serializable identifier, MessageListener l) {
		super.registerListener(identifier, l);
	}
	@Override
	public void unregisterListener(Serializable identifier) {
		super.unregisterListener(identifier);
	}
	
	//	PRIVATE METHODS --
	
	/**
	 * Creates a message. It creates a wrapped message for emulation, or a regular
	 * message for real applications
	 */
	private Message createMessage(Address dst, Serializable content) {
		Message msg;
		InetAddress dstInetAddress = getInetAddress(dst);
		//Set destination if it is null (it is a Multicast address)
		if (dstInetAddress==null) dstInetAddress = mcastAddr.getMcastAddress();
		if (content instanceof Message){
			msg = (Message) content;
		} else {
			msg = new Message();
			if (PropertiesLoader.isEmulated()){ 
				seqNumber++;
				msg.setObject(new SequenceNumberMessageWrapper(seqNumber,content));
				notifier.newMessageSent(msg,getLocalInetAddress(),dstInetAddress,seqNumber,((Channel) transport).getView());
			}else msg.setObject(content);
		}
		msg.setSrc(new IpAddress(getLocalInetAddress(),PropertiesLoader.getUnicastPort()));
		msg.setDest(new IpAddress(dstInetAddress,PropertiesLoader.getUnicastPort()));
		return msg;
	}
    private InetAddress	getInetAddress(Address dest) {
		return ((IpAddress)dest).getIpAddress();
	}
	/**
	 * Returns an InetAddress from the local Address
	 * @return
	 */
	private InetAddress getLocalInetAddress() {
		return ((IpAddress)getLocalAddress()).getIpAddress();
	}
	
    /* **********************************************
     * 				 MESSAGE RECEPTION
     * ********************************************/
	
	/**
	 * Returns the message. Since it can be wrapped with a sequence number object
	 * we must check if we are performing emulation or real tests
	 * @param msg
	 * @return
	 */
	private Message getReceivedMessage(Message msg) {		
		if (PropertiesLoader.isEmulated()==false){
			return msg;
		} else {
			//Check application messages
			//In the emulation we will add a seq number to check that all messages
			//get to their destinations
			if (msg.getObject() instanceof SequenceNumberMessageWrapper){
				SequenceNumberMessageWrapper messageWrapper = (SequenceNumberMessageWrapper)msg.getObject();
				int seqNumber = messageWrapper.getSeqNumber();
				Serializable content = messageWrapper.getContent();
				msg.setObject(content);
				Address addr = msg.getDest();
				if (addr.isMulticastAddress()){
					//Now notify that we have received this message
					notifier.newMessageReceived(msg,getInetAddress(msg.getSrc()),getInetAddress(msg.getDest()),getLocalInetAddress(),seqNumber);
				} else {
					//Now notify that we have received this message
					notifier.newMessageReceived(msg,getInetAddress(msg.getSrc()),getInetAddress(msg.getDest()),getInetAddress(msg.getDest()),seqNumber);
				}
			}
			return msg;
		}
	}
}