package suite.editor;

/**
 * A boring editor.
 */
public class Editor {

	public static void main(String args[]) {
		String os = System.getenv("OS");

		if (os != null && os.startsWith("Windows")) {
			System.setProperty("awt.useSystemAAFontSettings", "off");
			System.setProperty("swing.aatext", "false");
		}

		new Editor().open();
	}

	public void open() {
		EditorController controller = new EditorController();
		controller.newWindow(null);
	}

}
