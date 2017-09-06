package suite.editor;

import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JEditorPane;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Segment;
import javax.swing.undo.UndoManager;

import suite.adt.pair.Pair;
import suite.editor.Listen.SinkEx;
import suite.streamlet.Signal;
import suite.util.FunUtil.Fun;
import suite.util.String_;

public class EditorPane extends JEditorPane {

	private static final long serialVersionUID = 1l;

	public EditorPane(EditorModel model) {
		Document document = getDocument();
		UndoManager undoManager = new UndoManager();

		SinkEx<ActionEvent, BadLocationException> tabize = event -> {
			if (isSelectedText())
				replaceLines(segment -> {
					StringBuilder sb = new StringBuilder("\t");
					for (char ch : String_.chars(segment)) {
						sb.append(ch);
						sb.append(ch == 10 ? "\t" : "");
					}
					return sb.toString();
				});
			else
				document.insertString(getCaretPosition(), "\t", null);
		};

		SinkEx<ActionEvent, BadLocationException> untabize = event -> {
			if (isSelectedText())
				replaceLines(segment -> {
					String s = segment.toString();
					s = s.charAt(0) == '\t' ? s.substring(1) : s;
					return s.replace("\n\t", "\n");
				});
		};

		bind(KeyEvent.VK_TAB, 0).wire(Listen.catchAll(tabize));
		bind(KeyEvent.VK_TAB, Event.SHIFT_MASK).wire(Listen.catchAll(untabize));
		bind(KeyEvent.VK_Y, Event.CTRL_MASK).wire(undoManager::redo);
		bind(KeyEvent.VK_Z, Event.CTRL_MASK).wire(undoManager::undo);

		document.addUndoableEditListener(event -> undoManager.addEdit(event.getEdit()));
		Listen.documentChanged(document).wire(event -> model.changeModified(true));
	}

	private void replaceLines(Fun<Segment, String> fun) throws BadLocationException {
		Document document = getDocument();
		int length = document.getLength();
		int ss = getSelectionStart();
		int se = Math.max(ss, getSelectionEnd() - 1);

		while (0 < ss && document.getText(ss, 1).charAt(0) != 10)
			ss--;
		while (se < length && document.getText(se, 1).charAt(0) != 10)
			se++;

		// do not include first and last LFs
		int start = document.getText(ss, 1).charAt(0) == 10 ? ss + 1 : ss;
		int end = se;

		replace(document, start, end, fun);
	}

	private Signal<ActionEvent> bind(int keyCode, int modifiers) {
		KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, modifiers);
		Object key = Pair.of(keyCode, modifiers);
		getInputMap().put(keyStroke, key);
		return Listen.actionPerformed(this, key);
	}

	private boolean isSelectedText() {
		return getSelectionStart() != getSelectionEnd();
	}

	private void replace(Document document, int start, int end, Fun<Segment, String> f) throws BadLocationException {
		Segment segment_ = new Segment();
		document.getText(start, end - start, segment_);

		String s = f.apply(segment_);
		document.remove(start, end - start);
		document.insertString(start, s, null);
		setSelectionStart(start);
		setSelectionEnd(start + s.length());
	}

}
