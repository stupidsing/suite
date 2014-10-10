package suite.fp.intrinsic;

import java.util.List;

import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.fp.intrinsic.Intrinsics.IntrinsicCallback;
import suite.instructionexecutor.IndexedCharsReader;
import suite.instructionexecutor.ThunkUtil;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
import suite.node.Suspend;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.primitive.Chars;
import suite.util.To;

public class CharsIntrinsics {

	public Intrinsic append = (callback, inputs) -> {
		Chars chars0 = Data.get(inputs.get(0));
		Chars chars1 = Data.get(inputs.get(1));
		return new Data<>(chars0.append(chars1));
	};

	public Intrinsic charsString = new Intrinsic() {
		public Node invoke(IntrinsicCallback callback, List<Node> inputs) {
			Chars chars = Data.get(inputs.get(0));

			if (!chars.isEmpty()) {
				Node left = Intrinsics.enclose(callback, Int.of(chars.get(0)));
				Node right = callback.enclose(this, new Data<>(chars.subchars(1)));
				return Tree.of(TermOp.OR____, left, right);
			} else
				return Atom.NIL;
		}
	};

	public Intrinsic drain = new Intrinsic() {
		public Node invoke(IntrinsicCallback callback, List<Node> inputs) {
			IndexedCharsReader.Pointer intern = Data.get(inputs.get(0));
			Chars chars = intern.head();

			// Suspend the right node to avoid stack overflow when input
			// data is very long under eager mode
			if (chars != null) {
				Node left = Intrinsics.enclose(callback, new Data<>(chars));
				Node right = new Suspend(() -> callback.enclose(this, new Data<>(intern.tail())));
				return Tree.of(TermOp.OR____, left, right);
			} else
				return Atom.NIL;
		}
	};

	public Intrinsic split = (callback, inputs) -> {
		Chars chars = Data.get(inputs.get(0));
		int sep = ((Int) inputs.get(1)).number;
		int pos = 0;
		while (pos < chars.size() && chars.get(pos) != sep)
			pos++;
		return Tree.of(TermOp.AND___ //
				, callback.enclose(Intrinsics.id_, new Data<>(chars.subchars(0, pos))) //
				, callback.enclose(Intrinsics.id_, new Data<>(chars.subchars(pos))));
	};

	public Intrinsic stringChars = (callback, inputs) -> {
		String value = ThunkUtil.yawnString(callback::yawn, inputs.get(0));
		return new Data<>(To.chars(value));
	};

	public Intrinsic subchars = (callback, inputs) -> {
		int start = ((Int) inputs.get(0)).number;
		int end = ((Int) inputs.get(1)).number;
		Chars chars = Data.get(inputs.get(2));
		return new Data<>(chars.subchars(start, end));
	};

}
