package suite.math;

import java.util.Arrays;

import suite.adt.pair.Pair;
import suite.math.linalg.Vector_;
import suite.primitive.Floats_;
import suite.primitive.Int_Dbl;
import suite.primitive.Ints_;

public class Polynomial {

	private Vector_ vec = new Vector_();

	public double evaluate(float[] ps, float x) {
		double y = 0d;
		for (float p : ps)
			y = y * x + p;
		return y;
	}

	public float[] add(float[] ps0, float[] ps1) {
		int length = Math.max(ps0.length, ps1.length);
		return Floats_.toArray(length, i -> {
			float p0 = length < ps0.length ? ps0[length] : 0f;
			float p1 = length < ps1.length ? ps1[length] : 0f;
			return p0 + p1;
		});
	}

	public float[] mul(float[] ps0, float[] ps1) {
		int length0 = ps0.length;
		int length1 = ps1.length;
		return Floats_.toArray(length0 + length1, i -> (float) Ints_ //
				.range(Math.max(0, i - length1 + 1), Math.min(i + 1, length0)) //
				.collectAsDouble(Int_Dbl.sum(j -> ps0[j] * ps1[i - j])));
	}

	public Pair<float[], float[]> div(float[] nom, float[] denom) {
		int denomLength = denom.length, denomLength1;
		double head;

		while ((head = denom[denomLength1 = denomLength - 1]) == 0d)
			denomLength = denomLength1;

		float[] rem = vec.of(nom);
		int pd = rem.length - denomLength;
		float[] dividend = new float[pd + 1];

		while (0 <= pd) {
			double r = rem[pd + denomLength1] / head;
			dividend[pd] = (float) r;
			for (int i = 0; i < denomLength; i++)
				rem[i + pd] -= denom[i] * r;
			pd--;
		}

		return Pair.of(dividend, Arrays.copyOfRange(rem, 0, denomLength1));
	}

	public float[] scale(float[] ps, double d) {
		return vec.scale(ps, d);
	}

}
