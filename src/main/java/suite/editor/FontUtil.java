package suite.editor;

import suite.util.RunUtil;

import java.awt.*;

public class FontUtil {

	public Font monoFont;
	public Font sansFont;

	public FontUtil() {
		String monoFontName;
		String sansFontName;

		if (!RunUtil.isLinux()) {
			System.setProperty("awt.useSystemAAFontSettings", "off");
			System.setProperty("swing.aatext", "false");
			monoFontName = "Courier New";
			sansFontName = "Arial";
		} else {
			monoFontName = "Akkurat-Mono";
			sansFontName = "Sans";
		}

		var size = Toolkit.getDefaultToolkit().getScreenSize().getWidth() > 1920 ? 24 : 12;

		monoFont = new Font(monoFontName, Font.PLAIN, size);
		sansFont = new Font(sansFontName, Font.PLAIN, size);
	}

}
