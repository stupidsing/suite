package suite.fractal;

import java.awt.Color;
import java.awt.image.BufferedImage;

import suite.image.Render;
import suite.math.Complex;

public class Mandelbrot {

	private int width;
	private int height;

	public Mandelbrot(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public BufferedImage trace() {
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
