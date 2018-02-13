package urv.app.samples;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.MessageListener;

import urv.app.Application;
import urv.app.messages.ReplyObject;
import urv.app.messages.RequestObject;
import urv.log.Log;
import urv.machannel.MChannel;
import urv.olsr.data.neighbour.NeighborTable;
import urv.olsr.data.routing.RoutingTable;
import urv.olsr.data.topology.TopologyInformationBaseTable;
import urv.olsr.mcast.MulticastAddress;
import urv.util.network.NetworkUtils;

/**
 * @author Gerard Paris Aixala
 *
 */
public class RealTest extends Application{

	private JFrame parent;
	
	private MChannel app = null;  //  @jve:decl-index=0:
	private JTextField jTextFieldMessage = null;
	private JButton jButtonSend = null;
	private JButton jButtonStart = null;
	private JButton jButtonDump = null;
	private JTextArea jTextAreaDump = null;
	private JPanel jPanelButtons = null;
	private JPanel jPanelMain = null;  //  @jve:decl-index=0:visual-constraint="10,10"
	private JPanel jPanelSend = null;
	private JComboBox jComboBoxDest = null;
	private JScrollPane jScrollPaneMsgConsole = null;
	private JTextArea jTextAreaMsgConsole = null;
	private JButton jButtonPingTest = null;
	private int requestsSent = 0;
	private Set<Address> repliesReceived = new HashSet<Address>();
	private long lastRequestId = 0;
	private Object lock = new Object();	
	private JButton jButtonMcastPingTest = null;
	private String name ="RT";
	InetAddress mcastAddress = null;
	
	//	CONSTRUCTORS --
	
	public RealTest(){
		super();
		parent = new JFrame();
		initialize();
	}
	
	//	MAIN --
	
	public static void main(String[] args) {
		try {
			new RealTest().parent.setVisible(true);
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	//	OVERRIDDEN METHODS --
	
	@Override
	public void start() {
		parent.setVisible(true);
	}

	//	PRIVATE METHODS --
	
	/**
	 * Creates new applications, that is, new JChannels
	 *
	 */
	private void createApp(){		
		Log.getInstance().setCurrentLevel(Log.INFO);		
		try {
			mcastAddress = InetAddress.getByName("224.0.0.10");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}		
		MulticastAddress mcastAddr = new MulticastAddress();
		mcastAddr.setValue(mcastAddress);		
		app = createMChannel(mcastAddr);
		MessageListener messageListener = new MessageListener(){
			public byte[] getState() {
				return null;
			}
			public void receive(Message msg) {
				Object obj = msg.getObject();
				if (obj instanceof String){
					String str = (String)obj;
					jTextAreaMsgConsole.append("["+shortName(msg.getSrc().toString())+"] "+str+"\n");
					jTextAreaMsgConsole.setCaretPosition(jTextAreaMsgConsole.getDocument().getLength());
				}
				else if (obj instanceof RequestObject){
					RequestObject req = (RequestObject)obj;					
					ReplyObject rep = new ReplyObject(req.getId());					
					app.send(msg.getSrc(), app.getLocalAddress(), rep);
				}
				else if (obj instanceof ReplyObject){
					synchronized (lock) {
						ReplyObject rep = (ReplyObject)obj;	
						if (rep.getId() == lastRequestId){
							repliesReceived.add(msg.getSrc());
							String reqIdString = lastRequestId + "";							
							String str = "REQ_x"+reqIdString.substring(reqIdString.length()-4,reqIdString.length())+
								": Replies received: ["+repliesReceived.size()+"/" +
								requestsSent+"]: "+repliesReceived;
							System.out.println(str);
							jTextAreaDump.append(str+"\n");
						}else {
							System.err.println("Reply id different than last request id!");
						}
					}
				}else{
					System.err.println("Error: I received an"+obj);
				}
			}
			public void setState(byte[] state) {}			
		};
		app.registerListener(name ,messageListener);
	}	
	/**
	 * This method initializes jButtonDump	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButtonDump() {
		if (jButtonDump == null) {
			jButtonDump = new JButton();
			jButtonDump.setText("Dump");
			jButtonDump.setEnabled(false);
			jButtonDump.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					String str = Log.getInstance().printLoggables();
					jTextAreaDump.setText(str);
					
					
					parseDump(str);
				}
			});
		}
		return jButtonDump;
	}
	/**
	 * This method initializes jButtonMcastPingTest	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButtonMcastPingTest() {
		if (jButtonMcastPingTest == null) {
			jButtonMcastPingTest = new JButton();
			jButtonMcastPingTest.setText("Mcast ping test");
			jButtonMcastPingTest.setEnabled(false);
			jButtonMcastPingTest.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					startMcastPingTest();
				}
			});
		}
		return jButtonMcastPingTest;
	}	
	/**
	 * This method initializes jButtonPingTest	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButtonPingTest() {
		if (jButtonPingTest == null) {
			jButtonPingTest = new JButton();
			jButtonPingTest.setText("Ping test");
			jButtonPingTest.setEnabled(false);
			jButtonPingTest.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					startPingTest();
				}
			});
		}
		return jButtonPingTest;
	}
	/**
	 * This method initializes jButtonSend	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButtonSend() {
		if (jButtonSend == null) {
			jButtonSend = new JButton();
			jButtonSend.setText("Send!");
			jButtonSend.setEnabled(false);
			jButtonSend.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					sendMessage();
				}
			});
		}
		return jButtonSend;
	}	
	/**
	 * This method initializes jButtonStart	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButtonStart() {
		if (jButtonStart == null) {
			jButtonStart = new JButton();
			jButtonStart.setText("Start");
			jButtonStart.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					// Creates a MChannel for this host
					createApp();
					
					registerDumpingClasses();
					
					jButtonStart.setEnabled(false);
					jButtonDump.setEnabled(true);
					jButtonSend.setEnabled(true);
					jButtonPingTest.setEnabled(true);
					jButtonMcastPingTest.setEnabled(true);
				}
			});
		}
		return jButtonStart;
	}
	/**
	 * This method initializes jComboBoxDest	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getJComboBoxDest() {
		if (jComboBoxDest == null) {
			jComboBoxDest = new JComboBox();
			jComboBoxDest.setEditable(true);
		}
		return jComboBoxDest;
	}
	/**
	 * This method initializes jPanelButtons	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanelButtons() {
		if (jPanelButtons == null) {
			GridBagConstraints gridBagConstraints12 = new GridBagConstraints();
			gridBagConstraints12.gridx = 0;
			gridBagConstraints12.insets = new Insets(5, 0, 0, 0);
			gridBagConstraints12.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints12.gridy = 3;
			GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
			gridBagConstraints11.gridx = 0;
			gridBagConstraints11.insets = new Insets(5, 0, 0, 0);
			gridBagConstraints11.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints11.gridy = 2;
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.gridx = 0;
			gridBagConstraints1.insets = new Insets(5, 0, 0, 0);
			gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints1.gridy = 1;
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints.insets = new Insets(5, 0, 0, 0);
			gridBagConstraints.gridy = 0;
			jPanelButtons = new JPanel();
			jPanelButtons.setLayout(new GridBagLayout());
			jPanelButtons.add(getJButtonStart(), gridBagConstraints);
			jPanelButtons.add(getJButtonDump(), gridBagConstraints1);
			jPanelButtons.add(getJButtonPingTest(), gridBagConstraints11);
			jPanelButtons.add(getJButtonMcastPingTest(), gridBagConstraints12);
		}
		return jPanelButtons;
	}

	/**
	 * This method initializes jPanelMain	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanelMain() {
		if (jPanelMain == null) {
			GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
			gridBagConstraints8.fill = GridBagConstraints.BOTH;
			gridBagConstraints8.gridy = 0;
			gridBagConstraints8.weightx = 1.0;
			gridBagConstraints8.weighty = 1.0;
			gridBagConstraints8.insets = new Insets(5, 0, 5, 0);
			gridBagConstraints8.ipadx = 0;
			gridBagConstraints8.gridx = 1;
			GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
			gridBagConstraints7.gridx = 0;
			gridBagConstraints7.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints7.gridwidth = 3;
			gridBagConstraints7.insets = new Insets(5, 5, 5, 5);
			gridBagConstraints7.gridy = 1;
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.gridx = 2;
			gridBagConstraints3.weightx = 0.1;
			gridBagConstraints3.anchor = GridBagConstraints.NORTH;
			gridBagConstraints3.gridy = 0;
			GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.fill = GridBagConstraints.BOTH;
			gridBagConstraints2.gridy = 0;
			gridBagConstraints2.weightx = 1.0;
			gridBagConstraints2.weighty = 1.0;
			gridBagConstraints2.insets = new Insets(10, 10, 10, 10);
			gridBagConstraints2.gridx = 0;
			jPanelMain = new JPanel();
			jPanelMain.setLayout(new GridBagLayout());
			jPanelMain.setSize(new Dimension(472, 342));
			jPanelMain.add(getJTextAreaDump(), gridBagConstraints2);
			jPanelMain.add(getJPanelButtons(), gridBagConstraints3);
			jPanelMain.add(getJPanelSend(), gridBagConstraints7);
			jPanelMain.add(getJScrollPaneMsgConsole(), gridBagConstraints8);
		}
		return jPanelMain;
	}
	
	
	/**
	 * This method initializes jPanelSend	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanelSend() {
		if (jPanelSend == null) {
			GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
			gridBagConstraints5.fill = GridBagConstraints.BOTH;
			gridBagConstraints5.gridy = 0;
			gridBagConstraints5.weightx = 0.5;
			gridBagConstraints5.insets = new Insets(0, 5, 0, 5);
			gridBagConstraints5.gridx = 1;
			GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
			gridBagConstraints6.gridx = 2;
			gridBagConstraints6.gridy = 0;
			GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
			gridBagConstraints4.fill = GridBagConstraints.BOTH;
			gridBagConstraints4.gridy = 0;
			gridBagConstraints4.weightx = 1.0;
			gridBagConstraints4.insets = new Insets(0, 5, 0, 5);
			gridBagConstraints4.gridx = 0;
			jPanelSend = new JPanel();
			jPanelSend.setLayout(new GridBagLayout());
			jPanelSend.add(getJTextFieldMessage(), gridBagConstraints4);
			jPanelSend.add(getJButtonSend(), gridBagConstraints6);
			jPanelSend.add(getJComboBoxDest(), gridBagConstraints5);
		}
		return jPanelSend;
	}
	/**
	 * This method initializes jScrollPaneMsgConsole	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPaneMsgConsole() {
		if (jScrollPaneMsgConsole == null) {
			jScrollPaneMsgConsole = new JScrollPane();
			jScrollPaneMsgConsole.setViewportView(getJTextAreaMsgConsole());
		}
		return jScrollPaneMsgConsole;
	}
	/**
	 * This method initializes jTextAreaDump	
	 * 	
	 * @return javax.swing.JTextArea	
	 */
	private JTextArea getJTextAreaDump() {
		if (jTextAreaDump == null) {
			jTextAreaDump = new JTextArea();
		}
		return jTextAreaDump;
	}
	/**
	 * This method initializes jTextAreaMsgConsole	
	 * 	
	 * @return javax.swing.JTextArea	
	 */
	private JTextArea getJTextAreaMsgConsole() {
		if (jTextAreaMsgConsole == null) {
			jTextAreaMsgConsole = new JTextArea();
		}
		return jTextAreaMsgConsole;
	}
	/**
	 * This method initializes jTextFieldMessage	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTextFieldMessage() {
		if (jTextFieldMessage == null) {
			jTextFieldMessage = new JTextField();
			jTextFieldMessage.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					sendMessage();
				}
			});
		}
		return jTextFieldMessage;
	}
	private void initialize() {
		this.parent.setSize(new Dimension(863, 432));
		this.parent.setContentPane(getJPanelMain());
		this.parent.setTitle("RealTest");
		this.parent.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
	}
	private void parseDump(String str) {
		String regex = "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(str); // Creates Matcher with subject str and Pattern p.
		
		HashSet<String> ips = new HashSet<String>();		
		while (m.find()){
			String ip = m.group();
			ips.add(ip);
		}		
		jComboBoxDest.removeAllItems();
		for (String ip:ips){
			jComboBoxDest.addItem((String)ip);
		}
	}
	private void registerDumpingClasses() {
		Log log = Log.getInstance();
		log.registerDumpingClass(NeighborTable.class.getName());
		log.registerDumpingClass(RoutingTable.class.getName());
		log.registerDumpingClass(TopologyInformationBaseTable.class.getName());
	}	
	private void sendMessage(){
		InetAddress addr = null;
		try {
			addr = InetAddress.getByName((String)jComboBoxDest.getSelectedItem());
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		if (addr!=null){
			app.send(NetworkUtils.getJGroupsAddresFor(addr), 
					app.getLocalAddress(), jTextFieldMessage.getText());
			jTextAreaMsgConsole.append(">> "+jTextFieldMessage.getText()+"\n");
			jTextAreaMsgConsole.setCaretPosition(jTextAreaMsgConsole.getDocument().getLength());
		}
	}
	/**
	 * Given a string representation of an IP address, returns a short representation of the address
	 * @param ip
	 * @return
	 */
	private String shortName(String ip){		
		int index1stDot = ip.indexOf('.', 0);
		if (index1stDot==-1){
			return null;
		}
		int index2ndDot = ip.indexOf('.',index1stDot+1);
		if (index2ndDot==-1){
			return null;
		}
		int indexColon = ip.indexOf(':');
		if (indexColon==-1){
			return null;
		}		
		return ".."+ip.substring(index2ndDot, indexColon);
	}

	private void startMcastPingTest() {		
		synchronized (lock) {
			requestsSent = 0;
			repliesReceived.clear();			
			RequestObject reqObject = new RequestObject();
			lastRequestId = reqObject.getId();			
			// Send a multicast request message
			app.send(NetworkUtils.getJGroupsAddresFor(mcastAddress), 
					app.getLocalAddress(), reqObject);
			requestsSent=1;			
			System.out.println(requestsSent + " multicast request sent");
		}
	}
	private void startPingTest() {
		String str = Log.getInstance().printLoggables();
		jTextAreaDump.setText(str);
		parseDump(str);		
		synchronized (lock) {
			requestsSent = 0;
			repliesReceived.clear();			
			RequestObject reqObject = new RequestObject();
			lastRequestId = reqObject.getId();
			
			// Send unicast request messages to all known peers			
			int addressesNumber = jComboBoxDest.getItemCount();
			for (int i=0;i<addressesNumber;i++){
				String ipStr = (String)jComboBoxDest.getItemAt(i);				
				Address localAddr = app.getLocalAddress();
				String addrStr = localAddr.toString();				
				if (!addrStr.contains(ipStr)){
					InetAddress destAddr = null;
					try {
						destAddr = InetAddress.getByName(ipStr);
					} catch (UnknownHostException e) {
						e.printStackTrace();
					}
					app.send(NetworkUtils.getJGroupsAddresFor(destAddr), 
							app.getLocalAddress(), reqObject);					
					requestsSent++;
				}
			}			
			System.out.println(requestsSent + " requests sent");
		}
	}
}
