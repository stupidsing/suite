package suite.math.linalg;

import suite.primitive.IntInt_Flt;
import suite.primitive.Int_Dbl;
import suite.primitive.Int_Flt;
import suite.primitive.Ints_;
import suite.util.To;

public class VirtualMatrix {

	public final int height;
	public final int width_;
	public final IntInt_Flt get;

	public static VirtualMatrix ofIdentity(int rank) {
		return of(rank, rank, (i, j) -> i != j ? 0f : 1f);
	}

	public static VirtualMatrix ofDiagonal(float[] fs) {
		int rank = fs.length;
		return of(rank, rank, (i, j) -> i != j ? 0f : fs[i]);
	}

	public static VirtualMatrix of(float[] fs) {
		return of(fs.length, 1, (i, j) -> fs[i]);
	}

	public static VirtualMatrix ofTranspose(float[] fs) {
		return of(1, fs.length, (i, j) -> fs[j]);
	}

	public static VirtualMatrix of(float[][] matrix) {
		Matrix_ mtx = VirtualMatrixUtil.mtx;
		return of(mtx.height(matrix), mtx.width(matrix), (i, j) -> matrix[i][j]);
	}

	public static VirtualMatrix of(int height, int width_, IntInt_Flt get) {
		return new VirtualMatrix(height, width_, get);
	}

	private VirtualMatrix(int height, int width_, IntInt_Flt get) {
		this.height = height;
		this.width_ = width_;
		this.get = get;
	}

	public VirtualMatrix add(VirtualMatrix vm1) {
		VirtualMatrix vm0 = this;
		IntInt_Flt f0 = vm0.get;
		IntInt_Flt f1 = vm1.get;
		return VirtualMatrixUtil.checkSizes(vm0, vm1, (i, j) -> f0.apply(i, j) + f1.apply(i, j));
	}

	public VirtualMatrix buffer() {
		return of(matrix());
	}

	public String dump() {
		StringBuilder sb = new StringBuilder();
		dump(sb);
		return sb.toString();
	}

	public void dump(StringBuilder sb) {
		int h = height;
		int w = width_;
		sb.append("[ ");
		for (int i = 0; i < h; i++) {
			for (int j = 0; i < w; j++)
				sb.append(To.string(get.apply(i, j)) + " ");
			sb.append("\n");
		}
	}

	public VirtualVector mul(float[] nT) {
		VirtualMatrix vm0 = this;
		int ix = vm0.height;
		int jx = vm0.width_;
		IntInt_Flt f0 = vm0.get;
		float[] o = new float[ix];
		int i1, j1;
		if (jx == nT.length)
			for (int i0 = 0; i0 < ix; i0 = i1) {
				i1 = Math.min(i0 + 64, ix);
				for (int j0 = 0; j0 < jx; j0 = j1) {
					j1 = Math.min(j0 + 64, jx);
					for (int i = i0; i < i1; i++)
						for (int j = j0; j < j1; j++)
							o[i] += f0.apply(i, j) * nT[j];
				}
			}
		else
			throw new RuntimeException("wrong input sizes");
		return VirtualVector.of(o);
	}

	public VirtualVector mul(VirtualVector vv) {
		VirtualMatrix vm = this;
		IntInt_Flt f0 = vm.get;
		Int_Flt f1 = vv.get;
		int l = vm.width_;
		if (l == vv.length)
			return VirtualVector.of(vm.height,
					i -> (float) Ints_.range(l).collectAsDouble(Int_Dbl.sum(j -> f0.apply(i, j) * f1.apply(j))));
		else
			throw new RuntimeException("wrong input sizes");
	}

	public VirtualMatrix mul(VirtualMatrix vm1) {
		VirtualMatrix vm0 = this;
		IntInt_Flt f0 = vm0.get;
		IntInt_Flt f1 = vm1.get;

		int ks = vm0.width_;
		int height = vm0.height;
		int width_ = vm1.width_;
		float[][] o = new float[height][width_];
		int i1, j1, k1;

		if (ks == vm1.height)
			for (int i0 = 0; i0 < height; i0 = i1) {
				i1 = Math.min(i0 + 64, height);
				for (int j0 = 0; j0 < width_; j0 = j1) {
					j1 = Math.min(j0 + 64, width_);
					for (int k0 = 0; k0 < ks; k0 = k1) {
						k1 = Math.min(k0 + 64, ks);
						for (int i = i0; i < i1; i++)
							for (int j = j0; j < j1; j++)
								for (int k = k0; k < k1; k++)
									o[i][j] += f0.apply(i, k) * f1.apply(k, j);
					}
				}
			}
		else
			throw new RuntimeException("wrong input sizes");

		return of(o);
	}

	public VirtualMatrix mulLazy(VirtualMatrix vm1) {
		VirtualMatrix vm0 = this;
		IntInt_Flt f0 = vm0.get;
		IntInt_Flt f1 = vm1.get;
		int ks = vm0.width_;

		if (ks == vm1.height)
			return of(vm0.height, vm1.width_,
					(i, j) -> (float) Ints_.range(ks).collectAsDouble(Int_Dbl.sum(k -> f0.apply(i, k) * f1.apply(k, j))));
		else
			throw new RuntimeException("wrong input sizes");
	}

	public VirtualMatrix scale(double d) {
		return of(height, width_, (i, j) -> (float) (get.apply(i, j) * d));
	}

	public VirtualMatrix transpose() {
		return of(width_, height, (i, j) -> get.apply(j, i));
	}

	public float[][] matrix() {
		int h = height;
		int w = width_;
		float[][] matrix = new float[h][w];
		for (int i = 0; i < h; i++)
			for (int j = 0; i < w; j++)
				matrix[i][j] = get.apply(i, j);
		return matrix;
	}

}

class VirtualMatrixUtil {

	public static Matrix_ mtx = new Matrix_();

	public static VirtualMatrix checkSizes(VirtualMatrix vm0, VirtualMatrix vm1, IntInt_Flt fun) {
		int height = vm0.height;
		int width_ = vm0.width_;
		if (height == vm1.height && width_ == vm1.width_)
			return VirtualMatrix.of(height, width_, fun);
		else
			throw new RuntimeException("wrong input sizes");
	}

}
