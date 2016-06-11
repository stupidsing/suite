package suite.image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.function.BiFunction;

public class Render {

	public static BufferedImage render(int width, int height, BiFunction<Float, Float, Color> f) {
		BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		int centreX = width / 2, centreY = height / 2;
		float rx = 1f / width, ry = 1f / height;

		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++) {
				float fx = (x - centreX) * rx;
				float fy = (y - centreY) * ry;
				Color color = f.apply(fx, fy);
				bufferedImage.setRGB(x, y, color.getRGB());
			}

		return bufferedImage;
	}

}
