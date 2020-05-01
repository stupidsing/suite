package suite.editor;

import primal.Nouns.Utf8;
import primal.Verbs.ReadString;
import primal.Verbs.Start;
import primal.Verbs.WriteFile;
import primal.fp.Funs.Iterate;
import suite.Suite;
import suite.node.io.Formatter;
import suite.node.pp.PrettyPrinter;
import suite.os.FileUtil;
import suite.util.To;

import javax.swing.*;
import java.io.File;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import static primal.statics.Rethrow.ex;

public class EditorControl {

	private Thread runThread;
	private EditorModel model;
	private EditorView view;

	public EditorControl() {
	}

	public void _init(EditorModel model, EditorView view, EditorControl control) {
		this.model = model;
		this.view = view;
	}

	public void bottom() {
		view.toggleBottom();
	}

	public void close() {
		confirmSave(view.getFrame()::dispose);
	}

	public void copy(boolean isAppend) {
		var clipboardUtil = new ClipboardUtil();
		var selectedText = view.getEditor().getSelectedText();
		if (selectedText != null)
			clipboardUtil.setClipboardText((isAppend ? clipboardUtil.getClipboardText() : "") + selectedText);
	}

	public void downToSearchList() {
		view.focusLeftList();
	}

	public void evaluate() {
		run(text -> {
			String result;
			try {
				var node = Suite.evaluateFun(text, true);
				result = Formatter.dump(node);
			} catch (Exception ex) {
				result = To.string(ex);
			}
			return result;
		});
	}

	public void evaluateType() {
		run(text -> {
			String result;
			try {
				var node = Suite.evaluateFunType(text);
				result = Formatter.dump(node);
			} catch (Exception ex) {
				result = To.string(ex);
			}
			return result;
		});
	}

	public void format() {
		var editor = view.getEditor();
		var node = Suite.parse(editor.getText());
		editor.setText(new PrettyPrinter().prettyPrint(node));
	}

	public void funFilter() {
		var isDo = false;
		var frame = view.getFrame();
		var editor = view.getEditor();

		var fun = JOptionPane.showInputDialog(frame
				, "Enter " + (isDo ? "do " : "") + "function:", "Functional Filter", JOptionPane.PLAIN_MESSAGE);

		editor.setText(Suite.evaluateFilterFun(fun, editor.getText(), false, false));
	}

	public void left() {
		view.toggleLeft();
	}

	public void newFile() {
		confirmSave(() -> {
			var editor = view.getEditor();
			editor.setText("");
			editor.requestFocusInWindow();

			model.changeFilename("pad");
			model.changeIsModified(false);
		});
	}

	public void newWindow() {
		var model = new EditorModel();
		var control = new EditorControl();
		var view1 = new EditorView();

		view1._init(model, view1, control);
		control._init(model, view1, control);

		view1.run(control, EditorMain.class.getSimpleName());
	}

	public void open() {
		confirmSave(() -> {
			var dir = new File(model.filename()).getParentFile();
			var fileChooser = dir != null ? new JFileChooser(dir) : new JFileChooser();
			if (fileChooser.showOpenDialog(view.getFrame()) == JFileChooser.APPROVE_OPTION)
				load(fileChooser.getSelectedFile().getPath());
		});
	}

	public void paste() {
		var editor = view.getEditor();
		var orig = editor.getText();
		var pasteText = new ClipboardUtil().getClipboardText();

		if (pasteText != null) {
			var s = editor.getSelectionStart();
			var e = editor.getSelectionEnd();
			editor.setText(orig.substring(0, s) + pasteText + orig.substring(e, orig.length()));
			editor.setCaretPosition(s + pasteText.length());
		}
	}

	public void save() {
		WriteFile.to(model.filename()).doWrite(os -> os.write(view.getEditor().getText().getBytes(Utf8.charset)));
		model.changeIsModified(false);
	}

	public void right() {
		view.toggleRight();
	}

	public void searchFor() {
		view.focusSearchTextField();
	}

	public void searchFiles(String text) {
		if (!text.isEmpty()) {
			var files = FileUtil
					.findPaths(Paths.get("."))
					.map(Path::toString)
					.filter(filename -> filename.contains(text));

			view.showSearchFileResult(files);
		}
	}

	public void selectList(String selectedValue) {
		load(selectedValue);
	}

	public void top() {
		view.toggleTop();
	}

	public void unixFilter() {
		var frame = view.getFrame();
		var editor = view.getEditor();

		var command = JOptionPane.showInputDialog(frame, "Enter command:", "Unix Filter", JOptionPane.PLAIN_MESSAGE);

		var text0 = editor.getText();

		var text1 = ex(() -> {
			var process = Runtime.getRuntime().exec(command);

			try (var pos = process.getOutputStream(); var writer = new OutputStreamWriter(pos, Utf8.charset)) {
				writer.write(text0);
			}

			process.waitFor();

			return ReadString.from(process.getInputStream());
		});

		editor.setText(text1);
	}

	private void load(String filename) {
		var text = ReadString.from(filename);

		var editor = view.getEditor();
		editor.setText(text);
		editor.setCaretPosition(0);
		editor.requestFocusInWindow();

		model.changeFilename(filename);
		model.changeIsModified(false);
	}

	private void confirmSave(Runnable action) {
		var frame = view.getFrame();
		if (model.isModified())
			switch (JOptionPane.showConfirmDialog(frame,
					"Would you like to save your changes?",
					"Close",
					JOptionPane.YES_NO_CANCEL_OPTION)) {
			case JOptionPane.YES_OPTION:
				save();
			case JOptionPane.NO_OPTION:
				action.run();
				break;
			default:
			}
		else
			action.run();
	}

	private void run(Iterate<String> fun) {
		var editor = view.getEditor();
		var selectedText = editor.getSelectedText();
		var text = selectedText != null ? selectedText : editor.getText();

		if (runThread == null || !runThread.isAlive())
			runThread = Start.thread(() -> {
				view.showMessageRunning();
				view.showMessage(fun.apply(text));
				view.getEditor().requestFocusInWindow();
			});
		else
			JOptionPane.showMessageDialog(view.getFrame(), "Previous evaluation in progress");
	}

}
