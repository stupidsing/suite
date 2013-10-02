package suite.editor;

import javax.swing.JLabel;
import javax.swing.JPanel;

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
		JPanel leftPanel = view.getLeftPanel();
		leftPanel.setVisible(!leftPanel.isVisible());
		view.getLeftTextField().requestFocus();
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

		JLabel bottomLabel = view.getBottomLabel();
		bottomLabel.setText(Formatter.dump(node));
		bottomLabel.setVisible(true);
		view.repaint();
	}

	public void top(EditorView view) {
		JLabel topLabel = view.getTopLabel();
		topLabel.setVisible(!topLabel.isVisible());
		view.repaint();
	}

}
