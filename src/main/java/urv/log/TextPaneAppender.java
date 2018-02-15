package urv.log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 */
@Plugin(name = "TextPaneWriter", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class TextPaneAppender extends AbstractAppender {
	private static final PatternLayout defaultLayout = PatternLayout.createDefaultLayout();
	private TextPaneOutStream textPaneOutStream = null;
	private boolean closed = true;
	PatternLayout logLayout = defaultLayout;

	public TextPaneAppender() {
	    super("TextPaneWriter", (Filter)null, defaultLayout, true);
//		setWriter(createWriter(getTextPaneOutStream()));
//		super.activateOptions();
	}

	public TextPaneAppender(PatternLayout layout) {
	    super("TextPaneWriter", (Filter)null, layout, true);
//		setWriter(createWriter(getTextPaneOutStream()));
//		super.activateOptions();
	}
	
	public synchronized void doAppend(LogEvent event) {
		if (closed) {
//			LogLog.error("Attempted to append to closed appender named [" + name + "].");
			return;
		}
		this.appendByLevel(event);
	}

	public TextPaneOutStream getTextPaneOutStream() {
		if (textPaneOutStream == null)
			textPaneOutStream = new TextPaneOutStream();
		return textPaneOutStream;
	}

	protected void appendByLevel(LogEvent event) {
		this.getTextPaneOutStream().writeLeveled(logLayout.toSerializable(event), event.getLevel());
		this.getTextPaneOutStream().flush();
	}

	@Override
	public void append(LogEvent event) {
		appendByLevel(event);
	}

	/**
	 * @author ADolgarev OutputStream that writes messages to the document of
	 *         JTextPane
	 */
	public static class TextPaneOutStream extends OutputStream {
		private final Map<Level, StringBuffer> buffers = new HashMap<>();
		private final Map<Level, JTextPane> panes = new HashMap<>();
		private final Map<StringBuffer, Boolean> flushables = new HashMap<>();
		
		public TextPaneOutStream() {
			// ALL level is always active
			StringBuffer sb = new StringBuffer();
			buffers.put(Level.ALL,sb);
			flushables.put(sb, Boolean.TRUE);
		}

		public void addTextPane(JTextPane textPane, Level l) {
			synchronized (panes) {
				panes.put(l, textPane);
				synchronized (buffers) {
					if (!buffers.containsKey(l)) {
						StringBuffer sb = new StringBuffer();
						buffers.put(l, sb);
						flushables.put(sb, Boolean.TRUE);
					}
				}
			}
		}

		@Override
		public void close() {
			// do nothing
		}

		@Override
		public void flush() {
			for (Level level : buffers.keySet()) {
				JTextPane textPane = panes.get(level);
				StringBuffer buff = buffers.get(level);
				synchronized (buff) {
					if (buff.length() > 1 && textPane != null && isBufferFlushable(buff)) {
						try {
							Document document = textPane.getDocument();
							synchronized(document){
								document.insertString(document.getLength(), buff
										.toString(), new SimpleAttributeSet());
								buff.setLength(0);
								textPane.setCaretPosition(document.getLength());
							}
						} catch (BadLocationException e) {
//							LogLog.warn(e.getMessage());
						}
					}
				}
			}

		}

		public StringBuffer getTextBuffer(Level level) {
			return buffers.get(level);
		}

		public JTextPane getTextPane(Level level) {
			return panes.get(level);
		}

		public boolean isBufferFlushable(StringBuffer sb) {
			boolean ret = false;
			synchronized (sb){
				ret = flushables.get(sb);
			}
			return ret;
		}

		public void setBufferFlushable(StringBuffer sb, boolean b) {
			synchronized (sb){
				flushables.put(sb, b);
			}
		}

		@Override
		public void write(int b) throws IOException {
//			LogLog.error("bad write call!");
		}

		public void writeLeveled(String msg, Level level) {
			StringBuffer buff = this.buffers.get(level);
			if (buff != null){
				synchronized (buff) {
					buff.append(msg);
				}
			}
			// add it to all as well
			buff = this.buffers.get(Level.ALL);
			synchronized (buff) {
				buff.append(msg);
			}
		}
	}

}
