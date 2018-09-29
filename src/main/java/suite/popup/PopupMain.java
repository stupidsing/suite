package suite.popup;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import suite.Suite;
import suite.editor.ClipboardUtil;
import suite.editor.FontUtil;
import suite.editor.LayoutCalculator;
import suite.editor.Listen;
import suite.os.Execute;
import suite.streamlet.FunUtil.Fun;
import suite.util.RunUtil;
import suite.util.String_;

/**
 * Volume up: Alt-A
 *
 * Volume down: Alt-Z
 *
 * @author ywsing
 */
// mvn compile exec:java -Dexec.mainClass=suite.popup.PopupMain
public class PopupMain {

	public static void main(String[] args) {
		RunUtil.run(() -> new PopupMain().run());
	}

	private boolean run() throws Exception {
		var screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int centerX = screenSize.width / 2, centerY = screenSize.height / 2;
		int width = screenSize.width / 2, height = screenSize.height / 8;

		var inTextField = new JTextField();
		inTextField.setFont(new FontUtil().monoFont);
		inTextField.addActionListener(event -> {
			execute(inTextField.getText());
			System.exit(0);
		});

		var outLabel = new JLabel();

		var frame = new JFrame("Pop-up");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setLocation(centerX - width / 2, centerY - height / 2);
		frame.setSize(new Dimension(width, height));
		frame.setVisible(true);

		Fun<String, Execute> volumeControl = (String c) -> {
			inTextField.requestFocusInWindow();
			return new Execute(new String[] { "/usr/bin/amixer", "set", "PCM", "2" + c, });
		};

		var volLabel = new JLabel("Volume");

		var volUpButton = new JButton("+");
		volUpButton.setMnemonic(KeyEvent.VK_A);
		volUpButton.addActionListener(event -> volumeControl.apply("+"));

		var volDnButton = new JButton("-");
		volDnButton.setMnemonic(KeyEvent.VK_Z);
		volDnButton.addActionListener(event -> volumeControl.apply("-"));

		var lay = new LayoutCalculator(frame.getContentPane());

		var layout = lay.boxv( //
				lay.fx(32, lay.boxh( //
						lay.ex(32, lay.c(inTextField)), //
						lay.fx(64, lay.c(volLabel)), //
						lay.fx(48, lay.c(volUpButton)), //
						lay.fx(48, lay.c(volDnButton)))), //
				lay.ex(32, lay.c(outLabel)));

		Runnable refresh = () -> {
			lay.arrange(layout);
			frame.repaint();
		};

		Listen.componentResized(frame).wire(refresh::run);

		refresh.run();
		System.in.read();
		return true;
	}

	private void execute(String cmd) {
		if (!String_.isBlank(cmd)) {
			var clipboardUtil = new ClipboardUtil();
			var text0 = clipboardUtil.getClipboardText();
			var text1 = Suite.evaluateFilterFun(cmd, text0, true, false);
			clipboardUtil.setClipboardText(text1);
		}
	}

}
