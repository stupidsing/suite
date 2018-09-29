package suite.editor;

import suite.util.RunUtil;

/**
 * A boring editor.
 */
public class EditorMain {

	public static void main(String[] args) {
		RunUtil.run(() -> {
			new EditorControl().newWindow();
			return true;
		});
	}

}
