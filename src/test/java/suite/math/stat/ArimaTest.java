package suite.math.stat;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import suite.math.stat.Arima.Arima_;
import suite.primitive.Floats_;
import suite.primitive.Int_Dbl;
import suite.primitive.Ints_;
import suite.util.To;

public class ArimaTest {

	private Arima arima = new Arima();
	private Random random = new Random();

	@Test
	public void testArma20() {
		test(new float[] { .5f, .5f, }, new float[] {});
	}

	@Test
	public void testArma02() {
		test(new float[] {}, new float[] { .5f, .5f, });
	}

	@Test
	public void testMa2() {
		float[] mas = new float[] { .5f, -.5f, };
		float[] xs = generate(256, new float[] {}, Floats_.concat(new float[] { 1f, }, mas));
		System.out.println(Arrays.toString(arima.armaBackcast(xs, new float[] {}, new float[] { 1f, 1f, 1f, 1f, }).mas));
	}

	private void test(float[] ars, float[] mas) {
		float[] xs = generate(256, ars, mas);
		Arima_ a = arima.arimaBackcast(xs, ars.length, 0, mas.length).t1;
		System.out.println("x = " + Arrays.toString(xs));
		System.out.println("ar = " + Arrays.toString(a.ars));
		System.out.println("ma = " + Arrays.toString(a.mas));
		System.out.println("x1 = " + a.x1);
	}

	private float[] generate(int length, float[] ars, float[] mas) {
		int p = ars.length;
		int q = mas.length;
		int qp = q - p;
		int lengthp = length + p;
		float[] xs = new float[lengthp];
		float[] eps = To.vector(length + q, i -> random.nextGaussian());
		int t = 0;

		while (t < p)
			xs[t++] = 8f * random.nextFloat();

		while (t < lengthp) {
			int tm1 = t - 1;
			int tm1qp = tm1 + qp;
			xs[t++] = (float) (0d //
					+ Ints_.range(p).toDouble(Int_Dbl.sum(i -> ars[i] * xs[tm1 - i])) //
					+ Ints_.range(q).toDouble(Int_Dbl.sum(i -> mas[i] * eps[tm1qp - i])));
		}

		return Arrays.copyOfRange(xs, p, xs.length);
	}

}
