package suite.image;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import suite.Constants;
import suite.math.Vector;
import suite.os.LogUtil;
import suite.primitive.Floats_;
import suite.primitive.IntInt_Obj;
import suite.primitive.Ints_;
import suite.util.FunUtil2.BiFun;
import suite.util.Thread_;

public class Render {

	public class Image extends BufferedImage {
		public Image(int width, int height, int type) {
			super(width, height, type);
		}

		public boolean view() {
			BufferedImage image = this;
			JLabel label = new JLabel(new ImageIcon(image));

			JFrame frame = new JFrame();
			frame.getContentPane().add(label, BorderLayout.CENTER);
			frame.setSize(image.getWidth(), image.getHeight());
			frame.setVisible(true);
			return true;
		}
	}

	public Image render(int width, int height, BiFun<Float, Vector> f) {
		float scale = 1f / Math.max(width, height);
		int centerX = width / 2, centerY = height / 2;
		float[] xs = Floats_.toArray(width + 1, x -> (x - centerX) * scale);
		float[] ys = Floats_.toArray(height + 1, y -> (y - centerY) * scale);

		return renderPixels(width, height, (IntInt_Obj<Vector>) (x, y) -> {
			Vector color;
			try {
				color = f.apply(xs[x], ys[y]);
			} catch (Exception ex) {
				LogUtil.error(new RuntimeException("at (" + x + ", " + y + ")", ex));
				color = new Vector(1d, 1d, 1d);
			}
			return color;
		});
	}

	public Image renderPixels(int width, int height, IntInt_Obj<Vector> f) {
		int nThreads = Constants.nThreads;

		int[] txs = Ints_.toArray(nThreads + 1, i -> width * i / nThreads);
		Vector[][] pixels = new Vector[width][height];

		List<Thread> threads = Ints_ //
				.range(nThreads) //
				.map(t -> Thread_.newThread(() -> {
					for (int x = txs[t]; x < txs[t + 1]; x++)
						for (int y = 0; y < height; y++)
							pixels[x][y] = f.apply(x, y);
				})) //
				.toList();

		Thread_.startJoin(threads);

		Image image = new Image(width, height, BufferedImage.TYPE_INT_RGB);

		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++) {
				Vector pixel = limit(pixels[x][y]);
				image.setRGB(x, y, new Color(pixel.x, pixel.y, pixel.z).getRGB());
			}

		return image;
	}

	private Vector limit(Vector u) {
		return new Vector(limit(u.x), limit(u.y), limit(u.z));
	}

	private float limit(float f) {
		return Math.min(1f, Math.max(0f, f));
	}

}
