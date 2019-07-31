package suite.nn;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import primal.adt.Pair;
import primal.fp.Funs2.BinOp;
import suite.math.linalg.Matrix;
import suite.math.linalg.Vector;
import suite.streamlet.Read;

public class NeuralNetworkMinibatchRmspropTest {

	private boolean[] booleans = new boolean[] { false, true, };
	private Matrix mtx = new Matrix();
	private Vector vec = new Vector();

	@Test
	public void test() {
		Pair<String, BinOp<Boolean>> op0 = Pair.of("and", (b0, b1) -> b0 && b1);
		Pair<String, BinOp<Boolean>> op1 = Pair.of("or_", (b0, b1) -> b0 || b1);
		Pair<String, BinOp<Boolean>> op2 = Pair.of("xor", (b0, b1) -> b0 ^ b1);

		var inputs = new float[][] { //
				input(false, false), //
				input(false, true), //
				input(true, false), //
				input(true, true), //
		};

		var result = Read.each2(op0, op1, op2).fold(true, (b, name, oper) -> {
			var expect = Read //
					.from(inputs) //
					.map(input -> new float[] { f(oper.apply(c(input[0]), c(input[1]))), }) //
					.toArray(float[].class);

			var nn = new NeuralNetwork();
			var train = nn.mlMinibatchRmsprop(new int[] { mtx.width(inputs), 4, mtx.width(expect), });

			for (var i = 0; i < 1024; i++) { // overfit
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
					b &= out == c(f);
				}

			return b;
		});

		assertTrue(result);
	}

	private float[] input(boolean b0, boolean b1) {
		return vec.of(f(b0), f(b1), 1d);
	}

	private boolean c(float f) {
		return 0f < f;
	}

	private float f(boolean b) {
		return b ? 1f : -1f;
	}

}
