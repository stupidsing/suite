package suite.sample;

import suite.util.RunUtil;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AnimationMain {

	public static void main(String[] args) {
		class PaintInput {
			private int i;
		}

		RunUtil.run(() -> {
			var frame = new JFrame("Animation");
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			frame.setSize(new Dimension(1024, 768));
			frame.setVisible(true);

			var pi = new PaintInput();

			frame.getContentPane().add(new JPanel() {
				private static final long serialVersionUID = 1l;

				protected void paintComponent(Graphics graphics) {
					super.paintComponent(graphics);

					var g2d = (Graphics2D) graphics;
					g2d.setColor(Color.BLACK);
					g2d.fillRect(pi.i, pi.i, 32, 32);
				}
			});

			var executor = Executors.newScheduledThreadPool(4);

			executor.scheduleAtFixedRate(() -> {
				pi.i++;
				frame.repaint();
			}, 10, 10, TimeUnit.MILLISECONDS);

			return executor.awaitTermination(100, TimeUnit.SECONDS);
		});
	}

}
