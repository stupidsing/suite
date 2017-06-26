package suite.editor;

import suite.util.Util;
import suite.util.Util.ExecutableProgram;

/**
 * A boring editor.
 */
public class EditorMain extends ExecutableProgram {

	public static void main(String[] args) {
		Util.run(EditorMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		open();
		return true;
	}

	public void open() {
		new EditorController().newWindow();
	}

}
