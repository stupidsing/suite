package suite.math.stat;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import suite.math.stat.Arima.Arima_;
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
		float[] mas = new float[] { .5f, .5f, };
		float[] xs = generate(new float[] {}, mas);
		System.out.println(Arrays.toString(arima.maIa(xs, mas.length)));
	}

	private void test(float[] ars, float[] mas) {
		float[] xs = generate(ars, mas);
		Arima_ a = arima.armaIa(xs, ars.length, mas.length);
		System.out.println("x = " + Arrays.toString(xs));
		System.out.println("ar = " + Arrays.toString(a.ars));
		System.out.println("ma = " + Arrays.toString(a.mas));
		System.out.println("x1 = " + a.x1);
	}

	private float[] generate(float[] ars, float[] mas) {
		int length = 256;
		int p = ars.length;
		int q = mas.length;
		float[] xs = new float[length];
		float[] eps = To.arrayOfFloats(length, i -> random.nextGaussian());
		int i = 0;

		while (i < Math.max(p, q))
			xs[i++] = 8f * random.nextFloat();

		while (i < length) {
			int im1 = i - 1;
			xs[i++] = (float) (0d //
					+ Ints_.range(p).toDouble(Int_Dbl.sum(j -> ars[j] * xs[im1 - j])) //
					+ Ints_.range(q).toDouble(Int_Dbl.sum(j -> mas[j] * eps[im1 - j])));
		}

		return xs;
	}

}
