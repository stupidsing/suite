package suite.math.stat;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import suite.math.stat.Arima.Arima_;
import suite.primitive.Int_Dbl;
import suite.primitive.Ints_;

public class ArimaTest {

	private Arima arima = new Arima();
	private Random random = new Random();

	@Test
	public void testAr() {
		test(new float[] { .5f, .5f, }, new float[] {});
	}

	@Test
	public void testMa() {
		test(new float[] {}, new float[] { .5f, .5f, });
	}

	private void test(float[] ars, float[] mas) {
		int length = 256;
		int p = ars.length;
		int q = mas.length;
		float[] xsp = new float[length + p];
		float[] eps = new float[length + q];

		for (int i = 0; i < p; i++)
			xsp[i] = 8f * random.nextFloat();
		for (int i = 0; i < length; i++)
			eps[i] = (float) random.nextGaussian();

		float[] xs = new float[length];

		for (int t = 0; t < length; t++) {
			int tm1 = t - 1;
			xs[t] = xsp[t + p] = (float) (0d //
					+ Ints_.range(p).toDouble(Int_Dbl.sum(i -> ars[i] * xsp[tm1 - i + p])) //
					+ Ints_.range(q).toDouble(Int_Dbl.sum(i -> mas[i] * eps[tm1 - i + q])));
		}

		Arima_ a = arima.armaIa(xs, p, q);
		System.out.println("x = " + Arrays.toString(xs));
		System.out.println("ar = " + Arrays.toString(a.ars));
		System.out.println("ma = " + Arrays.toString(a.mas));
		System.out.println("x1 = " + a.x1);
	}

}
