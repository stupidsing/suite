package suite.algo;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Random;
import java.util.function.BiFunction;

import org.junit.Test;

import suite.adt.Pair;

public class ArtificialNeuralNetworkTest {

	@Test
	public void test() {
		Pair<String, BiFunction<Boolean, Boolean, Boolean>> op0 = Pair.of("and", (b0, b1) -> b0 && b1);
		Pair<String, BiFunction<Boolean, Boolean, Boolean>> op1 = Pair.of("or", (b0, b1) -> b0 || b1);
		Pair<String, BiFunction<Boolean, Boolean, Boolean>> op2 = Pair.of("xor", (b0, b1) -> b0 ^ b1);
		boolean[] booleans = new boolean[] { false, true, };
		Random random = new Random();

		// random.setSeed(0l);

		for (Pair<String, BiFunction<Boolean, Boolean, Boolean>> pair : Arrays.asList(op0, op1, op2)) {
			String name = pair.t0;
			BiFunction<Boolean, Boolean, Boolean> oper = pair.t1;
			ArtificialNeuralNetwork ann = new ArtificialNeuralNetwork(Arrays.asList(2, 4, 1), random);

			for (int i = 0; i < 16384; i++) {
				boolean b0 = random.nextBoolean();
				boolean b1 = random.nextBoolean();
				ann.train(input(b0, b1), new float[] { f(oper.apply(b0, b1)), });
			}

			boolean result = true;

			for (boolean b0 : booleans)
				for (boolean b1 : booleans) {
					float f = ann.feed(input(b0, b1))[0];
					System.out.println(b0 + " " + name + " " + b1 + " = " + f);
					result &= oper.apply(b0, b1) == .5f < f;
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
