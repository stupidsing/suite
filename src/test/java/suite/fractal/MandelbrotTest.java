package suite.fractal;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.Test;

import suite.os.FileUtil;
import suite.util.Util;

public class MandelbrotTest {

	@Test
	public void testMandelbrot() throws IOException {
		String filename = FileUtil.tmp + "/" + Util.getStackTrace(2).getMethodName() + ".png";

		BufferedImage bufferedImage = new BufferedImage(640, 640, BufferedImage.TYPE_INT_RGB);
		new Mandelbrot().trace(bufferedImage);
		ImageIO.write(bufferedImage, "png", new File(filename));
	}

}
