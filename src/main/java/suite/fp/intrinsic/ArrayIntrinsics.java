package suite.fp.intrinsic;

import java.util.List;

import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.fp.intrinsic.Intrinsics.IntrinsicCallback;
import suite.instructionexecutor.ThunkUtil;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.TermOp;
import suite.util.FunUtil.Source;
import suite.util.To;
import suite.util.Util;

public class ArrayIntrinsics {

	public Intrinsic append = (callback, inputs) -> {
		List<Node> array0 = ((Tuple) inputs.get(0)).getNodes();
		List<Node> array1 = ((Tuple) inputs.get(1)).getNodes();
		return new Tuple(Util.add(array0, array1));
	};

	public Intrinsic arrayList = new Intrinsic() {
		public Node invoke(IntrinsicCallback callback, List<Node> inputs) {
			List<Node> array = ((Tuple) inputs.get(0)).getNodes();

			if (!array.isEmpty()) {
				Node left = Intrinsics.enclose(callback, array.get(0));
				Node right = callback.enclose(this, new Tuple(array.subList(1, array.size())));
				return Tree.of(TermOp.OR____, left, right);
			} else
				return Atom.NIL;
		}
	};

	public Intrinsic left = (callback, inputs) -> {
		int position = ((Int) inputs.get(0)).getNumber();
		List<Node> array = ((Tuple) inputs.get(1)).getNodes();
		return new Tuple(Util.left(array, position));
	};

	public Intrinsic listArray = (callback, inputs) -> {
		Source<Node> value = ThunkUtil.yawnList(callback::yawn, inputs.get(0), true);
		return new Tuple(To.list(value));
	};

	public Intrinsic right = (callback, inputs) -> {
		int position = ((Int) inputs.get(0)).getNumber();
		List<Node> array = ((Tuple) inputs.get(1)).getNodes();
		return new Tuple(Util.right(array, position));
	};

}
