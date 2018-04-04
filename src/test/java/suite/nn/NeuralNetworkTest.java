package suite.nn;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.Test;

import suite.adt.pair.Pair;
import suite.math.linalg.Vector;
import suite.nn.NeuralNetwork.Layer;
import suite.nn.NeuralNetwork.Out;
import suite.util.FunUtil2.BinOp;

public class NeuralNetworkTest {

	private Vector vec = new Vector();

	@Test
	public void test() {
		Pair<String, BinOp<Boolean>> op0 = Pair.of("and", (b0, b1) -> b0 && b1);
		Pair<String, BinOp<Boolean>> op1 = Pair.of("or", (b0, b1) -> b0 || b1);
		Pair<String, BinOp<Boolean>> op2 = Pair.of("xor", (b0, b1) -> b0 ^ b1);
		boolean[] booleans = new boolean[] { false, true, };
		Random random = new Random();
		boolean result = true;

		for (Pair<String, BinOp<Boolean>> pair : List.of(op0, op1, op2))
			result &= pair.map((name, oper) -> {
				NeuralNetwork nn = new NeuralNetwork();
				Layer<float[], float[]> train = nn.ml(new int[] { 2, 4, 1, });

				for (int i = 0; i < 16384; i++) {
					boolean b0 = random.nextBoolean();
					boolean b1 = random.nextBoolean();
					float[] in = input(b0, b1);
					float[] expect = new float[] { f(oper.apply(b0, b1)), };
					Out<float[], float[]> out = train.feed(in);
					var actual = out.output;
					out.backprop.apply(vec.sub(expect, actual));
				}

				boolean result_ = true;

				for (boolean b0 : booleans)
					for (boolean b1 : booleans) {
						float[] in = input(b0, b1);
						boolean out = oper.apply(b0, b1);
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
