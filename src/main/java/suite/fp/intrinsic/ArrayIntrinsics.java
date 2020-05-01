package suite.fp.intrinsic;

import primal.Verbs.Copy;
import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.instructionexecutor.thunk.ThunkUtil;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tuple;

import java.util.Arrays;

public class ArrayIntrinsics {

	public Intrinsic append = (callback, inputs) -> {
		var array0 = Tuple.t(inputs.get(0));
		var array1 = Tuple.t(inputs.get(1));
		var array = new Node[array0.length + array1.length];
		Copy.array(array0, 0, array, 0, array0.length);
		Copy.array(array1, 0, array, array0.length, array1.length);
		return Tuple.of(array);
	};

	public Intrinsic arrayList = (callback, inputs) -> {
		var array = Tuple.t(inputs.get(0));
		return Intrinsics.drain(callback, i -> array[i], array.length);
	};

	public Intrinsic listArray = (callback, inputs) -> {
		return Tuple.of(ThunkUtil.yawnList(callback::yawn, inputs.get(0), true).toArray(Node.class));
	};

	public Intrinsic slice = (callback, inputs) -> {
		var s = Int.num(inputs.get(0));
		var e = Int.num(inputs.get(1));
		var array = Tuple.t(inputs.get(2));
		if (s < 0)
			s += array.length;
		if (e < s)
			e += array.length;
		return Tuple.of(Arrays.copyOfRange(array, s, e, Node[].class));
	};

}
