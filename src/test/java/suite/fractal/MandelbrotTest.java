package suite.fractal;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.Test;

import suite.os.FileUtil;
import suite.util.Util;

public class MandelbrotTest {

	@Test
	public void testMandelbrot() throws IOException {
		Path path = FileUtil.tmp.resolve(Util.getStackTrace(2).getMethodName() + ".png");

		BufferedImage bufferedImage = new Mandelbrot(640, 640).trace();

		try (OutputStream os = FileUtil.out(path)) {
			ImageIO.write(bufferedImage, "png", os);
		}
	}

}
