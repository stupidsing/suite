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

		ArtificialNeuralNetwork ann = new ArtificialNeuralNetwork(Arrays.asList(2, 2, 1), random);

		for (int i = 0; i < 1024; i++) {
			boolean b0 = random.nextBoolean();
			boolean b1 = random.nextBoolean();
			ann.train(input(b0, b1), new float[] { i(oper(b0, b1)) });
		}

		for (int i = 0; i < 16; i++) {
			boolean b0 = random.nextBoolean();
			boolean b1 = random.nextBoolean();
			boolean actual = ann.feed(input(b0, b1))[0] > 0f;
			System.out.println(b0 + " ^ " + b1 + " = " + actual);
			assertEquals(oper(b0, b1), actual);
		}
	}

	private boolean oper(boolean b0, boolean b1) {
		return b0 ^ b1;
	}

	private float[] input(boolean b0, boolean b1) {
		return new float[] { i(b0), i(b1) };
	}

	private float i(boolean b) {
		return b ? 1f : -1f;
	}

}
