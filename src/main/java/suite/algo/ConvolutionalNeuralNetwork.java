package suite.algo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import suite.adt.Pair;
import suite.util.Util;

public class ConvolutionalNeuralNetwork {

	private float learningRate = 1f;
	private int nLayers;
	private List<LayerWeight> lws = new ArrayList<>();

	private class LayerWeight {
		private int inputStart;
		private int inputEnd;
		private int inputStride;
		private int nInputs;
		private int nOutputs;
		private float weights[];

		private LayerWeight(int nInputs, int nOutputs, float weights[]) {
			this.nInputs = nInputs;
			this.nOutputs = nOutputs;
			this.weights = weights;
			inputStride = nInputs / nOutputs;
			inputStart = (inputStride - weights.length) / 2;
			inputEnd = nInputs - inputStart;
		}

		private int b(int index) {
			return Math.max(0, Math.min(nInputs - 1, index));
		}
	}

	public ConvolutionalNeuralNetwork(int nInputs, List<Pair<Integer, Integer>> parameters, Random random) {
		nLayers = parameters.size() - 1;

		for (int layer = 0; layer < nLayers; layer++) {
			Pair<Integer, Integer> parameter = parameters.get(layer);
			int nOutputs = parameter.t0;
			int nWeights = parameter.t1;
			float weights[] = new float[nWeights];

			for (int i = 0; i < nWeights; i++)
				weights[i] = random.nextFloat();

			lws.add(new LayerWeight(nInputs, nOutputs, weights));
			nInputs = nOutputs;
		}
	}

	public float[] feed(float inputs[]) {
		return Util.last(forwardActivations(inputs));
	}

	public void train(float inputs[], float expected[]) {
		backwardPropagate(forwardActivations(inputs), expected);
	}

	private List<float[]> forwardActivations(float values[]) {
		List<float[]> outputs = new ArrayList<>();
		outputs.add(values);

		for (LayerWeight lw : lws) {
			float values1[] = new float[lw.nOutputs];

			for (int i = lw.inputStart, j = 0; i < lw.inputEnd; i += lw.inputStride, j++) {
				float sum = 0f;
				for (int d = 0; d < lw.weights.length; d++)
					sum += values[lw.b(i + d)] * lw.weights[d];
				values1[j] = activationFunction(sum);
			}

			outputs.add(values = values1);
		}

		return outputs;
	}

	private void backwardPropagate(List<float[]> activations, float expected[]) {
		float errors[] = null;

		for (int layer = nLayers; 0 < layer; layer--) {
			LayerWeight lw0 = lws.get(layer - 1);
			LayerWeight lw1 = layer < lws.size() ? lws.get(layer) : null;

			float ins[] = activations.get(layer - 1);
			float outs[] = activations.get(layer);
			float diffs[] = new float[lw0.nOutputs];

			if (lw1 != null)
				for (int j = lw1.inputStart, k = 0; j < lw1.inputEnd; j += lw1.inputStride, k++) {
					float sum = 0f;
					for (int d = 0; d < lw1.weights.length; d++)
						sum += errors[lw1.b(k + d)] * lw1.weights[d];
					diffs[j] = sum;
				}
			else
				for (int j = 0; j < lw0.nOutputs; j++)
					diffs[j] = expected[j] - outs[j];

			float errors1[] = new float[lw0.nOutputs];

			for (int i = lw0.inputStart, j = 0; i < lw0.inputEnd; i += lw0.inputStride, j++) {
				errors1[j] = diffs[j] * activationFunctionGradient(outs[j]);
				for (int d = 0; d < lw0.weights.length; d++)
					lw0.weights[d] += learningRate * errors1[j] * ins[lw0.b(i + d)] / lw0.nOutputs;
			}

			errors = errors1;
		}
	}

	private float activationFunction(float value) {
		return 1f / (1f + (float) Math.exp(-value));
	}

	private float activationFunctionGradient(float value) {
		return value * (1f - value);
	}

}
