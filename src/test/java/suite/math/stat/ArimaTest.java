package suite.math.stat;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import suite.math.stat.Arima.Em;
import suite.primitive.Floats_;
import suite.primitive.Int_Dbl;
import suite.primitive.Ints_;

public class ArimaTest {

	private Arima arima = new Arima();
	private Random random = new Random();

	@Test
	public void testArma() {
		int length = 128;
		float[] ars = new float[] { .5f, .5f, };
		float[] mas = new float[] {};
		float[] xs = new float[length];
		float[] eps = Floats_.toArray(length, i -> (float) random.nextGaussian());
		int i = 0;

		while (i < ars.length)
			xs[i++] = random.nextFloat();

		while (i < length) {
			int im1 = i - 1;
			xs[i++] = (float) (0d //
					+ Ints_.range(ars.length).collectAsDouble(Int_Dbl.sum(j -> ars[j] * xs[im1 - j])) //
					+ Ints_.range(mas.length).collectAsDouble(Int_Dbl.sum(j -> mas[j] * eps[im1 - j])));
		}

		Em em = arima.em(xs, ars.length, mas.length);
		System.out.println(Arrays.toString(em.ars));
		System.out.println(Arrays.toString(em.mas));
		System.out.println(Arrays.toString(xs));
		System.out.println(em.x1);
	}

}
