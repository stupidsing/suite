package suite.editor;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import suite.Suite;
import suite.node.Node;
import suite.node.io.Formatter;

public class EditorController {

	public void bottom(EditorView view) {
		JPanel bottomPanel = view.getBottomPanel();
		bottomPanel.setVisible(!bottomPanel.isVisible());
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

		JTextArea bottomTextArea = view.getBottomTextArea();
		bottomTextArea.setText(Formatter.dump(node));
		bottomTextArea.setVisible(true);
		view.repaint();
	}

	public void top(EditorView view) {
		JLabel topLabel = view.getTopLabel();
		topLabel.setVisible(!topLabel.isVisible());
		view.repaint();
	}

}
