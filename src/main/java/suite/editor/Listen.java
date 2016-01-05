package suite.editor;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import suite.streamlet.Reactive;
import suite.util.FunUtil.Sink;

public class Listen {

	public interface SinkEx<T, Ex extends Exception> {
		public void sink(T t) throws Ex;
	}

	public static Reactive<ActionEvent> actionPerformed(JComponent component, Object key) {
		Reactive<ActionEvent> reactive = new Reactive<>();
		component.getActionMap().put(key, new AbstractAction() {
			private static final long serialVersionUID = 1l;

			public void actionPerformed(ActionEvent event) {
				reactive.fire(event);
			}
		});
		return reactive;
	}

	public static <T, Ex extends Exception> Sink<T> catchAll(SinkEx<T, Ex> sink) {
		return t -> {
			try {
				sink.sink(t);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		};
	}

	public static Reactive<ComponentEvent> componentResized(Component component) {
		Reactive<ComponentEvent> reactive = new Reactive<>();
		component.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent event) {
				reactive.fire(event);
			}
		});
		return reactive;
	}

	public static Reactive<DocumentEvent> documentChanged(JTextComponent textComponent) {
		return documentChanged(textComponent.getDocument());
	}

	public static Reactive<DocumentEvent> documentChanged(Document document) {
		Reactive<DocumentEvent> reactive = new Reactive<>();
		document.addDocumentListener(new DocumentListener() {
			public void removeUpdate(DocumentEvent event) {
				reactive.fire(event);
			}

			public void insertUpdate(DocumentEvent event) {
				reactive.fire(event);
			}

			public void changedUpdate(DocumentEvent event) {
				reactive.fire(event);
			}
		});
		return reactive;
	}

	public static Reactive<KeyEvent> keyPressed(Component component) {
		Reactive<KeyEvent> reactive = new Reactive<>();
		component.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				reactive.fire(event);
			}
		});
		return reactive;
	}

	public static Reactive<MouseEvent> mouseClicked(Component component) {
		Reactive<MouseEvent> reactive = new Reactive<>();
		component.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent event) {
				reactive.fire(event);
			}
		});
		return reactive;
	}

	public static Reactive<WindowEvent> windowClosing(Window window) {
		Reactive<WindowEvent> reactive = new Reactive<>();
		window.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				reactive.fire(event);
			}
		});
		return reactive;
	}

}
