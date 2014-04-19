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
import suite.util.FunUtil.Fun;
import suite.util.LogUtil;
import suite.util.Util;

public class Intrinsics {

	private static final Atom ATOM = Atom.create("ATOM");
	private static final Atom NUMBER = Atom.create("NUMBER");
	private static final Atom STRING = Atom.create("STRING");
	private static final Atom TREE = Atom.create("TREE");
	private static final Atom UNKNOWN = Atom.create("UNKNOWN");

	public static class AtomString implements Intrinsic {
		public Node invoke(IntrinsicBridge bridge, List<Node> inputs) {
			String name = ((Atom) inputs.get(0)).getName();

			if (!name.isEmpty()) {
				Node left = bridge.wrapIntrinsic(new Id(), Int.create(name.charAt(0)));
				Node right = bridge.wrapIntrinsic(this, Atom.create(name.substring(1)));
				return Tree.create(TermOp.OR____, left, right);
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
			Fun<Node, Node> unwrapper = bridge.getUnwrapper();
			Node node = inputs.get(0);
			LogUtil.info(Formatter.display(ExpandUtil.expandFully(unwrapper, node)));
			return node;
		}
	}

	public static class Log2 implements Intrinsic {
		public Node invoke(IntrinsicBridge bridge, List<Node> inputs) {
			Fun<Node, Node> unwrapper = bridge.getUnwrapper();
			LogUtil.info(ExpandUtil.expandString(unwrapper, inputs.get(0)));
			return inputs.get(1);
		}
	}

	public static class Source_ implements Intrinsic {
		private static class Source0 implements Intrinsic {
			public Node invoke(IntrinsicBridge bridge, List<Node> inputs) {
				IndexedReaderPointer intern = Data.get(inputs.get(0));
				int ch = intern.head();

				if (ch != -1) {
					Node left = bridge.wrapIntrinsic(new Id(), Int.create(ch));
					Node right = bridge.wrapIntrinsic(this, new Data<>(intern.tail()));
					return Tree.create(TermOp.OR____, left, right);
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
			String message = ExpandUtil.expandString(bridge.getUnwrapper(), inputs.get(0));
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
