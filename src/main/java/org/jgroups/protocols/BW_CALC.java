package org.jgroups.protocols;

import org.jgroups.Event;
import org.jgroups.Message;
import org.jgroups.annotations.Property;
import org.jgroups.stack.Protocol;
import urv.bwcalc.BwData;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Bandwidth Calculator protocol.
 * It passes the information periodically, depending on the configured parameter info_millis
 * 
 * @author Marc Espelt
 */
public class BW_CALC extends Protocol {
	
	//	CONSTANTS --
	
	/**
	 * The default value of the period (in milliseconds) that the task will be executed.
	 * @see BW_CALC#info_millis
	 * @see BW_CALC#scheduler
	 */
	private static final long default_info_millis = 1000;	
	private static final BwCalcHeader msg_header = new BwCalcHeader();

    //	CLASS FIELDS --
    
    /**
	 * The period (in milliseconds) that the information snooped will be sent.
	 * @see BW_CALC#default_info_millis
	 * @see BW_CALC#scheduler
	 */
    @Property(description="The period (in milliseconds) that the information snooped will be sent")
    protected long info_millis = default_info_millis;


    /**
	 * Minimum capacity in bytes for a node. It is directly related with the size of the fragmentation packet.
	 */
    @Property(description="Minimum capacity in bytes for a node. It is directly related with the size of the fragmentation packet")
    protected long minimumCapacityInBytes;

    /**
	 * Minimum capacity in messages for a node. 
	 */
    @Property(description="Minimum capacity in messages for a node")
    protected long minimumCapacityInMessages;


    /**
     * Thread that manages the periodic task.
     * @see UpdateTask#run()
     * @see BW_CALC#task
     */
    private Timer scheduler;    
    /**
     * The task executed by the scheduler
     * @see BW_CALC#scheduler
     * @see UpdateTask#run()
     */
    private TimerTask task;
    
    //////////////////////////// Calculated Values ////////////////////////////
    //////////////// All the values calculated are per second /////////////////
    
    private long current_incoming_packets;
    private long current_incoming_bytes;
    private long max_incoming_packets = minimumCapacityInMessages;
    private long max_incoming_bytes = minimumCapacityInBytes;
    
    ///////////////////////////////////////////////////////////////////////////
    
    //	OVERRIDDEN METHODS --
    
    public Object down(Event evt) {
    	return down_prot.down(evt); // does not affect the message itself
    }

	public void resetStats() {
        super.resetStats();
        this.info_millis = BW_CALC.default_info_millis;
        if (this.scheduler != null)
        	this.scheduler.cancel();
    }

    public void init() throws Exception {
        super.init();
        max_incoming_packets = minimumCapacityInMessages;
        max_incoming_bytes = minimumCapacityInBytes;
    }

    public void start() throws Exception {
        super.start();
        if (this.scheduler == null) {
        	this.scheduler = new Timer("BW_CALC scheduler [" + info_millis + "]"); // the only "new" for the Timer allowed (due to memory consumption)
        }
        if (this.task == null){
        	this.task = new UpdateTask(); // the only "new" for the UpdateTask allowed (due to memory consumption)
        }
        this.scheduler.scheduleAtFixedRate(task, this.getInfoMillis(), this.getInfoMillis());
    }

    public void stop() {
        super.stop();
        if (this.scheduler != null) {
        	this.scheduler.cancel();
        }
    }    
    public Object up(Event evt) {
    	switch (evt.getType()) {
    	// Set the default values to the local node before start the communication with the other nodes in the network
		case Event.SET_LOCAL_ADDRESS:
			BwData bd = new BwData();
			bd.setMaxIncomingBytes(minimumCapacityInBytes);
			bd.setMaxIncomingPackets(minimumCapacityInMessages);
			Message msg = new Message(null,bd);
	    	msg.putHeader(Constants.BW_CALC_ID, BW_CALC.msg_header);
            up_prot.up(msg);
    	}
    	return up_prot.up(evt); // does not affect the message itself
    }

    public Object up(Message msg) {
        current_incoming_packets++;
        current_incoming_bytes += msg.size();
        return up_prot.up(msg);
    }

    //	ACCESS METHODS --
    
    public long getInfoMillis() {
		return this.info_millis;
	}    
    public void setInfoMillis(long im) {
		this.info_millis = im;
		//TODO: reschedule the task according to this new parameter.
	}

    //	PRIVATE METHODS --
    
    private synchronized void sendInfo() {
    	BwData data = new BwData();
    	//If the collected bytes is lower than the minimum defined, we send that the 
    	//bandwidth capacity in bytes for this node is the minimum
    	if (this.max_incoming_bytes < this.minimumCapacityInBytes){
    		data.setMaxIncomingBytes(this.minimumCapacityInBytes);
    	}else data.setMaxIncomingBytes(this.max_incoming_bytes);
    	//If the collected messages are lower than the minimum defined, is because
    	//this node hasn't been used as receiver or intermedium node of a transmission
    	//and we use the default minimum
    	if (this.max_incoming_packets < this.minimumCapacityInMessages){
    		data.setMaxIncomingPackets(this.minimumCapacityInMessages);
    	}else data.setMaxIncomingPackets(this.max_incoming_packets);
    	Message msg = new Message(null,data);
    	// the same header for every message
    	msg.putHeader(Constants.BW_CALC_ID, BW_CALC.msg_header);
    	up_prot.up(msg);
    	System.out.println("BW_CALC => " + data);
	}    

    
    //	INNER CLASSES --
    
    /**
     * The class in charge to update the values gathered during the "info_millis" period
     * @author Marc Espelt
     * @see BW_CALC#task
     */
    class UpdateTask extends TimerTask {
        private long old_max_incoming_packets = 0;
        private long old_max_incoming_bytes = 0;
    	public void run() {
        	// max packets
    		old_max_incoming_packets = max_incoming_packets;
        	max_incoming_packets = (long) Math.max(max_incoming_packets, (current_incoming_packets * 1000.0) / ((double)info_millis));
        	current_incoming_packets = 0;
        	// max bytes
        	old_max_incoming_bytes = max_incoming_bytes;
        	max_incoming_bytes   = (long) Math.max(max_incoming_bytes, (current_incoming_bytes * 1000.0) / ((double)info_millis));
        	current_incoming_bytes = 0;        	
        	if (old_max_incoming_bytes < max_incoming_bytes || old_max_incoming_packets < max_incoming_packets){
        		sendInfo();
        	}
        }    	
    }
}