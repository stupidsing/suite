package suite.fp.intrinsic;

import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.immutable.IPointer;
import suite.instructionexecutor.thunk.IPointerMapper;
import suite.instructionexecutor.thunk.IndexedSourceReader;
import suite.instructionexecutor.thunk.ThunkUtil;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
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

	public Intrinsic drain = (callback, inputs) -> {
		IPointer<Chars> pointer = Data.get(inputs.get(0));
		return Intrinsics.drain(callback, new IPointerMapper<Chars, Node>(Data<Chars>::new).map(pointer));
	};

	public Intrinsic concatSplit = (callback, inputs) -> {
		Chars delim = Data.get(inputs.get(0));
		Source<Node> s0 = ThunkUtil.yawnList(callback::yawn, inputs.get(1), true);
		Source<Chars> s1 = FunUtil.map(n -> Data.<Chars> get(callback.yawn(n)), s0);
		Source<Chars> s2 = CharsUtil.concatSplit(s1, delim);
		Source<Node> s3 = FunUtil.map(Data<Chars>::new, s2);
		IPointer<Node> p = new IndexedSourceReader<>(s3).pointer();
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
