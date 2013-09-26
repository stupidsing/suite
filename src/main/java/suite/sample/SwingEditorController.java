package suite.sample;

import javax.swing.JLabel;

public class SwingEditorController {

	public void quit(SwingEditorView view) {
		System.exit(0);
	}

	public void run(SwingEditorView view) {
		// TODO
	}

	public void top(SwingEditorView view) {
		JLabel topLabel = view.getTopLabel();

		topLabel.setVisible(!topLabel.isVisible());
		view.getPanel().repaint();
	}

}
