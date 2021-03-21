package suite.node.io;

import static primal.statics.Fail.fail;

import java.util.ArrayList;
import java.util.List;

import primal.MoreVerbs.Read;
import primal.adt.Pair;
import primal.fp.Funs.Iterate;
import primal.parser.Operator;
import primal.streamlet.Streamlet2;
import suite.node.Atom;
import suite.node.Dict;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.util.Comparer;

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
		public final Streamlet2<Node, Node> children;

		public static NodeRead of(Node node) {
			Tree tree;

			if (node instanceof Dict dict) {
				var map = dict.getMap();
				return new NodeRead(ReadType.DICT, null, null, Read //
						.from2(map) //
						.sort((p0, p1) -> Comparer.comparer.compare(p0.k, p1.k)) //
						.mapValue(Node::finalNode) //
						.collect());
			} else if ((tree = Tree.decompose(node)) != null) {
				var p0 = Pair.of(LEFT_, tree.getLeft());
				var p1 = Pair.of(RIGHT, tree.getRight());
				return new NodeRead(ReadType.TREE, null, tree.getOperator(), Read.each2(p0, p1));
			} else if (node instanceof Tuple tuple) {
				var nodes = tuple.nodes;
				return new NodeRead(ReadType.TUPLE, null, null, Read //
						.from(nodes) //
						.<Node, Node> map2(n -> Atom.NIL, Node::finalNode) //
						.collect());
			} else
				return new NodeRead(ReadType.TERM, node, null, Read.empty2());
		}

		private NodeRead(ReadType type, Node terminal, Operator op, Streamlet2<Node, Node> children) {
			super(type, terminal, op);
			this.children = children;
		}
	}

	public static class NodeWrite {
		public final Node node;

		public NodeWrite(ReadType type, Node terminal, Operator op, List<Pair<Node, Node>> children) {
			node = switch (type) {
			case DICT -> Dict.of(Read.from2(children).mapValue(Reference::of).toMap());
			case TERM -> terminal;
			case TREE -> Tree.of(op, children.get(0).v, children.get(1).v);
			case TUPLE -> Tuple.of(Read.from(children).map(p -> p.v).toArray(Node.class));
			default -> fail();
			};
		}
	}

	public static Node map(Node node, Iterate<Node> fun) {
		var nr = NodeRead.of(node);
		var children1 = new ArrayList<Pair<Node, Node>>();
		var isSame = true;

		for (var pair : nr.children) {
			var child0 = pair.v;
			var childx = fun.apply(child0);
			if (child0 != childx) {
				isSame = false;
				children1.add(Pair.of(pair.k, childx));
			} else
				children1.add(pair);
		}

		return isSame ? node : new NodeWrite(nr.type, nr.terminal, nr.op, children1).node;
	}

}
