package suite.math.stat;

import suite.math.linalg.Matrix_;
import suite.math.linalg.Vector_;

public class Kalman {

	private Matrix_ mtx = new Matrix_();
	private Vector_ vec = new Vector_();

	private int stateSize = 16;
	private float[][] F; // state transition (stateSize x stateSize)
	private float[][] B; // input control (stateSize x inputSize)
	private float[][] H; // observation (observeSize x stateSize)
	private float[][] Q; // state noise (stateSize x stateSize)
	private float[][] R; // observation noise (observeSize x observeSize)
	private float[][] estimatedStateCov0; // (stateSize x stateSize)
	private float[] estimatedState0; // estimated state (stateSize)

	// hidden equations:
	// state1 = F * state0 + B * input0 + noise(0, Q)
	// observation1 = H * state1 + noise(0, R)

	public void kalman(float[] input0, float[] observed0) {
		float[][] identity = mtx.identity(stateSize);
		float[][] Ft = mtx.transpose(F);
		float[][] Ht = mtx.transpose(H);

		// predict
		// predicted next state (stateSize)
		// predicted next state covariance (stateSize x stateSize)
		float[] predictedState1 = vec.add(mtx.mul(F, estimatedState0), mtx.mul(B, input0));
		float[][] predictedStateCov1 = mtx.add(mul(F, estimatedStateCov0, Ft), Q);

		// update
		// Kalman gain (stateSize x observeSize)
		// estimated next state (stateSize)
		// estimated next state covariance (stateSize x stateSize)
		float[][] kalmanGain = mul(predictedStateCov1, Ht, mtx.inverse(mtx.add(R, mul(H, predictedStateCov1, Ht))));
		float[] estimatedState1 = vec.add(predictedState1, mtx.mul(kalmanGain, vec.sub(observed0, mtx.mul(H, predictedState1))));
		float[][] estimatedStateCov1 = mtx.mul(mtx.add(identity, mtx.neg(mtx.mul(kalmanGain, H))), predictedStateCov1);
		// residual1 = observed0 - H * estimatedState1

		estimatedStateCov0 = estimatedStateCov1;
		estimatedState0 = estimatedState1;
	}

	private float[][] mul(float[][] m0, float[][] m1, float[][] m2) {
		return mtx.mul(mtx.mul(m0, m1), m2);
	}

}
