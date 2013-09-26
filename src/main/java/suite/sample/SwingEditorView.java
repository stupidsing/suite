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
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

public class SwingEditorView {

	private Font font = new Font("Akkurat-Mono", Font.PLAIN, 12);

	private SwingEditorController controller;

	private JLabel leftLabel;
	private JLabel topLabel;
	private JEditorPane editor;
	private JFrame frame;

	public JFrame run() {
		JLabel leftLabel = this.leftLabel = applyDefaults(new JLabel("Left"));
		leftLabel.setVisible(false);

		JLabel topLabel = this.topLabel = applyDefaults(new JLabel("Top"));
		topLabel.setVisible(false);

		JEditorPane editor = this.editor = applyDefaults(new JEditorPane());

		Component box = Box.createRigidArea(new Dimension(8, 8));
		JLabel okLabel = applyDefaults(new JLabel("OK"));

		JPanel verticalPanel = new JPanel();
		verticalPanel.setLayout(new BoxLayout(verticalPanel, BoxLayout.Y_AXIS));
		verticalPanel.add(topLabel);
		verticalPanel.add(editor);
		verticalPanel.add(box);
		verticalPanel.add(okLabel);

		// Flow layout allows the components to be their preferred size
		JPanel horizontalPanel = new JPanel();
		horizontalPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		horizontalPanel.setLayout(new BoxLayout(horizontalPanel, BoxLayout.X_AXIS));
		horizontalPanel.add(leftLabel);
		horizontalPanel.add(verticalPanel);

		JFrame frame = this.frame = new JFrame(getClass().getSimpleName());
		frame.setContentPane(horizontalPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setJMenuBar(createMenuBar());
		frame.setSize(new Dimension(1280, 768));
		frame.setVisible(true);

		okLabel.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent event) {
				System.out.println("GOT " + event);
			}
		});

		return frame;
	}

	private JMenuBar createMenuBar() {
		JMenuItem openMenuItem = applyDefaults(new JMenuItem("Open...", KeyEvent.VK_O));
		openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));

		JMenuItem saveMenuItem = applyDefaults(new JMenuItem("Save", KeyEvent.VK_S));
		saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));

		JMenuItem quitMenuItem = applyDefaults(new JMenuItem("Quit", KeyEvent.VK_Q));
		quitMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				controller.quit(SwingEditorView.this);
			}
		});

		JMenu fileMenu = applyDefaults(new JMenu("File"));
		fileMenu.setMnemonic(KeyEvent.VK_F);
		fileMenu.add(openMenuItem);
		fileMenu.add(saveMenuItem);
		fileMenu.add(quitMenuItem);

		JMenu editMenu = applyDefaults(new JMenu("Edit"));
		editMenu.setMnemonic(KeyEvent.VK_E);

		JMenuItem leftMenuItem = applyDefaults(new JMenuItem("Left", KeyEvent.VK_L));
		leftMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
		leftMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				controller.left(SwingEditorView.this);
			}
		});

		JMenuItem topMenuItem = applyDefaults(new JMenuItem("Top", KeyEvent.VK_T));
		topMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.ALT_MASK));
		topMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				controller.top(SwingEditorView.this);
			}
		});

		JMenu viewMenu = applyDefaults(new JMenu("View"));
		viewMenu.setMnemonic(KeyEvent.VK_V);
		viewMenu.add(leftMenuItem);
		viewMenu.add(topMenuItem);

		JMenuItem runMenuItem = applyDefaults(new JMenuItem("Run", KeyEvent.VK_R));
		runMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				controller.run(SwingEditorView.this);
			}
		});

		JMenu projectMenu = applyDefaults(new JMenu("Project"));
		projectMenu.setMnemonic(KeyEvent.VK_P);
		projectMenu.add(runMenuItem);

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(viewMenu);
		menuBar.add(projectMenu);
		return menuBar;
	}

	public void setController(SwingEditorController controller) {
		this.controller = controller;
	}

	public JLabel getLeftLabel() {
		return leftLabel;
	}

	public JLabel getTopLabel() {
		return topLabel;
	}

	public JEditorPane getEditor() {
		return editor;
	}

	public JFrame getFrame() {
		return frame;
	}

	private <T extends JComponent> T applyDefaults(T t) {
		t.setFont(font);
		t.setAlignmentX(Component.CENTER_ALIGNMENT);
		return t;
	}

}
