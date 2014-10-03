package suite.fp.intrinsic;

import java.util.List;

import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.fp.intrinsic.Intrinsics.IntrinsicCallback;
import suite.instructionexecutor.ThunkUtil;
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

	private Atom ATOM = Atom.of("ATOM");
	private Atom NUMBER = Atom.of("NUMBER");
	private Atom STRING = Atom.of("STRING");
	private Atom TREE = Atom.of("TREE");
	private Atom TUPLE = Atom.of("TUPLE");
	private Atom UNKNOWN = Atom.of("UNKNOWN");

	public Intrinsic atomString = new Intrinsic() {
		public Node invoke(IntrinsicCallback callback, List<Node> inputs) {
			String name = ((Atom) inputs.get(0)).name;

			if (!name.isEmpty()) {
				Node left = callback.enclose(Intrinsics.id_, Int.of(name.charAt(0)));
				Node right = callback.enclose(this, Atom.of(name.substring(1)));
				return Tree.of(TermOp.OR____, left, right);
			} else
				return Atom.NIL;
		}
	};

	public Intrinsic id = Intrinsics.id_;

	public Intrinsic log1 = (callback, inputs) -> {
		Node node = inputs.get(0);
		LogUtil.info(Formatter.display(ThunkUtil.yawnFully(callback::yawn, node)));
		return node;
	};

	public Intrinsic log2 = (callback, inputs) -> {
		LogUtil.info(ThunkUtil.yawnString(callback::yawn, inputs.get(0)));
		return inputs.get(1);
	};

	public Intrinsic throw_ = (callback, inputs) -> {
		String message = ThunkUtil.yawnString(callback::yawn, inputs.get(0));
		throw new RuntimeException(Util.isNotBlank(message) ? message : "Error termination");
	};

	public Intrinsic typeOf = (callback, inputs) -> {
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
