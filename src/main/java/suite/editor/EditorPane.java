package suite.editor;

import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JEditorPane;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Segment;
import javax.swing.undo.UndoManager;

import suite.editor.Listen.SinkEx;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.Pair;
import suite.util.Util;

public class EditorPane extends JEditorPane {

	private static final long serialVersionUID = 1l;

	private boolean isModified = false;

	public EditorPane() {
		Document document = getDocument();
		UndoManager undoManager = new UndoManager();

		SinkEx<ActionEvent, BadLocationException> tabize = event -> {
			if (isSelectedText())
				replaceLines(segment -> {
					StringBuilder sb = new StringBuilder("\t");
					for (char ch : Util.chars(segment)) {
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

		bind(KeyEvent.VK_TAB, 0, Listen.catchAll(tabize));
		bind(KeyEvent.VK_TAB, Event.SHIFT_MASK, Listen.catchAll(untabize));
		bind(KeyEvent.VK_Y, Event.CTRL_MASK, event -> undoManager.redo());
		bind(KeyEvent.VK_Z, Event.CTRL_MASK, event -> undoManager.undo());

		document.addUndoableEditListener(event -> undoManager.addEdit(event.getEdit()));

		document.addDocumentListener(new DocumentListener() {
			public void removeUpdate(DocumentEvent event) {
				changed();
			}

			public void insertUpdate(DocumentEvent event) {
				changed();
			}

			public void changedUpdate(DocumentEvent event) {
			}

			private void changed() {
				setModified(true);
			}
		});
	}

	private void replaceLines(Fun<Segment, String> fun) throws BadLocationException {
		Document document = getDocument();
		int length = document.getLength();
		int ss = getSelectionStart();
		int se = Math.max(ss, getSelectionEnd() - 1);

		while (ss > 0 && document.getText(ss, 1).charAt(0) != 10)
			ss--;
		while (se < length && document.getText(se, 1).charAt(0) != 10)
			se++;

		// Do not include first and last LFs
		int start = document.getText(ss, 1).charAt(0) == 10 ? ss + 1 : ss;
		int end = se;

		replace(document, start, end, fun);
	}

	private void bind(int keyCode, int modifiers, Sink<ActionEvent> sink) {
		KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, modifiers);
		Object key = Pair.of(keyCode, modifiers);
		getInputMap().put(keyStroke, key);
		getActionMap().put(key, Listen.actionPerformed(sink));
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
		this.setSelectionStart(start);
		this.setSelectionEnd(start + s.length());
	}

	public boolean isModified() {
		return isModified;
	}

	public void setModified(boolean isModified) {
		this.isModified = isModified;
	}

}
