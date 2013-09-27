package suite.editor;

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

public class EditorView {

	private Font font = new Font("Akkurat-Mono", Font.PLAIN, 12);

	private EditorController controller;

	private JLabel bottomLabel;
	private JEditorPane editor;
	private JFrame frame;
	private JLabel leftLabel;
	private JLabel rightLabel;
	private JLabel topLabel;

	public JFrame run() {
		JLabel bottomLabel = this.bottomLabel = applyDefaults(new JLabel("Bottom"));
		bottomLabel.setVisible(false);

		JLabel leftLabel = this.leftLabel = applyDefaults(new JLabel("Left"));
		leftLabel.setVisible(false);

		JLabel rightLabel = this.rightLabel = applyDefaults(new JLabel("Right"));
		rightLabel.setVisible(false);

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
		verticalPanel.add(bottomLabel);

		// Flow layout allows the components to be their preferred size
		JPanel horizontalPanel = new JPanel();
		horizontalPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		horizontalPanel.setLayout(new BoxLayout(horizontalPanel, BoxLayout.X_AXIS));
		horizontalPanel.add(leftLabel);
		horizontalPanel.add(verticalPanel);
		horizontalPanel.add(rightLabel);

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
		final EditorView view = this;

		JMenuItem openMenuItem = applyDefaults(new JMenuItem("Open...", KeyEvent.VK_O));
		openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));

		JMenuItem saveMenuItem = applyDefaults(new JMenuItem("Save", KeyEvent.VK_S));
		saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));

		JMenuItem quitMenuItem = applyDefaults(new JMenuItem("Quit", KeyEvent.VK_Q));
		quitMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				controller.quit(EditorView.this);
			}
		});

		JMenu fileMenu = applyDefaults(new JMenu("File"));
		fileMenu.setMnemonic(KeyEvent.VK_F);
		fileMenu.add(openMenuItem);
		fileMenu.add(saveMenuItem);
		fileMenu.add(quitMenuItem);

		JMenu editMenu = applyDefaults(new JMenu("Edit"));
		editMenu.setMnemonic(KeyEvent.VK_E);

		JMenuItem bottomMenuItem = applyDefaults(new JMenuItem("Bottom", KeyEvent.VK_B));
		bottomMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.ALT_MASK));
		bottomMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				controller.bottom(view);
			}
		});

		JMenuItem leftMenuItem = applyDefaults(new JMenuItem("Left", KeyEvent.VK_L));
		leftMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
		leftMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				controller.left(view);
			}
		});

		JMenuItem rightMenuItem = applyDefaults(new JMenuItem("Right", KeyEvent.VK_R));
		rightMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
		rightMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				controller.right(view);
			}
		});

		JMenuItem topMenuItem = applyDefaults(new JMenuItem("Top", KeyEvent.VK_T));
		topMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.ALT_MASK));
		topMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				controller.top(view);
			}
		});

		JMenu viewMenu = applyDefaults(new JMenu("View"));
		viewMenu.setMnemonic(KeyEvent.VK_V);
		viewMenu.add(bottomMenuItem);
		viewMenu.add(leftMenuItem);
		viewMenu.add(rightMenuItem);
		viewMenu.add(topMenuItem);

		JMenuItem runMenuItem = applyDefaults(new JMenuItem("Run", KeyEvent.VK_R));
		runMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				controller.run(view);
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

	public void repaint() {
		frame.repaint();
	}

	private <T extends JComponent> T applyDefaults(T t) {
		t.setFont(font);
		t.setAlignmentX(Component.CENTER_ALIGNMENT);
		return t;
	}

	public void setController(EditorController controller) {
		this.controller = controller;
	}

	public JLabel getBottomLabel() {
		return bottomLabel;
	}

	public JEditorPane getEditor() {
		return editor;
	}

	public JLabel getLeftLabel() {
		return leftLabel;
	}

	public JLabel getRightLabel() {
		return rightLabel;
	}

	public JLabel getTopLabel() {
		return topLabel;
	}

}
