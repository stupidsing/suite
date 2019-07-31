package suite.fp.intrinsic;

import primal.os.Log_;
import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.instructionexecutor.thunk.ThunkUtil;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.Formatter;
import suite.node.io.SwitchNode;

public class BasicIntrinsics {

	private Atom ATOM = Atom.of("ATOM");
	private Atom NUMBER = Atom.of("NUMBER");
	private Atom STRING = Atom.of("STRING");
	private Atom TREE = Atom.of("TREE");
	private Atom TUPLE = Atom.of("TUPLE");
	private Atom UNKNOWN = Atom.of("UNKNOWN");

	public Intrinsic atomString = (callback, inputs) -> {
		var name = Atom.name(inputs.get(0));
		return Intrinsics.drain(callback, p -> Int.of(name.charAt(p)), name.length());
	};

	public Intrinsic id = Intrinsics.id_;

	public Intrinsic log1 = (callback, inputs) -> {
		var node = inputs.get(0);
		Log_.info(Formatter.display(ThunkUtil.deepYawn(callback::yawn, node)));
		return node;
	};

	public Intrinsic log2 = (callback, inputs) -> {
		Log_.info(ThunkUtil.yawnString(callback::yawn, inputs.get(0)));
		return inputs.get(1);
	};

	public Intrinsic typeOf = (callback, inputs) -> {
		var node = inputs.get(0);

		return new SwitchNode<Atom>(node) //
				.applyIf(Atom.class, n -> ATOM) //
				.applyIf(Int.class, n -> NUMBER) //
				.applyIf(Str.class, n -> STRING) //
				.applyIf(Tree.class, n -> TREE) //
				.applyIf(Tuple.class, n -> TUPLE) //
				.applyIf(Node.class, n -> UNKNOWN) //
				.nonNullResult();
	};

}
