package suite.lp.intrinsic;

import java.util.List;

import suite.instructionexecutor.ThunkUtil;
import suite.lp.intrinsic.Intrinsics.Intrinsic;
import suite.lp.intrinsic.Intrinsics.IntrinsicCallback;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.primitive.Chars;
import suite.util.To;

public class CharsIntrinsics {

	public static Intrinsic append = (callback, inputs) -> {
		Chars chars0 = Data.get(inputs.get(0));
		Chars chars1 = Data.get(inputs.get(1));
		return new Data<>(chars0.append(chars1));
	};

	public static Intrinsic charsString = new Intrinsic() {
		public Node invoke(IntrinsicCallback callback, List<Node> inputs) {
			Chars chars = Data.get(inputs.get(0));

			if (!chars.isEmpty()) {
				Node left = Intrinsics.wrap(callback, Int.of(chars.get(0)));
				Node right = callback.wrap(this, new Data<>(chars.subchars(1)));
				return Tree.of(TermOp.OR____, left, right);
			} else
				return Atom.NIL;
		}
	};

	public static Intrinsic stringChars = (callback, inputs) -> {
		String value = ThunkUtil.evaluateToString(callback::unwrap, inputs.get(0));
		return new Data<>(To.chars(value));
	};

	public static Intrinsic subchars = (callback, inputs) -> {
		int start = ((Int) inputs.get(0)).getNumber();
		int end = ((Int) inputs.get(1)).getNumber();
		Chars chars = Data.get(inputs.get(2));
		return new Data<>(chars.subchars(start, end));
	};

}
