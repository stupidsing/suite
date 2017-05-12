package suite.algo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import suite.math.Matrix;
import suite.math.Sigmoid;
import suite.util.List_;

public class ArtificialNeuralNetwork {

	private Matrix mtx = new Matrix();

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

	public ArtificialNeuralNetwork(List<Integer> layerSizes, Random random) {
		nLayers = layerSizes.size() - 1;

		for (int layer = 0; layer < nLayers; layer++) {
			int nInputs = layerSizes.get(layer);
			int nOutputs = layerSizes.get(layer + 1);
			float[][] weights = new float[nInputs][nOutputs];

			for (int i = 0; i < nInputs; i++)
				for (int j = 0; j < nOutputs; j++)
					weights[i][j] = random.nextFloat();

			lws.add(new LayerWeight(nInputs, nOutputs, weights));
		}
	}

	public float[] feed(float[] inputs) {
		return List_.last(activateForward(inputs));
	}

	public void train(float[] inputs, float[] expected) {
		propagateBackward(activateForward(inputs), expected);
	}

	private List<float[]> activateForward(float[] values) {
		List<float[]> outputs = new ArrayList<>();
		outputs.add(values);

		for (LayerWeight lw : lws) {
			float[] values1 = mtx.mul(values, lw.weights);

			for (int j = 0; j < lw.nOutputs; j++)
				values1[j] = activationFunction(values1[j]);

			outputs.add(values = values1);
		}

		return outputs;
	}

	private void propagateBackward(List<float[]> activations, float[] expected) {
		float[] errors = null;

		for (int layer = nLayers; 0 < layer; layer--) {
			LayerWeight lw0 = lws.get(layer - 1);
			LayerWeight lw1 = layer < nLayers ? lws.get(layer) : null;

			float[] ins = activations.get(layer - 1);
			float[] outs = activations.get(layer);
			float[] errors1 = lw1 != null ? mtx.mul(lw1.weights, errors) : mtx.sub(expected, outs);

			for (int j = 0; j < lw0.nOutputs; j++)
				errors1[j] *= activationFunctionGradient(outs[j]);

			for (int j = 0; j < lw0.nOutputs; j++)
				for (int i = 0; i < lw0.nInputs; i++)
					lw0.weights[i][j] += learningRate * ins[i] * errors1[j];

			errors = errors1;
		}
	}

	private float activationFunction(float value) {
		return Sigmoid.sigmoid(value);
	}

	private float activationFunctionGradient(float value) {
		return Sigmoid.sigmoidGradient(value);
	}

}
