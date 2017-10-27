package suite.fractal;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import suite.image.Render;
import suite.math.Complex;
import suite.math.Vector;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

public class MandelbrotMain extends ExecutableProgram {

	private int width = 1024;
	private int height = 768;

	public static void main(String[] args) {
		Util.run(MandelbrotMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		BufferedImage image = trace();

		JLabel label = new JLabel(new ImageIcon(image));

		JFrame frame = new JFrame();
		frame.getContentPane().add(label, BorderLayout.CENTER);
		frame.setSize(width, height);
		frame.setVisible(true);
		return true;
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
