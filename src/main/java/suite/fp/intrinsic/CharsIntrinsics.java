package suite.fp.intrinsic;

import primal.puller.Puller;
import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.instructionexecutor.thunk.IPointerMapper;
import suite.instructionexecutor.thunk.IndexedSourceReader;
import suite.instructionexecutor.thunk.ThunkUtil;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
import suite.persistent.PerPointer;
import suite.primitive.Chars;
import suite.primitive.Chars_;
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
		PerPointer<Chars> pointer = Data.get(inputs.get(0));
		return Intrinsics.drain(callback, IPointerMapper.map(Data<Chars>::new, pointer));
	};

	public Intrinsic concatSplit = (callback, inputs) -> {
		Chars delim = Data.get(inputs.get(0));
		var s0 = ThunkUtil.yawnList(callback::yawn, inputs.get(1), true);
		var s1 = s0.map(n -> Data.<Chars> get(callback.yawn(n)));
		var s2 = Chars_.split(delim).apply(s1);
		Puller<Node> s3 = s2.map(Data<Chars>::new);
		var p = IndexedSourceReader.of(s3.source());
		return Intrinsics.drain(callback, p);
	};

	public Intrinsic replace = (callback, inputs) -> {
		Chars from = Data.get(inputs.get(0));
		Chars to = Data.get(inputs.get(1));
		Chars chars = Data.get(inputs.get(2));
		return new Data<>(chars.replace(from, to));
	};

	public Intrinsic stringChars = (callback, inputs) -> {
		var value = ThunkUtil.yawnString(callback::yawn, inputs.get(0));
		return new Data<>(To.chars(value));
	};

	public Intrinsic subchars = (callback, inputs) -> {
		var start = Int.num(inputs.get(0));
		var end = Int.num(inputs.get(1));
		Chars chars = Data.get(inputs.get(2));
		return new Data<>(chars.range(start, end));
	};

}
