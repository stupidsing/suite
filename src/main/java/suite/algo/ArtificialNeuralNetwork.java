package suite.algo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import suite.math.Sigmoid;
import suite.math.linalg.Matrix_;
import suite.math.linalg.Vector_;
import suite.primitive.Ints;
import suite.util.List_;
import suite.util.To;

public class ArtificialNeuralNetwork {

	private Matrix_ mtx = new Matrix_();
	private Vector_ vec = new Vector_();

	private float learningRate = 1f;
	private int nLayers;
	private List<LayerWeight> lws = new ArrayList<>();

	private class LayerWeight {
		private int nInputs;
		private int nOutputs;
		private float[][] weights;

		private LayerWeight(int nInputs, int nOutputs, float[][] weights) {
			this.nInputs = nInputs;
			this.nOutputs = nOutputs;
			this.weights = weights;
		}
	}

	public ArtificialNeuralNetwork(Ints layerSizes, Random random) {
		nLayers = layerSizes.size() - 1;

		for (int layer = 0; layer < nLayers; layer++) {
			int nInputs = layerSizes.get(layer);
			int nOutputs = layerSizes.get(layer + 1);
			float[][] weights = To.arrayOfFloats(nInputs, nOutputs, (i, j) -> random.nextFloat());
			lws.add(new LayerWeight(nInputs, nOutputs, weights));
		}
	}

	public float[] feed(float[] inputs) {
		return List_.last(activateForward(inputs));
	}

	public void train(float[] inputs, float[] expect) {
		propagateBackward(activateForward(inputs), expect);
	}

	private List<float[]> activateForward(float[] values) {
		List<float[]> outputs = new ArrayList<>();
		outputs.add(values);

		for (LayerWeight lw : lws) {
			float[] values1 = mtx.mul(values, lw.weights);

			for (int j = 0; j < lw.nOutputs; j++)
				values1[j] = (float) activationFunction(values1[j]);

			outputs.add(values = values1);
		}

		return outputs;
	}

	private void propagateBackward(List<float[]> activations, float[] expect) {
		float[] outs = activations.get(nLayers);
		float[] errors = vec.sub(expect, outs);

		for (int layer = nLayers - 1; 0 <= layer; layer--) {
			float[] ins = activations.get(layer);
			LayerWeight lw = lws.get(layer);

			for (int j = 0; j < lw.nOutputs; j++) {
				float e = errors[j] *= activationFunctionGradient(outs[j]);

				for (int i = 0; i < lw.nInputs; i++)
					lw.weights[i][j] += learningRate * ins[i] * e;

			}

			errors = mtx.mul(lw.weights, errors);
			outs = ins;
		}
	}

	private double activationFunction(double value) {
		return Sigmoid.sigmoid(value);
	}

	private double activationFunctionGradient(double value) {
		return Sigmoid.sigmoidGradient(value);
	}

}
