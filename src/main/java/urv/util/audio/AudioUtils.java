package urv.util.audio;

import java.awt.GridLayout;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Vector;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import urv.app.messages.SoundMessage;
import urv.machannel.MChannel;
import urv.util.network.NetworkUtils;

/**
 * Class that provide a wide set of methods to send and receive
 * audio through a channel
 * 
 * @author Raul Gracia
 *
 */
public class AudioUtils {
	
	//	CLASS FIELDS --
	
	private static Capture capture = null;	

	// Audio line to output the sound data
    private static SourceDataLine line = AudioUtils.initAudioLine();
	
	//	PUBLIC METHODS --
	
	/**
	 * Returns the format of the retransmitted audio
	 */
	public static synchronized AudioFormat getFormat() {
		return new FormatControls().getFormat();
	}
	/**
	 * Method used to setup the audio line
	 * @return AudioLine
	 */
	public static synchronized SourceDataLine initAudioLine(){
		FormatControls formatControls = new FormatControls();
		AudioFormat format = formatControls.getFormat();
		// define the required attributes for our line, 
        // and make sure a compatible line is supported.
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = null;
        //Standard buffer size
        int bufSize = 10000;
        
        if (!AudioSystem.isLineSupported(info)) {
            return null;
        }
        try {
			line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(format, bufSize);
		} catch (LineUnavailableException e1) {
			e1.printStackTrace();
		}
        // start the source data line
        line.start();
        return line;
	}
	/**
	 * Returns if the sender thread is capturing audio or not
	 */
	public static synchronized boolean isCapturing() {
		return capture != null;
	}
	/**
	 * Reproduces the audio formed by an array of data received
	 */
	public static void playSoundData (byte[] soundData){
        // get and open the source data line for playback					
	 	try {		                    
            line.write(soundData, 0, soundData.length);
        } catch (Exception e) {
        	e.printStackTrace();
        }
	}
	/**
	 * Runs the sound sender thread. It will send constantly audio
	 * packets to the receiver nodes.
	 * 
	 * @param groupChannel
	 * @param channelID
	 * @param destAddr
	 */
	public static synchronized void startAudioCapture (MChannel groupChannel, String channelID, String destAddr){
		capture = new Capture();
		capture.start(groupChannel, channelID, destAddr);
	}
	/**
	 * Stops the sender audio thread
	 */
	public static synchronized void stopAudioCapture() {
		capture.stop();		
		capture = null;
	}
}
	//	INNER CLASSES --

	/** 
	 * Reads data from the input channel and writes to the output stream
	 */
	class Capture implements Runnable {

	    TargetDataLine line;
	    Thread thread;

	    AudioInputStream audioInputStream;
	    MChannel channel;
	    String channelId;
	    InetAddress destAddr = null;

	    public void run() {	        
	        // define the required attributes for our line, 
	        // and make sure a compatible line is supported.

	        AudioFormat format = AudioUtils.getFormat();
	        DataLine.Info info = new DataLine.Info(TargetDataLine.class, 
	            format);
	                    
	        if (!AudioSystem.isLineSupported(info)) {
	            shutDown("Line matching " + info + " not supported.");
	            return;
	        }

	        // get and open the target data line for capture.

	        try {
	            line = (TargetDataLine) AudioSystem.getLine(info);
	            line.open(format, line.getBufferSize());
	        } catch (LineUnavailableException ex) { 
	            shutDown("Unable to open the line: " + ex);
	            return;
	        } catch (SecurityException ex) { 
	            shutDown(ex.toString());
	            return;
	        } catch (Exception ex) { 
	            shutDown(ex.toString());
	            return;
	        }

	        // play back the captured audio data
	        int frameSizeInBytes = format.getFrameSize();
	        int bufferLengthInFrames = line.getBufferSize() / 8;
	        int bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
	        byte[] data = new byte[bufferLengthInBytes];
	        int numBytesRead;
	        
	        line.start();
	        SoundMessage sm = new SoundMessage();
	        while (thread != null) {
	            if((numBytesRead = line.read(data, 0, bufferLengthInBytes)) == -1) {
	                break;
	            }
	            sm.setContent(data);
	            if (destAddr!=null){
	            	channel.send(NetworkUtils.getJGroupsAddresFor(destAddr),
	            			channel.getLocalAddress(), sm);
	            	System.out.println("Sending Audio To: "+destAddr.getCanonicalHostName());
	            }
	         }
	        // we reached the end of the stream.  stop and close the line.
	        line.stop();
	        line.close();
	        line = null;
	    }

	    public void start(MChannel channel, String channelId, String address) {
	    	this.channel = channel;    	
			try {
				destAddr = InetAddress.getByName(address);
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			}
	    	this.channelId = channelId;
	        thread = new Thread(this);
	        thread.setName("Capture");
	        thread.start();
	    }
	    
	    public void stop() {
	        thread = null;
	    }

	    private void shutDown(String message) {
	        if (thread != null) {
	            thread = null;
	        }
	    }
	}
	/**
	 * Controls for the AudioFormat.
	 */
	class FormatControls extends JPanel {
	
	    Vector groups = new Vector();
	    JToggleButton linrB, ulawB, alawB, rate8B, rate11B, rate16B, rate22B, rate44B;
	    JToggleButton size8B, size16B, signB, unsignB, litB, bigB, monoB,sterB;
	
	    public FormatControls() {
	        setLayout(new GridLayout(0,1));
	        EmptyBorder eb = new EmptyBorder(0,0,0,5);
	        BevelBorder bb = new BevelBorder(BevelBorder.LOWERED);
	        CompoundBorder cb = new CompoundBorder(eb, bb);
	        setBorder(new CompoundBorder(cb, new EmptyBorder(8,5,5,5)));
	        JPanel p1 = new JPanel();
	        ButtonGroup encodingGroup = new ButtonGroup();
	        linrB = addToggleButton(p1, encodingGroup, "linear", true);
	        ulawB = addToggleButton(p1, encodingGroup, "ulaw", false);
	        alawB = addToggleButton(p1, encodingGroup, "alaw", false);
	        add(p1);
	        groups.addElement(encodingGroup);
	           
	        JPanel p2 = new JPanel();
	        JPanel p2b = new JPanel();
	        ButtonGroup sampleRateGroup = new ButtonGroup();
	        rate8B = addToggleButton(p2, sampleRateGroup, "8000", false);
	        rate11B = addToggleButton(p2, sampleRateGroup, "11025", false);
	        rate16B = addToggleButton(p2b, sampleRateGroup, "16000", false);
	        rate22B = addToggleButton(p2b, sampleRateGroup, "22050", false);
	        rate44B = addToggleButton(p2b, sampleRateGroup, "44100", true);
	        add(p2);
	    add(p2b);
	        groups.addElement(sampleRateGroup);
	
	        JPanel p3 = new JPanel();
	        ButtonGroup sampleSizeInBitsGroup = new ButtonGroup();
	        size8B = addToggleButton(p3, sampleSizeInBitsGroup, "8", false);
	        size16B = addToggleButton(p3, sampleSizeInBitsGroup, "16", true);
	        add(p3);
	        groups.addElement(sampleSizeInBitsGroup);
	
	        JPanel p4 = new JPanel();
	        ButtonGroup signGroup = new ButtonGroup();
	        signB = addToggleButton(p4, signGroup, "signed", true);
	        unsignB = addToggleButton(p4, signGroup, "unsigned", false);
	        add(p4);
	        groups.addElement(signGroup);
	
	        JPanel p5 = new JPanel();
	        ButtonGroup endianGroup = new ButtonGroup();
	        litB = addToggleButton(p5, endianGroup, "little endian", false);
	        bigB = addToggleButton(p5, endianGroup, "big endian", true);
	        add(p5);
	        groups.addElement(endianGroup);
	
	        JPanel p6 = new JPanel();
	        ButtonGroup channelsGroup = new ButtonGroup();
	        monoB = addToggleButton(p6, channelsGroup, "mono", false);
	        sterB = addToggleButton(p6, channelsGroup, "stereo", true);
	        add(p6);
	        groups.addElement(channelsGroup);
	    }	
	    public AudioFormat getFormat() {	
	        Vector v = new Vector(groups.size());
	        for (int i = 0; i < groups.size(); i++) {
	            ButtonGroup g = (ButtonGroup) groups.get(i);
	            for (Enumeration e = g.getElements();e.hasMoreElements();) {
	                AbstractButton b = (AbstractButton) e.nextElement();
	                if (b.isSelected()) {
	                    v.add(b.getText());
	                    break;
	                }
	            }
	        }
	        AudioFormat.Encoding encoding = AudioFormat.Encoding.ULAW;
	        String encString = (String) v.get(0);
	        float rate = Float.valueOf((String) v.get(1)).floatValue();
	        int sampleSize = Integer.valueOf((String) v.get(2)).intValue();
	        String signedString = (String) v.get(3);
	        boolean bigEndian = ((String) v.get(4)).startsWith("big");
	        int channels = ((String) v.get(5)).equals("mono") ? 1 : 2;
	
	        if (encString.equals("linear")) {
	            if (signedString.equals("signed")) {
	                encoding = AudioFormat.Encoding.PCM_SIGNED;
	            } else {
	                encoding = AudioFormat.Encoding.PCM_UNSIGNED;
	            }
	        } else if (encString.equals("alaw")) {
	            encoding = AudioFormat.Encoding.ALAW;
	        }
	        return new AudioFormat(encoding, rate, sampleSize, 
	                      channels, (sampleSize/8)*channels, rate, bigEndian);
	    }	
	    public void setFormat(AudioFormat format) {
	        AudioFormat.Encoding type = format.getEncoding();
	        if (type == AudioFormat.Encoding.ULAW) {
	            ulawB.doClick();
	        } else if (type == AudioFormat.Encoding.ALAW) {
	            alawB.doClick();
	        } else if (type == AudioFormat.Encoding.PCM_SIGNED) {
	            linrB.doClick(); signB.doClick(); 
	        } else if (type == AudioFormat.Encoding.PCM_UNSIGNED) {
	            linrB.doClick(); unsignB.doClick(); 
	        }
	        float rate = format.getFrameRate();
	        if (rate == 8000) {
	            rate8B.doClick();
	        } else if (rate == 11025) {
	            rate11B.doClick();
	        } else if (rate == 16000) {
	            rate16B.doClick();
	        } else if (rate == 22050) {
	            rate22B.doClick();
	        } else if (rate == 44100) {
	            rate44B.doClick();
	        }
	        switch (format.getSampleSizeInBits()) {
	            case 8  : size8B.doClick(); break;
	            case 16 : size16B.doClick(); break;
	        }
	        if (format.isBigEndian()) {
	            bigB.doClick(); 
	        } else { 
	            litB.doClick();
	        }
	        if (format.getChannels() == 1) {
	            monoB.doClick(); 
	        } else { 
	            sterB.doClick();
	        }
	    }
	
	    private JToggleButton addToggleButton(JPanel p, ButtonGroup g, 
	                                 String name, boolean state) {
	        JToggleButton b = new JToggleButton(name, state);
	        p.add(b);
	        g.add(b);
	        return b;
	    }
	}