package suite.algo;

import static java.lang.Math.abs;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import primal.adt.FixieArray;
import primal.primitive.FltPrim.FltSource;
import suite.inspect.Dump;
import suite.math.linalg.Matrix;

public class GaussianMixtureModelTest {

	private Matrix mtx = new Matrix();
	private Random random = new Random();
	private FltSource gaussian = () -> (float) random.nextGaussian();

	@Test
	public void test1() {
		var size = 512;
		var obs = new float[size][];

		for (var i = 0; i < size; i++)
			if (i < size / 2)
				obs[i] = new float[] { -5f + gaussian.g(), };
			else
				obs[i] = new float[] { +5f + gaussian.g(), };

		var gmm = new GaussianMixtureModel(2, obs);
		Dump.details(gmm);

		FixieArray.of(gmm.components).map((comp0, comp1) -> {
			for (var j = 0; j < comp0.mean.length; j++) {
				assertTrue(comp0.mean[j] + comp1.mean[j] < 1f);
				assertTrue(9f < abs(comp0.mean[j]) + abs(comp1.mean[j]));
			}
			assertTrue(.8f < mtx.det(comp0.covar));
			assertTrue(.8f < mtx.det(comp1.covar));
			assertTrue(abs(comp0.scale - .5f) < .1f);
			assertTrue(abs(comp1.scale - .5f) < .1f);
			return true;
		});
	}

	@Test
	public void test2() {
		var size = 512;
		var obs = new float[size][];

		for (var i = 0; i < size; i++)
			if (i < size / 2)
				obs[i] = new float[] { -5f + gaussian.g(), -5f + gaussian.g(), };
			else
				obs[i] = new float[] { +5f + gaussian.g(), +5f + gaussian.g(), };

		var gmm = new GaussianMixtureModel(2, obs);
		Dump.details(gmm);

		FixieArray.of(gmm.components).map((comp0, comp1) -> {
			for (var j = 0; j < comp0.mean.length; j++) {
				assertTrue(comp0.mean[j] + comp1.mean[j] < 1f);
				assertTrue(9f < abs(comp0.mean[j]) + abs(comp1.mean[j]));
			}
			assertTrue(.8f < mtx.det(comp0.covar));
			assertTrue(.8f < mtx.det(comp1.covar));
			assertTrue(abs(comp0.scale - .5f) < .1f);
			assertTrue(abs(comp1.scale - .5f) < .1f);
			return true;
		});
	}

}
