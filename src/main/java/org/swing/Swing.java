package org.swing;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class Swing {

	public static void main(String args[]) {
		JLabel label = new JLabel("Hello World~~~");

		JButton button = new JButton("Click Me!");
		button.setMnemonic(KeyEvent.VK_C); // Alt-C as hot key
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				System.out.println("GOT " + event);
			}
		});

		// Flow layout allows the components to be their preferred size
		JPanel panel = new JPanel(new FlowLayout());
		panel.add(label);
		panel.add(button);

		JFrame frame = new JFrame();
		frame.setContentPane(panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// frame.setLocation(200, 200);
		frame.pack(); // Pack it up for display
		frame.setVisible(true);
	}

}
