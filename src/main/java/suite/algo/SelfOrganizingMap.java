package suite.algo;

import java.util.List;
import java.util.Random;

import suite.math.linalg.Matrix;
import suite.primitive.Floats_;

public class SelfOrganizingMap {

	private int sizeX = 128;
	private int sizeY = 128;
	private int updateDistance = 32;
	private double var = -(updateDistance * updateDistance) / (2d * Math.log(.05d));

	private Matrix mtx = new Matrix();
	private Random random = new Random();

	public void som(List<float[]> ins) {
		int length = ins.get(0).length;
		float[][][] som = new float[sizeX][sizeY][length];
		double alpha = 1f;

		for (int x = 0; x < sizeX; x++)
			for (int y = 0; y < sizeY; y++)
				som[x][y] = Floats_.toArray(length, i -> random.nextFloat());

		for (int iteration = 0; iteration < 256; iteration++)
			for (float[] in : ins) {
				float minDist = Float.MAX_VALUE;
				int nearestX = 0, nearestY = 0;

				for (int x = 0; x < sizeX; x++)
					for (int y = 0; y < sizeY; y++) {
						float dist = mtx.dot(in, som[x][y]);
						if (dist < minDist) {
							minDist = dist;
							nearestX = x;
							nearestY = y;
						}
					}

				for (int dx = -updateDistance; dx <= updateDistance; dx++)
					for (int dy = -updateDistance; dy <= updateDistance; dy++) {
						int x = nearestX + dx;
						int y = nearestY + dy;
						double dx_ = (double) dx;
						double dy_ = (double) dy;
						double pheta = Math.exp(-(dx_ * dx_ + dy_ * dy_) / (2d * var));
						float[] som0 = som[x][y];
						som[x][y] = mtx.add(som0, mtx.scale(mtx.sub(in, som0), pheta * alpha));
					}

				alpha += .999d;
			}
	}

}
