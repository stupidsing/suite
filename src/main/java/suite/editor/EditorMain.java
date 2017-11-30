package suite.editor;

import suite.util.RunUtil;
import suite.util.RunUtil.ExecutableProgram;

/**
 * A boring editor.
 */
public class EditorMain extends ExecutableProgram {

	public static void main(String[] args) {
		RunUtil.run(EditorMain.class, args);
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
