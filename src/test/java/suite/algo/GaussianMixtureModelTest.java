package suite.algo;

import static java.lang.Math.abs;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import suite.inspect.Dump;
import suite.math.linalg.Matrix;

public class GaussianMixtureModelTest {

	private Matrix mtx = new Matrix();
	private Random random = new Random();

	@Test
	public void test1() {
		var size = 512;
		var obs = new float[size][];
		var i = 0;

		while (i < size / 2)
			obs[i++] = new float[] { (float) (-5d + random.nextGaussian()), };

		while (i < size)
			obs[i++] = new float[] { (float) (+5d + random.nextGaussian()), };

		var gmm = new GaussianMixtureModel(2, obs);
		Dump.details(gmm);

		var comp0 = gmm.components.get(0);
		var comp1 = gmm.components.get(1);
		assertTrue(comp0.mean[0] + comp1.mean[0] < 1f);
		assertTrue(9f < abs(comp0.mean[0]) + abs(comp1.mean[0]));
		assertTrue(.8f < mtx.det(comp0.covar));
		assertTrue(.8f < mtx.det(comp1.covar));
		assertTrue(abs(comp0.scale - .5f) < .1f);
		assertTrue(abs(comp1.scale - .5f) < .1f);
	}

	@Test
	public void test2() {
		var size = 512;
		var obs = new float[size][];
		var i = 0;

		while (i < size / 2)
			obs[i++] = new float[] { (float) (-5d + random.nextGaussian()), (float) (-5d + random.nextGaussian()), };

		while (i < size)
			obs[i++] = new float[] { (float) (+5d + random.nextGaussian()), (float) (+5d + random.nextGaussian()), };

		var gmm = new GaussianMixtureModel(2, obs);
		Dump.details(gmm);

		var comp0 = gmm.components.get(0);
		var comp1 = gmm.components.get(1);
		assertTrue(comp0.mean[0] + comp1.mean[0] < 1f);
		assertTrue(comp0.mean[1] + comp1.mean[1] < 1f);
		assertTrue(9f < abs(comp0.mean[0]) + abs(comp1.mean[0]));
		assertTrue(9f < abs(comp0.mean[1]) + abs(comp1.mean[1]));
		assertTrue(.8f < mtx.det(comp0.covar));
		assertTrue(.8f < mtx.det(comp1.covar));
		assertTrue(abs(comp0.scale - .5f) < .1f);
		assertTrue(abs(comp1.scale - .5f) < .1f);
	}

}
