package suite.algo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import suite.math.Sigmoid;
import suite.util.Util;

public class ArtificialNeuralNetwork {

	private float learningRate = 1f;
	private int nLayers;
	private List<LayerWeight> lws = new ArrayList<>();

	private class LayerWeight {
		private int nInputs;
		private int nOutputs;
		private float weights[][];

		private LayerWeight(int nInputs, int nOutputs, float weights[][]) {
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
			float weights[][] = new float[nInputs][nOutputs];

			for (int i = 0; i < nInputs; i++)
				for (int j = 0; j < nOutputs; j++)
					weights[i][j] = random.nextFloat();

			lws.add(new LayerWeight(nInputs, nOutputs, weights));
		}
	}

	public float[] feed(float inputs[]) {
		return Util.last(activateForward(inputs));
	}

	public void train(float inputs[], float expected[]) {
		propagateBackward(activateForward(inputs), expected);
	}

	private List<float[]> activateForward(float values[]) {
		List<float[]> outputs = new ArrayList<>();
		outputs.add(values);

		for (LayerWeight lw : lws) {
			float values1[] = new float[lw.nOutputs];

			for (int j = 0; j < lw.nOutputs; j++)
				for (int i = 0; i < lw.nInputs; i++)
					values1[j] += values[i] * lw.weights[i][j];

			for (int j = 0; j < lw.nOutputs; j++)
				values1[j] = activationFunction(values1[j]);

			outputs.add(values = values1);
		}

		return outputs;
	}

	private void propagateBackward(List<float[]> activations, float expected[]) {
		float errors[] = null;

		for (int layer = nLayers; 0 < layer; layer--) {
			LayerWeight lw0 = lws.get(layer - 1);
			LayerWeight lw1 = layer < nLayers ? lws.get(layer) : null;

			float ins[] = activations.get(layer - 1);
			float outs[] = activations.get(layer);
			float diffs[] = new float[lw0.nOutputs];

			if (lw1 != null)
				for (int j = 0; j < lw1.nInputs; j++)
					for (int k = 0; k < lw1.nOutputs; k++)
						diffs[j] += errors[k] * lw1.weights[j][k];
			else
				for (int j = 0; j < lw0.nOutputs; j++)
					diffs[j] = expected[j] - outs[j];

			float errors1[] = new float[lw0.nOutputs];

			for (int j = 0; j < lw0.nOutputs; j++) {
				errors1[j] = diffs[j] * activationFunctionGradient(outs[j]);
				for (int i = 0; i < lw0.nInputs; i++)
					lw0.weights[i][j] += learningRate * errors1[j] * ins[i];
			}

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
