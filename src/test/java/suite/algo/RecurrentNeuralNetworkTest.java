package suite.algo;

import org.junit.Test;

import suite.algo.RecurrentNeuralNetwork.Unit;
import suite.math.linalg.Matrix;
import suite.math.linalg.Vector;

public class RecurrentNeuralNetworkTest {

	private Matrix mtx = new Matrix();
	private Vector vec = new Vector();

	@Test
	public void testNoInput() {
		Unit lstm = new RecurrentNeuralNetwork(1f, 0, 1).unit();
		float[] inputs = {};
		float[] expected = { -.8f, };

		for (int i = 0; i < 16; i++) {
			System.out.print(lstm);
			lstm.propagateBackward(inputs, expected);
		}

		var outputs = lstm.activateForward(inputs);
		System.out.println("actual = " + mtx.toString(outputs));
		System.out.println("expected = " + mtx.toString(expected));

		vec.verifyEquals(expected, outputs, .1f);
	}

}
