package suite.math;

import java.util.Arrays;
import java.util.Random;

import suite.primitive.Floats_;
import suite.util.To;

public class JenkinsTraub {

	private double epsilon = .00001d;
	private double epsilon2 = epsilon * epsilon;

	private Random random = new Random();

	public Complex jt(Complex[] poly0) {
		Complex inv0 = poly0[0].inverse();
		Complex[] poly1 = To.array(poly0.length, Complex.class, i -> Complex.mul(poly0[i], inv0));
		if (poly0[0].abs2() != 0d)
			return jt_(poly1);
		else
			return Complex.zero;
	}

	private Complex jt_(Complex[] poly) {
		int length = poly.length;
		Complex[] h = d(poly);

		// stage 1 no-shift process
		for (int i = 0; i < 5; i++)
			h = shift(poly, h, Complex.zero);

		// stage 2 fixed-shift process
		float[] equation = Floats_.toArray(length, i -> (float) Math.sqrt(poly[length - 1 - i].abs2()));
		equation[0] = -equation[0];

		double root = newtonRaphson(equation, 0f);
		int n = 0;
		int nSteps = 0;
		int maxIterations = 9;

		re: while (true) {
			if (21 < ++n)
				throw new RuntimeException();
			else if (3 < ++nSteps) {
				nSteps = 0;
				maxIterations *= 2;
			}

			Complex s = Complex.exp(2d * Math.PI * random.nextDouble()).scale(root);
			Complex ph;

			Complex ar0 = Complex.sub(s, ph = ph(poly, h, s));
			h = shift(poly, h, s, ph);
			Complex ar1 = Complex.sub(s, ph = ph(poly, h, s));
			h = shift(poly, h, s, ph);
			Complex ar2 = Complex.sub(s, ph = ph(poly, h, s));
			{
				int i = 3;
				while (!(Complex.sub(ar0, ar1).abs2() <= .25d * ar0.abs2() && Complex.sub(ar1, ar2).abs2() <= .25d * ar1.abs2())) {
					h = shift(poly, h, s, ph);
					ar0 = ar1;
					ar1 = ar2;
					ar2 = Complex.sub(s, ph = ph(poly, h, s));
					if (maxIterations < ++i)
						continue re;
				}
			}

			// stage 3 variable-shift process
			Complex ar0_ = ar2;
			h = shift(poly, h, ar0_, ph);
			Complex ar1_ = Complex.sub(ar0_, ph = ph(poly, h, ar0_));
			{
				int i = 2;
				while (!(Complex.sub(ar0_, ar1_).abs2() <= epsilon2 * ar0_.abs2())) {
					h = shift(poly, h, ar1_, ph);
					ar0_ = ar1_;
					ar1_ = Complex.sub(ar0_, ph = ph(poly, h, ar0_));
					if (maxIterations < ++i)
						continue re;
				}
				return ar1_;
			}
		}
	}

	private double newtonRaphson(float[] poly, double x) {
		float[] d = d(poly);
		double diff;
		do
			x = x - (diff = evaluate(poly, x) / evaluate(d, x));
		while (Math.abs(x * epsilon) < Math.abs(diff));
		return x;
	}

	private Complex[] shift(Complex[] poly, Complex[] h, Complex s) {
		return shift(poly, h, s, ph(poly, h, s));
	}

	private Complex[] shift(Complex[] poly, Complex[] h, Complex s, Complex ph) {
		Complex[] scaled0 = scale(h, ph);
		Complex[] scaled1 = Arrays.copyOf(scaled0, scaled0.length + 1);
		scaled1[h.length] = Complex.zero;
		Complex[] sub = To.array(poly.length, Complex.class, i -> Complex.sub(poly[i], scaled1[i]));
		return Boolean.TRUE ? divXms(sub, s) : div(sub, new Complex[] { s.scale(-1d), Complex.of(1f, 0f), });
	}

	private Complex ph(Complex[] poly, Complex[] h, Complex s) {
		return div(evaluate(poly, s), evaluate(h, s));
	}

	private Complex[] divXms(Complex[] nom, Complex s) {
		int lengthm1 = nom.length - 1;
		Complex[] div = new Complex[lengthm1];
		Complex nomx = nom[lengthm1];
		for (int i = lengthm1 - 1; 0 <= i; i--) {
			div[i] = nomx;
			nomx = Complex.add(nom[i], Complex.mul(nomx, s));
		}
		return div;
	}

	private Complex[] div(Complex[] nom, Complex[] denom) {
		int nomLength = nom.length;
		int denomLength = denom.length;
		int diff;
		while (0 <= (diff = nomLength - denomLength)) {
			Complex scale = div(nom[nomLength - 1], denom[denomLength - 1]);
			Complex[] scaled0 = scale(denom, scale);
			Complex[] scaled1 = new Complex[nomLength];
			{
				int i = -1;
				while (++i < diff)
					scaled1[i] = Complex.zero;
				while (++i < nomLength)
					scaled1[i] = scaled0[i - diff];
			}
			Complex[] nom_ = nom;
			Complex[] sub = To.array(nomLength, Complex.class, i -> Complex.sub(nom_[i], scaled1[i]));
			nom = Arrays.copyOfRange(sub, 0, nomLength - 1);
		}
		return nom;
	}

	private Complex div(Complex nom, Complex denom) {
		return Complex.mul(nom, denom.inverse());
	}

	private Complex[] scale(Complex[] cs, Complex scale) {
		return To.array(cs.length, Complex.class, i -> Complex.mul(cs[i], scale));
	}

	private Complex[] d(Complex[] poly) {
		return To.array(poly.length - 1, Complex.class, i -> {
			int i1 = i + 1;
			return poly[i1].scale(i1);
		});
	}

	private float[] d(float[] poly) {
		return Floats_.toArray(poly.length - 1, i -> {
			int i1 = i + 1;
			return poly[i1] * i1;
		});
	}

	private Complex evaluate(Complex[] poly, Complex x) {
		Complex y = Complex.zero;
		for (int i = poly.length - 1; 0 <= i; i--)
			y = Complex.add(Complex.mul(y, x), poly[i]);
		return y;
	}

	private double evaluate(float[] poly, double x) {
		double y = 0d;
		for (int i = poly.length - 1; 0 <= i; i--)
			y = y * x + poly[i];
		return y;
	}

}
