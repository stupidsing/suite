package suite.algo;

import static java.lang.Math.abs;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import suite.inspect.Dump;

public class GaussianMixtureModelTest {

	private Random random = new Random();

	@Test
	public void test1() {
		var size = 512;
		var obs = new float[size][];
		var i = 0;

		while (i < size / 2)
			obs[i++] = new float[] { (float) (-5f + random.nextGaussian()), };

		while (i < size)
			obs[i++] = new float[] { (float) (+5f + random.nextGaussian()), };

		var gmm = new GaussianMixtureModel(2, obs);
		Dump.details(gmm);

		var comp0 = gmm.components.get(0);
		var comp1 = gmm.components.get(1);
		assertTrue(comp0.mean[0] + comp1.mean[0] < 1f);
		assertTrue(9f < abs(comp0.mean[0]) + abs(comp1.mean[0]));
		assertTrue(.8f < comp0.covar[0][0]);
		assertTrue(.8f < comp1.covar[0][0]);
		assertTrue(abs(comp0.scale - .5f) < .1f);
		assertTrue(abs(comp1.scale - .5f) < .1f);
	}

}
