package suite.sample;

import java.awt.Font;
import java.awt.event.KeyEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.table.AbstractTableModel;

import suite.util.RunUtil;

/**
 * A boring demonstration of various Swing components.
 */
public class SwingTableMain {

	public static void main(String[] args) {
		RunUtil.run(() -> {
			var label = new JLabel("Hello World~~~");

			var editor = new JTextPane();
			editor.setFont(new Font("Monospac821 BT", Font.PLAIN, 10));

			var table = new JTable(new AbstractTableModel() {
				private static final long serialVersionUID = -1;

				public int getColumnCount() {
					return 3;
				}

				public int getRowCount() {
					return 3;
				}

				public Object getValueAt(int row, int col) {
					return row * col;
				}

				public boolean isCellEditable(int row, int col) {
					return true;
				}
			});

			var button = new JButton("Click Me!");
			button.setMnemonic(KeyEvent.VK_C); // alt-C as hot key
			button.addActionListener(event -> System.out.println("GOT " + event));

			// flow layout allows the components to be their preferred size
			var panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			panel.add(label);
			panel.add(editor);
			panel.add(table);
			panel.add(button);

			var frame = new JFrame();
			frame.setContentPane(panel);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			// frame.setLocation(200, 200);
			frame.pack(); // pack it up for display
			frame.setVisible(true);
			return true;
		});
	}

}
