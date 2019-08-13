package suite.editor;

import static java.lang.Math.max;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JEditorPane;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Segment;
import javax.swing.undo.UndoManager;

import primal.Verbs.Build;
import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.statics.Rethrow.SinkEx;
import suite.streamlet.Pusher;
import suite.streamlet.ReadChars;

public class EditorPane extends JEditorPane {

	private static final long serialVersionUID = 1l;

	public EditorPane(EditorModel model) {
		var gc = model;
		var document = getDocument();
		var undoManager = new UndoManager();

		SinkEx<ActionEvent, BadLocationException> tabize = event -> {
			if (isSelectedText())
				replaceLines(segment -> Build.string(sb -> {
					sb.append("\t");
					for (var ch : ReadChars.from(segment)) {
						sb.append(ch);
						sb.append(ch == 10 ? "\t" : "");
					}
				}));
			else
				document.insertString(getCaretPosition(), "\t", null);
		};

		SinkEx<ActionEvent, BadLocationException> untabize = event -> {
			if (isSelectedText())
				replaceLines(segment -> {
					var s = segment.toString();
					s = s.charAt(0) == '\t' ? s.substring(1) : s;
					return s.replace("\n\t", "\n");
				});
		};

		bind(KeyEvent.VK_TAB, 0).wire(gc, Listen.catchAll(tabize));
		bind(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK).wire(gc, Listen.catchAll(untabize));
		bind(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK).wire(gc, undoManager::redo);
		bind(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK).wire(gc, undoManager::undo);

		document.addUndoableEditListener(event -> undoManager.addEdit(event.getEdit()));
		Listen.documentChanged(document).wire(gc, event -> model.changeIsModified(true));
	}

	private void replaceLines(Fun<Segment, String> fun) throws BadLocationException {
		var document = getDocument();
		var length = document.getLength();
		var ss = getSelectionStart();
		var se = max(ss, getSelectionEnd() - 1);

		while (0 < ss && document.getText(ss, 1).charAt(0) != 10)
			ss--;
		while (se < length && document.getText(se, 1).charAt(0) != 10)
			se++;

		// do not include first and last LFs
		var start = document.getText(ss, 1).charAt(0) == 10 ? ss + 1 : ss;
		var end = se;

		replace(document, start, end, fun);
	}

	private Pusher<ActionEvent> bind(int keyCode, int modifiers) {
		var keyStroke = KeyStroke.getKeyStroke(keyCode, modifiers);
		var key = Pair.of(keyCode, modifiers);
		getInputMap().put(keyStroke, key);
		return Listen.actionPerformed(this, key);
	}

	private boolean isSelectedText() {
		return getSelectionStart() != getSelectionEnd();
	}

	private void replace(Document document, int start, int end, Fun<Segment, String> f) throws BadLocationException {
		var segment_ = new Segment();
		document.getText(start, end - start, segment_);

		var s = f.apply(segment_);
		document.remove(start, end - start);
		document.insertString(start, s, null);
		setSelectionStart(start);
		setSelectionEnd(start + s.length());
	}

}
