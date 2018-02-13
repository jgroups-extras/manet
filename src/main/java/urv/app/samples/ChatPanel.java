package urv.app.samples;

import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import urv.app.messages.FileMessage;
import urv.conf.PropertiesLoader;
import urv.machannel.MChannel;
import urv.util.audio.AudioUtils;
import urv.util.network.NetworkUtils;

public class ChatPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private  String ChannelID = PropertiesLoader.getChannelId();  //  @jve:decl-index=0:

	private JButton jButtonSendMsg = null;
	private JButton jButtonSendMultiMsg = null;
	private JButton jButtonSendNeighborMsg = null;
	private JButton jButtonSendSound = null;
	private JButton jButtonBrowseFile = null;
	private JTextField jTextFieldChatMsg = null;
	private JLabel jLabelChatMsg = null;

	private String destAddr = null;
	private String groupAddr = null;
	private MChannel groupChannel = null;
	private JButton jButtonCloseChatPanel = null;
	private JScrollPane jScrollPaneChat = null;
	private JTextArea jTextAreaChatMsgs = null;
	
	private JTabbedPane tabPanel = null;
	private JPanel me = null;
	private JPanel jPanel1 = null;
	private JPanel jPanel2 = null;
	private java.io.File filetosend = null;
	
	/**
	 * This is the default constructor
	 */
	public ChatPanel(JTabbedPane tabPanel, String groupAddr,String destAddr, MChannel groupChannel) {
		super();

		this.groupAddr = groupAddr;
		this.destAddr = destAddr;
		this.groupChannel = groupChannel;
		this.tabPanel = tabPanel;
		
		this.me = this;

		initialize();
	}

	/*
	 * Method that adds a new message in the chat text area
	 */
	public void addChatMsg(String msg){
		jTextAreaChatMsgs.append(msg+"\n");
		jTextAreaChatMsgs.setCaretPosition(jTextAreaChatMsgs.getDocument().getLength());
	}

	/**
	 * Returns a FileMessage object filled witj the selected file content and name
	 * 
	 * @return FileMessage
	 */
	private FileMessage getFileMessageObjectFromSelectedFile(){
		FileMessage fileMessage = null;
		try {
			//Read the content of the message
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(filetosend));
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte [] content = new byte[1024];
			int readResult = 0;
			while ((readResult = in.read(content))>=0){
				out.write(content, 0, readResult);
			}
			fileMessage = new FileMessage(filetosend.getName(), out.toByteArray());
			//Close channels
			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fileMessage;
	}
	private JButton getJButtonBrowseFile(){
		if (jButtonBrowseFile == null) {
			final java.awt.Component c = this;
			filetosend  = null;
			jButtonBrowseFile = new JButton();
			jButtonBrowseFile.setBounds(new Rectangle(131, 480, 50, 21));
			jButtonBrowseFile.setText("+");
			jButtonBrowseFile.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					JFileChooser jfch = new JFileChooser();
					int res = jfch.showOpenDialog(c);
					filetosend = jfch.getSelectedFile();
					jTextFieldChatMsg.setText(filetosend.getAbsolutePath());
		        }		        	
			});	
		}
		return jButtonBrowseFile;
	}
	/**
	 * This method initializes jButtonCloseChatPanel
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonCloseChatPanel() {
		if (jButtonCloseChatPanel == null) {
			jButtonCloseChatPanel = new JButton();
			jButtonCloseChatPanel.setBounds(new Rectangle(413, 5, 14, 14));
			//TODO add to a folder inside the project
			jButtonCloseChatPanel.setIcon(AppTestUtil.getCloseIcon());
			jButtonCloseChatPanel.addActionListener(new java.awt.event.ActionListener() {
				//Before closing the tab chat, a warn dialog is shown
				public void actionPerformed(java.awt.event.ActionEvent e) {
					Object[] options = {"Yes","No"};
					int n = JOptionPane.showOptionDialog(
						    me,
						    "Are you sure you want to leave the chat?",
						    "Chat",
						    JOptionPane.YES_NO_OPTION,
						    JOptionPane.QUESTION_MESSAGE,
						    null,     //do not use a custom Icon
						    options,  //the titles of buttons
						    options[0]);
					if(n==0){
						tabPanel.remove(tabPanel.getSelectedComponent());
					}
				}
			});
		}
		return jButtonCloseChatPanel;
	}

	/**
	 * This method initializes jButtonSendMsg
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonSendMsg() {
		if (jButtonSendMsg == null) {
			jButtonSendMsg = new JButton();
			jButtonSendMsg.setBounds(new Rectangle(346, 510, 66, 21));
			jButtonSendMsg.setText("Send");
			jButtonSendMsg.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					sendMessage();
				}
			});
		}
		return jButtonSendMsg;
	}
	private JButton getJButtonSendMultiMsg(){
		if (jButtonSendMultiMsg == null) {
			jButtonSendMultiMsg = new JButton();
			jButtonSendMultiMsg.setBounds(new Rectangle(183, 480, 229, 21));
			jButtonSendMultiMsg.setText("Send 100 messages in a row!");
			jButtonSendMultiMsg.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					int i = 0;
					while (i<100){
						sendMessage("WOW! 100 Messages in a ROW!");
						i++;
					}
		        }		        	
			});	
		}
		return jButtonSendMultiMsg;
	}
	private JButton getJButtonSendNeighborMsg(){
		if (jButtonSendNeighborMsg == null) {
			jButtonSendNeighborMsg = new JButton();
			jButtonSendNeighborMsg.setBounds(new Rectangle(183, 480, 229, 21));
			jButtonSendNeighborMsg.setText("Send 100 messages in a row!");
			jButtonSendNeighborMsg.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					int i = 0;
					while (i<100){
						sendMessage("WOW! 100 Messages in a ROW!");
						i++;
					}
		        }		        	
			});	
		}
		return jButtonSendNeighborMsg;
	}
	
	/**
	 * This method initializes jScrollPaneChat
	 *
	 * @return javax.swing.JScrollPane
	 */
	private JScrollPane getJScrollPaneChat() {
		if (jScrollPaneChat == null) {
			jScrollPaneChat = new JScrollPane();
			jScrollPaneChat.setBounds(new Rectangle(33, 27, 360, 442));
			jScrollPaneChat.setViewportView(getJTextAreaChatMsgs2());

		}
		return jScrollPaneChat;
	}

	/**
	 * This method initializes jTextAreaChatMsgs
	 *
	 * @return javax.swing.JTextArea
	 */
	private JTextArea getJTextAreaChatMsgs2() {
		if (jTextAreaChatMsgs == null) {
			jTextAreaChatMsgs = new JTextArea();
			jTextAreaChatMsgs.setEditable(false);
		}
		return jTextAreaChatMsgs;
	}

	/**
	 * This method initializes jTextFieldChatMsg
	 *
	 * @return javax.swing.JTextField
	 */
	private JTextField getJTextFieldChatMsg() {
		if (jTextFieldChatMsg == null) {
			jTextFieldChatMsg = new JTextField();
			jTextFieldChatMsg.setBounds(new Rectangle(11, 510, 327, 24));
			jTextFieldChatMsg.addKeyListener(new java.awt.event.KeyAdapter() {
				public void keyTyped(java.awt.event.KeyEvent e) {
					if (e.getKeyChar()==e.VK_ENTER)
						sendMessage();
				}
			});
		}
		return jTextFieldChatMsg;
	}

	/**
	 * This method initializes this
	 *
	 * @return void
	 */
	private void initialize() {
		
		java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        jButtonSendMultiMsg = new javax.swing.JButton();
        jButtonSendNeighborMsg = new javax.swing.JButton();
        jButtonSendSound = new javax.swing.JButton();
        jButtonBrowseFile = new javax.swing.JButton();
        jLabelChatMsg = new javax.swing.JLabel();
        jTextFieldChatMsg = new javax.swing.JTextField();
        jButtonSendMsg = new javax.swing.JButton();
        jScrollPaneChat = new javax.swing.JScrollPane();
        jTextAreaChatMsgs = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        jButtonCloseChatPanel = new javax.swing.JButton();

        setLayout(new java.awt.BorderLayout(3, 8));

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jButtonSendMultiMsg.setText("Send 100 messages");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(jButtonSendMultiMsg, gridBagConstraints);
        
        jButtonSendNeighborMsg.setText("Send To Neighbors");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(jButtonSendNeighborMsg, gridBagConstraints);
        
        jButtonSendSound.setText("Send Sound!");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(jButtonSendSound, gridBagConstraints);

        jButtonBrowseFile.setText("Browse File");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(jButtonBrowseFile, gridBagConstraints);

        jLabelChatMsg.setText("Write here:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(jLabelChatMsg, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(jTextFieldChatMsg, gridBagConstraints);

        jButtonSendMsg.setText("Send");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(jButtonSendMsg, gridBagConstraints);

        add(jPanel1, java.awt.BorderLayout.SOUTH);

        jScrollPaneChat.setPreferredSize(new java.awt.Dimension(100, 300));

        jTextAreaChatMsgs.setColumns(1);
        jTextAreaChatMsgs.setRows(1);
        jScrollPaneChat.setViewportView(jTextAreaChatMsgs);

        add(jScrollPaneChat, java.awt.BorderLayout.CENTER);

        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        jButtonCloseChatPanel.setIcon(AppTestUtil.getCloseIcon());
        jButtonCloseChatPanel.setToolTipText("Close this chat");
        jButtonCloseChatPanel.setMaximumSize(new java.awt.Dimension(14, 15));
        jButtonCloseChatPanel.setMinimumSize(new java.awt.Dimension(14, 15));
        jButtonCloseChatPanel.setPreferredSize(new java.awt.Dimension(14, 15));
        jPanel2.add(jButtonCloseChatPanel);
      
        //Only add the close button when it's not the multicast chat
		if(!groupAddr.equals(destAddr))
        add(jPanel2, java.awt.BorderLayout.NORTH);
        
        
        /*============================ setup action listeners =================================*/
        
        jButtonSendMsg.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				sendMessage();
			}
		});
        jButtonSendNeighborMsg.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				sendMessage();
			}
		});
        jButtonSendMultiMsg.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				int i = 0;
				while (i<100){
					sendMessage("WOW! 100 Messages in a ROW!: "+i);
					i++;
				}
	        }		        	
		});
        jButtonSendSound.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				if(AudioUtils.isCapturing()){
					AudioUtils.stopAudioCapture();
					jButtonSendSound.setText("Send Audio");
				}else{
					AudioUtils.startAudioCapture(groupChannel, ChannelID, destAddr);
					jButtonSendSound.setText("Stop SendingAudio");
				}
	        }		        	
		});
		jButtonBrowseFile.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				JFileChooser jfch = new JFileChooser();
				jfch.showOpenDialog(null);
				filetosend = jfch.getSelectedFile();
				jTextFieldChatMsg.setText(filetosend.getAbsolutePath());
	        }		        	
		});
		jTextFieldChatMsg.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyTyped(java.awt.event.KeyEvent e) {
				if (e.getKeyChar()==e.VK_ENTER)
					sendMessage();
			}
		});
		jButtonCloseChatPanel.addActionListener(new java.awt.event.ActionListener() {
			//Before closing the tab chat, a warn dialog is shown
			public void actionPerformed(java.awt.event.ActionEvent e) {
				Object[] options = {"Yes","No"};
				int n = JOptionPane.showOptionDialog(
					    me,
					    "Are you sure you want to leave the chat?",
					    "Chat",
					    JOptionPane.YES_NO_OPTION,
					    JOptionPane.QUESTION_MESSAGE,
					    null,     //do not use a custom Icon
					    options,  //the titles of buttons
					    options[0]);
				if(n==0){
					tabPanel.remove(tabPanel.getSelectedComponent());
				}
			}
		});
	}

	/*
	 * Method that sends a new message through the MChannel
	 */
	private void sendMessage(){
		InetAddress addr = null;
		try {
			addr = InetAddress.getByName(destAddr);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}

		java.io.Serializable msg = null;
		String path = jTextFieldChatMsg.getText();
		//If we are sending a file
		if (filetosend != null && path.compareTo(filetosend.getAbsolutePath()) == 0){
			msg = getFileMessageObjectFromSelectedFile();
			jTextFieldChatMsg.setText("Sending File -> " + filetosend.getName());
		}else{
			msg = path;
		}		

		if (addr!=null){
			groupChannel.send(NetworkUtils.getJGroupsAddresFor(addr),
					groupChannel.getLocalAddress(), msg);
			//My own message is written in the chat
			jTextAreaChatMsgs.append(">> "+jTextFieldChatMsg.getText()+"\n");
			jTextAreaChatMsgs.setCaretPosition(jTextAreaChatMsgs.getDocument().getLength());
			

			jTextFieldChatMsg.setText("");
		}
	}

	private void sendMessage(String content){
		InetAddress addr = null;
		try {
			addr = InetAddress.getByName(destAddr);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		if (addr!=null){
			groupChannel.send(NetworkUtils.getJGroupsAddresFor(addr),
					groupChannel.getLocalAddress(), content);		
			//My own message is written in the chat
			jTextAreaChatMsgs.append(">> "+content+"\n");
			jTextAreaChatMsgs.setCaretPosition(jTextAreaChatMsgs.getDocument().getLength());
		}
	}

}  //  @jve:decl-index=0:visual-constraint="10,71"