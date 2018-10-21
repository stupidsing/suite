package suite.nn;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.adt.pair.Pair;
import suite.math.linalg.Matrix;
import suite.math.linalg.Vector;
import suite.streamlet.FunUtil2.BinOp;
import suite.streamlet.Read;
import suite.util.To;

public class NeuralNetworkRmsPropTest {

	private boolean[] booleans = new boolean[] { false, true, };
	private Matrix mtx = new Matrix();
	private Vector vec = new Vector();

	@Test
	public void test() {
		Pair<String, BinOp<Boolean>> op0 = Pair.of("and", (b0, b1) -> b0 && b1);
		Pair<String, BinOp<Boolean>> op1 = Pair.of("or_", (b0, b1) -> b0 || b1);
		Pair<String, BinOp<Boolean>> op2 = Pair.of("xor", (b0, b1) -> b0 ^ b1);

		var inputs = new float[][] { //
				{ -1f, -1f, 1f, }, //
				{ -1f, 1f, 1f, }, //
				{ 1f, -1f, 1f, }, //
				{ 1f, 1f, 1f, }, //
		};
		var result = Read.each2(op0, op1, op2).fold(true, (b, name, oper) -> {
			var nn = new NeuralNetwork();
			var train = nn.mlRmsProp(new int[] { 3, 4, 1, });

			var expect = To.array(inputs.length, float[].class,
					i -> new float[] { f(oper.apply(0f < inputs[i][0], 0f < inputs[i][1])), });

			for (var i = 0; i < 16384; i++) {
				var out = train.feed(inputs);
				var actual = out.output;
				out.backprop.apply(mtx.sub(expect, actual));
			}

			for (var b0 : booleans)
				for (var b1 : booleans) {
					var in = input(b0, b1);
					var out = oper.apply(b0, b1);
					var f = train.feed(new float[][] { in, }).output[0][0];
					System.out.println(b0 + " " + name + " " + b1 + " = " + f);
					b &= out == 0f < f;
				}

			return b;
		});

		assertTrue(result);
	}

	private float[] input(boolean b0, boolean b1) {
		return vec.of(f(b0), f(b1), 1d);
	}

	private float f(boolean b) {
		return b ? 1f : -1f;
	}

}
