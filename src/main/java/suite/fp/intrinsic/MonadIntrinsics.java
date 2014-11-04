package suite.fp.intrinsic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

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
import suite.util.FileUtil;
import suite.util.FunUtil.Fun;
import suite.util.LogUtil;

public class MonadIntrinsics {

	private Map<Node, Map<Node, Node>> mutables = new WeakHashMap<>();

	public Intrinsic get = (callback, inputs) -> getFrame(inputs).get(inputs.get(1));

	public Intrinsic popen = (callback, inputs) -> {
		Fun<Node, Node> yawn = callback::yawn;
		List<String> list = ThunkUtil.yawnList(yawn, inputs.get(0), false).map(node -> ThunkUtil.yawnString(yawn, node)).toList();

		Node in = inputs.get(1);

		try {
			Process process = Runtime.getRuntime().exec(list.toArray(new String[list.size()]));

			Node n0 = Intrinsics.enclose(callback, new Suspend(() -> {
				try {
					return Int.of(process.waitFor());
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}));

			Node n1 = createReader(callback, process.getInputStream());
			Node n2 = createReader(callback, process.getErrorStream());

			// Use a separate thread to write to the process, so that read
			// and write occur at the same time and would not block up.
			// The input stream is also closed by this thread.
			// Have to make sure the executors are thread-safe!
			new Thread(() -> {
				try {
					try (OutputStream pos = process.getOutputStream(); Writer writer = new OutputStreamWriter(pos)) {
						ThunkUtil.yawnWriter(yawn, in, writer);
					}

					process.waitFor();
				} catch (Exception ex) {
					LogUtil.error(ex);
				}
			}).start();

			return Tree.of(TermOp.AND___, n0 //
					, Intrinsics.enclose(callback, Tree.of(TermOp.AND___, n1, n2)));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	};

	public Intrinsic put = (callback, inputs) -> {
		getFrame(inputs).put(inputs.get(1), inputs.get(2));
		return Atom.NIL;
	};

	private Map<Node, Node> getFrame(List<Node> inputs) {
		Node frame = inputs.get(0);
		return mutables.computeIfAbsent(frame, f -> new HashMap<>());
	}

	private Node createReader(IntrinsicCallback callback, InputStream is) {
		BufferedReader br = new BufferedReader(new InputStreamReader(is, FileUtil.charset));
		IPointer<Chars> icrp = Intrinsics.read(br);
		return callback.enclose(new CharsIntrinsics().drain, new Data<>(icrp));
	}

}
