package suite.fractal;

import suite.game.Render;
import suite.math.Complex;
import suite.math.R3;
import suite.util.RunUtil;
import suite.util.RunUtil.ExecutableProgram;

public class MandelbrotMain extends ExecutableProgram {

	private int width = 1024;
	private int height = 768;

	public static void main(String[] args) {
		RunUtil.run(MandelbrotMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		return new Render() //
				.render(width, height, (fx, fy) -> {
					var n = mandelbrot(Complex.of(fx * 4f, fy * 4f)) / 256f;
					return new R3(n, n, n);
				}) //
				.view();
	}

	private int mandelbrot(Complex z) {
		var n = 0;
		var c = z;
		while (n++ < 240 && z.abs2() < 4f)
			z = Complex.add(Complex.mul(z, z), c);
		return n;
	}

}
