package suite.math.linalg;

import static suite.util.Friends.fail;

import suite.primitive.Int_Flt;
import suite.util.To;

public class VirtualVector {

	public final int length;
	public final Int_Flt get;

	public interface Apply<T> {
		public T apply(int length, Int_Flt get);
	}

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
		var vv0 = this;
		var f0 = vv0.get;
		var f1 = vv1.get;
		return VirtualVectorUtil.checkSizes(vv0, vv1, i -> f0.apply(i) + f1.apply(i));
	}

	public <T> T apply(Apply<T> apply) {
		return apply_(apply);
	}

	public VirtualVector buffer() {
		return of(matrix());
	}

	public VirtualVector dot(VirtualVector vv1) {
		return apply((length0, f0) -> apply((length1, f1) -> {
			return length0 == length1 ? of(length, i -> f0.apply(i) * f1.apply(i)) : fail("wrong input sizes");
		}));
	}

	public String dump() {
		var sb = new StringBuilder();
		dump(sb);
		return sb.toString();
	}

	public void dump(StringBuilder sb) {
		sb.append("[ ");
		for (var i = 0; i < length; i++)
			sb.append(To.string(get.apply(i)) + " ");
		sb.append("\n");
	}

	public VirtualVector scale(double d) {
		return of(length, i -> (float) (get.apply(i) * d));
	}

	public float[] matrix() {
		return apply((l, get) -> {
			var matrix = new float[l];
			for (var i = 0; i < l; i++)
				matrix[i] = get.apply(i);
			return matrix;
		});
	}

	private <T> T apply_(Apply<T> apply) {
		return apply.apply(length, get);
	}

}

class VirtualVectorUtil {

	public static Matrix mtx = new Matrix();

	public static VirtualVector checkSizes(VirtualVector vv0, VirtualVector vv1, Int_Flt fun) {
		return vv0.apply((length0, f0) -> vv1.apply((length1, f1) -> {
			return length0 == length1 ? VirtualVector.of(length0, fun) : fail("wrong input sizes");
		}));
	}

}
