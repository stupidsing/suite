package suite.nn;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.Test;

import suite.adt.pair.Pair;
import suite.math.linalg.Vector;
import suite.nn.NeuralNetwork.Layer;
import suite.nn.NeuralNetwork.Out;
import suite.streamlet.FunUtil2.BinOp;

public class NeuralNetworkTest {

	private Vector vec = new Vector();

	@Test
	public void test() {
		Pair<String, BinOp<Boolean>> op0 = Pair.of("and", (b0, b1) -> b0 && b1);
		Pair<String, BinOp<Boolean>> op1 = Pair.of("or", (b0, b1) -> b0 || b1);
		Pair<String, BinOp<Boolean>> op2 = Pair.of("xor", (b0, b1) -> b0 ^ b1);
		var booleans = new boolean[] { false, true, };
		var random = new Random();
		var result = true;

		for (var pair : List.of(op0, op1, op2))
			result &= pair.map((name, oper) -> {
				var nn = new NeuralNetwork();
				Layer<float[], float[]> train = nn.ml(new int[] { 2, 4, 1, });

				for (var i = 0; i < 16384; i++) {
					var b0 = random.nextBoolean();
					var b1 = random.nextBoolean();
					var in = input(b0, b1);
					var expect = new float[] { f(oper.apply(b0, b1)), };
					Out<float[], float[]> out = train.feed(in);
					var actual = out.output;
					out.backprop.apply(vec.sub(expect, actual));
				}

				var result_ = true;

				for (var b0 : booleans)
					for (var b1 : booleans) {
						var in = input(b0, b1);
						var out = oper.apply(b0, b1);
						var f = train.feed(in).output[0];
						System.out.println(b0 + " " + name + " " + b1 + " = " + f);
						result_ &= out == .5f < f;
					}

				return result_;
			});

		assertTrue(result);
	}

	private float[] input(boolean b0, boolean b1) {
		return new float[] { f(b0), f(b1), };
	}

	private float f(boolean b) {
		return b ? 1f : 0f;
	}

}
