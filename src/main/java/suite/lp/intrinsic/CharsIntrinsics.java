package suite.lp.intrinsic;

import java.util.List;

import suite.instructionexecutor.ExpandUtil;
import suite.lp.intrinsic.Intrinsics.Intrinsic;
import suite.lp.intrinsic.Intrinsics.IntrinsicBridge;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.primitive.Chars;

public class CharsIntrinsics {

	public static Intrinsic append = (bridge, inputs) -> {
		Chars chars0 = Data.get(inputs.get(0));
		Chars chars1 = Data.get(inputs.get(1));
		return new Data<>(chars0.append(chars1));
	};

	public static Intrinsic charsString = new Intrinsic() {
		public Node invoke(IntrinsicBridge bridge, List<Node> inputs) {
			Chars chars = Data.get(inputs.get(0));

			if (!chars.isEmpty()) {
				Node left = bridge.wrap(BasicIntrinsics.id, Int.of(chars.get(0)));
				Node right = bridge.wrap(this, new Data<>(chars.subchars(1)));
				return Tree.of(TermOp.OR____, left, right);
			} else
				return Atom.NIL;
		}
	};

	public static Intrinsic stringChars = (bridge, inputs) -> {
		String value = ExpandUtil.expandString(bridge::unwrap, inputs.get(0));
		return new Data<>(new Chars(value.toCharArray()));
	};

	public static Intrinsic subchars = (bridge, inputs) -> {
		int start = ((Int) inputs.get(0)).getNumber();
		int end = ((Int) inputs.get(1)).getNumber();
		Chars chars = Data.get(inputs.get(2));
		return new Data<>(chars.subchars(start, end));
	};

}
