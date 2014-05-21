package suite.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import suite.math.Matrix;

public class RgbImage {

	private Matrix red, green, blue;

	public RgbImage(Matrix red, Matrix green, Matrix blue) {
		this.red = red;
		this.green = green;
		this.blue = blue;
	}

	public RgbImage convolute(Matrix kernel) {
		return new RgbImage(Matrix.convolute(red, kernel), Matrix.convolute(green, kernel), Matrix.convolute(blue, kernel));
	}

	public static RgbImage load(File file) throws IOException {
		BufferedImage bufferedImage = ImageIO.read(file);
		int w = bufferedImage.getWidth(), h = bufferedImage.getHeight();
		int rgbs[] = new int[h * w];
		bufferedImage.getRGB(0, 0, w, h, rgbs, 0, w);

		float cr[][] = new float[w][h];
		float cg[][] = new float[w][h];
		float cb[][] = new float[w][h];

		for (int x = 0; x < w; x++)
			for (int y = 0; y < h; y++) {
				cr[x][y] = (rgbs[y * w + x] >> 16 & 0xFF) / 256f;
				cg[x][y] = (rgbs[y * w + x] >> 8 & 0xFF) / 256f;
				cb[x][y] = (rgbs[y * w + x] >> 0 & 0xFF) / 256f;
			}

		return new RgbImage(new Matrix(cr), new Matrix(cg), new Matrix(cb));
	}

	public static void save(File file, RgbImage rgbImage) throws IOException {
		int w = rgbImage.getWidth(), h = rgbImage.getHeight();
		BufferedImage bufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

		for (int x = 0; x < w; x++)
			for (int y = 0; y < h; y++) {
				int r = (int) (rgbImage.getRed().get()[x][y] * 256f);
				int g = (int) (rgbImage.getGreen().get()[x][y] * 256f);
				int b = (int) (rgbImage.getBlue().get()[x][y] * 256f);
				bufferedImage.setRGB(x, y, r << 16 + g << 8 + b);
			}

		ImageIO.write(bufferedImage, "png", file);
	}

	public int getWidth() {
		return red.width();
	}

	public int getHeight() {
		return red.height();
	}

	public Matrix getRed() {
		return red;
	}

	public Matrix getGreen() {
		return green;
	}

	public Matrix getBlue() {
		return blue;
	}

}
