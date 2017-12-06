package suite.nn;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.Test;

import suite.adt.pair.Pair;
import suite.util.FunUtil2.BinOp;
import suite.util.FunUtil2.Fun2;

public class NeuralNetworkTest {

	@Test
	public void test() {
		Pair<String, BinOp<Boolean>> op0 = Pair.of("and", (b0, b1) -> b0 && b1);
		Pair<String, BinOp<Boolean>> op1 = Pair.of("or", (b0, b1) -> b0 || b1);
		Pair<String, BinOp<Boolean>> op2 = Pair.of("xor", (b0, b1) -> b0 ^ b1);
		boolean[] booleans = new boolean[] { false, true, };
		Random random = new Random();

		for (Pair<String, BinOp<Boolean>> pair : List.of(op0, op1, op2)) {
			String name = pair.t0;
			BinOp<Boolean> oper = pair.t1;
			NeuralNetwork nn = new NeuralNetwork();
			Fun2<float[], float[], float[]> train = nn.ml(new int[] { 2, 4, 1, });

			for (int i = 0; i < 16384; i++) {
				boolean b0 = random.nextBoolean();
				boolean b1 = random.nextBoolean();
				float[] in = input(b0, b1);
				float[] out = new float[] { f(oper.apply(b0, b1)), };
				train.apply(in, out);
			}

			boolean result = true;

			for (boolean b0 : booleans)
				for (boolean b1 : booleans) {
					float[] in = input(b0, b1);
					boolean out = oper.apply(b0, b1);
					float f = train.apply(in, null)[0];
					System.out.println(b0 + " " + name + " " + b1 + " = " + f);
					result &= out == .5f < f;
				}

			assertTrue(result);
		}
	}

	private float[] input(boolean b0, boolean b1) {
		return new float[] { f(b0), f(b1), };
	}

	private float f(boolean b) {
		return b ? 1f : 0f;
	}

}
