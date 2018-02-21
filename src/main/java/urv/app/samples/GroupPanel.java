package urv.app.samples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.ReceiverAdapter;

import urv.app.messages.FileMessage;
import urv.app.messages.SoundMessage;
import urv.conf.PropertiesLoader;
import urv.machannel.MChannel;
import urv.util.audio.AudioUtils;
import urv.util.date.DateUtils;
import urv.util.file.FileUtils;


public class GroupPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	static int refreshMembersPeriod = 3;
	static int refreshPajekFilePeriod = 5;
	private static String ChannelID = PropertiesLoader.getChannelId();  //  @jve:decl-index=0:

	JTabbedPane jTabbedPaneChats = null;
	private JButton jButtonStartChat = null;
	JList<String> jListGroupMembers = null;  //  @jve:decl-index=0:visual-constraint="610,10"
	private JScrollPane jScrollPaneGroupMembers = null;
	private DefaultListModel<String> listModelMembers = null;  //  @jve:decl-index=0:visual-constraint="606,638"
	private Object lock = new Object();  //  @jve:decl-index=0:
	MChannel groupChannel = null;
	String groupAddr = "";  //  @jve:decl-index=0:
	private RefreshGroupMembersThread refreshThread = null;  //  @jve:decl-index=0:
	GraphPanel netGraphPanel = null;
	private JButton jButtonCloseGroupPanel = null;
	JTabbedPane tabPanel = null;
	
	JPanel me = null;
	private JTextField jTextFieldIpUnicast = null;
	private JLabel jLabelListMembers = null;
	JLabel jLabelNewMsg = null;
	int newMessages = 0;
	private JSplitPane jSplitPane1;
	private JPanel jPanel1;
	private JPanel jPanel2;
	private JPanel jPanel3;

	/**
	 * This is the default constructor
	 */
	public GroupPanel(JTabbedPane tabPanel, MChannel groupChannel, String groupAddr) {
		this.groupChannel=groupChannel;
		this.groupAddr = groupAddr;
		this.tabPanel = tabPanel;		
		this.me = this;
		initialize();
		registerMChannel();

	}
	/*
	 * Method used to close all the group tabs and remove the group tab panel
	 */
	public void closeGroupPanel(){
		int cnt=jTabbedPaneChats.getTabCount();
		for(int i=0; i<cnt; i++){
			((ChatPanel)jTabbedPaneChats.getComponentAt(0)).removeAll();
		}
		jTabbedPaneChats.removeAll();
		refreshThread.stop();
		groupChannel.setReceiver(null);
		groupChannel.close();
		tabPanel.remove(me);
	}

	public void goRefresh(){
		if(refreshThread == null){
			refreshThread= new RefreshGroupMembersThread();
		}
		refreshThread.start();
	}

	public void stopRefresh(){
		refreshThread.stop();
	}

	/*
	 * Method that adds a new member to the list only if it's not already in the list
	 */
	void addMemberToList(String inetAddr,DefaultListModel<String> list){
		synchronized(lock){
			int size = list.getSize();
			for (int i=0;i<size;i++){
				String element = list.getElementAt(i);
				if (element.equals(inetAddr)){
					return;
				}
			}
			list.addElement(inetAddr);

			jListGroupMembers.ensureIndexIsVisible(0);
		}
	}
	/**
	 * Method that test if the tab with a specific title exists
	 */
	int existTab(String title, boolean select){
		for(int i=0;i<jTabbedPaneChats.getTabCount();i++){
			if (jTabbedPaneChats.getTitleAt(i).equals(title)){
				if(select) jTabbedPaneChats.setSelectedIndex(i);
				return i;
			}
		}
		return -1;
	}	
	/**
	 * This method initializes listModelMembers
	 *
	 * @return javax.swing.DefaultListModel
	 */
	private DefaultListModel<String> getListModelMembers() {
		if (listModelMembers == null) {
			listModelMembers = new DefaultListModel<>();
		}
		return listModelMembers;
	}
	/**
	 * This method initializes this
	 *
	 * @return void
	 */
	private void initialize() {
		
		jSplitPane1 = new JSplitPane();
		jTabbedPaneChats = new JTabbedPane();
        netGraphPanel = new GraphPanel(groupChannel);
        jPanel1 = new JPanel();
        jLabelListMembers = new JLabel();
        jScrollPaneGroupMembers = new JScrollPane();
        jListGroupMembers = new JList<>(getListModelMembers());
        jPanel2 = new JPanel();
        jPanel3 = new JPanel();
        jButtonStartChat = new JButton();
        jLabelNewMsg = new JLabel();
        jButtonCloseGroupPanel = new JButton();

        setLayout(new java.awt.BorderLayout());

        jTabbedPaneChats.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        jSplitPane1.setLeftComponent(jTabbedPaneChats);

        jSplitPane1.setRightComponent(netGraphPanel);

        add(jSplitPane1, java.awt.BorderLayout.CENTER);

        jPanel3.setLayout(new FlowLayout(FlowLayout.RIGHT));

        jButtonCloseGroupPanel.setIcon(AppTestUtil.getCloseIcon()); // NOI18N
        jButtonCloseGroupPanel.setToolTipText("Close this chat");
        jButtonCloseGroupPanel.setMaximumSize(new Dimension(14, 15));
        jButtonCloseGroupPanel.setMinimumSize(new Dimension(14, 15));
        jButtonCloseGroupPanel.setPreferredSize(new Dimension(14, 15));
        jPanel3.add(jButtonCloseGroupPanel);

        add(jPanel3, BorderLayout.NORTH);

        jPanel1.setLayout(new BorderLayout(20, 20));

        jLabelListMembers.setText("Group members:");
        jPanel1.add(jLabelListMembers, BorderLayout.NORTH);

        jListGroupMembers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jListGroupMembers.setPreferredSize(new Dimension(130, 95));
        jScrollPaneGroupMembers.setViewportView(jListGroupMembers);

        jPanel1.add(jScrollPaneGroupMembers, BorderLayout.CENTER);

        jButtonStartChat.setText("Chat");
        jPanel2.add(jButtonStartChat);

        jPanel1.add(jPanel2, BorderLayout.SOUTH);

        add(jPanel1, BorderLayout.WEST);
        add(jLabelNewMsg, BorderLayout.SOUTH);
		
		//The multicast channel is creted and added in the chat tab panel by default
		jTabbedPaneChats.add(groupAddr, new ChatPanel(jTabbedPaneChats,groupAddr,groupAddr, groupChannel));
		jTabbedPaneChats.setSelectedIndex(jTabbedPaneChats.getTabCount()-1);
		
		getListModelMembers();

		/*============================ setup action listeners =================================*/
		
		jTabbedPaneChats.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {//When the selected tab changes
				if(jTabbedPaneChats.getTabCount()>0 && jTabbedPaneChats.getSelectedIndex()>=0){
					if(jTabbedPaneChats.getForegroundAt(jTabbedPaneChats.getSelectedIndex()).equals(Color.blue)){//If the new selection has a blue name, it's because it has a new message 
						jTabbedPaneChats.setForegroundAt(jTabbedPaneChats.getSelectedIndex(),Color.black);
						newMessages--;
						if(newMessages==0){//If there aren't more new messages
							jLabelNewMsg.setText("");//The warning of a new message is deleted
						}
					}						
				}
			}
		});
		
		jButtonStartChat.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
				startNewChat();
				
			}
		});
		
		jListGroupMembers.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount()==2){//When a double-click is done on a list element, a chat tab is opened
					startNewChat();
				}
			}
		});
		
		jButtonCloseGroupPanel.addActionListener(new ActionListener() {
			//Before closing the tab, show a dialog
			@Override
			public void actionPerformed(ActionEvent e) {
				Object[] options = {"Yes","No"};
				int n = JOptionPane.showOptionDialog(
					    me,
					    "Are you sure you want to leave the group?",
					    "Group",
					    JOptionPane.YES_NO_OPTION,
					    JOptionPane.QUESTION_MESSAGE,
					    null,     //do not use a custom Icon
					    options,  //the titles of buttons
					    options[0]);
				if(n==0){
					closeGroupPanel();
				}
			}
		});
	}
	/**
	 * Method that register the receptions of the messages that belong to that group
	 */
	private void registerMChannel(){
		Receiver messageListener = new ReceiverAdapter() {
			/*
			 * Action that is taken when one message is received
			 * @see org.jgroups.MessageListener#receive(org.jgroups.Message)
			 */
			@Override
			public void receive(Message msg) {
				Object obj = msg.getObject();
				if (obj instanceof String){//If we receive a string, it's a message of our application
					String str = (String)obj;
				
					String ip = msg.getSrc().toString().split(":")[0];
					if(msg.getDest().toString().split(":")[0].equals(groupAddr))//if it's a multicast we need to use the group address and not the source address
						ip=groupAddr;
					
					int numTab=existTab(ip,false);
					
					//if the message is from a new node, we need to open a new chat tab
					if(numTab==-1){
						jTabbedPaneChats.add(ip, new ChatPanel(jTabbedPaneChats,groupAddr,ip, groupChannel));
						numTab=jTabbedPaneChats.getTabCount()-1;
					}
					
					//If it's my own message, don't print it
					if (!((InetAddress) msg.getDest()).isMulticastAddress() || !msg.getSrc().equals(groupChannel.getLocalAddress())){
						((ChatPanel)jTabbedPaneChats.getComponentAt(numTab)).addChatMsg("["+shortName(msg.getSrc().toString())+"] "+str);
					}
					
					//If the message is for a tab which is not selected
					if(numTab != jTabbedPaneChats.getSelectedIndex()){
						if(!jTabbedPaneChats.getForegroundAt(numTab).equals(Color.blue)){
							jLabelNewMsg.setText("You have new messages from any of the group member");
							newMessages++;//We add the warning message
						}
						jTabbedPaneChats.setForegroundAt(numTab,Color.blue);//and put in blue color the name of the tab
					}
					
					//If the tab group that has received the message is not selected, we change the color of the tab's name
					if(!tabPanel.getTitleAt(tabPanel.getSelectedIndex()).equals(groupAddr)){
						for(int i=0; i<tabPanel.getTabCount(); i++){
							if(tabPanel.getTitleAt(i).equals(groupAddr)){
								tabPanel.setForegroundAt(i, Color.blue);
								break;
							}
						}
					}
				}else if (obj instanceof FileMessage){ //If we receive a file
					//Create the file with the data received
					FileUtils.generateReceivedFile((FileMessage)obj);
					//Print the advice into the screen
					String ip = msg.getSrc().toString().split(":")[0];
					//if it's a multicast we need to use the group address and not the source address
					if(msg.getDest().toString().split(":")[0].equals(groupAddr))
						ip=groupAddr;					
					jLabelNewMsg.setText("You have received a file named "
							+((FileMessage)obj).getFileName()+" from "+ip);
					newMessages++;//We add the warning message			
					
				}else if (obj instanceof SoundMessage){
					//Reproduce the sound data received
			        AudioUtils.playSoundData(((SoundMessage)obj).getContent());
		            System.out.println("Receiving Audio From: "+msg.getSrc());

				}else{
					System.err.println("Error: I received an"+obj);
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
		};
		groupChannel.setReceiver(messageListener);
	}

	/*
	 * Method that removes the elements from the listModelMembers that don't appear in the vInetAddr
	 * Remove the exceeded elements
	 */
	static void removeOldMembersFromList(List<Address> vInetAddr, DefaultListModel<String> list){
		boolean remove = true;

		for(int i=0; i<list.size();i++){
			remove=true;
			for(int j=0; j<vInetAddr.size(); j++){
				System.out.println(list.getElementAt(i)+" = "+vInetAddr.get(j).toString());
				if(list.getElementAt(i).equals(vInetAddr.get(j).toString())){
					remove = false;
					break;
				}
			}
			if(remove){//if the element is not found, it will be removed
				list.remove(i);
				i--;
			}
		}
	}
	
	/*
	 * Method that creats a new chat tab if it is necessary
	 */
	void startNewChat(){
		String addr = jListGroupMembers.getSelectedValue();
		if(addr.length()!=0){
			if(existTab(addr,true)==-1){//if the tab with that title exists, create a new chat tab
				jTabbedPaneChats.add(addr, new ChatPanel(jTabbedPaneChats,groupAddr,addr, groupChannel));
				jTabbedPaneChats.setSelectedIndex(jTabbedPaneChats.getTabCount()-1);
			}
		}
		jListGroupMembers.clearSelection();
	}
	/*
	 * Thread respondable of the refresh task
	 * This thread will refresh the list of members in the group, the graph when it changes and print the graph into a pajek file 
	 */
	private class RefreshGroupMembersThread extends Thread{

		private BufferedWriter f;
		
		public RefreshGroupMembersThread(){
		}

		@Override
		public void run() {
			int contMembers = -1;
			int contPajek = -1;
			String fileName = "";
			while (true) {
				try {
					contMembers ++;
					contPajek++;
					if(contMembers%refreshMembersPeriod == 0){//Every refresh member period
						contMembers=0;
						String elementSelected = jListGroupMembers.getSelectedValue();
	
						for(int i=0; i<groupChannel.getAddressesOfGroupMebers().size();i++){//adds the new members
							addMemberToList(groupChannel.getAddressesOfGroupMebers().get(i).toString(),listModelMembers);
						}						
						//remove the old members
						removeOldMembersFromList(groupChannel.getAddressesOfGroupMebers(), listModelMembers);	
						jListGroupMembers.setSelectedValue(elementSelected, true);
					}
					if(contPajek%refreshPajekFilePeriod==0){//Every file period
						contPajek=0;	
						fileName = writePajek();//create a new pajek file 	
						f.write(groupChannel.getNetworkGraph().toPajek("",false));
						f.close();
						netGraphPanel.loadGraphFromPajekFile(fileName);//and refreh the graph if it has change	
					}
					Thread.sleep(1000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		/*
		 * Method that helps to write the graph into a pajek file
		 */
		private String writePajek(){
			String fileName = "";
			try {
				//Gets the date to use it as a part of the file name
				String dateStr = DateUtils.getTimeFormatString();
				String dir = "runResults" + File.separator;
				File baseDir = new File(dir);
				//Create log directory
				if (!baseDir.exists())
					baseDir.mkdir();
				dateStr=dateStr.replace(":",".");
				//create the file name
				fileName = dir+dateStr+"_"+groupAddr+"_"+groupChannel.getLocalAddress().toString().split(":")[0]+".net";
				//creates the file tht will be use to write in the information.
				f = new BufferedWriter(new FileWriter(new File(fileName)));
				System.currentTimeMillis();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return fileName;
		}
	}
}  //  @jve:decl-index=0:visual-constraint="10,10"