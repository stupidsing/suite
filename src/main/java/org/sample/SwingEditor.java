package org.sample;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;

/**
 * A boring editor.
 */
public class SwingEditor {

	private Font font = new Font("Lucida Sans Typewriter", Font.PLAIN, 10);

	public static void main(String args[]) {
		new SwingEditor().run();
	}

	private void run() {
		final JLabel topLabel = new JLabel("Top");
		topLabel.setVisible(false);

		JTextPane editor = new JTextPane();

		Component box = Box.createRigidArea(new Dimension(8, 8));
		JLabel showHideLabel = new JLabel("Show / Hide");
		JLabel okLabel = new JLabel("OK");

		// Flow layout allows the components to be their preferred size
		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		panel.add(applyDefaults(topLabel));
		panel.add(applyDefaults(editor));
		panel.add(box);
		panel.add(applyDefaults(showHideLabel));
		panel.add(applyDefaults(okLabel));

		JFrame frame = new JFrame(getClass().getSimpleName());
		frame.setSize(new Dimension(1024, 768));
		frame.setContentPane(panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		okLabel.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent event) {
				System.out.println("GOT " + event);
			}
		});

		showHideLabel.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent event) {
				topLabel.setVisible(!topLabel.isVisible());
				panel.repaint();
			}
		});

		frame.setVisible(true);
	}

	private <T extends JComponent> T applyDefaults(T t) {
		t.setFont(font);
		t.setAlignmentX(Component.CENTER_ALIGNMENT);
		return t;
	}

}
