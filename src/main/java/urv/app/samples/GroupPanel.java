package urv.app.samples;

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

import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;


public class GroupPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private static int refreshMembersPeriod = 3;
	private static int refreshPajekFilePeriod = 5;
	private static String ChannelID = PropertiesLoader.getChannelId();  //  @jve:decl-index=0:

	private JTabbedPane jTabbedPaneChats = null;
	private JButton jButtonStartChat = null;
	private JList jListGroupMembers = null;  //  @jve:decl-index=0:visual-constraint="610,10"
	private JScrollPane jScrollPaneGroupMembers = null;
	private DefaultListModel listModelMembers = null;  //  @jve:decl-index=0:visual-constraint="606,638"
	private Object lock = new Object();  //  @jve:decl-index=0:
	private MChannel groupChannel = null;
	private String groupAddr = "";  //  @jve:decl-index=0:
	private RefreshGroupMembersThread refreshThread = null;  //  @jve:decl-index=0:
	private GraphPanel netGraphPanel = null;
	private JButton jButtonCloseGroupPanel = null;
	private JTabbedPane tabPanel = null;
	
	private JPanel me = null;
	private JTextField jTextFieldIpUnicast = null;
	private JLabel jLabelListMembers = null;
	private JLabel jLabelNewMsg = null;
	private int newMessages = 0;
	private JSplitPane jSplitPane1;
	private JPanel jPanel1;
	private JPanel jPanel2;
	private JPanel jPanel3;

	/**
	 * This is the default constructor
	 */
	public GroupPanel(JTabbedPane tabPanel, MChannel groupChannel, String groupAddr) {
		super();
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
	private void addMemberToList(String inetAddr,DefaultListModel list){
		synchronized(lock){
			int size = list.getSize();
			for (int i=0;i<size;i++){
				String element = (String)list.getElementAt(i);
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
	private int existTab(String title, boolean select){
		for(int i=0;i<jTabbedPaneChats.getTabCount();i++){
			if (jTabbedPaneChats.getTitleAt(i).equals(title)){
				if(select) jTabbedPaneChats.setSelectedIndex(i);
				return i;
			}
		}
		return -1;
	}	
	/**
	 * This method initializes GraphPanel
	 *
	 * @return javax.swing.JTabbedPane
	 */
	private GraphPanel getGraphPanel() {
		if (netGraphPanel == null) {
			netGraphPanel = new GraphPanel(groupChannel);
			netGraphPanel.setBounds(new Rectangle(579, 25, 400, 574));
		}
		return netGraphPanel;
	}
	/**
	 * This method initializes jButtonCloseGroupPanel
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonCloseGroupPanel() {
		if (jButtonCloseGroupPanel == null) {
			jButtonCloseGroupPanel = new JButton();
			jButtonCloseGroupPanel.setBounds(new Rectangle(964, 5, 14, 14));
			jButtonCloseGroupPanel.setIcon(AppTestUtil.getCloseIcon());
			jButtonCloseGroupPanel.addActionListener(new java.awt.event.ActionListener() {
				//Before closing the tab, show a dialog
				public void actionPerformed(java.awt.event.ActionEvent e) {
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
		return jButtonCloseGroupPanel;
	}

	/**
	 * This method initializes jButtonStartChat
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonStartChat() {
		if (jButtonStartChat == null) {
			jButtonStartChat = new JButton();
			jButtonStartChat.setBounds(new Rectangle(35, 560, 72, 23));
			jButtonStartChat.setText("Chat");
			jButtonStartChat.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					
					startNewChat();
					
				}
			});
		}
		return jButtonStartChat;
	}
	/**
	 * This method initializes jListGroupMembers
	 *
	 * @return javax.swing.JList
	 */
	private JList getJListGroupMembers() {
		if (jListGroupMembers == null) {
			jListGroupMembers = new JList(getListModelMembers());
			jListGroupMembers.setBounds(new Rectangle(16, 14, 120, 500));
			jListGroupMembers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			
			jListGroupMembers.addMouseListener(new java.awt.event.MouseAdapter() {
				public void mouseClicked(java.awt.event.MouseEvent e) {
					if(e.getClickCount()==2){//When a double-click is done on a list element, a chat tab is opened
						startNewChat();
					}
				}
			});
		}

		return jListGroupMembers;
	}

	/**
	 * This method initializes jTabbedPaneChats
	 *
	 * @return javax.swing.JTabbedPane
	 */
	private JTabbedPane getJTabbedPaneChats() {
		if (jTabbedPaneChats == null) {
			jTabbedPaneChats = new JTabbedPane();
			jTabbedPaneChats.setBounds(new Rectangle(143, 15, 400, 200));
			jTabbedPaneChats.addChangeListener(new javax.swing.event.ChangeListener() {
				public void stateChanged(javax.swing.event.ChangeEvent e) {//When the selected tab changes
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
			jTabbedPaneChats.setTabLayoutPolicy(jTabbedPaneChats.SCROLL_TAB_LAYOUT);
			
		}
		return jTabbedPaneChats;
	}
	/**
	 * This method initializes jTextFieldIpUnicast	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTextFieldIpUnicast() {
		if (jTextFieldIpUnicast == null) {
			jTextFieldIpUnicast = new JTextField();
			jTextFieldIpUnicast.setBounds(new Rectangle(16, 530, 120, 23));
		}
		return jTextFieldIpUnicast;
	}
	/**
	 * This method initializes listModelMembers
	 *
	 * @return javax.swing.DefaultListModel
	 */
	private DefaultListModel getListModelMembers() {
		if (listModelMembers == null) {
			listModelMembers = new DefaultListModel();
		}
		return listModelMembers;
	}
	/**
	 * This method initializes this
	 *
	 * @return void
	 */
	private void initialize() {
		
		jSplitPane1 = new javax.swing.JSplitPane();
		jTabbedPaneChats = new javax.swing.JTabbedPane();
        netGraphPanel = new GraphPanel(groupChannel);;
        jPanel1 = new javax.swing.JPanel();
        jLabelListMembers = new javax.swing.JLabel();
        jScrollPaneGroupMembers = new javax.swing.JScrollPane();
        jListGroupMembers = new javax.swing.JList(getListModelMembers());
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jButtonStartChat = new javax.swing.JButton();
        jLabelNewMsg = new javax.swing.JLabel();
        jButtonCloseGroupPanel = new javax.swing.JButton();

        setLayout(new java.awt.BorderLayout());

        jTabbedPaneChats.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        jSplitPane1.setLeftComponent(jTabbedPaneChats);

        jSplitPane1.setRightComponent(netGraphPanel);

        add(jSplitPane1, java.awt.BorderLayout.CENTER);

        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        jButtonCloseGroupPanel.setIcon(AppTestUtil.getCloseIcon()); // NOI18N
        jButtonCloseGroupPanel.setToolTipText("Close this chat");
        jButtonCloseGroupPanel.setMaximumSize(new java.awt.Dimension(14, 15));
        jButtonCloseGroupPanel.setMinimumSize(new java.awt.Dimension(14, 15));
        jButtonCloseGroupPanel.setPreferredSize(new java.awt.Dimension(14, 15));
        jPanel3.add(jButtonCloseGroupPanel);

        add(jPanel3, java.awt.BorderLayout.NORTH);

        jPanel1.setLayout(new java.awt.BorderLayout(20, 20));

        jLabelListMembers.setText("Group members:");
        jPanel1.add(jLabelListMembers, java.awt.BorderLayout.NORTH);

        jListGroupMembers.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jListGroupMembers.setPreferredSize(new java.awt.Dimension(130, 95));
        jScrollPaneGroupMembers.setViewportView(jListGroupMembers);

        jPanel1.add(jScrollPaneGroupMembers, java.awt.BorderLayout.CENTER);

        jButtonStartChat.setText("Chat");
        jPanel2.add(jButtonStartChat);

        jPanel1.add(jPanel2, java.awt.BorderLayout.SOUTH);

        add(jPanel1, java.awt.BorderLayout.WEST);
        add(jLabelNewMsg, java.awt.BorderLayout.SOUTH);
		
		//The multicast channel is creted and added in the chat tab panel by default
		jTabbedPaneChats.add(groupAddr, new ChatPanel(jTabbedPaneChats,groupAddr,groupAddr, groupChannel));
		jTabbedPaneChats.setSelectedIndex(jTabbedPaneChats.getTabCount()-1);
		
		getListModelMembers();

		/*============================ setup action listeners =================================*/
		
		jTabbedPaneChats.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent e) {//When the selected tab changes
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
		
		jButtonStartChat.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				
				startNewChat();
				
			}
		});
		
		jListGroupMembers.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent e) {
				if(e.getClickCount()==2){//When a double-click is done on a list element, a chat tab is opened
					startNewChat();
				}
			}
		});
		
		jButtonCloseGroupPanel.addActionListener(new java.awt.event.ActionListener() {
			//Before closing the tab, show a dialog
			public void actionPerformed(java.awt.event.ActionEvent e) {
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
	/*
	 * Method that helps to know if the addr is a correct ip
	 */
	private boolean isCorrectIp(String addr){
		// if addr is empty or there's an exception, it's not a correct ip
		try {
			if(addr.length()==0) return false; 
			InetAddress.getByName(addr);
			return true;
		} catch (UnknownHostException e) {
			return false;
		}
	}

	/**
	 * Method that register the receptions of the messages that belong to that group
	 */
	private void registerMChannel(){
		Receiver messageListener = new ReceiverAdapter() {
			public byte[] getState() {
				return null;
			}
			/*
			 * Action that is taken when one message is received
			 * @see org.jgroups.MessageListener#receive(org.jgroups.Message)
			 */
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
					if (msg != null || !msg.getSrc().equals(groupChannel.getLocalAddress())){
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
			public void setState(byte[] state) {
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
	private void removeOldMembersFromList(List<InetAddress> vInetAddr, DefaultListModel list){
		boolean remove = true;

		for(int i=0; i<list.size();i++){
			remove=true;
			for(int j=0; j<vInetAddr.size(); j++){
				//System.out.println((String)list.getElementAt(i)+" = "+vInetAddr.get(j).getHostAddress());
				if(((String)list.getElementAt(i)).equals(vInetAddr.get(j).getHostAddress())){
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
	private void startNewChat(){
		String addr = (String)jListGroupMembers.getSelectedValue();
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
		private long initialTime;

		public RefreshGroupMembersThread(){
		}

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
						String elementSelected = (String)jListGroupMembers.getSelectedValue();
	
						for(int i=0; i<groupChannel.getInetAddressesOfGroupMebers().size();i++){//adds the new members
							addMemberToList(groupChannel.getInetAddressesOfGroupMebers().get(i).getHostAddress(),listModelMembers);
						}						
						//remove the old members
						removeOldMembersFromList(groupChannel.getInetAddressesOfGroupMebers(), listModelMembers);	
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
				initialTime = System.currentTimeMillis();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return fileName;
		}
	}
}  //  @jve:decl-index=0:visual-constraint="10,10"