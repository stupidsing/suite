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
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import suite.streamlet.Signal;
import suite.util.FunUtil.Sink;

public class Listen {

	public interface SinkEx<T, Ex extends Exception> {
		public void sink(T t) throws Ex;
	}

	public static Signal<ActionEvent> action(AbstractButton component) {
		return Signal.of(fire -> component.addActionListener(event -> fire.sink(event)));
	}

	public static Signal<ActionEvent> action(JTextField component) {
		return Signal.of(fire -> component.addActionListener(event -> fire.sink(event)));
	}

	public static Signal<ActionEvent> actionPerformed(JComponent component, Object key) {
		return Signal.of(fire -> component //
				.getActionMap() //
				.put(key, new AbstractAction() {
					private static final long serialVersionUID = 1l;

					public void actionPerformed(ActionEvent event) {
						fire.sink(event);
					}
				}));
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

	public static Signal<ComponentEvent> componentResized(Component component) {
		return Signal.of(fire -> component.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent event) {
				fire.sink(event);
			}
		}));
	}

	public static Signal<DocumentEvent> documentChanged(JTextComponent textComponent) {
		return documentChanged(textComponent.getDocument());
	}

	public static Signal<DocumentEvent> documentChanged(Document document) {
		return Signal.of(fire -> document.addDocumentListener(new DocumentListener() {
			public void removeUpdate(DocumentEvent event) {
				fire.sink(event);
			}

			public void insertUpdate(DocumentEvent event) {
				fire.sink(event);
			}

			public void changedUpdate(DocumentEvent event) {
				fire.sink(event);
			}
		}));
	}

	public static Signal<KeyEvent> keyPressed(Component component) {
		return Signal.of(fire -> component.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				fire.sink(event);
			}
		}));
	}

	public static Signal<KeyEvent> keyPressed(Component component, int keyCode) {
		return keyPressed(component).filter(event -> event.getKeyCode() == keyCode);
	}

	public static Signal<MouseEvent> mouseClicked(Component component) {
		return Signal.of(fire -> {
			component.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent event) {
					fire.sink(event);
				}
			});
		});
	}

	public static Signal<MouseEvent> mouseDoubleClicked(Component component) {
		return mouseClicked(component).filter(event -> event.getClickCount() == 2);
	}

	public static Signal<WindowEvent> windowClosing(Window window) {
		return Signal.of(fire -> {
			window.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent event) {
					fire.sink(event);
				}
			});
		});
	}

}
