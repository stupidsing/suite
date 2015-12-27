package suite.popup;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import suite.Suite;
import suite.editor.ClipboardUtil;
import suite.editor.FontUtil;
import suite.editor.LayoutCalculator;
import suite.editor.LayoutCalculator.Node;
import suite.editor.LayoutCalculator.Orientation;
import suite.editor.Listen;
import suite.os.ExecUtil;
import suite.os.LogUtil;
import suite.util.FunUtil.Fun;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

/**
 * Volume up: Alt-A
 *
 * Volume down: Alt-Z
 *
 * @author ywsing
 */
// mvn assembly:single &&
// java -cp target/suite-1.0-jar-with-dependencies.jar suite.popup.PopupMain
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
		inTextField.setFont(new FontUtil().monoFont);
		inTextField.addActionListener(event -> {
			execute(inTextField.getText());
			System.exit(0);
		});

		JLabel outLabel = new JLabel();

		JFrame frame = new JFrame("Pop-up");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setLocation(centreX - width / 2, centreY - height / 2);
		frame.setSize(new Dimension(width, height));
		frame.setVisible(true);

		Fun<String, ExecUtil> volumeControl = (String c) -> {
			inTextField.requestFocusInWindow();
			try {
				return new ExecUtil(new String[] { "/usr/bin/amixer", "set", "PCM", "2" + c }, "");
			} catch (IOException ex) {
				LogUtil.error(ex);
				return null;
			}
		};

		JLabel volLabel = new JLabel("Volume");

		JButton volUpButton = new JButton("+");
		volUpButton.setMnemonic(KeyEvent.VK_A);
		volUpButton.addActionListener(event -> volumeControl.apply("+"));

		JButton volDnButton = new JButton("-");
		volDnButton.setMnemonic(KeyEvent.VK_Z);
		volDnButton.addActionListener(event -> volumeControl.apply("-"));

		LayoutCalculator lay = new LayoutCalculator(frame.getContentPane());

		Node layout = lay.box(Orientation.VERTICAL //
				, lay.fx(32, lay.box(Orientation.HORIZONTAL //
						, lay.ex(32, lay.c(inTextField)) //
						, lay.fx(64, lay.c(volLabel)) //
						, lay.fx(48, lay.c(volUpButton)) //
						, lay.fx(48, lay.c(volDnButton)) //
						) //
				) //
				, lay.ex(32, lay.c(outLabel)) //
				);

		Runnable refresh = () -> {
			lay.arrange(layout);
			frame.repaint();
		};

		Listen.componentResized(frame).register(event -> refresh.run());

		refresh.run();
		System.in.read();
		return true;
	}

	public void execute(String cmd) {
		if (!Util.isBlank(cmd)) {
			ClipboardUtil clipboardUtil = new ClipboardUtil();
			String text0 = clipboardUtil.getClipboardText();
			String text1 = Suite.evaluateFilterFun(cmd, text0, true, false);
			clipboardUtil.setClipboardText(text1);
		}
	}

}
