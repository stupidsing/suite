package suite.fp.intrinsic;

import java.util.List;

import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.instructionexecutor.thunk.ThunkUtil;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tuple;
import suite.streamlet.Outlet;
import suite.util.Util;

public class ArrayIntrinsics {

	public Intrinsic append = (callback, inputs) -> {
		List<Node> array0 = ((Tuple) inputs.get(0)).nodes;
		List<Node> array1 = ((Tuple) inputs.get(1)).nodes;
		return new Tuple(Util.add(array0, array1));
	};

	public Intrinsic arrayList = (callback, inputs) -> {
		List<Node> array = ((Tuple) inputs.get(0)).nodes;
		return Intrinsics.drain(callback, array::get, array.size());
	};

	public Intrinsic concat = (callback, inputs) -> {
		Outlet<Node> list = ThunkUtil.yawnList(callback::yawn, inputs.get(0), true);
		return new Tuple(list.concatMap(n -> Outlet.from(((Tuple) n).nodes)).toList());
	};

	public Intrinsic listArray = (callback, inputs) -> {
		return new Tuple(ThunkUtil.yawnList(callback::yawn, inputs.get(0), true).toList());
	};

	public Intrinsic slice = (callback, inputs) -> {
		int s = ((Int) inputs.get(0)).number;
		int e = ((Int) inputs.get(1)).number;
		List<Node> array = ((Tuple) inputs.get(2)).nodes;
		if (s < 0)
			s += array.size();
		if (e < s)
			e += array.size();
		return new Tuple(array.subList(s, e));
	};

}
