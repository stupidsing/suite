package suite.adt.pair; import static suite.util.Fail.fail;

import java.util.List;

public class FixieArray<T> extends Fixie<T, T, T, T, T, T, T, T, T, T> {

	public static <T> FixieArray<T> of(T[] ts) {
		var length = ts.length;

		return length < 10 ? new FixieArray<>( //
				0 < length ? ts[0] : null, //
				1 < length ? ts[1] : null, //
				2 < length ? ts[2] : null, //
				3 < length ? ts[3] : null, //
				4 < length ? ts[4] : null, //
				5 < length ? ts[5] : null, //
				6 < length ? ts[6] : null, //
				7 < length ? ts[7] : null, //
				8 < length ? ts[8] : null, //
				9 < length ? ts[9] : null) : fail();
	}

	public static <T> FixieArray<T> of(List<T> ts) {
		var length = ts.size();

		return length < 10 ? new FixieArray<>( //
				0 < length ? ts.get(0) : null, //
				1 < length ? ts.get(1) : null, //
				2 < length ? ts.get(2) : null, //
				3 < length ? ts.get(3) : null, //
				4 < length ? ts.get(4) : null, //
				5 < length ? ts.get(5) : null, //
				6 < length ? ts.get(6) : null, //
				7 < length ? ts.get(7) : null, //
				8 < length ? ts.get(8) : null, //
				9 < length ? ts.get(9) : null) : fail();
	}

	private FixieArray(T t0, T t1, T t2, T t3, T t4, T t5, T t6, T t7, T t8, T t9) {
		super(t0, t1, t2, t3, t4, t5, t6, t7, t8, t9);
	}
}
