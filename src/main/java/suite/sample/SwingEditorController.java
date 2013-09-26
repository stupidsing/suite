package suite.sample;

import javax.swing.JLabel;

import suite.Suite;
import suite.node.Node;
import suite.node.io.Formatter;

public class SwingEditorController {

	public void left(SwingEditorView view) {
		JLabel leftLabel = view.getLeftLabel();

		leftLabel.setVisible(!leftLabel.isVisible());
		view.getFrame().repaint();
	}

	public void quit(SwingEditorView view) {
		System.exit(0);
	}

	public void run(SwingEditorView view) {
		Node node = Suite.evaluateFun(view.getEditor().getText(), true);

		JLabel topLabel = view.getTopLabel();
		topLabel.setText(Formatter.dump(node));
		topLabel.setVisible(true);
	}

	public void top(SwingEditorView view) {
		JLabel topLabel = view.getTopLabel();

		topLabel.setVisible(!topLabel.isVisible());
		view.getFrame().repaint();
	}

}
