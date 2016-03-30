package suite.fractal;

import java.awt.Color;
import java.awt.image.BufferedImage;

import suite.math.Complex;

public class Mandelbrot {

	public void trace(BufferedImage bufferedImage) {
		int width = bufferedImage.getWidth(), height = bufferedImage.getHeight();
		int centreX = width / 2, centreY = height / 2;
		float rx = 4f / width, ry = 4f / height;

		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++) {
				float fx = (x - centreX) * rx;
				float fy = (y - centreY) * ry;
				int n = mandelbrot(Complex.of(fx, fy));
				bufferedImage.setRGB(x, y, new Color(n, n, n).getRGB());
			}
	}

	private int mandelbrot(Complex z) {
		int n = 0;
		Complex c = z;
		while (n++ < 240 && z.abs2() < 4f)
			z = Complex.add(Complex.mul(z, z), c);
		return n;
	}

}
