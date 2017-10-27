package suite.image;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class View {

	public static boolean image(BufferedImage image) {
		JLabel label = new JLabel(new ImageIcon(image));

		JFrame frame = new JFrame();
		frame.getContentPane().add(label, BorderLayout.CENTER);
		frame.setSize(image.getWidth(), image.getHeight());
		frame.setVisible(true);
		return true;
	}

}
