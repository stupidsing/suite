package suite.editor;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JTextArea;

import suite.Suite;
import suite.node.Node;
import suite.node.io.Formatter;
import suite.util.To;

public class EditorController {

	public void bottom(EditorView view) {
		JComponent bottom = view.getBottomToolbar();
		bottom.setVisible(!bottom.isVisible());
		view.repaint();
	}

	public void left(EditorView view) {
		JComponent left = view.getLeftToolbar();
		left.setVisible(!left.isVisible());
		view.getLeftTextField().requestFocus();
		view.repaint();
	}

	public void quit(EditorView view) {
		System.exit(0);
	}

	public void right(EditorView view) {
		JComponent right = view.getRightToolbar();
		right.setVisible(!right.isVisible());
		view.repaint();
	}

	public void run(EditorView view) {
		JEditorPane editor = view.getEditor();
		String selectedText = editor.getSelectedText();
		String text = selectedText != null ? selectedText : editor.getText();
		String result;

		try {
			Node node = Suite.evaluateFun(text, true);
			result = Formatter.dump(node);
		} catch (Exception ex) {
			result = To.string(ex);
		}

		JTextArea bottomTextArea = view.getBottomTextArea();
		bottomTextArea.setText(result);
		bottomTextArea.setVisible(true);
		view.repaint();
	}

	public void top(EditorView view) {
		JComponent top = view.getTopToolbar();
		top.setVisible(!top.isVisible());
		view.repaint();
	}

}
