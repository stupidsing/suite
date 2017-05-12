package suite.fractal;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import suite.image.Render;
import suite.math.Complex;
import suite.math.Vector;

public class Mandelbrot {

	private int width;
	private int height;

	public static void main(String[] args) {
		int width = 1024;
		int height = 768;
		BufferedImage image = new Mandelbrot(width, height).trace();

		JLabel label = new JLabel(new ImageIcon(image));

		JFrame frame = new JFrame();
		frame.getContentPane().add(label, BorderLayout.CENTER);
		frame.setSize(width, height);
		frame.setVisible(true);
	}

	public Mandelbrot(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public BufferedImage trace() {
		return Render.render(width, height, (fx, fy) -> {
			float n = mandelbrot(Complex.of(fx * 4f, fy * 4f)) / 256f;
			return new Vector(n, n, n);
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
