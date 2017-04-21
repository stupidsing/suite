package suite.animation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import suite.util.Util;
import suite.util.Util.ExecutableProgram;

public class AnimationMain extends ExecutableProgram {

	private class PaintInput {
		private int i;
	}

	public static void main(String[] args) {
		Util.run(AnimationMain.class, args);
	}

	protected boolean run(String[] args) throws InterruptedException {
		JFrame frame = new JFrame("Animation");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(new Dimension(1024, 768));
		frame.setVisible(true);

		PaintInput pi = new PaintInput();

		frame.getContentPane().add(new JPanel() {
			private static final long serialVersionUID = 1l;

			protected void paintComponent(Graphics graphics) {
				super.paintComponent(graphics);

				Graphics2D g2d = (Graphics2D) graphics;
				g2d.setColor(Color.BLACK);
				g2d.fillRect(pi.i, pi.i, 32, 32);
			}
		});

		ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);

		executor.scheduleAtFixedRate(() -> {
			pi.i++;
			frame.repaint();
		}, 10, 10, TimeUnit.MILLISECONDS);

		executor.awaitTermination(100, TimeUnit.SECONDS);

		return true;
	}

}
