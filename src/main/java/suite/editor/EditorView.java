package suite.editor;

import java.awt.BorderLayout;
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
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

public class EditorView {

	private static final int windowWidth = 1280;
	private static final int windowHeight = 768;

	private Font font = new Font("Akkurat-Mono", Font.PLAIN, 12);

	private EditorController controller;

	private JFrame frame;

	private JPanel leftPanel;
	private JLabel rightLabel;
	private JLabel topLabel;
	private JPanel bottomPanel;

	private JTextField leftTextField;

	private JTextArea bottomTextArea;

	private JEditorPane editor;

	public JFrame run() {
		JTextField leftTextField = this.leftTextField = applyDefaults(new JTextField(32));

		DefaultListModel<String> listModel = new DefaultListModel<>();
		listModel.addElement("Jane Doe");
		listModel.addElement("John Smith");
		listModel.addElement("Kathy Green");

		JList<String> leftList = applyDefaults(new JList<>(listModel));

		Dimension verticalPanelDim = new Dimension(windowWidth / 4, windowHeight);

		JLabel rightLabel = this.rightLabel = fixSize(applyDefaults(new JLabel("Right")), verticalPanelDim);
		rightLabel.setVisible(false);

		JLabel topLabel = this.topLabel = applyDefaults(new JLabel("Top"));
		topLabel.setVisible(false);

		JTextArea bottomTextArea = this.bottomTextArea = applyDefaults(new JTextArea("Bottom"));
		bottomTextArea.setEditable(false);
		bottomTextArea.setRows(12);
		bottomTextArea.setVisible(false);

		JScrollPane scrollPane = new JScrollPane(bottomTextArea //
				, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED //
				, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		JPanel leftPanel = this.leftPanel = fixSize(new JPanel(), verticalPanelDim);
		leftPanel.setLayout(new BorderLayout());
		leftPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		leftPanel.add(leftTextField, BorderLayout.PAGE_START);
		leftPanel.add(leftList, BorderLayout.CENTER);

		JPanel bottomPanel = this.bottomPanel = createBoxLayoutPanel(BoxLayout.Y_AXIS, scrollPane);
		bottomPanel.setMaximumSize(new Dimension(windowWidth, windowHeight / 4));

		JEditorPane editor = this.editor = applyDefaults(new JEditorPane());

		Component box = Box.createRigidArea(new Dimension(8, 8));

		JButton okButton = applyDefaults(new JButton("OK"));
		okButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent event) {
				controller.run(EditorView.this);
			}
		});

		JPanel verticalPanel = createBoxLayoutPanel(BoxLayout.Y_AXIS, topLabel, editor, box, okButton, bottomPanel);

		// Flow layout allows the components to be their preferred size
		JPanel horizontalPanel = createBoxLayoutPanel(BoxLayout.X_AXIS, leftPanel, verticalPanel, rightLabel);
		horizontalPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JFrame frame = this.frame = new JFrame(getClass().getSimpleName());
		frame.setContentPane(horizontalPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setJMenuBar(createMenuBar());
		frame.setSize(new Dimension(windowWidth, windowHeight));
		frame.setVisible(true);

		repaint();

		editor.requestFocus();

		return frame;
	}

	public void repaint() {
		frame.revalidate();
		frame.repaint();
	}

	private JMenuBar createMenuBar() {
		final EditorView view = this;

		JMenuItem openMenuItem = applyDefaults(new JMenuItem("Open...", KeyEvent.VK_O));
		openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));

		JMenuItem saveMenuItem = applyDefaults(new JMenuItem("Save", KeyEvent.VK_S));
		saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));

		JMenuItem exitMenuItem = applyDefaults(new JMenuItem("Exit", KeyEvent.VK_X));
		exitMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				controller.quit(EditorView.this);
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

		JMenuItem bottomMenuItem = applyDefaults(new JMenuItem("Bottom", KeyEvent.VK_B));
		bottomMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.ALT_MASK));
		bottomMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				controller.bottom(view);
			}
		});

		JMenuItem runMenuItem = applyDefaults(new JMenuItem("Run", KeyEvent.VK_R));
		runMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				controller.run(view);
			}
		});

		JMenu fileMenu = createMenu("File", KeyEvent.VK_F, openMenuItem, saveMenuItem, exitMenuItem);
		JMenu editMenu = createMenu("Edit", KeyEvent.VK_E);
		JMenu viewMenu = createMenu("View", KeyEvent.VK_V, leftMenuItem, rightMenuItem, topMenuItem, bottomMenuItem);
		JMenu projectMenu = createMenu("Project", KeyEvent.VK_P, runMenuItem);

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(viewMenu);
		menuBar.add(projectMenu);

		return menuBar;
	}

	private JMenu createMenu(String title, int keyEvent, JMenuItem... menuItems) {
		JMenu menu = applyDefaults(new JMenu(title));
		menu.setMnemonic(keyEvent);

		for (Component component : menuItems)
			menu.add(component);

		return menu;
	}

	private JPanel createBoxLayoutPanel(int boxLayout, Component... components) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, boxLayout));

		for (Component component : components)
			panel.add(component);

		return panel;
	}

	private <T extends JComponent> T fixSize(T t, Dimension dim) {
		t.setMinimumSize(dim);
		t.setMaximumSize(dim);
		return t;
	}

	private <T extends JComponent> T applyDefaults(T t) {
		t.setFont(font);
		t.setAlignmentX(Component.CENTER_ALIGNMENT);
		return t;
	}

	public void setController(EditorController controller) {
		this.controller = controller;
	}

	public JPanel getBottomToolbar() {
		return bottomPanel;
	}

	public JPanel getLeftToolbar() {
		return leftPanel;
	}

	public JComponent getRightToolbar() {
		return rightLabel;
	}

	public JLabel getTopToolbar() {
		return topLabel;
	}

	public JTextField getLeftTextField() {
		return leftTextField;
	}

	public JTextArea getBottomTextArea() {
		return bottomTextArea;
	}

	public JEditorPane getEditor() {
		return editor;
	}

}
