package suite.fp.intrinsic;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import suite.Constants;
import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.fp.intrinsic.Intrinsics.IntrinsicCallback;
import suite.immutable.IPointer;
import suite.instructionexecutor.thunk.ThunkUtil;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
import suite.node.Suspend;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.primitive.Chars;
import suite.util.FunUtil.Iterate;
import suite.util.Rethrow;
import suite.util.Thread_;

public class MonadIntrinsics {

	private Map<Node, Map<Node, Node>> mutables = new WeakHashMap<>();

	public Intrinsic get = (callback, inputs) -> getFrame(inputs).get(inputs.get(1));

	public Intrinsic popen = (callback, inputs) -> {
		Iterate<Node> yawn = callback::yawn;
		String[] array = ThunkUtil.yawnList(yawn, inputs.get(0), false) //
				.map(node -> ThunkUtil.yawnString(yawn, node)) //
				.toArray(String.class);

		var in = inputs.get(1);

		return Rethrow.ex(() -> {
			var process = Runtime.getRuntime().exec(array);

			Node n0 = Intrinsics.enclose(callback, new Suspend(() -> Rethrow.ex(() -> Int.of(process.waitFor()))));

			Node n1 = newReader(callback, process.getInputStream());
			Node n2 = newReader(callback, process.getErrorStream());

			// use a separate thread to write to the process, so that read
			// and write occur at the same time and would not block up.
			// the input stream is also closed by this thread.
			// have to make sure the executors are thread-safe!
			Thread_.startThread(() -> {
				try (var pos = process.getOutputStream(); var writer = new OutputStreamWriter(pos)) {
					ThunkUtil.yawnWriter(yawn, in, writer);
				}

				process.waitFor();
			});

			return Tree.of(TermOp.AND___, //
					n0, //
					Intrinsics.enclose(callback, Tree.of(TermOp.AND___, n1, n2)));
		});
	};

	public Intrinsic put = (callback, inputs) -> {
		getFrame(inputs).put(inputs.get(1), inputs.get(2));
		return Atom.NIL;
	};

	private Map<Node, Node> getFrame(List<Node> inputs) {
		var frame = inputs.get(0);
		return mutables.computeIfAbsent(frame, f -> new HashMap<>());
	}

	private Node newReader(IntrinsicCallback callback, InputStream is) {
		BufferedReader br = new BufferedReader(new InputStreamReader(is, Constants.charset));
		IPointer<Chars> icrp = Intrinsics.read(br);
		return callback.enclose(new CharsIntrinsics().drain, new Data<>(icrp));
	}

}
