package suite.math.stat;

import suite.math.linalg.Matrix;

public class Kalman {

	private Matrix mtx = new Matrix();

	private int stateSize = 16;
	private float[][] F; // state transition
	private float[][] B; // input control
	private float[][] H; // observation
	private float[][] Q; // state noise
	private float[][] R; // observation noise
	private float[][] covariance0;
	private float[] estimatedState0;

	// hidden equations:
	// state1 = F * state0 + B * input0 + noise(0, Q)
	// observation1 = H * state1 + noise(0, R)

	public void kalman(float[] input0, float[] observed0) {
		float[][] identity = mtx.identity(stateSize);
		float[][] Ft = mtx.transpose(F);
		float[][] Ht = mtx.transpose(H);

		// predict
		float[] predictedState1 = mtx.add(mtx.mul(F, estimatedState0), mtx.mul(B, input0));
		float[][] predictedCovariance1 = mtx.add(mul(F, covariance0, Ft), Q);

		// update
		float[][] kalmanGain = mul(predictedCovariance1, Ht, mtx.inverse(mtx.add(R, mul(H, predictedCovariance1, Ht))));
		float[] estimatedState1 = mtx.add(predictedState1, mtx.mul(kalmanGain, mtx.sub(observed0, mtx.mul(H, predictedState1))));
		float[][] covariance1 = mtx.mul(mtx.add(identity, mtx.neg(mtx.mul(kalmanGain, H))), predictedCovariance1);
		// residual1 = observed0 - H * estimatedState1

		covariance0 = covariance1;
		estimatedState0 = estimatedState1;
	}

	private float[][] mul(float[][] m0, float[][] m1, float[][] m2) {
		return mtx.mul(mtx.mul(m0, m1), m2);
	}

}
