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
import javax.swing.text.JTextComponent;

import primal.streamlet.Streamlet;

public class EditorView {

	private int windowWidth = 1280;
	private int windowHeight = 768;

	private FontUtil fontUtil = new FontUtil();
	private Font monoFont = fontUtil.monoFont;
	private Font sansFont = fontUtil.sansFont;

	private EditorModel model;
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

	public void _init(EditorModel model, EditorView view, EditorControl control) {
		this.model = model;
	}

	public JFrame run(EditorControl control, String title) {
		var gc = model;

		var searchTextField = this.searchTextField = applyDefaults(new JTextField(32));

		var listModel = this.listModel = new DefaultListModel<>();
		listModel.addElement("<Empty>");

		var searchList = this.searchList = applyDefaults(new JList<>(listModel));
		searchList.setFont(sansFont);

		var rightLabel = this.rightLabel = applyDefaults(new JLabel("Right"));
		rightLabel.setVisible(false);

		var filenameTextField = this.filenameTextField = applyDefaults(new JTextField("pad"));
		filenameTextField.setVisible(false);

		var messageTextArea = this.messageTextArea = applyDefaults(new JTextArea("Bottom"));
		messageTextArea.setEditable(false);
		messageTextArea.setRows(12);

		var messageScrollPane = this.messageScrollPane = newScrollPane(messageTextArea);
		messageScrollPane.setVisible(false);

		var editor = this.editor = applyDefaults(new EditorPane(model));

		var editorScrollPane = newScrollPane(editor);

		var okButton = applyDefaults(new JButton("OK"));
		Listen.mouseClicked(okButton).wire(gc, control::evaluate);

		var frame = this.frame = new JFrame(title);
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.setJMenuBar(newMenuBar(control));
		frame.setSize(new Dimension(windowWidth, windowHeight));
		frame.setVisible(true);

		int u = 64, u3 = u * 3;

		lay = new LayoutCalculator(frame.getContentPane());

		layout = lay.boxh( //
				lay.ex(u, lay.boxv( //
						lay.fx(24, lay.c(searchTextField)), //
						lay.ex(u, lay.c(searchList)))), //
				lay.ex(u3, lay.boxv( //
						lay.fx(24, lay.c(filenameTextField)), //
						lay.ex(u3, lay.c(editorScrollPane)), //
						lay.fx(8, lay.b()), //
						lay.fx(24, lay.boxh( //
								lay.ex(u3, lay.b()), //
								lay.fx(64, lay.c(okButton)), //
								lay.ex(u3, lay.b()))), //
						lay.ex(u, lay.c(messageScrollPane)))), //
				lay.ex(u, lay.c(rightLabel)));

		Listen.action(searchTextField).wire(gc, event -> control.searchFiles(model.searchText()));
		Listen.componentResized(frame).wire(gc, this::refresh);
		Listen.documentChanged(filenameTextField).wire(gc, event -> model.changeFilename(filenameTextField.getText()));
		Listen.documentChanged(searchTextField).wire(gc, event -> model.changeSearchText(searchTextField.getText()));
		Listen.keyPressed(searchTextField, KeyEvent.VK_DOWN).wire(gc, event -> control.downToSearchList());
		Listen.keyPressed(searchList, KeyEvent.VK_ENTER).wire(gc,
				event -> control.selectList(searchList.getSelectedValue()));
		Listen.mouseDoubleClicked(searchList).wire(gc, event -> control.selectList(searchList.getSelectedValue()));
		Listen.windowClosing(frame).wire(gc, control::close);

		model.filenameChanged().wire(gc, filename -> {
			filenameTextField.setText(filename);
			repaint();
		});
		model.isModifiedChanged().wire(gc, this::repaint);

		control.newFile();
		refresh();
		editor.requestFocusInWindow();

		return frame;
	}

	public void refresh() {
		if (lay != null && layout != null)
			lay.arrange(layout);
		repaint();
	}

	public void repaint() {
		frame.setTitle((model.isModified() ? "* " : "") + model.filename().replace(File.separatorChar, '/'));
		frame.revalidate();
		frame.repaint();
	}

	private JMenuBar newMenuBar(EditorControl control) {
		var gc = model;

		var newMenuItem = applyDefaults(new JMenuItem("New...", KeyEvent.VK_N));
		newMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
		Listen.action(newMenuItem).wire(gc, control::newFile);

		var openMenuItem = applyDefaults(new JMenuItem("Open...", KeyEvent.VK_O));
		openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
		Listen.action(openMenuItem).wire(gc, control::open);

		var saveMenuItem = applyDefaults(new JMenuItem("Save", KeyEvent.VK_S));
		saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		Listen.action(saveMenuItem).wire(gc, control::save);

		var searchMenuItem = applyDefaults(new JMenuItem("Search"));
		searchMenuItem
				.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		Listen.action(searchMenuItem).wire(gc, control::searchFor);

		var exitMenuItem = applyDefaults(new JMenuItem("Close", KeyEvent.VK_C));
		Listen.action(exitMenuItem).wire(gc, control::close);

		var copyMenuItem = applyDefaults(new JMenuItem("Copy"));
		copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
		Listen.action(copyMenuItem).wire(gc, event -> control.copy(false));

		var copyAppendMenuItem = applyDefaults(new JMenuItem("Copy Append"));
		copyAppendMenuItem
				.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		Listen.action(copyAppendMenuItem).wire(gc, event -> control.copy(true));

		var pasteMenuItem = applyDefaults(new JMenuItem("Paste"));
		pasteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
		Listen.action(pasteMenuItem).wire(gc, control::paste);

		var formatMenuItem = applyDefaults(new JMenuItem("Format", KeyEvent.VK_F));
		Listen.action(formatMenuItem).wire(gc, control::format);

		var funFilterMenuItem = applyDefaults(new JMenuItem("Functional Filter...", KeyEvent.VK_U));
		Listen.action(funFilterMenuItem).wire(gc, control::funFilter);

		var unixFilterMenuItem = applyDefaults(new JMenuItem("Unix Filter...", KeyEvent.VK_X));
		Listen.action(unixFilterMenuItem).wire(gc, control::unixFilter);

		var leftMenuItem = applyDefaults(new JMenuItem("Left", KeyEvent.VK_L));
		leftMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
		Listen.action(leftMenuItem).wire(gc, control::left);

		var rightMenuItem = applyDefaults(new JMenuItem("Right", KeyEvent.VK_R));
		rightMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
		Listen.action(rightMenuItem).wire(gc, control::right);

		var topMenuItem = applyDefaults(new JMenuItem("Top", KeyEvent.VK_T));
		topMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.ALT_MASK));
		Listen.action(topMenuItem).wire(gc, control::top);

		var bottomMenuItem = applyDefaults(new JMenuItem("Bottom", KeyEvent.VK_B));
		bottomMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.ALT_MASK));
		Listen.action(bottomMenuItem).wire(gc, control::bottom);

		var evalMenuItem = applyDefaults(new JMenuItem("Evaluate", KeyEvent.VK_E));
		Listen.action(evalMenuItem).wire(gc, control::evaluate);

		var evalTypeMenuItem = applyDefaults(new JMenuItem("Evaluate Type", KeyEvent.VK_T));
		Listen.action(evalTypeMenuItem).wire(gc, control::evaluateType);

		var newWindowMenuItem = applyDefaults(new JMenuItem("New Window", KeyEvent.VK_N));
		Listen.action(newWindowMenuItem).wire(gc, control::newWindow);

		var fileMenu = newMenu("File", KeyEvent.VK_F, //
				newMenuItem, openMenuItem, saveMenuItem, searchMenuItem, exitMenuItem);

		var editMenu = newMenu("Edit", KeyEvent.VK_E, //
				copyMenuItem, copyAppendMenuItem, pasteMenuItem, formatMenuItem, funFilterMenuItem, unixFilterMenuItem);

		var viewMenu = newMenu("View", KeyEvent.VK_V, //
				leftMenuItem, rightMenuItem, topMenuItem, bottomMenuItem);

		var projectMenu = newMenu("Project", KeyEvent.VK_P, //
				evalMenuItem, evalTypeMenuItem);

		var windowMenu = newMenu("Window", KeyEvent.VK_W, //
				newWindowMenuItem, newWindowMenuItem);

		var menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(viewMenu);
		menuBar.add(projectMenu);
		menuBar.add(windowMenu);

		return menuBar;
	}

	private JMenu newMenu(String title, int keyEvent, JMenuItem... menuItems) {
		var menu = applyDefaults(new JMenu(title));
		menu.setMnemonic(keyEvent);

		for (var component : menuItems)
			menu.add(component);

		return menu;
	}

	private JScrollPane newScrollPane(Component component) {
		return new JScrollPane(component //
				, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED //
				, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	}

	private <T extends JComponent> T applyDefaults(T t) {
		t.setFont(monoFont);
		t.setAlignmentX(Component.CENTER_ALIGNMENT);
		return t;
	}

	public void focusLeftList() {
		searchList.requestFocusInWindow();

		if (!listModel.isEmpty())
			searchList.setSelectedValue(listModel.get(0), true);
	}

	public void focusSearchTextField() {
		focus(searchTextField);
	}

	public void showMessage(String text) {
		var textArea = messageTextArea;
		textArea.setText(text);
		textArea.setEnabled(true);
		textArea.setVisible(true);
		refresh();
	}

	public void showMessageRunning() {
		var textArea = messageTextArea;
		textArea.setEnabled(false);
		textArea.setText("RUNNING...");
	}

	public void showSearchFileResult(Streamlet<String> filenames) {
		listModel.clear();
		for (var filename : filenames)
			listModel.addElement(filename);

	}

	public void toggleBottom() {
		toggleVisible(messageScrollPane);
	}

	public void toggleLeft() {
		toggleVisible(searchTextField);
	}

	public void toggleRight() {
		toggleVisible(rightLabel);
	}

	public void toggleTop() {
		toggleVisible(filenameTextField);
	}

	private boolean toggleVisible(JComponent component) {
		var visible = !component.isVisible();
		component.setVisible(visible);
		refresh();
		if (visible)
			if (component instanceof JTextComponent)
				focus((JTextComponent) component);
			else
				component.requestFocusInWindow();
		else if (isOwningFocus(component))
			getEditor().requestFocusInWindow();
		return visible;
	}

	private void focus(JTextComponent component) {
		component.selectAll();
		component.requestFocusInWindow();
	}

	private boolean isOwningFocus(Component component) {
		var isFocusOwner = component.isFocusOwner();
		if (component instanceof JComponent)
			for (var c : ((JComponent) component).getComponents())
				isFocusOwner |= isOwningFocus(c);
		return isFocusOwner;
	}

	public JEditorPane getEditor() {
		return editor;
	}

	public JFrame getFrame() {
		return frame;
	}

	public JList<String> getSearchList() {
		return searchList;
	}

}
