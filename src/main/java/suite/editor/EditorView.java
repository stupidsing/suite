package suite.editor;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
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
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import suite.editor.Layout.Node;
import suite.editor.Layout.Orientation;

public class EditorView {

	private static final int windowWidth = 1280;
	private static final int windowHeight = 768;

	private Font font = new Font("Akkurat-Mono", Font.PLAIN, 12);
	private Font narrowFont = new Font("Sans", Font.PLAIN, 12);

	private EditorController controller;

	private JTextArea bottomTextArea;
	private JEditorPane editor;
	private JFrame frame;
	private Node layout;
	private JList<String> leftList;
	private JTextField leftTextField;
	private DefaultListModel<String> listModel;
	private JLabel rightLabel;
	private JScrollPane scrollPane;
	private JLabel topLabel;

	public JFrame run(String title) {
		JTextField leftTextField = this.leftTextField = applyDefaults(new JTextField(32));
		leftTextField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				controller.searchFiles(EditorView.this);
			}
		});
		leftTextField.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.VK_DOWN)
					controller.downLeftList(EditorView.this);
			}
		});

		DefaultListModel<String> listModel = this.listModel = new DefaultListModel<>();
		listModel.addElement("<Empty>");

		JList<String> leftList = this.leftList = applyDefaults(new JList<>(listModel));
		leftList.setFont(narrowFont);
		leftList.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.VK_ENTER)
					controller.selectList(EditorView.this);
			}
		});
		leftList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent event) {
				if (event.getClickCount() == 2)
					controller.selectList(EditorView.this);
			}
		});

		JLabel rightLabel = this.rightLabel = applyDefaults(new JLabel("Right"));
		rightLabel.setVisible(false);

		JLabel topLabel = this.topLabel = applyDefaults(new JLabel("Top"));
		topLabel.setVisible(false);

		JTextArea bottomTextArea = this.bottomTextArea = applyDefaults(new JTextArea("Bottom"));
		bottomTextArea.setEditable(false);
		bottomTextArea.setRows(12);
		bottomTextArea.setVisible(false);

		JScrollPane scrollPane = this.scrollPane = new JScrollPane(bottomTextArea //
				, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED //
				, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		JEditorPane editor = this.editor = applyDefaults(new JEditorPane());

		Component box = Box.createRigidArea(new Dimension(8, 8));

		JButton okButton = applyDefaults(new JButton("OK"));
		okButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent event) {
				controller.evaluate(EditorView.this);
			}
		});

		JFrame frame = this.frame = new JFrame(title);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setJMenuBar(createMenuBar());
		frame.setSize(new Dimension(windowWidth, windowHeight));
		frame.setVisible(true);
		frame.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent event) {
				calculateLayout();
			}
		});

		int u = 64, u2 = u * 2;

		layout = Layout.lay(Orientation.HORIZONTAL //
				, Layout.lay(Orientation.VERTICAL //
						, Layout.hb(leftTextField, u, 24) //
						, Layout.co(leftList, u, u) //
				) //
				, Layout.lay(Orientation.VERTICAL //
						, Layout.hb(topLabel, u2, 64) //
						, Layout.co(editor, u2, u2) //
						, Layout.hb(box, u2, 8) //
						, Layout.lay(Orientation.HORIZONTAL //
								, Layout.hb(null, u2, 24) //
								, Layout.fx(okButton, 64, 24) //
								, Layout.hb(null, u2, 24) //
						) //
						, Layout.lay(Orientation.VERTICAL //
								, Layout.co(scrollPane, u2, u) //
						) //
				) //
				, Layout.co(rightLabel, u, u) //
				);

		controller.newFile(this);
		repaint();

		return frame;
	}

	public void repaint() {
		calculateLayout();
		frame.repaint();
	}

	private void calculateLayout() {
		new LayoutCalculator().arrange(frame.getContentPane(), layout);
	}

	private JMenuBar createMenuBar() {
		final EditorView view = this;

		JMenuItem newMenuItem = applyDefaults(new JMenuItem("New...", KeyEvent.VK_N));
		newMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
		newMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				controller.newFile(view);
			}
		});

		JMenuItem openMenuItem = applyDefaults(new JMenuItem("Open...", KeyEvent.VK_O));
		openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));

		JMenuItem saveMenuItem = applyDefaults(new JMenuItem("Save", KeyEvent.VK_S));
		saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));

		JMenuItem searchMenuItem = applyDefaults(new JMenuItem("Search"));
		searchMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
		searchMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				controller.searchFor(view);
			}
		});

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

		JMenuItem evalMenuItem = applyDefaults(new JMenuItem("Evaluate", KeyEvent.VK_E));
		evalMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				controller.evaluate(view);
			}
		});

		JMenuItem evalTypeMenuItem = applyDefaults(new JMenuItem("Evaluate Type", KeyEvent.VK_T));
		evalTypeMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				controller.evaluateType(view);
			}
		});

		JMenu fileMenu = createMenu("File", KeyEvent.VK_F //
				, newMenuItem, openMenuItem, saveMenuItem, searchMenuItem, exitMenuItem);

		JMenu editMenu = createMenu("Edit", KeyEvent.VK_E //
		);

		JMenu viewMenu = createMenu("View", KeyEvent.VK_V //
				, leftMenuItem, rightMenuItem, topMenuItem, bottomMenuItem);

		JMenu projectMenu = createMenu("Project", KeyEvent.VK_P //
				, evalMenuItem, evalTypeMenuItem);

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

	private <T extends JComponent> T applyDefaults(T t) {
		t.setFont(font);
		t.setAlignmentX(Component.CENTER_ALIGNMENT);
		return t;
	}

	public void setController(EditorController controller) {
		this.controller = controller;
	}

	public JComponent getBottomToolbar() {
		return scrollPane;
	}

	public JList<String> getLeftList() {
		return leftList;
	}

	public JComponent getLeftToolbar() {
		return leftTextField;
	}

	public DefaultListModel<String> getListModel() {
		return listModel;
	}

	public JComponent getRightToolbar() {
		return rightLabel;
	}

	public JLabel getTopToolbar() {
		return topLabel;
	}

	public JTextArea getBottomTextArea() {
		return bottomTextArea;
	}

	public JEditorPane getEditor() {
		return editor;
	}

	public JFrame getFrame() {
		return frame;
	}

	public JTextField getLeftTextField() {
		return leftTextField;
	}

}
