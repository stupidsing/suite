package suite.algo;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

public class ArtificialNeuralNetworkTest {

	@Test
	public void test() {
		Random random = new Random();
		// random.setSeed(0l);

		ArtificialNeuralNetwork ann = new ArtificialNeuralNetwork(Arrays.asList(2, 4, 1), random);

		for (int i = 0; i < 16384; i++) {
			boolean b0 = random.nextBoolean();
			boolean b1 = random.nextBoolean();
			ann.train(input(b0, b1), new float[] { f(oper(b0, b1)), });
		}

		for (boolean b0 : new boolean[] { false, true, })
			for (boolean b1 : new boolean[] { false, true, }) {
				float f = ann.feed(input(b0, b1))[0];
				System.out.println(b0 + " ^ " + b1 + " = " + f);
			}

		for (boolean b0 : new boolean[] { false, true, })
			for (boolean b1 : new boolean[] { false, true, }) {
				float f = ann.feed(input(b0, b1))[0];
				assertEquals(oper(b0, b1), f > 0.5f);
			}
	}

	private boolean oper(boolean b0, boolean b1) {
		return b0 ^ b1;
	}

	private float[] input(boolean b0, boolean b1) {
		return new float[] { f(b0), f(b1), };
	}

	private float f(boolean b) {
		return b ? 1f : 0f;
	}

}
