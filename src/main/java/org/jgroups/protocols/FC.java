package org.jgroups.protocols;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.Global;
import org.jgroups.Header;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.annotations.GuardedBy;
import org.jgroups.annotations.ManagedAttribute;
import org.jgroups.annotations.ManagedOperation;
import org.jgroups.annotations.Property;
import org.jgroups.stack.Protocol;
import org.jgroups.util.BoundedList;

import urv.bwcalc.BwData;
import urv.conf.PropertiesLoader;
import urv.olsr.data.OLSRNode;
import urv.olsr.mcast.TopologyEvent;


/**
 * Simple flow control protocol based on a credit system. Each sender has a number of credits (bytes
 * to send). When the credits have been exhausted, the sender blocks. Each receiver also keeps track of
 * how many credits it has received from a sender. When credits for a sender fall below a threshold,
 * the receiver sends more credits to the sender. Works for both unicast and multicast messages.
 * <p/>
 * Note that this protocol must be located towards the top of the stack, or all down_threads from JChannel to this
 * protocol must be set to false ! This is in order to block JChannel.send()/JChannel.down().
 * <br/>This is the second simplified implementation of the same model. The algorithm is sketched out in
 * doc/FlowControl.txt
 * <br/>
 * Changes (Brian) April 2006:
 * <ol>
 * <li>Receivers now send credits to a sender when more than min_credits have been received (rather than when min_credits
 * are left)
 * <li>Receivers don't send the full credits (max_credits), but rather tha actual number of bytes received
 * <ol/>
 * @author Bela Ban
 * @version $Id: FC.java,v 1.86.2.1 2007/12/18 07:58:17 belaban Exp $
 */
public class FC extends Protocol {

    private final static FcHeader REPLENISH_HDR=new FcHeader(FcHeader.REPLENISH);
    private final static FcHeader CREDIT_REQUEST_HDR=new FcHeader(FcHeader.CREDIT_REQUEST);
    private final static String   RECEIVED_CREDITS = "RECEIVED_CREDITS";
    private final static String   SENT_CREDITS = "SENT_CREDITS";

    /**
     * Map<Address,Long>: keys are members, values are credits left. For each send, the
     * number of credits is decremented by the message size. A HashMap rather than a ConcurrentHashMap is
     * currently used as there might be null values
     */
    @GuardedBy("sent_lock")
    private final Map<Address, CreditPerNodeUnit> sent=new HashMap<>(11);

    /**
     * Map<Address,Long>: keys are members, values are credits left (in bytes).
     * For each receive, the credits for the sender are decremented by the size of the received message.
     * When the credits are 0, we refill and send a CREDIT message to the sender. Sender blocks until CREDIT
     * is received after reaching <tt>min_credits</tt> credits.
     */
    @GuardedBy("received_lock")
    private final Map<Address, CreditPerNodeUnit> received=new ConcurrentHashMap<>(11);

    /**
     * List of members from whom we expect credits
     */
    @GuardedBy("sent_lock")
    private final Set<Address> creditors=new HashSet<>(11);

    /** Peers who have asked for credit that we didn't have */
    private final Set<Address> pending_requesters=new HashSet<>(11);
    
    /**
     * Max number of bytes to send per receiver until an ack must
     * be received before continuing sending
     */
    @Property(description="Max number of credits (in bytes)")
    protected long max_credits=500;

    private Long max_credits_constant=max_credits;

    /**
     * Max time (in milliseconds) to block. If credit hasn't been received after max_block_time, we send
     * a REPLENISHMENT request to the members from which we expect credits. A value <= 0 means to
     * wait forever.
     */
    @Property(description="Max time (in milliseconds) to block. If credit hasn't been received after max_block_time, " +
      "we send a REPLENISHMENT request to the members from which we expect credits. A value <= 0 means to wait forever")
    protected long max_block_time=5000;

    /**
     * If credits fall below this limit, we send more credits to the sender. (We also send when
     * credits are exhausted (0 credits left))
     */
    @Property(description="Threshold (in bytes) at which a receiver sends credits to a sender")
    protected double min_threshold=0.25;

    /**
     * Computed as <tt>max_credits</tt> times <tt>min_theshold</tt>. If explicitly set, this will
     * override the above computation
     */
    @Property(description="Computed as max_credits times min_theshold. If set explicitly, this will override min_threshold")
    protected long min_credits;


    /**
     * When we use the DynamicCredit assignment by hop count, the credit decreases in function of
     * the distance in hops between neighbors. However, we need to ensure a minimal maximum of credits
     * because if the hop distance is high, the credit assigned will be so low that the workflow of the
     * credits between nodes will not work correctly
     */
    @Property(description="When we use the DynamicCredit assignment by hop count, the credit decreases in function of " +
      "the distance in hops between neighbors. However, we need to ensure a minimal maximum of credits because if the " +
      "hop distance is high, the credit assigned will be so low that the workflow of the " +
      "credits between nodes will not work correctly")
    protected long lowest_max_credits = max_credits;
    
    /**
     * Whether FC is still running, this is set to false when the protocol terminates (on stop())
     */
    private boolean running=true;

    /**
     * the lowest credits of any destination (sent_msgs)
     */
    @GuardedBy("sent_lock")
    private long lowest_credit=max_credits;

    /** Lock protecting sent credits table and some other vars (creditors for example) */
    private final Lock sent_lock=new ReentrantLock();

    /** Lock protecting received credits table */
    private final Lock received_lock=new ReentrantLock();

    /** Mutex to block on down() */
    private final Condition credits_available=sent_lock.newCondition();

    private Address localAddress;

    /**
     * Whether an up thread that comes back down should be allowed to
     * bypass blocking if all credits are exhausted. Avoids JGRP-465.
     * Set to false by default in 2.5 because we have OOB messages for credit replenishments - this flag should not be set
     * to true if the concurrent stack is used
     */
    @Property(description="hether an up thread that comes back down should be allowed to bypass blocking if all " +
      "credits are exhausted. Avoids JGRP-465. Set to false by default in 2.5 because we have OOB messages for credit " +
      "replenishments - this flag should not be set to true if the concurrent stack is used")
    protected boolean ignore_synchronous_response;

    /**
     * Thread that carries messages through up() and shouldn't be blocked
     * in down() if ignore_synchronous_response==true. JGRP-465.
     */
    private Thread ignore_thread;
    /** Last time a credit request was sent. Used to prevent credit request storms */
    @GuardedBy("sent_lock")
    private long last_credit_request=0;

    private int num_blockings;

    private int num_credit_requests_received=0, num_credit_requests_sent=0;
    private int num_credit_responses_sent=0, num_credit_responses_received=0;
    
    private long total_time_blocking;
    private final BoundedList last_blockings=new BoundedList(50);

    private static long computeLowestCredit(Map<Address, CreditPerNodeUnit> m) {
        Collection<Long> credits =new ArrayList<>();
        for (CreditPerNodeUnit creditPerNodeUnit : m.values()){
        	credits.add(creditPerNodeUnit.getCreditsLeft());
        }        
        return Collections.min(credits);
    }

    private static long computeLowestMaximumCredit(Map<Address, CreditPerNodeUnit> m) {
        Collection<Long> credits =new ArrayList<>();
        for (CreditPerNodeUnit creditPerNodeUnit : m.values()){
        	credits.add(creditPerNodeUnit.getMaxCredits());
        }        
        return Collections.min(credits);
    }

    private static String printMap(Map<Address,CreditPerNodeUnit> m) {
        StringBuilder sb=new StringBuilder();
        for(Map.Entry<Address,CreditPerNodeUnit> entry: m.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue().toString()).append("\n");
        }
        return sb.toString();
    }


    @ManagedAttribute(description="Average time blocked (in ms)")
	public double getAverageTimeBlocked() {
        return num_blockings == 0? 0.0 : total_time_blocking / (double)num_blockings;
    }

    @ManagedAttribute
	public long getLowest_max_credits() {
		return lowest_max_credits;
	}

	@ManagedAttribute
    public long getMaxBlockTime() {
        return max_block_time;
    }

    public long getMaxCredits() {
        return max_credits;
    }

    @ManagedAttribute
    public long getMinCredits() {
        return min_credits;
    }

    @ManagedAttribute
    public double getMinThreshold() {
        return min_threshold;
    }

    @ManagedAttribute
    public int getNumberOfBlockings() {
        return num_blockings;
    }

    @ManagedAttribute
    public int getNumberOfCreditRequestsReceived() {
        return num_credit_requests_received;
    }

    @ManagedAttribute
    public int getNumberOfCreditRequestsSent() {
        return num_credit_requests_sent;
    }

    @ManagedAttribute
    public int getNumberOfCreditResponsesReceived() {
        return num_credit_responses_received;
    }

    @ManagedAttribute
    public int getNumberOfCreditResponsesSent() {
        return num_credit_responses_sent;
    }

    @ManagedAttribute
    public long getTotalTimeBlocked() {
        return total_time_blocking;
    }

    @ManagedOperation
    public String printCredits() {
        StringBuilder sb=new StringBuilder();
        sb.append("senders:\n").append(printMap(sent)).append("\n\nreceivers:\n").append(printMap(received));
        return sb.toString();
    }

    @ManagedOperation
    public String printReceiverCredits() {
        return printMap(received);
    }

    @ManagedOperation
    public String printSenderCredits() {
        return printMap(sent);
    }

    @Override
	public void resetStats() {
        super.resetStats();
        num_blockings=0;
        num_credit_responses_sent=num_credit_responses_received=num_credit_requests_received=num_credit_requests_sent=0;
        total_time_blocking=0;
        last_blockings.clear();
    }

    public void setLowest_max_credits(long lowest_max_credits) {
		this.lowest_max_credits = lowest_max_credits;
	}

    public void setMaxBlockTime(long t) {
        max_block_time=t;
    }


    public void setMaxCredits(long max_credits) {
        this.max_credits=max_credits;
        max_credits_constant=this.max_credits;
    }


    public void setMinCredits(long min_credits) {
        this.min_credits=min_credits;
    }

    public void setMinThreshold(double min_threshold) {
        this.min_threshold=min_threshold;
    }


    @ManagedOperation
    public String showLastBlockingTimes() {
        return last_blockings.toString();
    }


    @Override
	public Object down(Message msg) {
        int length=msg.getLength();
        Address dest=msg.getDest();

        sent_lock.lock();
        try {
            if(length > lowest_credit) { // then block and loop asking for credits until enough credits are available
                if(ignore_synchronous_response && ignore_thread == Thread.currentThread()) { // JGRP-465
                    if(log.isTraceEnabled())
                        log.trace("bypassing blocking to avoid deadlocking " + Thread.currentThread());
                }
                else {
                    determineCreditors(dest, length);
                    long start_blocking=System.currentTimeMillis();
                    num_blockings++; // we count overall blockings, not blockings for *all* threads
                    if(log.isTraceEnabled())
                        log.trace("Starting blocking. lowest_credit=" + lowest_credit + "; msg length =" + length);

                    while(length > lowest_credit && running) {
                        try {
                            boolean rc=credits_available.await(max_block_time, TimeUnit.MILLISECONDS);
                            if(rc || length <= lowest_credit || !running)
                                break;

                            long wait_time=System.currentTimeMillis() - last_credit_request;
                            if(wait_time >= max_block_time) {

                                // we have to set this var now, because we release the lock below (for sending a
                                // credit request), so all blocked threads would send a credit request, leading to
                                // a credit request storm
                                last_credit_request=System.currentTimeMillis();

                                // we need to send the credit requests down *without* holding the sent_lock, otherwise we might
                                // run into the deadlock described in http://jira.jboss.com/jira/browse/JGRP-292
                                Map<Address,CreditPerNodeUnit> sent_copy=new HashMap<>(sent);
                                sent_copy.keySet().retainAll(creditors);
                                sent_lock.unlock();
                                try {
                                    // System.out.println(new Date() + " --> credit request");
                                    for(Map.Entry<Address,CreditPerNodeUnit> entry: sent_copy.entrySet()) {
                                        sendCreditRequest(entry.getKey(), entry.getValue().getCreditsLeft());
                                    }
                                }
                                finally {
                                    sent_lock.lock();
                                }
                            }
                        }
                        catch(InterruptedException e) {
                            // set the interrupted flag again, so the caller's thread can handle the interrupt as well

                            // bela June 15 2007: don't do this as this will trigger an infinite loop !!
                            // (http://jira.jboss.com/jira/browse/JGRP-536)
                            // Thread.currentThread().interrupt();
                        }
                    }
                    // if(!running) // don't send the message if not running anymore
                    // return null;

                    long block_time=System.currentTimeMillis() - start_blocking;
                    if(log.isTraceEnabled())
                        log.trace("total time blocked: " + block_time + " ms");
                    total_time_blocking+=block_time;
                    last_blockings.add(block_time);
                }
            }

            long tmp=decrementCredit(sent, dest, length);
            if(tmp != -1)
                lowest_credit=Math.min(tmp, lowest_credit);
        }
        finally {
            sent_lock.unlock();
        }

        // send message - either after regular processing, or after blocking (when enough credits available again)
        return down_prot.down(msg);
    }

    @Override
	public void init() throws Exception {
        super.init();
        if(min_credits == 0)
            min_credits=(long)((double)max_credits * min_threshold);
        max_credits_constant=max_credits;
    }

    @Override
	public void start() throws Exception {
        super.start();
        sent_lock.lock();
        try {
            running=true;
            lowest_credit=max_credits;
        }
        finally {
            sent_lock.unlock();
        }
    }
    @Override
	public void stop() {
        super.stop();
        sent_lock.lock();
        try {
            running=false;
            ignore_thread=null;
            credits_available.signalAll(); // notify all threads waiting on the mutex that we are done
        }
        finally {
            sent_lock.unlock();
        }
    }

	/**
     * Allows to unblock a blocked sender from an external program, e.g. JMX
     */
    public void unblock() {
        sent_lock.lock();
        try {
            if(log.isTraceEnabled())
                log.trace("unblocking the sender and replenishing all members, creditors are " + creditors);

            for(Map.Entry<Address, CreditPerNodeUnit> entry: sent.entrySet()) {
                entry.getValue().setCreditsLeft(entry.getValue().getMaxCredits());
            }

            lowest_credit=computeLowestMaximumCredit(sent);
            creditors.clear();
            credits_available.signalAll();
        }
        finally {
            sent_lock.unlock();
        }
    }

    @Override
	public Object up(Event evt) {
        switch(evt.getType()) {
        	case Event.SET_LOCAL_ADDRESS:
				localAddress=evt.getArg();
				break;
            case Event.VIEW_CHANGE:
                handleViewChange(evt.getArg());
                break;
        }
        return up_prot.up(evt);
    }


    @Override
	public Object up(Message msg) {
        // JGRP-465. We only deal with msgs to avoid having to use a concurrent collection; ignore views,
        // suspicions, etc which can come up on unusual threads.
        if(ignore_thread == null && ignore_synchronous_response)
            ignore_thread=Thread.currentThread();

        FcHeader hdr=msg.getHeader(id);
        if(hdr != null) {
            switch(hdr.type) {
                case FcHeader.REPLENISH:
                    num_credit_responses_received++;
                    handleCredit(msg.getSrc(), msg.getObject());
                    break;
                case FcHeader.CREDIT_REQUEST:
                    num_credit_requests_received++;
                    Address sender=msg.getSrc();
                    Long sent_credits=msg.getObject();
                    handleCreditRequest(received, received_lock, sender, sent_credits);
                    break;
                default:
                    log.error("header type " + hdr.type + " not known");
                    break;
            }
            return null; // don't pass message up
        } else if (msg.getHeader(Constants.BW_CALC_ID)!=null && msg.getHeader(Constants.BW_CALC_ID) instanceof BwCalcHeader) {
            //FC will also use the BW_CALC messages to perform update its local node credits
            updateOurOwnBandwidth(msg);
            return null;
        } else {
            Address sender=msg.getSrc();
            long new_credits=adjustCredit(received, received_lock, sender, msg.getLength());
            try {
                return up_prot.up(msg);
            }
            finally {
                if(new_credits > 0) {
                    if(log.isTraceEnabled()) log.trace("sending " + new_credits + " credits to " + sender);
                    sendCredit(sender, new_credits);
                }
            }
        }
    }

    /**
     * Check whether sender has enough credits left. If not, send him some more
     * @param map The hashmap to use
     * @param lock The lock which can be used to lock map
     * @param sender The address of the sender
     * @param length The number of bytes received by this message. We don't care about the size of the headers for
     * the purpose of flow control
     * @return long Number of credits to be sent. Greater than 0 if credits needs to be sent, 0 otherwise
     */
    private long adjustCredit(Map<Address,CreditPerNodeUnit> map, final Lock lock, Address sender, int length) {
        if(sender == null) {
            if(log.isErrorEnabled()) log.error("src is null");
            return 0;
        }

        if(length == 0)
            return 0; // no effect

        lock.lock();
        try {
        	CreditPerNodeUnit receivedNodeCredit = map.get(sender);
            long remaining_cred=decrementCredit(map, sender, length);
            long credit_response=receivedNodeCredit.getMaxCredits() - remaining_cred;
            if(credit_response >= min_credits) {
                map.get(sender).setCreditsLeft(receivedNodeCredit.getMaxCredits());
                return credit_response; // this will trigger sending of new credits as we have received more than min_credits bytes from src
            }
        }
        finally {
            lock.unlock();
        }
        return 0;
    }


    /**
     * Decrements credits from a single member, or all members in sent_msgs, depending on whether it is a multicast
     * or unicast message. No need to acquire mutex (must already be held when this method is called)
     * @param dest
     * @param credits
     * @return The lowest number of credits left, or -1 if a unicast member was not found
     */
    private long decrementCredit(Map<Address, CreditPerNodeUnit> m, Address dest, long credits) {
        boolean multicast=dest == null;
        //The lowest variable is initialized with the max variable of the destination or the lowest maximum
        //of the whole network
        long lowest = (!multicast)? m.get(dest).getMaxCredits() : computeLowestCredit(m), new_credit;
        CreditPerNodeUnit val;

        if(multicast) {
            if(m.isEmpty())
                return -1;
            for(Map.Entry<Address, CreditPerNodeUnit> entry: m.entrySet()) {
                val=entry.getValue();
                new_credit=val.getCreditsLeft() - credits;
                entry.getValue().setCreditsLeft(new_credit);
                lowest=Math.min(new_credit, lowest);
            }
            return lowest;
        }
        else {
            val=m.get(dest);
            if(val != null) {
                lowest=val.getCreditsLeft();
                lowest-=credits;
                m.get(dest).setCreditsLeft(lowest);
                return lowest;
            }
        }
        return -1;
    }

    /**
     * Checks whether one member (unicast msg) or all members (multicast msg) have enough credits. Add those
     * that don't to the creditors list. Called with sent_lock held
     * @param dest
     * @param length
     */
    private void determineCreditors(Address dest, int length) {
        boolean multicast=dest == null;
        Address mbr;
        Long credits;
        if(multicast) {
            for(Map.Entry<Address,CreditPerNodeUnit> entry: sent.entrySet()) {
                mbr=entry.getKey();
                credits=entry.getValue().getCreditsLeft();
                if(credits <= length)
                    creditors.add(mbr);
            }
        }
        else {
            credits=sent.get(dest).getCreditsLeft();
            if(credits != null && credits <= length)
                creditors.add(dest);
        }
    }
    /**
     * This method assign the credits for the membership, when a VIEW_CHANGE event is
     * received.
     * 
     * @param neighbor
     * @param updateEvent
     * @return credit for node
     */
    private CreditPerNodeUnit getDynamicCredit(OLSRNode neighbor, View updateEvent, String creditType){
		if (PropertiesLoader.isDynamicCredit() && updateEvent instanceof TopologyEvent){
	    	/*
		     * The creditors will receive a credit depending on its hop distance from
		     * the local node as follow:
		     *	
		     *	Neighbor_Credit = max_credits_constant / sqrt(Hop_Count_To_Neighbor)
		     *
		     *	if (Neighbor_Credit > min_credits) return Neighbor_Credit
		     *	else return min_credits
		     *
		     * This approach tries to maximize the throughput into the network with
		     * the knowledge that jOLSR give to the layers above it 
		     */
    		TopologyEvent topologyEvent = (TopologyEvent)updateEvent;
	    	if (PropertiesLoader.isThroughputOptimizationHopCountEnabled()){
	    		//It enables a hybrid credits assignment: MAX_CREDITS given by the 
	    		//network self knowledgement and the decrement of it by hop count
	    		long max_creits_for_this_node = max_credits_constant;
	    		if (creditType.equals(SENT_CREDITS)){
		    		max_creits_for_this_node = (PropertiesLoader.isThroughputOptimizationNetworkSelfKnowledgementEnabled())?
		    				topologyEvent.getBandwidthCapacityInBytesOf(neighbor) : max_credits_constant;
	    		}else if (creditType.equals(RECEIVED_CREDITS)){
	    			max_creits_for_this_node = (PropertiesLoader.isThroughputOptimizationNetworkSelfKnowledgementEnabled())?
	    					topologyEvent.getMyBandwidthCapacityInBytes() : max_credits_constant;
	    		}
	        	//Initialize the variable to the max_credits available
	    		long optimumCreditByHopCount = max_creits_for_this_node;
	    		//Take the hops to the neighbor node
	    		int hopCount = topologyEvent.getHopCountTo(neighbor);
	    		//If we are sent to another node different of our own
	    		if (hopCount>0){
	    			optimumCreditByHopCount = ((Double)(max_creits_for_this_node / 
	             	Math.sqrt(hopCount))).longValue();
	    		}
	    		// The MAX_CREDITS per node needs a minimum limit, otherwise the protocol
	    		// flow doesn't work fine because hasn't enough credits 
	    		return (optimumCreditByHopCount > lowest_max_credits) ? 
	    			new CreditPerNodeUnit(optimumCreditByHopCount,optimumCreditByHopCount) :
	    				new CreditPerNodeUnit(lowest_max_credits, lowest_max_credits);	    
	    		/*
	    		 * The creditors receive a MAX_CREDITS corresponding to its own capacity given
	    		 * by the BW_CALC protocol, that calculates the maximum incoming throughput
	    		 */
	    	}else if (PropertiesLoader.isThroughputOptimizationNetworkSelfKnowledgementEnabled()){	
	    		long bandWidthOfNode = 0;
	    		//Select the credit type: for Senders or Receivers
	    		if (creditType.equals(SENT_CREDITS)){ 
	    			//If we don't found the credit of the node, apply the minimum default
	    			bandWidthOfNode = topologyEvent.getLowestBandwidthCapacityInBytesOfRouteTo(neighbor);
	    			bandWidthOfNode = (bandWidthOfNode!=-1)? bandWidthOfNode : 
	    				(neighbor.equals(topologyEvent.getLocalNode())? topologyEvent.getMyBandwidthCapacityInBytes() : max_credits); 
	    			//If is the receivers credit we apply our own capacity
	    		}else bandWidthOfNode = topologyEvent.getMyBandwidthCapacityInBytes();
	    		return  new CreditPerNodeUnit(bandWidthOfNode,bandWidthOfNode);
	    	}else return new CreditPerNodeUnit(max_credits,max_credits);
    	}else return new CreditPerNodeUnit(max_credits,max_credits);
    }

    private void handleCredit(Address sender, Number increase) {
        if(sender == null) return;
        StringBuilder sb=null;

        sent_lock.lock();
        try {
        	CreditPerNodeUnit creditOfTheReceivingNode = sent.get(sender);
        	Long old_credit = creditOfTheReceivingNode.getCreditsLeft();
            if(old_credit == null)
                return;
            Long new_credit=Math.min(creditOfTheReceivingNode.getMaxCredits(), old_credit + increase.longValue());
        	
            if(log.isTraceEnabled()) {
                sb=new StringBuilder();
                sb.append("received credit from ").append(sender).append(", old credit was ").append(old_credit)
                        .append(", new credits are ").append(new_credit).append(".\nCreditors before are: ").append(creditors);
            }

            sent.get(sender).setCreditsLeft(new_credit);
            lowest_credit=computeLowestCredit(sent);
            // boolean was_empty=true;
            if(!creditors.isEmpty()) {  // we are blocked because we expect credit from one or more members
                // was_empty=false;
                creditors.remove(sender);
                if(log.isTraceEnabled()) {
                    sb.append("\nCreditors after removal of ").append(sender).append(" are: ").append(creditors);
                    log.trace(sb);
                }
            }
            if(creditors.isEmpty()) {// && !was_empty) {
                credits_available.signalAll();
            }
        }
        finally {
            sent_lock.unlock();
        }
    }

    /**
     * @param map The map to modify
     * @param lock The lock to lock map
     * @param sender The sender who requests credits
     * @param left_credits Number of bytes that the sender has left to send messages to us
     */
    private void handleCreditRequest(Map<Address,CreditPerNodeUnit> map, Lock lock, Address sender, Long left_credits) {
        if(sender == null) return;
        long credit_response=0;

        lock.lock();
        try {
        	CreditPerNodeUnit senderCreditTowardsUs = map.get(sender);
            Long old_credit=senderCreditTowardsUs.getCreditsLeft();
            if(old_credit != null) {
                credit_response=Math.min(senderCreditTowardsUs.getMaxCredits(), senderCreditTowardsUs.getMaxCredits() - old_credit);
            }

            if(credit_response > 0) {
                if(log.isTraceEnabled())
                    log.trace("received credit request from " + sender + ": sending " + credit_response + " credits");
                map.get(sender).setCreditsLeft(senderCreditTowardsUs.getMaxCredits());
                pending_requesters.remove(sender);
            }
            else {
                if(pending_requesters.contains(sender)) {
                    // a sender might have negative credits, e.g. -20000. If we subtracted -20000 from max_credits,
                    // we'd end up with max_credits + 20000, and send too many credits back. So if the sender's
                    // credits is negative, we simply send max_credits back
                    long credits_left=Math.max(0, left_credits);
                    credit_response=senderCreditTowardsUs.getMaxCredits() - credits_left;
                    // credit_response = max_credits;
                    map.get(sender).setCreditsLeft(senderCreditTowardsUs.getMaxCredits());
                    pending_requesters.remove(sender);
                    if(log.isWarnEnabled())
                        log.warn("Received two credit requests from " + sender +
                                " without any intervening messages; sending " + credit_response + " credits");
                }
                else {
                    pending_requesters.add(sender);
                    if(log.isTraceEnabled())
                        log.trace("received credit request from " + sender + " but have no credits available");
                }
            }
        }
        finally {
            lock.unlock();
        }

        if(credit_response > 0)
            sendCredit(sender, credit_response);
    }





    private void handleViewChange(View updateEvent) {
        //Use the membership inside the list of the UpdateEvent
        List<Address> mbrs=updateEvent.getMembers();
        if(mbrs == null) return;
        if(log.isTraceEnabled()) log.trace("new membership: " + mbrs);

        sent_lock.lock();
        received_lock.lock();
        
        try {        	
        	OLSRNode neighbor = new OLSRNode();        	
            //add members not in membership to received and sent hashmap (with full credits)
            for(Address addr: mbrs) {
                neighbor.setValue(addr);
				//The received map is filled with our own capacity depending on the distance 
				//of the neighbour
                received.put(addr, getDynamicCredit(neighbor, updateEvent, RECEIVED_CREDITS));
                //The sent map is filled with the capacity of the rest of the nodes depending
                //on the distance from us
                sent.put(addr, getDynamicCredit(neighbor, updateEvent, SENT_CREDITS));
            }
            // remove members that left
            received.keySet().retainAll(mbrs);

            // remove members that left
            sent.keySet().retainAll(mbrs);

            // remove all creditors which are not in the new view
            creditors.retainAll(mbrs);

            if(log.isTraceEnabled()) log.trace("creditors are " + creditors);
            if(creditors.isEmpty()) {
                lowest_credit=computeLowestCredit(sent);
                credits_available.signalAll();
            }
        }
        finally {
            sent_lock.unlock();
            received_lock.unlock();
        }
    }
    private void sendCredit(Address dest, long credit) {
        Number number;
        if(credit < Integer.MAX_VALUE)
            number=(int)credit;
        else
            number=credit;
        Message msg=new Message(dest, number).setFlag(Message.Flag.OOB).putHeader(id, REPLENISH_HDR);
        down_prot.down(msg);
        num_credit_responses_sent++;
    }
    /**
     * We cannot send this request as OOB messages, as the credit request needs to queue up behind the regular messages;
     * if a receiver cannot process the regular messages, that is a sign that the sender should be throttled !
     * @param dest The member to which we send the credit request
     * @param credits_left The number of bytes (of credits) left for dest
     */
    private void sendCreditRequest(final Address dest, Long credits_left) {
        if(log.isTraceEnabled())
            log.trace("sending credit request to " + dest);
        Message msg=new Message(dest, credits_left).putHeader(id, CREDIT_REQUEST_HDR);
        down_prot.down(msg);
        num_credit_requests_sent++;
    }    
    /**
     * Update our receiving capacity once a BW_CALC event is received
     * 
     * @param msg
     */
	private void updateOurOwnBandwidth(Message msg) {
		//Get bandwidth information
		BwData bd =msg.getObject();
		//Update receiving capacity
		received_lock.lock();
		try {
            for(Map.Entry<Address,CreditPerNodeUnit> addressCreditPerNodeUnitEntry : received.entrySet()) {
                addressCreditPerNodeUnitEntry.getValue().setCreditsLeft(bd.getMaxIncomingBytes());
                addressCreditPerNodeUnitEntry.getValue().setMaxCredits(bd.getMaxIncomingBytes());
            }
        }
        finally {
            received_lock.unlock();
        }

		//Update sending to ourselves
		sent_lock.lock();
		try {
            if(sent.get(localAddress) != null) {
                sent.get(localAddress).setCreditsLeft(bd.getMaxIncomingBytes());
                sent.get(localAddress).setMaxCredits(bd.getMaxIncomingBytes());
            }
        }
        finally {
            sent_lock.unlock();
        }
	}
   
	//	INNER CLASSES --
	
    public static class FcHeader extends Header {
        public static final byte REPLENISH=1;
        public static final byte CREDIT_REQUEST=2; // the sender of the message is the requester

        byte type=REPLENISH;

        public FcHeader() {

        }

        public FcHeader(byte type) {
            this.type=type;
        }

        @Override
		public short                      getMagicId()     {return Constants.FC_ID;}
        @Override
		public Supplier<? extends Header> create()         {return FcHeader::new;}
        @Override
		public int                        serializedSize() {return Global.BYTE_SIZE;}

        @Override
		public void writeTo(DataOutput out) throws Exception {
            out.writeByte(type);
        }

        @Override
		public void readFrom(DataInput in) throws Exception {
            type=in.readByte();
        }

        @Override
		public String toString() {
            switch(type) {
                case REPLENISH:
                    return "REPLENISH";
                case CREDIT_REQUEST:
                    return "CREDIT_REQUEST";
                default:
                    return "<invalid type>";
            }
        }


    }    
    /**
     * This class will be the storage unit for the credits left and max_credits of each node into the network.
     */
    private class CreditPerNodeUnit {
    	
    	//	CLASS ATTRIBUTES --

        private long creditsLeft;
    	private long maxCredits = max_credits;
    	
    	//	CONSTRUCTORS --
    	
		public CreditPerNodeUnit(long creditsLeft, long maxCredits) {
			super();
			this.creditsLeft = creditsLeft;
			this.maxCredits = maxCredits;
		}
    	
		//	OVERRIDDEN METHODS --
		
		@Override
		public String toString() {
			return String.format("CREDIT LEFT: %s - MAX CREDITS: %d", getCreditsLeft(), getMaxCredits());
		}

		//	PUBLIC METHODS --
		
		public synchronized long getCreditsLeft() {
			return creditsLeft;
		} 
		
    	//	ACCESS METHODS --
    	
		public synchronized long getMaxCredits() {
			return maxCredits;
		}
		public synchronized void setCreditsLeft(long creditsLeft) {
			this.creditsLeft = creditsLeft;
		}
		public synchronized void setMaxCredits(long maxCredits) {
			this.maxCredits = maxCredits;
		}
    }
}