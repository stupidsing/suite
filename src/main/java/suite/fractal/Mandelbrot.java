package suite.fractal;

import java.awt.Color;
import java.awt.image.BufferedImage;

import suite.image.Render;
import suite.math.Complex;

public class Mandelbrot {

	public BufferedImage trace(int width, int height) {
		return Render.render(width, height, (fx, fy) -> {
			int n = mandelbrot(Complex.of(fx * 4, fy * 4));
			return new Color(n, n, n);
		});
	}

	private int mandelbrot(Complex z) {
		int n = 0;
		Complex c = z;
		while (n++ < 240 && z.abs2() < 4f)
			z = Complex.add(Complex.mul(z, z), c);
		return n;
	}

}
