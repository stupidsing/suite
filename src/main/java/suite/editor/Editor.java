package suite.editor;

/**
 * A boring editor.
 */
public class Editor {

	public static void main(String args[]) {
		new Editor().open();
	}

	public void open() {
		EditorController controller = new EditorController();
		controller.newWindow(null);
	}

}
