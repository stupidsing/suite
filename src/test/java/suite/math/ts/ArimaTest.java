package suite.math.ts;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import suite.math.linalg.Vector;
import suite.primitive.Floats_;
import suite.primitive.Int_Dbl;
import suite.primitive.Ints_;
import suite.primitive.adt.pair.DblObjPair;
import suite.ts.Arima;
import suite.ts.Arima.Arima_;
import suite.util.To;

public class ArimaTest {

	private Arima arima = new Arima();
	private Random random = new Random();
	private Vector vec = new Vector();

	private interface Estimate {
		public DblObjPair<Arima_> arima(float[] xs, int p, int d, int q);
	}

	@Test
	public void testArma20() {
		test(vec.of(.5f, .5f), vec.of());
	}

	@Test
	public void testArma02() {
		test(vec.of(), vec.of(.5f, .5f));
	}

	@Test
	public void testMa2() {
		test(vec.of(), vec.of(.5f, -.5f, 0f));
	}

	private void test(float[] ars, float[] mas) {
		test(ars, mas, arima::arimaBackcast);
	}

	private void test(float[] ars, float[] mas, Estimate estimate) {
		var xs = generate(256, ars, mas);
		var a = estimate.arima(xs, ars.length, 0, mas.length).t1;
		System.out.println("x = " + Arrays.toString(xs));
		System.out.println("ar = " + Arrays.toString(a.ars));
		System.out.println("ma = " + Arrays.toString(a.mas));
		System.out.println("x1 = " + a.x1);
	}

	private float[] generate(int length, float[] ars, float[] mas) {
		var p = ars.length;
		var q = mas.length;
		var xsp = Floats_.concat(To.vector(p, i -> 8f * random.nextDouble()), new float[length]);
		var epq = To.vector(length + q, i -> random.nextGaussian());

		for (var t = 0; t < length; t++) {
			int tp = t + p, tpm1 = tp - 1;
			int tq = t + q, tqm1 = tq - 1;
			xsp[tp++] = (float) (epq[tq] //
					+ Ints_.for_(p).toDouble(Int_Dbl.sum(i -> ars[i] * xsp[tpm1 - i])) //
					+ Ints_.for_(q).toDouble(Int_Dbl.sum(i -> mas[i] * epq[tqm1 - i])));
		}

		return Arrays.copyOfRange(xsp, p, xsp.length);
	}

}
