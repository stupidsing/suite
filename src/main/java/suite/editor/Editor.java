package suite.editor;

/**
 * A boring editor.
 */
public class Editor {

	public static String monoFont;
	public static String sansFont;

	static {
		String os = System.getenv("OS");

		if (os != null && os.startsWith("Windows")) {
			System.setProperty("awt.useSystemAAFontSettings", "off");
			System.setProperty("swing.aatext", "false");
			monoFont = "Courier";
			sansFont = "Arial";
		} else {
			monoFont = "Akkurat-Mono";
			sansFont = "Sans";
		}
	}

	public static void main(String args[]) {
		new Editor().open();
	}

	public void open() {
		EditorController controller = new EditorController();
		controller.newWindow(null);
	}

}
