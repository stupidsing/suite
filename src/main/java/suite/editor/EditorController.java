package suite.editor;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import suite.Suite;
import suite.node.Node;
import suite.node.io.Formatter;
import suite.node.io.PrettyPrinter;
import suite.util.FileUtil;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.To;

public class EditorController {

	private Thread runThread;

	public void bottom(EditorView view) {
		toggleVisible(view, view.getBottomToolbar());
		view.refresh();
	}

	public void close(EditorView view) {
		confirmSave(view, view.getFrame()::dispose);
	}

	public void copy(EditorView view, boolean isAppend) {
		ClipboardUtil clipboardUtil = new ClipboardUtil();
		String selectedText = view.getEditor().getSelectedText();
		if (selectedText != null)
			clipboardUtil.setClipboardText((isAppend ? clipboardUtil.getClipboardText() : "") + selectedText);
	}

	public void downToSearchList(EditorView view) {
		JList<String> leftList = view.getLeftList();
		DefaultListModel<String> listModel = view.getListModel();

		leftList.requestFocusInWindow();

		if (!listModel.isEmpty())
			leftList.setSelectedValue(listModel.get(0), true);
	}

	public void evaluate(EditorView view) {
		run(view, text -> {
			String result;
			try {
				Node node = Suite.evaluateFun(text, true);
				result = Formatter.dump(node);
			} catch (Exception ex) {
				result = To.string(ex);
			}
			return result;
		});
	}

	public void evaluateType(EditorView view) {
		run(view, text -> {
			String result;
			try {
				Node node = Suite.evaluateFunType(text);
				result = Formatter.dump(node);
			} catch (Exception ex) {
				result = To.string(ex);
			}
			return result;
		});
	}

	public void format(EditorView view) {
		JEditorPane editor = view.getEditor();
		Node node = Suite.parse(editor.getText());
		editor.setText(new PrettyPrinter().prettyPrint(node));
	}

	public void left(EditorView view) {
		JComponent left = view.getLeftToolbar();
		if (toggleVisible(view, left))
			view.getSearchTextField().requestFocusInWindow();
		view.refresh();
	}

	public void newFile(EditorView view) {
		JEditorPane editor = view.getEditor();
		editor.setText("");
		editor.requestFocusInWindow();

		view.getFilenameTextField().setText("pad");
		view.setModified(false);
	}

	public void newWindow(EditorView view) {
		EditorController controller = new EditorController();

		EditorView view1 = new EditorView();
		view1.setController(controller);
		view1.run(Editor.class.getSimpleName());
	}

	public void open(EditorView view) {
		confirmSave(view, () -> {
			File dir = new File(view.getFilenameTextField().getText()).getParentFile();
			JFileChooser fileChooser = dir != null ? new JFileChooser(dir) : new JFileChooser();
			if (fileChooser.showOpenDialog(view.getFrame()) == JFileChooser.APPROVE_OPTION)
				load(view, fileChooser.getSelectedFile().getPath());
		});
	}

	public void paste(EditorView view) {
		JEditorPane editor = view.getEditor();
		String orig = editor.getText();
		String pasteText = new ClipboardUtil().getClipboardText();

		if (pasteText != null) {
			int s = editor.getSelectionStart();
			int e = editor.getSelectionEnd();
			editor.setText(orig.substring(0, s) + pasteText + orig.substring(e, orig.length()));
			editor.setCaretPosition(s + pasteText.length());
		}
	}

	public void save(EditorView view) {
		try (OutputStream os = FileUtil.out(view.getFilenameTextField().getText())) {
			os.write(view.getEditor().getText().getBytes(FileUtil.charset));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		view.setModified(false);
	}

	public void right(EditorView view) {
		JComponent right = view.getRightToolbar();
		toggleVisible(view, right);
		view.refresh();
	}

	public void searchFor(EditorView view) {
		JTextField searchTextField = view.getSearchTextField();
		searchTextField.setCaretPosition(0);
		searchTextField.requestFocusInWindow();
	}

	public void searchFiles(EditorView view) {
		DefaultListModel<String> listModel = view.getListModel();
		listModel.clear();

		String text = view.getSearchTextField().getText();

		if (!text.isEmpty()) {
			Source<Path> paths0 = FileUtil.findPaths(Paths.get("."));
			Source<String> files1 = FunUtil.map(Path::toString, paths0);
			Source<String> files2 = FunUtil.filter(filename -> filename.contains(text), files1);

			for (String filename : FunUtil.iter(files2))
				listModel.addElement(filename);
		}
	}

	public void selectList(EditorView view) {
		load(view, view.getLeftList().getSelectedValue());
	}

	public void top(EditorView view) {
		JTextField filenameTextField = view.getFilenameTextField();
		if (toggleVisible(view, filenameTextField))
			filenameTextField.setCaretPosition(filenameTextField.getText().length());
		view.refresh();
	}

	public void unixFilter(EditorView view) {
		JFrame frame = view.getFrame();
		JEditorPane editor = view.getEditor();

		String command = JOptionPane.showInputDialog(frame //
				, "Enter command:", "Unix Filter", JOptionPane.PLAIN_MESSAGE);

		try {
			Process process = Runtime.getRuntime().exec(command);

			try (OutputStream pos = process.getOutputStream(); Writer writer = new OutputStreamWriter(pos, FileUtil.charset)) {
				writer.write(editor.getText());
			}

			process.waitFor();

			editor.setText(To.string(process.getInputStream()));
		} catch (IOException | InterruptedException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void load(EditorView view, String filename) {
		try {
			String text = FileUtil.read(filename);
			view.getFilenameTextField().setText(filename);

			JEditorPane editor = view.getEditor();
			editor.setText(text);
			editor.setCaretPosition(0);
			editor.requestFocusInWindow();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		view.setModified(false);
	}

	private boolean toggleVisible(EditorView view, JComponent component) {
		boolean visible = !component.isVisible();
		component.setVisible(visible);
		if (visible)
			component.requestFocusInWindow();
		else if (isOwningFocus(component))
			view.getEditor().requestFocusInWindow();
		return visible;
	}

	private boolean isOwningFocus(Component component) {
		boolean isFocusOwner = component.isFocusOwner();
		if (component instanceof JComponent)
			for (Component c : ((JComponent) component).getComponents())
				isFocusOwner |= isOwningFocus(c);
		return isFocusOwner;
	}

	private void confirmSave(EditorView view, Runnable action) {
		JFrame frame = view.getFrame();
		if (view.isModified())
			switch (JOptionPane.showConfirmDialog(frame //
					, "Would you like to save your changes?", "Close" //
					, JOptionPane.YES_NO_CANCEL_OPTION)) {
			case JOptionPane.YES_OPTION:
				save(view);
			case JOptionPane.NO_OPTION:
				action.run();
				break;
			default:
			}
	}

	private void run(EditorView view, Fun<String, String> fun) {
		JEditorPane editor = view.getEditor();
		String selectedText = editor.getSelectedText();
		String text = selectedText != null ? selectedText : editor.getText();

		if (runThread == null || !runThread.isAlive()) {
			runThread = new Thread(() -> {
				JTextArea bottomTextArea = view.getMessageTextArea();
				bottomTextArea.setEnabled(false);
				bottomTextArea.setText("RUNNING...");

				String result = fun.apply(text);

				bottomTextArea.setText(result);
				bottomTextArea.setEnabled(true);
				bottomTextArea.setVisible(true);

				view.refresh();
				view.getEditor().requestFocusInWindow();
			});

			runThread.start();
		} else
			JOptionPane.showMessageDialog(view.getFrame(), "Previous evaluation in progress");
	}

}
