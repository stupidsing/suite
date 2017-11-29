package suite.fp.intrinsic;

import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.instructionexecutor.thunk.ThunkUtil;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.Formatter;
import suite.os.LogUtil;

public class BasicIntrinsics {

	private Atom ATOM = Atom.of("ATOM");
	private Atom NUMBER = Atom.of("NUMBER");
	private Atom TREE = Atom.of("TREE");
	private Atom TUPLE = Atom.of("TUPLE");
	private Atom UNKNOWN = Atom.of("UNKNOWN");

	public Intrinsic atomString = (callback, inputs) -> {
		String name = ((Atom) inputs.get(0)).name;
		return Intrinsics.drain(callback, p -> Int.of(name.charAt(p)), name.length());
	};

	public Intrinsic id = Intrinsics.id_;

	public Intrinsic log1 = (callback, inputs) -> {
		Node node = inputs.get(0);
		LogUtil.info(Formatter.display(ThunkUtil.deepYawn(callback::yawn, node)));
		return node;
	};

	public Intrinsic log2 = (callback, inputs) -> {
		LogUtil.info(ThunkUtil.yawnString(callback::yawn, inputs.get(0)));
		return inputs.get(1);
	};

	public Intrinsic typeOf = (callback, inputs) -> {
		Node node = inputs.get(0);
		Atom type;

		if (node instanceof Atom)
			type = ATOM;
		else if (node instanceof Int)
			type = NUMBER;
		else if (node instanceof Tree)
			type = TREE;
		else if (node instanceof Tuple)
			type = TUPLE;
		else
			type = UNKNOWN;

		return type;
	};

}
