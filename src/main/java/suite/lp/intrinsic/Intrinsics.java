package suite.lp.intrinsic;

import java.util.Arrays;
import java.util.List;

import suite.instructionexecutor.ExpandUtil;
import suite.instructionexecutor.IndexedReader;
import suite.instructionexecutor.IndexedReaderPointer;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.TermOp;
import suite.util.LogUtil;
import suite.util.Util;

public class Intrinsics {

	private static Atom ATOM = Atom.of("ATOM");
	private static Atom NUMBER = Atom.of("NUMBER");
	private static Atom STRING = Atom.of("STRING");
	private static Atom TREE = Atom.of("TREE");
	private static Atom UNKNOWN = Atom.of("UNKNOWN");

	public static class AtomString implements Intrinsic {
		public Node invoke(IntrinsicBridge bridge, List<Node> inputs) {
			String name = ((Atom) inputs.get(0)).getName();

			if (!name.isEmpty()) {
				Node left = bridge.wrap(new Id(), Int.of(name.charAt(0)));
				Node right = bridge.wrap(this, Atom.of(name.substring(1)));
				return Tree.of(TermOp.OR____, left, right);
			} else
				return Atom.NIL;
		}
	}

	public static class Id implements Intrinsic {
		public Node invoke(IntrinsicBridge bridge, List<Node> inputs) {
			return inputs.get(0);
		}
	}

	public static class Log1 implements Intrinsic {
		public Node invoke(IntrinsicBridge bridge, List<Node> inputs) {
			Node node = inputs.get(0);
			LogUtil.info(Formatter.display(ExpandUtil.expandFully(bridge::unwrap, node)));
			return node;
		}
	}

	public static class Log2 implements Intrinsic {
		public Node invoke(IntrinsicBridge bridge, List<Node> inputs) {
			LogUtil.info(ExpandUtil.expandString(bridge::unwrap, inputs.get(0)));
			return inputs.get(1);
		}
	}

	public static class Source_ implements Intrinsic {
		private static class Source0 implements Intrinsic {
			public Node invoke(IntrinsicBridge bridge, List<Node> inputs) {
				IndexedReaderPointer intern = Data.get(inputs.get(0));
				int ch = intern.head();

				if (ch != -1) {
					Node left = bridge.wrap(new Id(), Int.of(ch));
					Node right = bridge.wrap(this, new Data<>(intern.tail()));
					return Tree.of(TermOp.OR____, left, right);
				} else
					return Atom.NIL;
			}
		}

		public Node invoke(IntrinsicBridge bridge, List<Node> inputs) {
			IndexedReader indexedReader = Data.get(inputs.get(0));
			Data<IndexedReaderPointer> data = new Data<>(new IndexedReaderPointer(indexedReader));
			return new Source0().invoke(bridge, Arrays.<Node> asList(data));
		}
	}

	public static class Throw implements Intrinsic {
		public Node invoke(IntrinsicBridge bridge, List<Node> inputs) {
			String message = ExpandUtil.expandString(bridge::unwrap, inputs.get(0));
			throw new RuntimeException(Util.isNotBlank(message) ? message : "Error termination");
		}
	}

	public static class TypeOf implements Intrinsic {
		public Node invoke(IntrinsicBridge bridge, List<Node> inputs) {
			Node node = inputs.get(0);
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

}
