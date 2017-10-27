package suite.image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;

import suite.Constants;
import suite.math.Vector;
import suite.os.LogUtil;
import suite.primitive.Floats_;
import suite.primitive.Ints_;
import suite.util.FunUtil2.BiFun;
import suite.util.Thread_;

public class Render {

	public static BufferedImage render(int width, int height, BiFun<Float, Vector> f) {
		int nThreads = Constants.nThreads;

		float scale = 1f / Math.max(width, height);
		int centerX = width / 2, centerY = height / 2;

		int[] txs = Ints_.toArray(nThreads + 1, i -> width * i / nThreads);
		float[] xs = Floats_.toArray(width + 1, x -> (x - centerX) * scale);
		float[] ys = Floats_.toArray(height + 1, y -> (y - centerY) * scale);
		Vector[][] pixels = new Vector[width][height];

		List<Thread> threads = Ints_ //
				.range(nThreads) //
				.map(t -> Thread_.newThread(() -> {
					for (int x = txs[t]; x < txs[t + 1]; x++)
						for (int y = 0; y < height; y++) {
							Vector color;
							try {
								color = f.apply(xs[x], ys[y]);
							} catch (Exception ex) {
								LogUtil.error(new RuntimeException("at (" + x + ", " + y + ")", ex));
								color = new Vector(1f, 1f, 1f);
							}
							pixels[x][y] = color;
						}
				})) //
				.toList();

		Thread_.startJoin(threads);

		BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++) {
				Vector pixel = limit(pixels[x][y]);
				bufferedImage.setRGB(x, y, new Color(pixel.x, pixel.y, pixel.z).getRGB());
			}

		return bufferedImage;
	}

	private static Vector limit(Vector u) {
		return new Vector(limit(u.x), limit(u.y), limit(u.z));
	}

	private static float limit(float f) {
		return Math.min(1f, Math.max(0f, f));
	}

}
