package suite.editor;

import java.awt.Font;

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

		monoFont = new Font(monoFontName, Font.PLAIN, 12);
		sansFont = new Font(sansFontName, Font.PLAIN, 12);
	}

}
