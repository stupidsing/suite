package suite.nn;

import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import primal.MoreVerbs.Read;
import primal.adt.Pair;
import primal.fp.Funs2.BinOp;
import suite.math.linalg.Vector;
import suite.nn.NeuralNetwork.Layer;

public class NeuralNetworkTest {

	private boolean[] booleans = new boolean[] { false, true, };
	private Random random = new Random();
	private Vector vec = new Vector();

	private Pair<String, BinOp<Boolean>> op0 = Pair.of("and", (b0, b1) -> b0 && b1);
	private Pair<String, BinOp<Boolean>> op1 = Pair.of("or_", (b0, b1) -> b0 || b1);
	private Pair<String, BinOp<Boolean>> op2 = Pair.of("xor", (b0, b1) -> b0 ^ b1);

	@Test
	public void test() {
		assertTrue(Read.each2(op0, op1, op2).fold(true, (b, name, oper) -> {
			var layerSizes = new int[] { 2, 4, 1, };

			return b //
					&& test(new NeuralNetwork().ml(layerSizes), "ff-" + name, oper) //
					&& test(new NeuralNetwork().mlRmsprop(layerSizes), "ff-rmsprop-" + name, oper);
		}));
	}

	private Boolean test(Layer<float[], float[]> train, String name, BinOp<Boolean> oper) {
		var b = true;

		for (var i = 0; i < 16384; i++) {
			var b0 = random.nextBoolean();
			var b1 = random.nextBoolean();
			var in = input(b0, b1);
			var expect = vec.of(f(oper.apply(b0, b1)));
			var out = train.feed(in);
			var actual = out.output;
			out.backprop.apply(vec.sub(expect, actual));
		}

		for (var b0 : booleans)
			for (var b1 : booleans) {
				var in = input(b0, b1);
				var out = oper.apply(b0, b1);
				var f = train.feed(in).output[0];
				System.out.println(name + "(" + b0 + ", " + b1 + ") = " + f);
				b &= out == .5f < f;
			}

		return b;
	}

	private float[] input(boolean b0, boolean b1) {
		return vec.of(f(b0), f(b1));
	}

	private float f(boolean b) {
		return b ? 1f : 0f;
	}

}
