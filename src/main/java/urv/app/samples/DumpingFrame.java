package urv.app.samples;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import urv.log.Log;
/**
 * @author Gerard Paris Aixala
 */
public class DumpingFrame extends javax.swing.JFrame {
	private javax.swing.JPanel ivjJFrameContentPane = null;
	private JScrollPane jScrollPaneDump = null;
	private JTextArea jTextAreaDump = null;

	public DumpingFrame() {
		super();
		initialize();
		updateDumping();
		new DumpingThread().start();
	}

	/**
	 * Return the JFrameContentPane property value.
	 * @return javax.swing.JPanel
	 */
	private javax.swing.JPanel getJFrameContentPane() {
		if (ivjJFrameContentPane == null) {
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.fill = GridBagConstraints.BOTH;
			gridBagConstraints.gridy = 0;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.weighty = 1.0;
			gridBagConstraints.gridx = 0;
			ivjJFrameContentPane = new javax.swing.JPanel();
			ivjJFrameContentPane.setName("JFrameContentPane");
			ivjJFrameContentPane.setLayout(new GridBagLayout());
			ivjJFrameContentPane.add(getJScrollPaneDump(), gridBagConstraints);
		}
		return ivjJFrameContentPane;
	}

	/**
	 * This method initializes jScrollPaneDump	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPaneDump() {
		if (jScrollPaneDump == null) {
			jScrollPaneDump = new JScrollPane();
			jScrollPaneDump.setViewportView(getJTextAreaDump());
		}
		return jScrollPaneDump;
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
	 * Initialize the class.
	 */
	private void initialize() {

		this.setName("JFrame1");
		this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		/*this
				.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);*/
		this.setBounds(45, 25, 608, 378);
		this.setTitle("Dump");
		this.setContentPane(getJFrameContentPane());

	}
	
	private void updateDumping(){
		jTextAreaDump.setText( Log.getInstance().printLoggables() );
	}
	
	private class DumpingThread extends Thread{
		public void run(){
			while(true){
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				updateDumping();
			}
		}
	}
	
}  //  @jve:decl-index=0:visual-constraint="10,10"
