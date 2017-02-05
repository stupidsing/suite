package suite.editor;

import java.awt.Font;
import java.awt.Toolkit;

public class FontUtil {

	public Font monoFont;
	public Font sansFont;

	public FontUtil() {
		String os = System.getenv("OS");
		String monoFontName;
		String sansFontName;

		if (os != null && os.startsWith("Windows")) {
			System.setProperty("awt.useSystemAAFontSettings", "off");
			System.setProperty("swing.aatext", "false");
			monoFontName = "Courier New";
			sansFontName = "Arial";
		} else {
			monoFontName = "Akkurat-Mono";
			sansFontName = "Sans";
		}

		int size = Toolkit.getDefaultToolkit().getScreenSize().getWidth() > 1920 ? 24 : 12;

		monoFont = new Font(monoFontName, Font.PLAIN, size);
		sansFont = new Font(sansFontName, Font.PLAIN, size);
	}

}
