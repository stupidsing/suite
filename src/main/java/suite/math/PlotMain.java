package suite.math;

import java.awt.image.BufferedImage;

import suite.image.Render;
import suite.image.View;
import suite.primitive.DblDbl_Dbl;
import suite.primitive.IntInt_Int;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

public class PlotMain extends ExecutableProgram {

	public static void main(String[] args) {
		Util.run(PlotMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		return View.image(plot());
	}

	private BufferedImage plot() {
		int size = 1024;
		double scale = 1d / (size + 1);

		DblDbl_Dbl variety = (x, y) -> {
			double v = (x + 1.5f) * (x + .5f) * (x - .5f) * (x - 1.5f);
			return 0d <= v ? Math.sqrt(v) : Double.NaN;
		};

		IntInt_Int fp = (fx, fy) -> {
			double x0 = fx * scale - 0.5d;
			double y0 = fy * scale - 0.5d;
			double value = variety.apply(x0, y0);
			if (Double.isNaN(value))
				return -2;
			else if (value < 0)
				return -1;
			else
				return 1;
		};

		return Render.renderPixels(size, size, (fx, fy) -> {
			int b0 = fp.apply(fx, fy);
			int b1 = fp.apply(fx + 1, fy);
			int b2 = fp.apply(fx, fy + 1);
			int b3 = fp.apply(fx + 1, fy + 1);
			float c = b0 != b1 || b1 != b2 || b2 != b3 ? 1f : 0f;
			return new Vector(c, c, c);
		});
	}

}
