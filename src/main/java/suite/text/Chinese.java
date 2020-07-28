package suite.text;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import primal.MoreVerbs.Read;
import primal.Verbs.Equals;
import primal.Verbs.Left;
import primal.Verbs.Right;
import primal.fp.Funs.Source;
import suite.editor.ClipboardUtil;
import suite.editor.FontUtil;
import suite.editor.LayoutCalculator;
import suite.editor.Listen;
import suite.node.util.Singleton;
import suite.streamlet.As;
import suite.util.Memoize;
import suite.util.RunUtil;

// urxvt -e sh -c 'BASE=~/suite/; java -cp $(cat ${BASE}/target/classpath):${BASE}/target/suite-1.0.jar suite.text.Chinese | xclip -selection c'
public class Chinese {

	private ClipboardUtil cb = new ClipboardUtil();

	public static void main(String[] args) {
		if (Boolean.TRUE)
			RunUtil.run(new Chinese()::runCli);
		else
			RunUtil.run(new Chinese()::runSwing);
	}

	private boolean runCli() throws IOException {
		try (var isr = new InputStreamReader(System.in, StandardCharsets.UTF_8); var br = new BufferedReader(isr);) {
			String line;
			while ((line = br.readLine()) != null) {
				var chinese = cjs(line);
				System.out.println(chinese);
				cb.setClipboardText(chinese);
			}
		}
		return true;
	}

	private boolean runSwing() throws Exception {
		var screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int centerX = screenSize.width / 2, centerY = screenSize.height / 2;
		int width = screenSize.width / 2, height = screenSize.height / 16;

		var inTextField = new JTextField();
		inTextField.setFont(new FontUtil().monoFont);
		inTextField.addActionListener(event -> {
			var text = cjs(inTextField.getText());
			System.out.println(text);
			cb.setClipboardText(text);
		});

		var frame = new JFrame("Chinese Input");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setLocation(centerX - width / 2, centerY - height / 2);
		frame.setSize(new Dimension(width, height));
		frame.setVisible(true);

		var lay = new LayoutCalculator(frame.getContentPane());

		var layout = lay.boxv(lay.fx(32, lay.boxh(lay.ex(32, lay.c(inTextField)))));

		Runnable refresh = () -> {
			lay.arrange(layout);
			frame.repaint();
		};

		Listen.componentResized(frame).wire(refresh, refresh::run);

		frame.addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				inTextField.requestFocus();
			}
		});

		frame.addWindowFocusListener(new WindowFocusListener() {
			public void windowGainedFocus(WindowEvent e) {
				inTextField.requestFocus();
			}

			public void windowLostFocus(WindowEvent e) {
			}
		});

		refresh.run();
		System.in.read();
		return true;
	}

	private Source<Map<String, List<String>>> cjTable = Memoize.source(() -> Singleton.me.storeCache //
			.http("https://pointless.online/storey/cangjie5.txt") //
			.collect(As::lines) //
			.dropWhile(line -> !Equals.string(line, "BEGIN_TABLE")) //
			.drop(1) //
			.takeWhile(line -> !Equals.string(line, "END_TABLE")) //
			.map(line -> line.split("\t")) //
			.map2(array -> array[0], array -> array[1]) //
			.toListMap());

	public String cjs(String sequences) {
		return Read //
				.from(sequences.split(" ")) //
				.filter(sequence -> !sequence.isEmpty()) //
				.map(this::cj) //
				.toJoinedString();
	}

	public String cj(String sequence0) {
		var digit = Right.of(sequence0, -1).charAt(0);
		String sequence1;
		int position;
		if ('1' <= digit && digit <= '9') {
			sequence1 = Left.of(sequence0, -1);
			position = digit - '1';
		} else {
			sequence1 = sequence0;
			position = 0;
		}
		var list = cjTable.g().get(sequence1);
		return list != null && position < list.size() ? list.get(position) : "";
	}

}
