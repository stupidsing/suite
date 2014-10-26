package suite.fp.intrinsic;

import java.util.List;

import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.instructionexecutor.ThunkUtil;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tuple;
import suite.util.FunUtil.Source;
import suite.util.To;
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

	public Intrinsic left = (callback, inputs) -> {
		int position = ((Int) inputs.get(0)).number;
		List<Node> array = ((Tuple) inputs.get(1)).nodes;
		return new Tuple(Util.left(array, position));
	};

	public Intrinsic listArray = (callback, inputs) -> {
		Source<Node> value = ThunkUtil.yawnList(callback::yawn, inputs.get(0), true);
		return new Tuple(To.list(value));
	};

	public Intrinsic right = (callback, inputs) -> {
		int position = ((Int) inputs.get(0)).number;
		List<Node> array = ((Tuple) inputs.get(1)).nodes;
		return new Tuple(Util.right(array, position));
	};

}
