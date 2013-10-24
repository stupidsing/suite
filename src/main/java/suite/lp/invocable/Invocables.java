package suite.lp.invocable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import suite.instructionexecutor.ExpandUtil;
import suite.instructionexecutor.FunInstructionExecutor;
import suite.instructionexecutor.IndexedReader;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.TermParser.TermOp;
import suite.util.FileUtil;
import suite.util.FunUtil.Fun;
import suite.util.LogUtil;

public class Invocables {

	private static final Atom ATOM = Atom.create("ATOM");
	private static final Atom NUMBER = Atom.create("NUMBER");
	private static final Atom STRING = Atom.create("STRING");
	private static final Atom TREE = Atom.create("TREE");
	private static final Atom UNKNOWN = Atom.create("UNKNOWN");

	public static abstract class Invocable {
		public abstract Node invoke(FunInstructionExecutor executor, List<Node> inputs);
	}

	public static class AtomString extends Invocable {
		public Node invoke(FunInstructionExecutor executor, List<Node> inputs) {
			String name = ((Atom) executor.getUnwrapper().apply(inputs.get(0))).getName();

			if (!name.isEmpty()) {
				Node left = executor.wrapInvocableNode(new Id(), Int.create(name.charAt(0)));
				Node right = executor.wrapInvocableNode(this, Atom.create(name.substring(1)));
				return Tree.create(TermOp.OR____, left, right);
			} else
				return Atom.NIL;
		}
	}

	// public static class Exec extends Invocable {
	// public Node invoke(FunInstructionExecutor executor, List<Node> inputs) {
	// Node program = executor.getUnwrapper().apply(inputs.get(0));
	//
	// for (Node step : Node.iter(TermOp.NEXT__, program)) {
	// List<Node> list = Node.tupleToList(step);
	//
	// if (list.get(0) == ATOM.create("return"))
	// ;
	// }
	// }
	// }

	public static class Fgetc extends Invocable {
		public Node invoke(FunInstructionExecutor executor, List<Node> inputs) {
			Data<?> data = (Data<?>) executor.getUnwrapper().apply(inputs.get(0));
			int p = ((Int) executor.getUnwrapper().apply(inputs.get(1))).getNumber();
			int c = ((IndexedReader) data.getData()).read(p);
			return Int.create(c);
		}
	}

	public static class GetType extends Invocable {
		public Node invoke(FunInstructionExecutor executor, List<Node> inputs) {
			Node node = executor.getUnwrapper().apply(inputs.get(0));
			Atom type;

			if (node instanceof Atom)
				type = ATOM;
			else if (node instanceof Int)
				type = NUMBER;
			else if (node instanceof Str)
				type = STRING;
			else if (node instanceof Tree)
				type = TREE;
			else
				type = UNKNOWN;

			return type;
		}
	}

	public static class Id extends Invocable {
		public Node invoke(FunInstructionExecutor executor, List<Node> inputs) {
			return inputs.get(0);
		}
	}

	public static class Log1 extends Invocable {
		public Node invoke(FunInstructionExecutor executor, List<Node> inputs) {
			Fun<Node, Node> unwrapper = executor.getUnwrapper();
			Node node = inputs.get(0);
			LogUtil.info(Formatter.display(ExpandUtil.expand(unwrapper, unwrapper.apply(node))));
			return node;
		}
	}

	public static class Log2 extends Invocable {
		public Node invoke(FunInstructionExecutor executor, List<Node> inputs) {
			Fun<Node, Node> unwrapper = executor.getUnwrapper();
			LogUtil.info(ExpandUtil.expandString(unwrapper, inputs.get(0)));
			return unwrapper.apply(inputs.get(1));
		}
	}

	public static class Popen extends Invocable {
		public Node invoke(FunInstructionExecutor executor, List<Node> inputs) {
			final Fun<Node, Node> unwrapper = executor.getUnwrapper();
			Node cmd = inputs.get(0);
			final Node in = inputs.get(1);

			try {
				final Process process = Runtime.getRuntime().exec(ExpandUtil.expandString(unwrapper, cmd));
				InputStreamReader isr = new InputStreamReader(process.getInputStream(), FileUtil.charset);
				Node result = new Data<IndexedReader>(new IndexedReader(isr));

				// Use a separate thread to write to the process, so that read
				// and write occur at the same time and would not block up.
				// The input stream is also closed by this thread.
				// Have to make sure the executors are thread-safe!
				new Thread() {
					public void run() {
						try (InputStream pes = process.getErrorStream();
								OutputStream pos = process.getOutputStream();
								Writer writer = new OutputStreamWriter(pos)) {
							ExpandUtil.expandToWriter(unwrapper, in, writer);
							process.waitFor();
						} catch (Exception ex) {
							LogUtil.error(ex);
						}
					}
				}.start();

				return result;
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}

		}
	}

	public static class StringLength extends Invocable {
		public Node invoke(FunInstructionExecutor executor, List<Node> inputs) {
			return Int.create(ExpandUtil.expandString(executor.getUnwrapper(), inputs.get(0)).length());
		}
	}

}
