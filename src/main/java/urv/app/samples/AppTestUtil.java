/**
 *
 */
package urv.app.samples;

import java.net.URL;

import javax.swing.ImageIcon;

/**
 * @author marcel
 *
 */
public class AppTestUtil {

	public static ImageIcon getCloseIcon(){
		URL url = AppTestUtil.class.getClassLoader().getResource("urv/resources/img/close.gif");
		return new ImageIcon(url);

	}
}
