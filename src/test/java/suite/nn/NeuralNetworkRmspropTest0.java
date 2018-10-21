package suite.nn;

import static org.junit.Assert.assertTrue;
import static suite.util.Friends.forInt;
import static suite.util.Friends.sqrt;

import java.util.Random;

import org.junit.Test;

import suite.math.Tanh;
import suite.math.linalg.Matrix;
import suite.primitive.Dbl_Dbl;
import suite.primitive.IntInt_Dbl;
import suite.primitive.Int_Dbl;
import suite.streamlet.Read;
import suite.util.To;

// https://gist.github.com/k15z/d6e986c4760fddf47061e3e383f139a4
public class NeuralNetworkRmspropTest0 {

	private Matrix mtx = new Matrix();
	private Random random = new Random();

	private double initRate = 1d;
	private double learningRate = .01d;

	@Test
	public void test() {
		var inputs = new float[][] { //
				{ -1f, -1f, 1f, }, //
				{ -1f, 1f, 1f, }, //
				{ 1f, -1f, 1f, }, //
				{ 1f, 1f, 1f, }, //
		};

		var outputs = new float[][] { { -1f, }, { -1f, }, { 1f, }, { 1f, }, };
		var nn = new Nn(new int[] { inputs[0].length, 4, 1, });
		float[][] results = null;

		for (var i = 0; i < 1024; i++) { // overfit
			var results_ = nn.feed(inputs);
			nn.backprop(mtx.sub(outputs, results_));
			System.out.println(mtx.toString(results = results_));
		}

		assertTrue(results[0][0] < -.5f);
		assertTrue(results[1][0] < -.5f);
		assertTrue(.5f < results[2][0]);
		assertTrue(.5f < results[3][0]);
	}

	private class Nn {
		private Layer[] layers;

		private Nn(int[] sizes) {
			layers = new Layer[sizes.length - 1];
			for (var i = 1; i < sizes.length; i++)
				layers[i - 1] = new Layer(sizes[i - 1], sizes[i]);
		}

		private float[][] feed(float[][] inputs) {
			return Read.from(layers).fold(inputs, (input, layer) -> layer.feed(input));
		}

		private float[][] backprop(float[][] errors) {
			return Read.from(layers).reverse().fold(errors, (error, layer) -> layer.backprop(error));
		}
	}

	private class Layer {
		private int nInputs;
		private int nOutputs;
		private float[][] inputs; // nPoints * nInputs
		private float[][] weights; // nInputs * nOutputs
		private float[][] outputs; // nPoints * nOutputs
		private float[][] rmsProps;
		Dbl_Dbl activate = Tanh::tanh;
		Dbl_Dbl activateGradient = Tanh::tanhGradient;

		private Layer(int nInputs, int nOutputs) {
			this.nInputs = nInputs;
			this.nOutputs = nOutputs;
			weights = newMatrix((i, j) -> random.nextGaussian() * initRate);
			rmsProps = newMatrix((i, j) -> Math.abs(random.nextGaussian()) * initRate);
		}

		private float[][] feed(float[][] inputs_) {
			return outputs = mtx.mapOn(mtx.mul(inputs = inputs_, weights), activate);
		}

		private float[][] backprop(float[][] errors) {
			var nPoints = mtx.height(errors);
			var derives = newMatrix(nPoints, (i, j) -> errors[i][j] * activateGradient.apply(outputs[i][j]));
			var deltas = newMatrix((i, o) -> forInt(nPoints).toDouble(Int_Dbl.sum(p -> inputs[p][i] * derives[p][o])));
			var deltaSqs = mtx.map(deltas, delta -> delta * delta);
			rmsProps = mtx.addOn(mtx.scaleOn(rmsProps, .99d), mtx.scaleOn(deltaSqs, .01d));

			var adjusts = newMatrix((i, j) -> deltas[i][j] * learningRate / sqrt(rmsProps[i][j]));
			return mtx.mul_mnT(derives, weights = mtx.add(weights, adjusts)); // nPoints * nInputs
		}

		private float[][] newMatrix(IntInt_Dbl f) {
			return newMatrix(nInputs, f);
		}

		private float[][] newMatrix(int n, IntInt_Dbl f) {
			return To.matrix(n, nOutputs, f);
		}
	}

}
