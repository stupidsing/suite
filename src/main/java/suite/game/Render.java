package suite.game;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static suite.util.Streamlet_.forInt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import primal.fp.Funs2.BiFun;
import primal.os.Log_;
import primal.primitive.IntInt_Obj;
import primal.primitive.IntVerbs.ToInt;
import suite.cfg.Defaults;
import suite.math.R3;
import suite.streamlet.As;
import suite.util.To;

public class Render {

	public class Image extends BufferedImage {
		public Image(int width, int height, int type) {
			super(width, height, type);
		}

		public boolean view() {
			var image = this;
			var label = new JLabel(new ImageIcon(image));

			var frame = new JFrame();
			frame.getContentPane().add(label, BorderLayout.CENTER);
			frame.setSize(image.getWidth(), image.getHeight());
			frame.setVisible(true);
			return true;
		}
	}

	public Image render(int width, int height, BiFun<Float, R3> f) {
		var scale = 1f / max(width, height);
		int centerX = width / 2, centerY = height / 2;
		var xs = To.vector(width + 1, x -> (x - centerX) * scale);
		var ys = To.vector(height + 1, y -> (y - centerY) * scale);

		return renderPixels(width, height, (IntInt_Obj<R3>) (x, y) -> {
			R3 color;
			try {
				color = f.apply(xs[x], ys[y]);
			} catch (Exception ex) {
				Log_.error(new RuntimeException("at (" + x + ", " + y + ")", ex));
				color = new R3(1d, 1d, 1d);
			}
			return color;
		});
	}

	public Image renderPixels(int width, int height, IntInt_Obj<R3> f) {
		var nThreads = Defaults.nThreads;
		var txs = ToInt.array(nThreads + 1, i -> width * i / nThreads);
		var pixels = new R3[width][height];

		forInt(nThreads).collect(As.executeThreadsByInt(t -> {
			for (var x = txs[t]; x < txs[t + 1]; x++)
				for (var y = 0; y < height; y++)
					pixels[x][y] = f.apply(x, y);
		}));

		var image = new Image(width, height, BufferedImage.TYPE_INT_RGB);

		for (var x = 0; x < width; x++)
			for (var y = 0; y < height; y++) {
				var pixel = limit(pixels[x][y]);
				image.setRGB(x, y, new Color(pixel.x, pixel.y, pixel.z).getRGB());
			}

		return image;
	}

	private R3 limit(R3 u) {
		return new R3(limit(u.x), limit(u.y), limit(u.z));
	}

	private float limit(float f) {
		return min(1f, max(0f, f));
	}

}
