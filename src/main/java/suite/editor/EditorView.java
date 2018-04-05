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

import suite.streamlet.Streamlet;

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

	public void _init(EditorModel model, EditorView view, EditorController controller) {
		this.model = model;
	}

	public JFrame run(EditorController controller, String title) {
		JTextField searchTextField = this.searchTextField = applyDefaults(new JTextField(32));

		DefaultListModel<String> listModel = this.listModel = new DefaultListModel<>();
		listModel.addElement("<Empty>");

		JList<String> searchList = this.searchList = applyDefaults(new JList<>(listModel));
		searchList.setFont(sansFont);

		JLabel rightLabel = this.rightLabel = applyDefaults(new JLabel("Right"));
		rightLabel.setVisible(false);

		JTextField filenameTextField = this.filenameTextField = applyDefaults(new JTextField("pad"));
		filenameTextField.setVisible(false);

		JTextArea messageTextArea = this.messageTextArea = applyDefaults(new JTextArea("Bottom"));
		messageTextArea.setEditable(false);
		messageTextArea.setRows(12);

		JScrollPane messageScrollPane = this.messageScrollPane = newScrollPane(messageTextArea);
		messageScrollPane.setVisible(false);

		JEditorPane editor = this.editor = applyDefaults(new EditorPane(model));

		JScrollPane editorScrollPane = newScrollPane(editor);

		JButton okButton = applyDefaults(new JButton("OK"));
		Listen.mouseClicked(okButton).wire(controller::evaluate);

		JFrame frame = this.frame = new JFrame(title);
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.setJMenuBar(newMenuBar(controller));
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

		Listen.action(searchTextField).wire(event -> controller.searchFiles(model.searchText()));
		Listen.componentResized(frame).wire(this::refresh);
		Listen.documentChanged(filenameTextField).wire(event -> model.changeFilename(filenameTextField.getText()));
		Listen.documentChanged(searchTextField).wire(event -> model.changeSearchText(searchTextField.getText()));
		Listen.keyPressed(searchTextField, KeyEvent.VK_DOWN).wire(event -> controller.downToSearchList());
		Listen.keyPressed(searchList, KeyEvent.VK_ENTER).wire(event -> controller.selectList(searchList.getSelectedValue()));
		Listen.mouseDoubleClicked(searchList).wire(event -> controller.selectList(searchList.getSelectedValue()));
		Listen.windowClosing(frame).wire(controller::close);

		model.filenameChanged().wire(filename -> {
			filenameTextField.setText(filename);
			repaint();
		});
		model.isModifiedChanged().wire(this::repaint);

		controller.newFile();
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

	private JMenuBar newMenuBar(EditorController controller) {
		JMenuItem newMenuItem = applyDefaults(new JMenuItem("New...", KeyEvent.VK_N));
		newMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
		Listen.action(newMenuItem).wire(controller::newFile);

		JMenuItem openMenuItem = applyDefaults(new JMenuItem("Open...", KeyEvent.VK_O));
		openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
		Listen.action(openMenuItem).wire(controller::open);

		JMenuItem saveMenuItem = applyDefaults(new JMenuItem("Save", KeyEvent.VK_S));
		saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		Listen.action(saveMenuItem).wire(controller::save);

		JMenuItem searchMenuItem = applyDefaults(new JMenuItem("Search"));
		searchMenuItem
				.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		Listen.action(searchMenuItem).wire(controller::searchFor);

		JMenuItem exitMenuItem = applyDefaults(new JMenuItem("Close", KeyEvent.VK_C));
		Listen.action(exitMenuItem).wire(controller::close);

		JMenuItem copyMenuItem = applyDefaults(new JMenuItem("Copy"));
		copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
		Listen.action(copyMenuItem).wire(event -> controller.copy(false));

		JMenuItem copyAppendMenuItem = applyDefaults(new JMenuItem("Copy Append"));
		copyAppendMenuItem
				.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		Listen.action(copyAppendMenuItem).wire(event -> controller.copy(true));

		JMenuItem pasteMenuItem = applyDefaults(new JMenuItem("Paste"));
		pasteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
		Listen.action(pasteMenuItem).wire(controller::paste);

		JMenuItem formatMenuItem = applyDefaults(new JMenuItem("Format", KeyEvent.VK_F));
		Listen.action(formatMenuItem).wire(controller::format);

		JMenuItem funFilterMenuItem = applyDefaults(new JMenuItem("Functional Filter...", KeyEvent.VK_U));
		Listen.action(funFilterMenuItem).wire(controller::funFilter);

		JMenuItem unixFilterMenuItem = applyDefaults(new JMenuItem("Unix Filter...", KeyEvent.VK_X));
		Listen.action(unixFilterMenuItem).wire(controller::unixFilter);

		JMenuItem leftMenuItem = applyDefaults(new JMenuItem("Left", KeyEvent.VK_L));
		leftMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
		Listen.action(leftMenuItem).wire(controller::left);

		JMenuItem rightMenuItem = applyDefaults(new JMenuItem("Right", KeyEvent.VK_R));
		rightMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
		Listen.action(rightMenuItem).wire(controller::right);

		JMenuItem topMenuItem = applyDefaults(new JMenuItem("Top", KeyEvent.VK_T));
		topMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.ALT_MASK));
		Listen.action(topMenuItem).wire(controller::top);

		JMenuItem bottomMenuItem = applyDefaults(new JMenuItem("Bottom", KeyEvent.VK_B));
		bottomMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.ALT_MASK));
		Listen.action(bottomMenuItem).wire(controller::bottom);

		JMenuItem evalMenuItem = applyDefaults(new JMenuItem("Evaluate", KeyEvent.VK_E));
		Listen.action(evalMenuItem).wire(controller::evaluate);

		JMenuItem evalTypeMenuItem = applyDefaults(new JMenuItem("Evaluate Type", KeyEvent.VK_T));
		Listen.action(evalTypeMenuItem).wire(controller::evaluateType);

		JMenuItem newWindowMenuItem = applyDefaults(new JMenuItem("New Window", KeyEvent.VK_N));
		Listen.action(newWindowMenuItem).wire(controller::newWindow);

		JMenu fileMenu = newMenu("File", KeyEvent.VK_F, //
				newMenuItem, openMenuItem, saveMenuItem, searchMenuItem, exitMenuItem);

		JMenu editMenu = newMenu("Edit", KeyEvent.VK_E, //
				copyMenuItem, copyAppendMenuItem, pasteMenuItem, formatMenuItem, funFilterMenuItem, unixFilterMenuItem);

		JMenu viewMenu = newMenu("View", KeyEvent.VK_V, //
				leftMenuItem, rightMenuItem, topMenuItem, bottomMenuItem);

		JMenu projectMenu = newMenu("Project", KeyEvent.VK_P, //
				evalMenuItem, evalTypeMenuItem);

		JMenu windowMenu = newMenu("Window", KeyEvent.VK_W, //
				newWindowMenuItem, newWindowMenuItem);

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(viewMenu);
		menuBar.add(projectMenu);
		menuBar.add(windowMenu);

		return menuBar;
	}

	private JMenu newMenu(String title, int keyEvent, JMenuItem... menuItems) {
		JMenu menu = applyDefaults(new JMenu(title));
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
		JTextArea textArea = messageTextArea;
		textArea.setText(text);
		textArea.setEnabled(true);
		textArea.setVisible(true);
		refresh();
	}

	public void showMessageRunning() {
		JTextArea textArea = messageTextArea;
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
		boolean visible = !component.isVisible();
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
		boolean isFocusOwner = component.isFocusOwner();
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
