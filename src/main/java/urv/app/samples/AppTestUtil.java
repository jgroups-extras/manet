/**
 *
 */
package urv.app.samples;

import javax.swing.*;
import java.net.URL;

/**
 * @author marcel
 *
 */
public class AppTestUtil {

	public static ImageIcon getCloseIcon(){
		URL url = AppTestUtil.class.getClassLoader().getResource("close.gif");
		return new ImageIcon(url);

	}
}
