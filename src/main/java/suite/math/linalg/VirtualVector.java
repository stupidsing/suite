package suite.math.linalg;

import suite.primitive.Int_Flt;
import suite.util.To;

public class VirtualVector {

	public final int length;
	public final Int_Flt get;

	public static VirtualVector of(float[] fs) {
		return of(fs.length, i -> fs[i]);
	}

	public static VirtualVector of(int length, Int_Flt get) {
		return new VirtualVector(length, get);
	}

	private VirtualVector(int length, Int_Flt get) {
		this.length = length;
		this.get = get;
	}

	public VirtualVector add(VirtualVector vv1) {
		VirtualVector vv0 = this;
		Int_Flt f0 = vv0.get;
		Int_Flt f1 = vv1.get;
		return VirtualVectorUtil.checkSizes(vv0, vv1, i -> f0.apply(i) + f1.apply(i));
	}

	public VirtualVector buffer() {
		return of(matrix());
	}

	public VirtualVector dot(VirtualVector vv1) {
		VirtualVector vv0 = this;
		Int_Flt f0 = vv0.get;
		Int_Flt f1 = vv1.get;
		int l = vv0.length;

		if (l == vv1.length)
			return of(length, i -> f0.apply(l) * f1.apply(l));
		else
			throw new RuntimeException("wrong input sizes");
	}

	public String dump() {
		StringBuilder sb = new StringBuilder();
		dump(sb);
		return sb.toString();
	}

	public void dump(StringBuilder sb) {
		int l = length;
		sb.append("[ ");
		for (int i = 0; i < l; i++)
			sb.append(To.string(get.apply(i)) + " ");
		sb.append("\n");
	}

	public VirtualVector scale(double d) {
		return of(length, i -> (float) (get.apply(i) * d));
	}

	public float[] matrix() {
		int l = length;
		float[] matrix = new float[l];
		for (int i = 0; i < l; i++)
			matrix[i] = get.apply(i);
		return matrix;
	}

}

class VirtualVectorUtil {

	public static Matrix_ mtx = new Matrix_();

	public static VirtualVector checkSizes(VirtualVector vv0, VirtualVector vv1, Int_Flt fun) {
		int length = vv0.length;
		if (length == vv1.length)
			return VirtualVector.of(length, fun);
		else
			throw new RuntimeException("wrong input sizes");
	}

}
