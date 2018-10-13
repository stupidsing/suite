package suite.math;

import static suite.util.Friends.forInt;
import static suite.util.Friends.max;
import static suite.util.Friends.min;

import java.util.Arrays;

import suite.adt.pair.Pair;
import suite.math.linalg.Vector;
import suite.primitive.Floats_;
import suite.primitive.Int_Dbl;
import suite.util.To;

public class Polynomial {

	private Vector vec = new Vector();

	public double evaluate(float[] ps, float x) {
		var y = 0d;
		for (var p : ps)
			y = y * x + p;
		return y;
	}

	public float[] add(float[] ps0, float[] ps1) {
		var length = max(ps0.length, ps1.length);
		return Floats_.toArray(length, i -> {
			var p0 = length < ps0.length ? ps0[length] : 0f;
			var p1 = length < ps1.length ? ps1[length] : 0f;
			return p0 + p1;
		});
	}

	public float[] mul(float[] ps0, float[] ps1) {
		var length0 = ps0.length;
		var length1 = ps1.length;
		return To.vector(length0 + length1,
				i -> forInt(max(0, i - length1 + 1), min(i + 1, length0)).toDouble(Int_Dbl.sum(j -> ps0[j] * ps1[i - j])));
	}

	public Pair<float[], float[]> div(float[] num, float[] denom) {
		int denomLength = denom.length, denomLength1;
		double head;

		while ((head = denom[denomLength1 = denomLength - 1]) == 0d)
			denomLength = denomLength1;

		var rem = vec.copyOf(num);
		var pd = rem.length - denomLength1;
		var dividend = new float[pd];

		while (0 <= --pd) {
			var r = rem[pd + denomLength1] / head;
			dividend[pd] = (float) r;
			for (var i = 0; i < denomLength; i++)
				rem[i + pd] -= denom[i] * r;
		}

		return Pair.of(dividend, Arrays.copyOfRange(rem, 0, denomLength1));
	}

	public float[] scale(float[] ps, double d) {
		return vec.scale(ps, d);
	}

}
