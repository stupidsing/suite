package suite.lp.intrinsic;

import java.util.List;

import suite.instructionexecutor.ExpandUtil;
import suite.lp.intrinsic.Intrinsics.Intrinsic;
import suite.lp.intrinsic.Intrinsics.IntrinsicBridge;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.Formatter;
import suite.node.io.TermOp;
import suite.util.LogUtil;
import suite.util.Util;

public class BasicIntrinsics {

	private static Atom ATOM = Atom.of("ATOM");
	private static Atom NUMBER = Atom.of("NUMBER");
	private static Atom STRING = Atom.of("STRING");
	private static Atom TREE = Atom.of("TREE");
	private static Atom TUPLE = Atom.of("TUPLE");
	private static Atom UNKNOWN = Atom.of("UNKNOWN");

	public static Intrinsic atomString = new Intrinsic() {
		public Node invoke(IntrinsicBridge bridge, List<Node> inputs) {
			String name = ((Atom) inputs.get(0)).getName();

			if (!name.isEmpty()) {
				Node left = bridge.wrap(id, Int.of(name.charAt(0)));
				Node right = bridge.wrap(this, Atom.of(name.substring(1)));
				return Tree.of(TermOp.OR____, left, right);
			} else
				return Atom.NIL;
		}
	};

	public static Intrinsic id = (bridge, inputs) -> {
		return inputs.get(0).finalNode(); // Forces suspended node evaluation
	};

	public static Intrinsic log1 = (bridge, inputs) -> {
		Node node = inputs.get(0);
		LogUtil.info(Formatter.display(ExpandUtil.expandFully(bridge::unwrap, node)));
		return node;
	};

	public static Intrinsic log2 = (bridge, inputs) -> {
		LogUtil.info(ExpandUtil.expandString(bridge::unwrap, inputs.get(0)));
		return inputs.get(1);
	};

	public static Intrinsic throw_ = (bridge, inputs) -> {
		String message = ExpandUtil.expandString(bridge::unwrap, inputs.get(0));
		throw new RuntimeException(Util.isNotBlank(message) ? message : "Error termination");
	};

	public static Intrinsic typeOf = (bridge, inputs) -> {
		Node node = inputs.get(0);
		Atom type;

		if (node instanceof Atom)
			type = ATOM;
		else if (node instanceof Int)
			type = NUMBER;
		else if (node instanceof Str)
			type = STRING;
		else if (node instanceof Tree)
			type = TREE;
		else if (node instanceof Tuple)
			type = TUPLE;
		else
			type = UNKNOWN;

		return type;
	};

}
