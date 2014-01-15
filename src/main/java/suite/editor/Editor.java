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

		EditorView view = new EditorView();
		view.setController(controller);
		view.run(Editor.class.getSimpleName());
	}

}
