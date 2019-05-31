package suite.editor;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class ImageViewer extends JFrame {

	private static final long serialVersionUID = -5820105568092949073L;

	public ImageViewer(final BufferedImage image) {
		this(image, "image");
	}

	public ImageViewer(final BufferedImage image, String title) {
		setTitle(title);

		var imagePanel = new JPanel() {
			private static final long serialVersionUID = 1l;

			public void paintComponent(Graphics g) {
				super.paintComponent(g);

				var g2d = (Graphics2D) g;
				var ph = getHeight();
				var pw = getWidth();
				var iw = image.getWidth();
				var ih = image.getHeight();

				if (pw * ih > ph * iw) {
					ih = ph;
					iw = image.getWidth() * ph / image.getHeight();
				} else {
					iw = pw;
					ih = image.getHeight() * pw / image.getWidth();
				}

				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.drawImage(image, 0, 0, iw, ih, null);
			}
		};

		getContentPane().add(imagePanel);

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
	}

}
