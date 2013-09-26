package suite.editor;

import javax.swing.JLabel;

import suite.Suite;
import suite.node.Node;
import suite.node.io.Formatter;

public class EditorController {

	public void left(EditorView view) {
		JLabel leftLabel = view.getLeftLabel();

		leftLabel.setVisible(!leftLabel.isVisible());
		view.getFrame().repaint();
	}

	public void quit(EditorView view) {
		System.exit(0);
	}

	public void run(EditorView view) {
		Node node = Suite.evaluateFun(view.getEditor().getText(), true);

		JLabel topLabel = view.getTopLabel();
		topLabel.setText(Formatter.dump(node));
		topLabel.setVisible(true);
	}

	public void top(EditorView view) {
		JLabel topLabel = view.getTopLabel();

		topLabel.setVisible(!topLabel.isVisible());
		view.getFrame().repaint();
	}

}
