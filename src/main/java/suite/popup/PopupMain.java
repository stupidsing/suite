package suite.popup;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import suite.editor.LayoutCalculator;
import suite.editor.LayoutCalculator.Orientation;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

public class PopupMain extends ExecutableProgram {

	public static void main(String args[]) {
		Util.run(PopupMain.class, args);
	}

	@Override
	protected boolean run(String args[]) throws Exception {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int centreX = screenSize.width / 2, centreY = screenSize.height / 2;
		int width = screenSize.width / 2, height = screenSize.height / 8;

		JTextField inTextField = new JTextField();
		JLabel outLabel = new JLabel();

		JFrame frame = new JFrame("Pop-up");
		// frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.setLocation(centreX - width / 2, centreY - height / 2);
		frame.setSize(new Dimension(width, height));
		frame.setVisible(true);

		LayoutCalculator lay = new LayoutCalculator(frame.getContentPane());

		lay.arrange(lay.box(Orientation.VERTICAL //
				, lay.fx(32, lay.c(inTextField)) //
				, lay.ex(32, lay.c(outLabel)) //
				));

		frame.repaint();

		System.in.read();

		return true;
	}

}
