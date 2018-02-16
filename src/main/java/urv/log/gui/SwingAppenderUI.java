package urv.log.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;


public class SwingAppenderUI extends JPanel {
	final String loggerName;
	final JTextArea outputArea = new JTextArea(10, 10);
	final Map<Level, Boolean> enabledLevels = new HashMap<>();
	final JCheckBox finestBox = new JCheckBox("FINEST", false);
	final JCheckBox finerBox = new JCheckBox("FINER", false);
	final JCheckBox fineBox = new JCheckBox("FINE", false);
	final JCheckBox infoBox = new JCheckBox("INFO", true);
	final JCheckBox warningBox = new JCheckBox("WARNING", false);
	final JCheckBox severeBox = new JCheckBox("SEVERE", false);

	final JCheckBox autoscroll = new JCheckBox("Auto-scroll", true);
	private final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss.SSS");
	private final Handler LogPanelHandler = new Handler() {

		@Override
		public void publish(final LogRecord record) {
			if (enabledLevels.get(record.getLevel()) != null && enabledLevels.get(record.getLevel()))
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						String logMessage = record.getMessage();
						Object[] args = record.getParameters();
						MessageFormat fmt = new MessageFormat(logMessage);
						outputArea.append(timeFormatter.format(new Date(record.getMillis())) + "\t" + record.getLevel()
								+ "\t" + fmt.format(args) + "\n");
						if (autoscroll.isSelected())
							outputArea.setCaretPosition(outputArea.getDocument().getLength() - 1);
					}
				});
		}

		@Override
		public void flush() {
			// do nothing
		}

		@Override
		public void close() throws SecurityException {
			// do nothing
		}
	};

	public SwingAppenderUI() {
		this(Logger.GLOBAL_LOGGER_NAME);
	}

	public SwingAppenderUI(String loggerName) {
		super(new BorderLayout());
		this.loggerName = loggerName;
		initGui();
		initLogging();
	}

	private void initGui() {
		JPanel controlsPanel = new JPanel();
		controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.X_AXIS));
		controlsPanel.add(finestBox);
		controlsPanel.add(finerBox);
		controlsPanel.add(fineBox);
		controlsPanel.add(infoBox);
		controlsPanel.add(warningBox);
		controlsPanel.add(severeBox);

		controlsPanel.add(Box.createHorizontalGlue());
		controlsPanel.add(autoscroll);

		finestBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				enabledLevels.put(Level.FINEST, finestBox.isSelected());
			}
		});
		finerBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				enabledLevels.put(Level.FINER, finerBox.isSelected());
			}
		});
		fineBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				enabledLevels.put(Level.FINE, fineBox.isSelected());
			}
		});
		infoBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				enabledLevels.put(Level.INFO, infoBox.isSelected());
			}
		});
		warningBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				enabledLevels.put(Level.WARNING, warningBox.isSelected());
			}
		});
		severeBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				enabledLevels.put(Level.SEVERE, severeBox.isSelected());
			}
		});


		add(new JScrollPane(outputArea), BorderLayout.CENTER);
		add(controlsPanel, BorderLayout.SOUTH);
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		outputArea.setEditable(false);
		outputArea.setMinimumSize(new Dimension(0, 100));
		outputArea.setBackground(Color.LIGHT_GRAY);
		outputArea.setForeground(Color.BLACK);
		outputArea.setFont(new Font("monospaced", Font.PLAIN, 14));
		outputArea.setText("");
	}

	private void initLogging() {
		enabledLevels.put(Level.FINEST, Boolean.FALSE);
		enabledLevels.put(Level.FINER, Boolean.FALSE);
		enabledLevels.put(Level.FINE, Boolean.FALSE);
		enabledLevels.put(Level.INFO, Boolean.FALSE);
		enabledLevels.put(Level.WARNING, Boolean.FALSE);
		enabledLevels.put(Level.SEVERE, Boolean.FALSE);

		final Logger LOGGER = Logger.getLogger(loggerName);
		LOGGER.addHandler(LogPanelHandler);
	}
}
