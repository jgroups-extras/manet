package urv.app.samples;

import urv.app.Application;
import urv.log.gui.SwingAppenderUI;
import urv.machannel.MChannel;
import urv.olsr.mcast.MulticastAddress;

import javax.swing.*;
import java.awt.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

public class AppTest extends Application{

	private JFrame jFrame = null;  //  @jve:decl-index=0:visual-constraint="10,10"

	private JPanel jContentPane = null;

	private JButton jButtonCreateGroup = null;

	private JTabbedPane jTabbedPaneGroups = null;

	private JLabel jLabelMAddr = null;

	private JTextField jTextFieldGroupMAddr = null;

	private JButton jButtonDump = null;
	
	//private boolean dumping = false;
	
	private final DumpingFrame dumpingFrame = new DumpingFrame();

	private JLabel jLabelLocalIP = null;
	
	private JPanel jPanel1 = null;


	/**
	 * Launches this application
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
            AppTest application = new AppTest();
            application.getJFrame().setVisible(true);
        });
	}

	@Override
	public void start() {
		getJFrame().setVisible(true);

	}

	/**
	 * Method that verify if the group multicast Address is correct
	 */
	private static boolean checkGroupMAddr(String mAddr){
		try {
			return (InetAddress.getByName(mAddr)).isMulticastAddress();
		} catch (UnknownHostException e) {
			//e.printStackTrace();
			return false;
		}

	}

	/**
	 * Method that test if the tab with a specific title exists
	 */
	private boolean existTab(String title){
		for(int i=0;i<jTabbedPaneGroups.getTabCount();i++){
			if (jTabbedPaneGroups.getTitleAt(i).compareTo(title)==0){
				jTabbedPaneGroups.setSelectedIndex(i);
				return true;
			}
		}

		return false;
	}

	/**
	 * This method initializes jButtonCreateGroup
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonCreateGroup() {
		if (jButtonCreateGroup == null) {
			jButtonCreateGroup = new JButton();
			jButtonCreateGroup.setBounds(new Rectangle(310, 13, 72, 23));
			jButtonCreateGroup.setText("Join");
			jButtonCreateGroup.addActionListener(e -> {

                if(checkGroupMAddr(jTextFieldGroupMAddr.getText())){//If it's a correct multicast address
                    if(!existTab(jTextFieldGroupMAddr.getText())){//If the tab doesn't exit, a new group panel is added and selected
                        jTabbedPaneGroups.add(jTextFieldGroupMAddr.getText(), new GroupPanel(jTabbedPaneGroups,startGroup(jTextFieldGroupMAddr.getText()),jTextFieldGroupMAddr.getText()));
                        jTabbedPaneGroups.setSelectedIndex(jTabbedPaneGroups.getTabCount()-1);
                        ((GroupPanel)jTabbedPaneGroups.getSelectedComponent()).goRefresh();
                    }

                }else{
                    showErrorMessage("Incorrect Multicast Address "+jTextFieldGroupMAddr.getText());
                }
                jTextFieldGroupMAddr.setText("");
            });
		}
		return jButtonCreateGroup;
	}
	/**
	 * This method initializes jButtonDump	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButtonDump() {
		if (jButtonDump == null) {
			jButtonDump = new JButton();
			jButtonDump.setBounds(new Rectangle(927, 13, 72, 23));
			jButtonDump.setText("Dump");
			jButtonDump.addActionListener(e -> dumpingFrame.setVisible(true));
		}
		return jButtonDump;
	}

	/**
	 * This method initializes jContentPane
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			
			java.awt.GridBagConstraints gridBagConstraints;

	        jContentPane = new javax.swing.JPanel();
	        jTabbedPaneGroups = new javax.swing.JTabbedPane();
	        jPanel1 = new javax.swing.JPanel();
	        jButtonCreateGroup = new javax.swing.JButton();
	        jLabelLocalIP = new javax.swing.JLabel();
	        jTextFieldGroupMAddr = new javax.swing.JTextField();
	        jButtonDump = new javax.swing.JButton();
	        jLabelMAddr = new javax.swing.JLabel();
	        
	        jContentPane.setLayout(new java.awt.BorderLayout());
	        
	        jTabbedPaneGroups.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
	        jContentPane.add(jTabbedPaneGroups, java.awt.BorderLayout.CENTER);

	        jPanel1.setLayout(new java.awt.GridBagLayout());

	        jButtonCreateGroup.setText("Join");
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 2;
	        gridBagConstraints.gridy = 0;
	        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
	        jPanel1.add(jButtonCreateGroup, gridBagConstraints);

	        jLabelLocalIP.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 3;
	        gridBagConstraints.gridy = 0;
	        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
	        gridBagConstraints.weightx = 0.5;
	        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
	        jPanel1.add(jLabelLocalIP, gridBagConstraints);

	        jTextFieldGroupMAddr.setColumns(11);
	        jTextFieldGroupMAddr.setText("225.222.222.222");
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 1;
	        gridBagConstraints.gridy = 0;
	        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
	        jPanel1.add(jTextFieldGroupMAddr, gridBagConstraints);

	        jButtonDump.setText("Dump");
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 5;
	        gridBagConstraints.gridy = 0;
	        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
	        jPanel1.add(jButtonDump, gridBagConstraints);

	        jLabelMAddr.setText("Group (Multicast Address):");
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy = 0;
	        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
	        jPanel1.add(jLabelMAddr, gridBagConstraints);

	        jContentPane.add(jPanel1, java.awt.BorderLayout.NORTH);
	        
	        /*============================ setup action listeners =================================*/
	        
	        jButtonCreateGroup.addActionListener(e -> {

                if(checkGroupMAddr(jTextFieldGroupMAddr.getText())){//If it's a correct multicast address
                    if(!existTab(jTextFieldGroupMAddr.getText())){//If the tab doesn't exit, a new group panel is added and selected
                        jTabbedPaneGroups.add(jTextFieldGroupMAddr.getText(), new GroupPanel(jTabbedPaneGroups,startGroup(jTextFieldGroupMAddr.getText()),jTextFieldGroupMAddr.getText()));
                        jTabbedPaneGroups.setSelectedIndex(jTabbedPaneGroups.getTabCount()-1);
                        ((GroupPanel)jTabbedPaneGroups.getSelectedComponent()).goRefresh();
                    }
                    //TODO: if exists the tab set it foreground
                }else{
                    showErrorMessage("Incorrect Multicast Address "+jTextFieldGroupMAddr.getText());
                }
                jTextFieldGroupMAddr.setText("");
            });
	        jTabbedPaneGroups.addChangeListener(e -> {
                if(jTabbedPaneGroups.getTabCount()>0 && jTabbedPaneGroups.getSelectedIndex()>=0)
                    jTabbedPaneGroups.setForegroundAt(jTabbedPaneGroups.getSelectedIndex(),Color.black);
            });
			jButtonDump.addActionListener(e -> dumpingFrame.setVisible(true));
		}
		return jContentPane;
	}

	/**
	 * This method initializes jFrame
	 *
	 * @return javax.swing.JFrame
	 */
	private JFrame getJFrame() {
		if (jFrame == null) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e) {
				e.printStackTrace();
			}
			JSplitPane splitPane = new JSplitPane();
			splitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
			splitPane.setResizeWeight(0.8);
			jFrame = new JFrame();
			jFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			jFrame.setResizable(true);
			splitPane.setTopComponent(getJContentPane());
			splitPane.setBottomComponent(getLogPane());
			jFrame.setContentPane(splitPane);
			jFrame.setTitle("AppTest");
			//Ask before closing the application
			jFrame.addWindowListener(new java.awt.event.WindowAdapter() {
				public void windowClosing(java.awt.event.WindowEvent e) {

					Object[] options = {"Yes","No"};
					int n = JOptionPane.showOptionDialog(
						    jFrame,
						    "Are you sure you want to close the application?",
						    "AppTest",
						    JOptionPane.YES_NO_OPTION,
						    JOptionPane.QUESTION_MESSAGE,
						    null,     //do not use a custom Icon
						    options,  //the titles of buttons
						    options[0]);
					//If the option choosen is "Yes", the application is closed
					if(n==0){
						int cnt=jTabbedPaneGroups.getTabCount();
						for(int i=0;i<cnt; i++){
							((GroupPanel)jTabbedPaneGroups.getComponentAt(0)).closeGroupPanel();
						}
						jFrame.removeAll();
						jFrame.dispose();
						System.exit(0);
					}
				}
			});
			jFrame.setBounds(10, 10, 800, 450);
		}
		return jFrame;
	}

	/**
	 * This method initializes jTextFieldGroupMAddr
	 *
	 * @return javax.swing.JTextField
	 */
	private JLabel getJLabelLocalIP(String Addr) {
		if (jLabelLocalIP == null) {
			jLabelLocalIP = new JLabel();
			jLabelLocalIP.setBounds(new Rectangle(456, 13, 200, 23));
			jLabelLocalIP.setText("Your local IP is: "+Addr.split(":")[0]);
		}
		return jLabelLocalIP;
	}

	/**
	 * This method initializes jTabbedPaneGroups
	 *
	 * @return javax.swing.JTabbedPane
	 */
	private JTabbedPane getJTabbedPaneGroups() {
		if (jTabbedPaneGroups == null) {
			jTabbedPaneGroups = new JTabbedPane();
			jTabbedPaneGroups.setBounds(new Rectangle(15, 44, 989, 657));
			jTabbedPaneGroups.addChangeListener(e -> {
                if(jTabbedPaneGroups.getTabCount()>0 && jTabbedPaneGroups.getSelectedIndex()>=0)
                    jTabbedPaneGroups.setForegroundAt(jTabbedPaneGroups.getSelectedIndex(),Color.black);
            });
			jTabbedPaneGroups.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		}
		return jTabbedPaneGroups;
	}
	
	/**
	 * This method initializes jTextFieldGroupMAddr
	 *
	 * @return javax.swing.JTextField
	 */
	private JTextField getJTextFieldGroupMAddr() {
		if (jTextFieldGroupMAddr == null) {
			jTextFieldGroupMAddr = new JTextField();
			jTextFieldGroupMAddr.setBounds(new Rectangle(180, 13, 123, 23));
			jTextFieldGroupMAddr.setText("225.222.222.222");
		}
		return jTextFieldGroupMAddr;
	}

	private static Component getLogPane() {
		//appenderUI.createTabsPerLevel();
		//appenderUI.addTab(Level.ALL);
		return new SwingAppenderUI();
	}

	private static void showErrorMessage(String string) {
		JOptionPane.showMessageDialog(null, string,"Error",JOptionPane.ERROR_MESSAGE);

	}

	/*
	 * Method that creats the inetAddress, multicastAddress, the MChannel
	 */
	private MChannel startGroup(String addr){

		InetAddress address = null;
		MulticastAddress mcastAddr = null;
		MChannel groupChannel = null;

		try {
			address = InetAddress.getByName(addr);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		mcastAddr = new MulticastAddress();
		mcastAddr.setValue(address);
		groupChannel = createMChannel(mcastAddr);//create a MChannel for the group in the multicast address
		//groupChannel.start();
		
		if(Objects.equals(jLabelLocalIP.getText(), "")){
			jLabelLocalIP.setText("Your local IP is: "+groupChannel.getLocalAddress().toString().split(":")[0]);
			jContentPane.updateUI();
		}
		return groupChannel;
	}
}