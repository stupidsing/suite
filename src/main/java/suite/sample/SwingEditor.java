package suite.sample;


/**
 * A boring editor.
 */
public class SwingEditor {

	public static void main(String args[]) {
		SwingEditorController controller = new SwingEditorController();

		SwingEditorView view = new SwingEditorView();
		view.setController(controller);
		view.run();
	}

}
