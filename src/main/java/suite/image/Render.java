package suite.image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.BiFunction;

import suite.Constants;
import suite.math.Vector;
import suite.os.LogUtil;
import suite.streamlet.Read;
import suite.util.Util;

public class Render {

	public static BufferedImage render(int width, int height, BiFunction<Float, Float, Vector> f) {
		int nThreads = Constants.nThreads;
		int xs[] = new int[nThreads + 1];

		for (int i = 0; i <= nThreads; i++)
			xs[i] = width * i / nThreads;

		Vector pixels[][] = new Vector[width][height];
		float scale = 1f / Math.max(width, height);
		int centreX = width / 2, centreY = height / 2;

		List<Thread> threads = Read.range(nThreads) //
				.map(t -> Util.newThread(() -> {
					for (int x = xs[t]; x < xs[t + 1]; x++)
						for (int y = 0; y < height; y++) {
							Vector color;
							try {
								color = f.apply((x - centreX) * scale, (y - centreY) * scale);
							} catch (Exception ex) {
								LogUtil.error(new RuntimeException("at (" + x + ", " + y + ")", ex));
								color = new Vector(1f, 1f, 1f);
							}
							pixels[x][y] = color;
						}
				})) //
				.toList();

		Util.startJoin(threads);

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
