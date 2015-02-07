package suite.editor;

import java.awt.Event;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JEditorPane;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Segment;
import javax.swing.undo.UndoManager;

import suite.util.FunUtil.Fun;

public class EditorPane extends JEditorPane {

	private static final long serialVersionUID = 1l;

	private boolean isModified = false;

	public EditorPane() {
		Document document = getDocument();
		UndoManager undoManager = new UndoManager();

		AbstractAction tabAction = Listen.actionPerformed(event -> {
			int ss = getSelectionStart();
			int se = getSelectionEnd();

			try {
				while (ss > 0 && document.getText(ss, 1).charAt(0) != 10)
					ss--;

				int start = ss, end = se;

				if (start != 0 && end != 0)
					replace(document, start, end, segment -> {
						StringBuilder sb = new StringBuilder(start != 0 ? "" : "\n");

						for (int i = 0; i < segment.length(); i++) {
							char ch = segment.charAt(i);
							sb.append(ch);
							if (ch == 10)
								sb.append('\t');
						}

						return sb.toString();
					});
				else
					document.insertString(getCaretPosition(), "\t", null);
			} catch (BadLocationException ex) {
				throw new RuntimeException(ex);
			}
		});

		InputMap inputMap = getInputMap();
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Event.CTRL_MASK), ".redo");
		inputMap.put(KeyStroke.getKeyStroke("TAB"), ".tab");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK), ".undo");

		ActionMap actionMap = getActionMap();
		actionMap.put(".redo", Listen.actionPerformed(event -> undoManager.redo()));
		actionMap.put(".tab", tabAction);
		actionMap.put(".undo", Listen.actionPerformed(event -> undoManager.undo()));

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

	private void replace(Document document, int ss, int se, Fun<Segment, String> f) throws BadLocationException {
		Segment segment_ = new Segment();
		document.getText(ss, se - ss, segment_);
		document.remove(ss, se - ss);
		document.insertString(ss, f.apply(segment_), null);
	}

	public boolean isModified() {
		return isModified;
	}

	public void setModified(boolean isModified) {
		this.isModified = isModified;
	}

}
