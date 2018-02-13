package urv.app.samples;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Random;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.jgroups.Message;
import org.jgroups.MessageListener;
import org.jgroups.stack.IpAddress;

import urv.app.Application;
import urv.app.messages.ReplyObject;
import urv.app.messages.RequestObject;
import urv.log.Log;
import urv.machannel.MChannel;
import urv.olsr.mcast.MulticastAddress;
import urv.util.network.NetworkUtils;

/**
 * @author Gerard Paris Aixala
 *
 */
public class MembershipTest extends Application {

	private JFrame parent;
	
	// fields
	private Hashtable<String,MChannel> applications = new Hashtable<String,MChannel>();  //  @jve:decl-index=0:
	private String groupToShow = "224.0.0.10";  //  @jve:decl-index=0:
	private DefaultListModel modelListMembers = new DefaultListModel();
	private Object lock = new Object();  //  @jve:decl-index=0:
	private Random rand = new Random();
	
	private String name = "MMT";
	
	private JPanel jContentPane = null;
	private JPanel jPanelDown = null;
	private JTextField jTextFieldGroup = null;
	private JButton jButtonJoin = null;
	private JButton jButtonLeave = null;
	private JLabel jLabelGroup = null;
	private JPanel jPanelUp = null;
	private JComboBox jComboBoxGroup = null;
	private JList jListMembers = null;
	private JLabel jLabelMembers = null;
	private JLabel jLabelGroupSize = null;
	private JPanel jPanelMembersTitle = null;
	private JButton jButtonClear = null;
	private JPanel jPanelGroupSelection = null;

	/**
	 * This is the default constructor
	 */
	public MembershipTest() {
		super();
		parent = new JFrame();
		initialize();		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				MembershipTest thisClass = new MembershipTest();
				thisClass.parent.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				thisClass.parent.setVisible(true);
			}
		});
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		this.parent.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.parent.setVisible(true);
		
		createApp("224.0.0.10");
		pingingThread();
	}

	private void addMemberToList(String mcastAddr){
		synchronized(lock){
			int size = modelListMembers.getSize();
			for (int i=0;i<size;i++){
				String element = (String)modelListMembers.getElementAt(i);
				if (element.equals(mcastAddr)){
					return;
				}
			}
			modelListMembers.addElement(mcastAddr);
			
			jListMembers.ensureIndexIsVisible(0);
		}
	}

	private void createApp(String mcastAddr){
		Log.getInstance().setCurrentLevel(Log.INFO);
		
		MulticastAddress multicastAddress = new MulticastAddress();
		multicastAddress.setValue(mcastAddr);
		
		MChannel app = createMChannel(multicastAddress);
		app.registerListener(name,new PingingMessageListener(app));
		
		synchronized(lock){
			applications.put(mcastAddr,app);
		}
	}

	/**
	 * This method initializes jButtonClear	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButtonClear() {
		if (jButtonClear == null) {
			jButtonClear = new JButton();
			jButtonClear.setText("Clear");
			jButtonClear.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					String selectedGroup = (String)jComboBoxGroup.getSelectedItem();
					preShowingMembers(selectedGroup);
					
					System.out.println("VIEW FROM MCHANNEL: "+applications.get(selectedGroup).getView());
				}
			});
		}
		return jButtonClear;
	}

	/**
	 * This method initializes jButtonJoin	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButtonJoin() {
		if (jButtonJoin == null) {
			jButtonJoin = new JButton();
			jButtonJoin.setText("Join");
			jButtonJoin.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					joinGroup();
				}
			});
		}
		return jButtonJoin;
	}

	/**
	 * This method initializes jButtonLeave	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButtonLeave() {
		if (jButtonLeave == null) {
			jButtonLeave = new JButton();
			jButtonLeave.setText("Leave");
			jButtonLeave.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					leaveGroup();
				}

			});
		}
		return jButtonLeave;
	}

	/**
	 * This method initializes jComboBoxGroup	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getJComboBoxGroup() {
		if (jComboBoxGroup == null) {
			jComboBoxGroup = new JComboBox();
			jComboBoxGroup.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					JComboBox cb = (JComboBox)e.getSource();
			        String selectedGroup = (String)cb.getSelectedItem();
					preShowingMembers(selectedGroup);
				}
			});
		}
		return jComboBoxGroup;
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
			gridBagConstraints8.gridx = 0;
			gridBagConstraints8.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints8.gridy = 1;
			GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
			gridBagConstraints7.gridx = 0;
			gridBagConstraints7.fill = GridBagConstraints.BOTH;
			gridBagConstraints7.weightx = 1.0;
			gridBagConstraints7.weighty = 1.0;
			gridBagConstraints7.gridy = 0;
			jContentPane = new JPanel();
			jContentPane.setLayout(new GridBagLayout());
			jContentPane.add(getJPanelUp(), gridBagConstraints7);
			jContentPane.add(getJPanelDown(), gridBagConstraints8);
		}
		return jContentPane;
	}

	/**
	 * This method initializes jLabelGroup	
	 * 	
	 * @return javax.swing.JLabel	
	 */
	private JLabel getJLabelGroup() {
		if (jLabelGroup == null) {
			jLabelGroup = new JLabel();
			jLabelGroup.setText("Groups");
		}
		return jLabelGroup;
	}

	/**
	 * This method initializes jLabelGroupSize	
	 * 	
	 * @return javax.swing.JLabel	
	 */
	private JLabel getJLabelGroupSize() {
		if (jLabelGroupSize == null) {
			jLabelGroupSize = new JLabel();
			jLabelGroupSize.setText("0");
		}
		return jLabelGroupSize;
	}

	/**
	 * This method initializes jLabelMembers	
	 * 	
	 * @return javax.swing.JLabel	
	 */
	private JLabel getJLabelMembers() {
		if (jLabelMembers == null) {
			jLabelMembers = new JLabel();
			jLabelMembers.setText("Members");
		}
		return jLabelMembers;
	}

	/**
	 * This method initializes jListMembers	
	 * 	
	 * @return javax.swing.JList	
	 */
	private JList getJListMembers() {
		if (jListMembers == null) {
			modelListMembers.addListDataListener(new ListDataListener(){

				public void contentsChanged(ListDataEvent evt) {
				}

				public void intervalAdded(ListDataEvent evt) {
					jLabelGroupSize.setText(modelListMembers.size()+"");
				}

				public void intervalRemoved(ListDataEvent evt) {
					jLabelGroupSize.setText(modelListMembers.size()+"");
				}
				
			});
				
			
			jListMembers = new JList(modelListMembers);
		}
		return jListMembers;
	}

	/**
	 * This method initializes jPanelDown	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanelDown() {
		if (jPanelDown == null) {
			GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.gridx = 2;
			gridBagConstraints2.insets = new Insets(5, 0, 5, 5);
			gridBagConstraints2.gridy = 0;
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.gridx = 1;
			gridBagConstraints1.insets = new Insets(5, 0, 5, 5);
			gridBagConstraints1.gridy = 0;
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.fill = GridBagConstraints.BOTH;
			gridBagConstraints.gridy = 0;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.insets = new Insets(5, 5, 5, 5);
			gridBagConstraints.gridx = 0;
			jPanelDown = new JPanel();
			jPanelDown.setLayout(new GridBagLayout());
			jPanelDown.add(getJTextFieldGroup(), gridBagConstraints);
			jPanelDown.add(getJButtonJoin(), gridBagConstraints1);
			jPanelDown.add(getJButtonLeave(), gridBagConstraints2);
		}
		return jPanelDown;
	}

	/**
	 * This method initializes jPanelGroupSelection	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanelGroupSelection() {
		if (jPanelGroupSelection == null) {
			GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
			gridBagConstraints9.gridx = 0;
			gridBagConstraints9.insets = new Insets(5, 0, 5, 5);
			gridBagConstraints9.anchor = GridBagConstraints.SOUTHEAST;
			gridBagConstraints9.gridy = 1;
			GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
			gridBagConstraints4.anchor = GridBagConstraints.NORTH;
			gridBagConstraints4.insets = new Insets(5, 5, 0, 5);
			gridBagConstraints4.gridx = 0;
			gridBagConstraints4.gridy = 0;
			gridBagConstraints4.weightx = 1.0;
			gridBagConstraints4.weighty = 1.0;
			gridBagConstraints4.fill = GridBagConstraints.HORIZONTAL;
			jPanelGroupSelection = new JPanel();
			jPanelGroupSelection.setLayout(new GridBagLayout());
			jPanelGroupSelection.add(getJComboBoxGroup(), gridBagConstraints4);
			jPanelGroupSelection.add(getJButtonClear(), gridBagConstraints9);
		}
		return jPanelGroupSelection;
	}

	/**
	 * This method initializes jPanelMembersTitle	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanelMembersTitle() {
		if (jPanelMembersTitle == null) {
			jPanelMembersTitle = new JPanel();
			jPanelMembersTitle.setLayout(new FlowLayout());
			jPanelMembersTitle.add(getJLabelMembers(), null);
			jPanelMembersTitle.add(getJLabelGroupSize(), null);
		}
		return jPanelMembersTitle;
	}
	
	// MembershipTest methods
	
	/**
	 * This method initializes jPanelUp	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanelUp() {
		if (jPanelUp == null) {
			GridBagConstraints gridBagConstraints10 = new GridBagConstraints();
			gridBagConstraints10.gridx = 0;
			gridBagConstraints10.weightx = 0.5;
			gridBagConstraints10.fill = GridBagConstraints.BOTH;
			gridBagConstraints10.gridy = 1;
			GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
			gridBagConstraints5.gridx = 1;
			gridBagConstraints5.gridy = 0;
			GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
			gridBagConstraints6.fill = GridBagConstraints.BOTH;
			gridBagConstraints6.gridy = 1;
			gridBagConstraints6.weightx = 1.0;
			gridBagConstraints6.weighty = 1.0;
			gridBagConstraints6.insets = new Insets(5, 5, 5, 5);
			gridBagConstraints6.gridx = 1;
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.gridx = 0;
			gridBagConstraints3.insets = new Insets(5, 0, 0, 0);
			gridBagConstraints3.gridy = 0;
			jPanelUp = new JPanel();
			jPanelUp.setLayout(new GridBagLayout());
			jPanelUp.add(getJLabelGroup(), gridBagConstraints3);
			jPanelUp.add(getJListMembers(), gridBagConstraints6);
			jPanelUp.add(getJPanelMembersTitle(), gridBagConstraints5);
			jPanelUp.add(getJPanelGroupSelection(), gridBagConstraints10);
		}
		return jPanelUp;
	}
	
	/**
	 * This method initializes jTextFieldGroup	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTextFieldGroup() {
		if (jTextFieldGroup == null) {
			jTextFieldGroup = new JTextField();
		}
		return jTextFieldGroup;
	}
	
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.parent.setSize(520, 272);
		this.parent.setContentPane(getJContentPane());
		this.parent.setTitle("Membership test");
	}
	
	private boolean isInComboBox(String mcastAddr){
		
		int size = jComboBoxGroup.getItemCount();
		for (int i=0;i<size;i++){
			String item = (String)jComboBoxGroup.getItemAt(i);
			if (item.equals(mcastAddr)){
				return true;
			}
		}
		return false;
		
	}
	
	private void joinGroup() {
		String mcastAddr = jTextFieldGroup.getText();
		createApp(mcastAddr);
	}
	
	private void leaveGroup() {
		String mcastAddr = jTextFieldGroup.getText();
		MChannel app = applications.get(mcastAddr);
		if (app!=null){
			app.close();			
			if (groupToShow.equals(mcastAddr)){
				groupToShow=null;
			}
			synchronized(lock){
				applications.remove(mcastAddr);
			}
		}
	}
	
	private void pingingThread() {
		
		new Thread() {
			
			public void run() {
				while (true) {

					try {
						Thread.sleep(3000);

					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					// Update jComboBoxGroup:
					updateComboBoxGroup();
					
					
					// Pinging thread for the groupToShow
					if (groupToShow!=null){
						RequestObject req = new RequestObject();
						
						MChannel app = applications.get(groupToShow);
						InetAddress dest = null;
						try {
							dest = InetAddress.getByName(groupToShow);
						} catch (UnknownHostException e) {
							e.printStackTrace();
						}
						app.send(NetworkUtils.getJGroupsAddresFor(dest), app.getLocalAddress(), req);
					}
					
				}
			}
		}.start();
		
			
	}

	
	private void preShowingMembers(String group){
		synchronized(lock){
			modelListMembers.removeAllElements();
			
			groupToShow = group;
		}
	}
	
	private void updateComboBoxGroup(){
		synchronized(lock){
			for (String mcastAddr : applications.keySet()){
				if (!isInComboBox(mcastAddr)){
					jComboBoxGroup.addItem(mcastAddr);
				}
			}
			
			int size = jComboBoxGroup.getItemCount();
			for (int i=0;i<size;i++){
				String item = (String)jComboBoxGroup.getItemAt(i);
				if (!applications.keySet().contains(item)){
					jComboBoxGroup.removeItemAt(i);
				}
			}
		}
	}

	private class PingingMessageListener implements MessageListener{
		
		MChannel app;
		
		public PingingMessageListener(MChannel app){
			this.app = app;
		}
		

		public byte[] getState() {
			return null;
		}

		public void receive(Message msg) {
			Object obj = msg.getObject();
			if (obj instanceof RequestObject){
				RequestObject req = (RequestObject)obj;
				
				ReplyObject rep = new ReplyObject(req.getId());
				InetAddress dest = ((IpAddress)msg.getSrc()).getIpAddress();
				
				app.send(NetworkUtils.getJGroupsAddresFor(dest), app.getLocalAddress(), rep);
			}
			else if (obj instanceof ReplyObject){
				InetAddress addr = ((IpAddress)msg.getSrc()).getIpAddress();
				addMemberToList(addr.getHostAddress());
			}
		}

		public void setState(byte[] state) {
		}
		
	}
	
	
}  //  @jve:decl-index=0:visual-constraint="10,10"
