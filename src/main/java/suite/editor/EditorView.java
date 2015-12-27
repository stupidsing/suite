package suite.editor;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;

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
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;

import suite.editor.LayoutCalculator.Orientation;

public class EditorView {

	private int windowWidth = 1280;
	private int windowHeight = 768;

	private FontUtil fontUtil = new FontUtil();
	private Font monoFont = fontUtil.monoFont;
	private Font sansFont = fontUtil.sansFont;

	private EditorController controller;
	private LayoutCalculator lay;
	private LayoutCalculator.Node layout;

	private EditorPane editor;
	private JFrame frame;
	private JList<String> searchList;
	private JTextField searchTextField;
	private JTextArea messageTextArea;
	private DefaultListModel<String> listModel;
	private JLabel rightLabel;
	private JScrollPane messageScrollPane;
	private JTextField filenameTextField;

	public JFrame run(String title) {
		JTextField searchTextField = this.searchTextField = applyDefaults(new JTextField(32));
		searchTextField.addActionListener(event -> controller.searchFiles(this));
		Listen.keyPressed(searchTextField).register(event -> {
			if (event.getKeyCode() == KeyEvent.VK_DOWN)
				controller.downToSearchList(this);
		});

		DefaultListModel<String> listModel = this.listModel = new DefaultListModel<>();
		listModel.addElement("<Empty>");

		JList<String> searchList = this.searchList = applyDefaults(new JList<>(listModel));
		searchList.setFont(sansFont);
		Listen.keyPressed(searchList).register(event -> {
			if (event.getKeyCode() == KeyEvent.VK_ENTER)
				controller.selectList(this);
		});
		Listen.mouseClicked(searchList).register(event -> {
			if (event.getClickCount() == 2)
				controller.selectList(this);
		});

		JLabel rightLabel = this.rightLabel = applyDefaults(new JLabel("Right"));
		rightLabel.setVisible(false);

		JTextField filenameTextField = this.filenameTextField = applyDefaults(new JTextField("pad"));
		filenameTextField.setVisible(false);

		JTextArea messageTextArea = this.messageTextArea = applyDefaults(new JTextArea("Bottom"));
		messageTextArea.setEditable(false);
		messageTextArea.setRows(12);
		messageTextArea.setVisible(false);

		JScrollPane messageScrollPane = this.messageScrollPane = createScrollPane(messageTextArea);

		JEditorPane editor = this.editor = applyDefaults(new EditorPane());

		JScrollPane editorScrollPane = createScrollPane(editor);

		JButton okButton = applyDefaults(new JButton("OK"));
		Listen.mouseClicked(okButton).register(event -> controller.evaluate(this));

		JFrame frame = this.frame = new JFrame(title);
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.setJMenuBar(createMenuBar());
		frame.setSize(new Dimension(windowWidth, windowHeight));
		frame.setVisible(true);
		Listen.componentResized(frame).register(event -> refresh());
		Listen.windowClosing(frame).register(event -> controller.close(this));

		int u = 64, u3 = u * 3;

		lay = new LayoutCalculator(frame.getContentPane());
		layout = lay.box(Orientation.HORIZONTAL, //
				lay.ex(u,
						lay.box(Orientation.VERTICAL, //
								lay.fx(24, lay.c(searchTextField)), //
								lay.ex(u, lay.c(searchList)))), //
				lay.ex(u3,
						lay.box(Orientation.VERTICAL, //
								lay.fx(24, lay.c(filenameTextField)), //
								lay.ex(u3, lay.c(editorScrollPane)), //
								lay.fx(8, lay.b()), //
								lay.fx(24,
										lay.box(Orientation.HORIZONTAL, //
												lay.ex(u3, lay.b()), //
												lay.fx(64, lay.c(okButton)), //
												lay.ex(u3, lay.b()))), //
								lay.ex(u, lay.c(messageScrollPane)))), //
				lay.ex(u, lay.c(rightLabel)));

		controller.newFile(this);
		refresh();
		editor.requestFocusInWindow();

		return frame;
	}

	public void setModified(boolean isModified) {
		editor.setModified(isModified);
		repaint();
	}

	public void refresh() {
		if (lay != null && layout != null)
			lay.arrange(layout);
		repaint();
	}

	private void repaint() {
		frame.setTitle((editor.isModified() ? "* " : "") + filenameTextField.getText().replace(File.separatorChar, '/'));
		frame.revalidate();
		frame.repaint();
	}

	private JMenuBar createMenuBar() {
		EditorView view = this;

		JMenuItem newMenuItem = applyDefaults(new JMenuItem("New...", KeyEvent.VK_N));
		newMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
		newMenuItem.addActionListener(event -> controller.newFile(view));

		JMenuItem openMenuItem = applyDefaults(new JMenuItem("Open...", KeyEvent.VK_O));
		openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
		openMenuItem.addActionListener(event -> controller.open(view));

		JMenuItem saveMenuItem = applyDefaults(new JMenuItem("Save", KeyEvent.VK_S));
		saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		saveMenuItem.addActionListener(event -> controller.save(view));

		JMenuItem searchMenuItem = applyDefaults(new JMenuItem("Search"));
		searchMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		searchMenuItem.addActionListener(event -> controller.searchFor(view));

		JMenuItem exitMenuItem = applyDefaults(new JMenuItem("Close", KeyEvent.VK_C));
		exitMenuItem.addActionListener(event -> controller.close(this));

		JMenuItem copyMenuItem = applyDefaults(new JMenuItem("Copy"));
		copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
		copyMenuItem.addActionListener(event -> controller.copy(view, false));

		JMenuItem copyAppendMenuItem = applyDefaults(new JMenuItem("Copy Append"));
		copyAppendMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		copyAppendMenuItem.addActionListener(event -> controller.copy(view, true));

		JMenuItem pasteMenuItem = applyDefaults(new JMenuItem("Paste"));
		pasteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
		pasteMenuItem.addActionListener(event -> controller.paste(view));

		JMenuItem formatMenuItem = applyDefaults(new JMenuItem("Format", KeyEvent.VK_F));
		formatMenuItem.addActionListener(event -> controller.format(this));

		JMenuItem funFilterMenuItem = applyDefaults(new JMenuItem("Functional Filter...", KeyEvent.VK_U));
		funFilterMenuItem.addActionListener(event -> controller.funFilter(this));

		JMenuItem unixFilterMenuItem = applyDefaults(new JMenuItem("Unix Filter...", KeyEvent.VK_X));
		unixFilterMenuItem.addActionListener(event -> controller.unixFilter(this));

		JMenuItem leftMenuItem = applyDefaults(new JMenuItem("Left", KeyEvent.VK_L));
		leftMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
		leftMenuItem.addActionListener(event -> controller.left(view));

		JMenuItem rightMenuItem = applyDefaults(new JMenuItem("Right", KeyEvent.VK_R));
		rightMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
		rightMenuItem.addActionListener(event -> controller.right(view));

		JMenuItem topMenuItem = applyDefaults(new JMenuItem("Top", KeyEvent.VK_T));
		topMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.ALT_MASK));
		topMenuItem.addActionListener(event -> controller.top(view));

		JMenuItem bottomMenuItem = applyDefaults(new JMenuItem("Bottom", KeyEvent.VK_B));
		bottomMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.ALT_MASK));
		bottomMenuItem.addActionListener(event -> controller.bottom(view));

		JMenuItem evalMenuItem = applyDefaults(new JMenuItem("Evaluate", KeyEvent.VK_E));
		evalMenuItem.addActionListener(event -> controller.evaluate(view));

		JMenuItem evalTypeMenuItem = applyDefaults(new JMenuItem("Evaluate Type", KeyEvent.VK_T));
		evalTypeMenuItem.addActionListener(event -> controller.evaluateType(view));

		JMenuItem newWindowMenuItem = applyDefaults(new JMenuItem("New Window", KeyEvent.VK_N));
		newWindowMenuItem.addActionListener(event -> controller.newWindow(view));

		JMenu fileMenu = createMenu("File", KeyEvent.VK_F, //
				newMenuItem, openMenuItem, saveMenuItem, searchMenuItem, exitMenuItem);

		JMenu editMenu = createMenu("Edit", KeyEvent.VK_E, //
				copyMenuItem, copyAppendMenuItem, pasteMenuItem, formatMenuItem, funFilterMenuItem, unixFilterMenuItem);

		JMenu viewMenu = createMenu("View", KeyEvent.VK_V, //
				leftMenuItem, rightMenuItem, topMenuItem, bottomMenuItem);

		JMenu projectMenu = createMenu("Project", KeyEvent.VK_P, //
				evalMenuItem, evalTypeMenuItem);

		JMenu windowMenu = createMenu("Window", KeyEvent.VK_W, //
				newWindowMenuItem, newWindowMenuItem);

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(viewMenu);
		menuBar.add(projectMenu);
		menuBar.add(windowMenu);

		return menuBar;
	}

	private JMenu createMenu(String title, int keyEvent, JMenuItem... menuItems) {
		JMenu menu = applyDefaults(new JMenu(title));
		menu.setMnemonic(keyEvent);

		for (Component component : menuItems)
			menu.add(component);

		return menu;
	}

	private JScrollPane createScrollPane(Component component) {
		return new JScrollPane(component //
				, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED //
				, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	}

	private <T extends JComponent> T applyDefaults(T t) {
		t.setFont(monoFont);
		t.setAlignmentX(Component.CENTER_ALIGNMENT);
		return t;
	}

	public void setController(EditorController controller) {
		this.controller = controller;
	}

	public boolean isModified() {
		return editor.isModified();
	}

	public JComponent getBottomToolbar() {
		return messageScrollPane;
	}

	public JList<String> getLeftList() {
		return searchList;
	}

	public JComponent getLeftToolbar() {
		return searchTextField;
	}

	public DefaultListModel<String> getListModel() {
		return listModel;
	}

	public JTextArea getMessageTextArea() {
		return messageTextArea;
	}

	public JComponent getRightToolbar() {
		return rightLabel;
	}

	public JTextField getSearchTextField() {
		return searchTextField;
	}

	public JTextField getFilenameTextField() {
		return filenameTextField;
	}

	public JEditorPane getEditor() {
		return editor;
	}

	public JFrame getFrame() {
		return frame;
	}

}
