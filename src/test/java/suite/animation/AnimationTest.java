package suite.animation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.lang.management.ManagementFactory;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.junit.Test;

import suite.os.LogUtil;

public class AnimationTest {

	private class PaintInput {
		private int i;
	}

	@Test
	public void testAnimation() throws InterruptedException {
		JFrame frame = new JFrame("Animation");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(new Dimension(1024, 768));
		frame.setVisible(true);

		PaintInput pi = new PaintInput();

		frame.getContentPane().add(new JPanel() {
			private static final long serialVersionUID = 1l;

			protected void paintComponent(Graphics graphics) {
				Graphics2D g2d = (Graphics2D) graphics;
				g2d.setColor(Color.BLACK);
				g2d.fillRect(pi.i, pi.i, pi.i, pi.i);
			}
		});

		long n0 = 0;

		while (true) {
			pi.i++;
			frame.repaint();
			Thread.sleep(1000 / 24);
			long n1 = System.nanoTime();
			int nGcs = ManagementFactory.getGarbageCollectorMXBeans().size();
			LogUtil.info(nGcs + "/" + (n1 - n0));
			n0 = n1;
		}
	}

}
