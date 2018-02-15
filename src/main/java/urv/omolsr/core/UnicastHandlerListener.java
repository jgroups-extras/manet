package urv.omolsr.core;

import org.jgroups.Message;

/**
 * The Handler extends this interface in order to handle the control and data
 * messages.
 * 
 * @author Gerard Paris Aixala
 *
 */
public interface UnicastHandlerListener {
	/**
	 * Handles incoming unicast data messages
	 * @param msg
	 * @return true if the message must be passed up
	 */
	boolean handleIncomingDataMessage(Message msg);
	
	void handleOutgoingDataMessage(Message msg);
	
}