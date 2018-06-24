package suite.node.io;

import java.util.ArrayList;
import java.util.List;

import suite.adt.pair.Pair;
import suite.node.Atom;
import suite.node.Dict;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.util.Comparer;
import suite.streamlet.Read;
import suite.util.Fail;
import suite.util.FunUtil.Iterate;

public class Rewrite_ {

	private static Node LEFT_ = Atom.of("l");
	private static Node RIGHT = Atom.of("r");

	public enum ReadType {
		DICT(0), TERM(1), TREE(2), TUPLE(3),;

		public byte value;

		public static ReadType of(byte value) {
			return Read.from(ReadType.values()).filter(rt -> rt.value == value).uniqueResult();
		}

		private ReadType(int value) {
			this.value = (byte) value;
		}
	};

	public static class NodeHead {
		public final ReadType type;
		public final Node terminal;
		public final Operator op;

		public NodeHead(ReadType type, Node terminal, Operator op) {
			this.type = type;
			this.terminal = terminal;
			this.op = op;
		}
	}

	public static class NodeRead extends NodeHead {
		public final List<Pair<Node, Node>> children;

		public static NodeRead of(Node node) {
			Tree tree;

			if (node instanceof Dict) {
				var map = Dict.m(node);
				return new NodeRead(ReadType.DICT, null, null, Read //
						.from2(map) //
						.sort((p0, p1) -> Comparer.comparer.compare(p0.t0, p1.t0)) //
						.mapValue(Node::finalNode) //
						.toList());
			} else if ((tree = Tree.decompose(node)) != null) {
				var p0 = Pair.of(LEFT_, tree.getLeft());
				var p1 = Pair.of(RIGHT, tree.getRight());
				return new NodeRead(ReadType.TREE, null, tree.getOperator(), List.of(p0, p1));
			} else if (node instanceof Tuple) {
				var nodes = Tuple.t(node);
				return new NodeRead(ReadType.TUPLE, null, null, Read //
						.from(nodes) //
						.map(n -> Pair.<Node, Node> of(Atom.NIL, n.finalNode())) //
						.toList());
			} else
				return new NodeRead(ReadType.TERM, node, null, List.of());
		}

		private NodeRead(ReadType type, Node terminal, Operator op, List<Pair<Node, Node>> children) {
			super(type, terminal, op);
			this.children = children;
		}
	}

	public static class NodeWrite {
		public final Node node;

		public NodeWrite(ReadType type, Node terminal, Operator op, List<Pair<Node, Node>> children) {
			switch (type) {
			case DICT:
				node = Dict.of(Read.from2(children).mapValue(Reference::of).toMap());
				break;
			case TERM:
				node = terminal;
				break;
			case TREE:
				node = Tree.of(op, children.get(0).t1, children.get(1).t1);
				break;
			case TUPLE:
				node = Tuple.of(Read.from(children).map(p -> p.t1).toArray(Node.class));
				break;
			default:
				node = Fail.t();
			}
		}
	}

	public static Node map(Node node, Iterate<Node> fun) {
		var nr = NodeRead.of(node);
		var children1 = new ArrayList<Pair<Node, Node>>();
		var isSame = true;

		for (var pair : nr.children) {
			var child0 = pair.t1;
			var childx = fun.apply(child0);
			if (child0 != childx) {
				isSame = false;
				children1.add(Pair.of(pair.t0, childx));
			} else
				children1.add(pair);
		}

		return isSame ? node : new NodeWrite(nr.type, nr.terminal, nr.op, children1).node;
	}

}
