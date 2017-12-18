package suite.math.stat;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import suite.math.stat.Arima.Arima_;
import suite.math.stat.Statistic.LinearRegression;
import suite.primitive.Int_Dbl;
import suite.primitive.Ints_;
import suite.primitive.adt.pair.FltObjPair;
import suite.util.To;

public class ArimaTest {

	private Arima arima = new Arima();
	private Random random = new Random();
	private Statistic stat = new Statistic();

	@Test
	public void testArma20() {
		testArma(new float[] { .5f, .5f, }, new float[] {});
	}

	@Test
	public void testArma02() {
		testArma(new float[] {}, new float[] { .5f, .5f, });
	}

	@Test
	public void testMa2() {
		float[] mas = new float[] { .5f, .5f, };
		float[] xs = generate(new float[] {}, mas);
		System.out.println(Arrays.toString(arima.maIa(xs, mas.length)));
	}

	@Test
	public void testMa() {
		float[] eps = To.arrayOfFloats(1000, i -> random.nextGaussian());
		float[] xs = To.arrayOfFloats(eps.length - 1, i -> (eps[i] + eps[i + 1]) * .5f);
		System.out.println("eps = " + Arrays.toString(eps));
		System.out.println("xs = " + Arrays.toString(xs));

		LinearRegression lr0 = stat.linearRegression(Ints_ //
				.range(xs.length) //
				.map(t -> FltObjPair.of(xs[t], new float[] { 1f, })) //
				.toList());

		System.out.println(lr0.toString());
		System.out.println("residuals = " + Arrays.toString(lr0.residuals));

		LinearRegression lr1 = stat.linearRegression(Ints_ //
				.range(xs.length) //
				.map(t -> FltObjPair.of(xs[t], new float[] { 1f, //
						1 <= t ? lr0.residuals[t - 1] : 0f, })) //
				.toList());

		System.out.println(lr1.toString());
		System.out.println("residuals = " + Arrays.toString(lr1.residuals));

		LinearRegression lr2 = stat.linearRegression(Ints_ //
				.range(xs.length) //
				.map(t -> FltObjPair.of(xs[t], new float[] { 1f, //
						1 <= t ? lr1.residuals[t - 1] : 0f, //
						2 <= t ? lr1.residuals[t - 2] : 0f, })) //
				.toList());

		System.out.println(lr2.toString());
		System.out.println("residuals = " + Arrays.toString(lr2.residuals));
	}

	private void testArma(float[] ars, float[] mas) {
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
		int qp = q - p;
		int lengthp = length + p;
		float[] xs = new float[lengthp];
		float[] eps = To.arrayOfFloats(length + q, i -> random.nextGaussian());
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
