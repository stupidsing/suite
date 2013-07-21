package suite.sample;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;

/**
 * A boring editor.
 */
public class SwingEditor {

	private Font font = new Font("Akkurat-Mono", Font.PLAIN, 12);

	public static void main(String args[]) {
		new SwingEditor().run();
	}

	private void run() {
		JLabel topLabel = applyDefaults(new JLabel("Top"));
		topLabel.setVisible(false);

		JTextPane editor = applyDefaults(new JTextPane());

		Component box = Box.createRigidArea(new Dimension(8, 8));
		JLabel okLabel = applyDefaults(new JLabel("OK"));

		// Flow layout allows the components to be their preferred size
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(topLabel);
		panel.add(editor);
		panel.add(box);
		panel.add(okLabel);

		JFrame frame = new JFrame(getClass().getSimpleName());
		frame.setContentPane(panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setJMenuBar(createMenuBar(panel, topLabel));
		frame.setSize(new Dimension(1024, 768));

		okLabel.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent event) {
				System.out.println("GOT " + event);
			}
		});

		frame.setVisible(true);
	}

	private JMenuBar createMenuBar(final JPanel panel, final JLabel topLabel) {
		JMenuItem openMenuItem = applyDefaults(new JMenuItem("Open...", KeyEvent.VK_O));
		openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));

		JMenuItem saveMenuItem = applyDefaults(new JMenuItem("Save", KeyEvent.VK_S));
		saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));

		JMenuItem quitMenuItem = applyDefaults(new JMenuItem("Quit", KeyEvent.VK_Q));
		quitMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				System.exit(0);
			}
		});

		JMenu fileMenu = applyDefaults(new JMenu("File"));
		fileMenu.setMnemonic(KeyEvent.VK_F);
		fileMenu.add(openMenuItem);
		fileMenu.add(saveMenuItem);
		fileMenu.add(quitMenuItem);

		JMenu editMenu = applyDefaults(new JMenu("Edit"));
		editMenu.setMnemonic(KeyEvent.VK_E);

		JMenuItem topMenuItem = applyDefaults(new JMenuItem("Top", KeyEvent.VK_T));
		topMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.ALT_MASK));
		topMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				topLabel.setVisible(!topLabel.isVisible());
				panel.repaint();
			}
		});

		JMenu viewMenu = applyDefaults(new JMenu("View"));
		viewMenu.setMnemonic(KeyEvent.VK_V);
		viewMenu.add(topMenuItem);

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(viewMenu);
		return menuBar;
	}

	private <T extends JComponent> T applyDefaults(T t) {
		t.setFont(font);
		t.setAlignmentX(Component.CENTER_ALIGNMENT);
		return t;
	}

}
