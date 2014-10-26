package suite.fp.intrinsic;

import java.util.List;

import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.fp.intrinsic.Intrinsics.IntrinsicCallback;
import suite.immutable.IPointer;
import suite.instructionexecutor.thunk.IndexedSourceReader;
import suite.instructionexecutor.thunk.ThunkUtil;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
import suite.node.Suspend;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.primitive.Chars;
import suite.primitive.CharsUtil;
import suite.util.FunUtil;
import suite.util.FunUtil.Source;
import suite.util.To;

public class CharsIntrinsics {

	public Intrinsic append = (callback, inputs) -> {
		Chars chars0 = Data.get(inputs.get(0));
		Chars chars1 = Data.get(inputs.get(1));
		return new Data<>(chars0.append(chars1));
	};

	public Intrinsic charsString = (callback, inputs) -> {
		Chars chars = Data.get(inputs.get(0));
		return Intrinsics.drain(callback, p -> Int.of(chars.get(p)), chars.size());
	};

	public Intrinsic drain = new Intrinsic() {
		public Node invoke(IntrinsicCallback callback, List<Node> inputs) {
			IPointer<Chars> intern = Data.get(inputs.get(0));
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
		int sep = ((Int) inputs.get(0)).number;
		Chars chars = Data.get(inputs.get(1));
		int pos = 0;
		while (pos < chars.size() && chars.get(pos) != sep)
			pos++;
		return Tree.of(TermOp.AND___ //
				, callback.enclose(Intrinsics.id_, new Data<>(chars.subchars(0, pos))) //
				, callback.enclose(Intrinsics.id_, new Data<>(chars.subchars(pos))));
	};

	public Intrinsic splits = (callback, inputs) -> {
		Chars delim = Data.get(inputs.get(0));
		Source<Node> s0 = ThunkUtil.yawnList(callback::yawn, inputs.get(1), true);
		Source<Chars> s1 = CharsUtil.split(FunUtil.map(n -> Data.get(n), s0), delim);
		Source<Node> s2 = FunUtil.map(node -> new Data<>(node), s1);
		IPointer<Node> p = new IndexedSourceReader<>(s2).pointer();
		return Intrinsics.drain(callback, p);
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
