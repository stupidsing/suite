package suite.editor;

import javax.swing.JLabel;

import suite.Suite;
import suite.node.Node;
import suite.node.io.Formatter;

public class EditorController {

	public void bottom(EditorView view) {
		JLabel bottomLabel = view.getBottomLabel();
		bottomLabel.setVisible(!bottomLabel.isVisible());
		view.repaint();
	}

	public void left(EditorView view) {
		JLabel leftLabel = view.getLeftLabel();
		leftLabel.setVisible(!leftLabel.isVisible());
		view.repaint();
	}

	public void quit(EditorView view) {
		System.exit(0);
	}

	public void right(EditorView view) {
		JLabel rightLabel = view.getRightLabel();
		rightLabel.setVisible(!rightLabel.isVisible());
		view.repaint();
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
		view.repaint();
	}

}
