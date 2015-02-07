package suite.editor;

import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.AbstractAction;

import suite.util.FunUtil.Sink;

public class Listen {

	public static AbstractAction actionPerformed(Sink<ActionEvent> sink) {
		return new AbstractAction() {
			private static final long serialVersionUID = 1l;

			public void actionPerformed(ActionEvent event) {
				sink.sink(event);
			}
		};
	}

	public static ComponentListener componentResized(Sink<ComponentEvent> sink) {
		return new ComponentAdapter() {
			public void componentResized(ComponentEvent event) {
				sink.sink(event);
			}
		};
	}

	public static KeyListener keyPressed(Sink<KeyEvent> sink) {
		return new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				sink.sink(event);
			}
		};
	}

	public static MouseListener mouseClicked(Sink<MouseEvent> sink) {
		return new MouseAdapter() {
			public void mouseClicked(MouseEvent event) {
				sink.sink(event);
			}
		};
	}

	public static WindowListener windowClosing(Sink<WindowEvent> sink) {
		return new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				sink.sink(event);
			}
		};
	}

}
