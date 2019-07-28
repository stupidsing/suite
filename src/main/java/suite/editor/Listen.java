package suite.editor;

import static suite.util.Rethrow.ex;

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
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import suite.streamlet.FunUtil.Sink;
import suite.streamlet.Pusher;
import suite.util.Rethrow.SinkEx;

public class Listen {

	public static Pusher<ActionEvent> action(AbstractButton component) {
		return pusher(push -> component.addActionListener(push::f));
	}

	public static Pusher<ActionEvent> action(JTextField component) {
		return pusher(push -> component.addActionListener(push::f));
	}

	public static Pusher<ActionEvent> actionPerformed(JComponent component, Object key) {
		return pusher(push -> component //
				.getActionMap() //
				.put(key, new AbstractAction() {
					private static final long serialVersionUID = 1l;

					public void actionPerformed(ActionEvent event) {
						push.f(event);
					}
				}));
	}

	public static <T, Ex extends Exception> Sink<T> catchAll(SinkEx<T, Ex> sink) {
		return t -> ex(() -> {
			sink.f(t);
			return t;
		});
	}

	public static Pusher<ComponentEvent> componentResized(Component component) {
		return pusher(push -> component.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent event) {
				push.f(event);
			}
		}));
	}

	public static Pusher<DocumentEvent> documentChanged(JTextComponent textComponent) {
		return documentChanged(textComponent.getDocument());
	}

	public static Pusher<DocumentEvent> documentChanged(Document document) {
		return pusher(push -> document.addDocumentListener(new DocumentListener() {
			public void removeUpdate(DocumentEvent event) {
				push.f(event);
			}

			public void insertUpdate(DocumentEvent event) {
				push.f(event);
			}

			public void changedUpdate(DocumentEvent event) {
				push.f(event);
			}
		}));
	}

	public static Pusher<KeyEvent> keyPressed(Component component) {
		return pusher(push -> component.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				push.f(event);
			}
		}));
	}

	public static Pusher<KeyEvent> keyPressed(Component component, int keyCode) {
		return keyPressed(component).filter(event -> event.getKeyCode() == keyCode);
	}

	public static Pusher<MouseEvent> mouseClicked(Component component) {
		return pusher(push -> {
			component.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent event) {
					push.f(event);
				}
			});
		});
	}

	public static Pusher<MouseEvent> mouseDoubleClicked(Component component) {
		return mouseClicked(component).filter(event -> event.getClickCount() == 2);
	}

	public static Pusher<WindowEvent> windowClosing(Window window) {
		return pusher(push -> {
			window.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent event) {
					push.f(event);
				}
			});
		});
	}

	private static <T> Pusher<T> pusher(Sink<Sink<T>> sink) {
		var pusher0 = new Pusher<T>();
		var pusher1 = new Pusher<T>();
		sink.f(pusher0::push);
		pusher0.wire(pusher1, t -> SwingUtilities.invokeLater(() -> pusher1.push(t)));
		return pusher1;
	}

}
