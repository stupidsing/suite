package suite.node.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.adt.IdentityKey;
import suite.adt.Pair;
import suite.lp.doer.ProverConstant;
import suite.node.Atom;
import suite.node.Dict;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.Rewriter.NodeHead;
import suite.node.io.Rewriter.NodeRead;
import suite.node.io.Rewriter.ReadType;
import suite.streamlet.As;
import suite.streamlet.Read;

/**
 * Converts a node into graph representation. The nodes link to other nodes via
 * an integer key.
 */
public class Grapher {

	private List<NodeGraph> ngs = new ArrayList<>();
	private int id;

	private static class IntPair {
		private int t0;
		private int t1;

		private static IntPair of(int t0, int t1) {
			IntPair pair = new IntPair();
			pair.t0 = t0;
			pair.t1 = t1;
			return pair;
		}
	}

	private class NodeGraph extends NodeHead {
		private List<IntPair> children;

		private NodeGraph(Node terminal) {
			this(ReadType.TERM, terminal, null, Collections.emptyList());
		}

		private NodeGraph(ReadType type, Node terminal, Operator op, List<IntPair> children) {
			super(type, terminal, op);
			this.children = children;
		}
	}

	public void graph(Node node) {
		id = graph0(new HashMap<>(), node);
	}

	private int graph0(Map<IdentityKey<Node>, Integer> ids, Node node) {
		IdentityKey<Node> key = IdentityKey.of(node.finalNode());
		Integer id = ids.get(key);

		if (id == null) {
			ids.put(key, id = ngs.size());
			ngs.add(null);

			NodeRead nr = NodeRead.of(node);

			List<IntPair> childrenx = Read.from(nr.children) //
					.map(p -> IntPair.of(graph0(ids, p.t0), graph0(ids, p.t1))) //
					.toList();

			ngs.set(id, new NodeGraph(nr.type, nr.terminal, nr.op, childrenx));
		}

		return id;
	}

	public Node ungraph() {
		int size = ngs.size();

		List<Node> nodes = Read.from(ngs).map(ng -> {
			switch (ng.type) {
			case DICT:
				return new Dict();
			case TERM:
				return ng.terminal;
			case TREE:
				return Tree.of(ng.op, null, null);
			case TUPLE:
				return new Tuple(new ArrayList<>(ng.children.size()));
			default:
				throw new RuntimeException();
			}
		}).toList();

		for (int i = 0; i < size; i++) {
			NodeGraph ng = ngs.get(i);
			Node node = nodes.get(i);
			List<Pair<Node, Node>> children = Read.from(ng.children).map(p -> Pair.of(nodes.get(p.t0), nodes.get(p.t1))).toList();

			switch (ng.type) {
			case DICT:
				((Dict) node).map.putAll(Read.from(children).toMap(p -> p.t0, p -> Reference.of(p.t1)));
				break;
			case TERM:
				break;
			case TREE:
				Tree tree = (Tree) node;
				Tree.forceSetLeft(tree, children.get(0).t1);
				Tree.forceSetRight(tree, children.get(1).t1);
				break;
			case TUPLE:
				List<Node> list = ((Tuple) node).nodes;
				list.addAll(Read.from(children).map(p -> p.t1).toList());
			}
		}

		return nodes.get(id);
	}

	public void generalize() {
		ngs = Read.from(ngs) //
				.map(ng -> {
					NodeGraph ng1;
					if (ng.type == ReadType.TERM) {
						Node node = ng.terminal.finalNode();
						if (node instanceof Atom && ((Atom) node).name.startsWith(ProverConstant.variablePrefix))
							ng1 = new NodeGraph(new Reference());
						else
							ng1 = ng;
					} else
						ng1 = ng;
					return ng1;
				}) //
				.toList();
	}

	public void specialize() {
		ngs = Read.from(ngs) //
				.map(ng -> {
					NodeGraph ng1;
					if (ng.type == ReadType.TERM) {
						Node node = ng.terminal.finalNode();
						if (node instanceof Reference)
							ng1 = new NodeGraph(Atom.of(ProverConstant.variablePrefix + ((Reference) node).getId()));
						else
							ng1 = ng;
					} else
						ng1 = ng;
					return ng1;
				}) //
				.toList();
	}

	public void load(DataInputStream dis) throws IOException {
		int size = dis.readInt();
		id = dis.readInt();

		for (int index = 0; index < size; index++) {
			ReadType type = ReadType.of(dis.readByte());
			Node terminal;
			Operator op;
			List<IntPair> children = new ArrayList<>();

			if (type == ReadType.TERM) {
				char ch = (char) dis.readByte();

				switch (ch) {
				case 'a':
					terminal = Atom.of(dis.readUTF());
					break;
				case 'i':
					terminal = Int.of(dis.readInt());
					break;
				case 'r':
					terminal = new Reference();
					break;
				case 's':
					terminal = new Str(dis.readUTF());
					break;
				default:
					throw new RuntimeException("Unknown type " + ch);
				}
			} else
				terminal = null;

			if (type == ReadType.TREE) {
				op = TermOp.find(dis.readUTF());
				children.add(IntPair.of(0, dis.readInt() + index));
				children.add(IntPair.of(0, dis.readInt() + index));
			} else
				op = null;

			if (type == ReadType.DICT || type == ReadType.TUPLE) {
				int size1 = dis.readInt();
				for (int i = 0; i < size1; i++) {
					int i0 = type != ReadType.DICT ? 0 : dis.readInt() + index;
					int i1 = dis.readInt() + index;
					children.add(IntPair.of(i0, i1));
				}
			}

			ngs.add(new NodeGraph(type, terminal, op, children));
		}
	}

	public void save(DataOutputStream dos) throws IOException {
		int size = ngs.size();
		dos.writeInt(size);
		dos.writeInt(id);

		for (int index = 0; index < size; index++) {
			NodeGraph ng = ngs.get(index);
			ReadType type = ng.type;
			List<IntPair> children = ng.children;

			dos.writeByte(type.value);

			if (type == ReadType.TERM) {
				Node terminal = ng.terminal.finalNode();

				if (terminal instanceof Atom) {
					dos.writeByte((byte) 'a');
					dos.writeUTF(((Atom) terminal).name);
				} else if (terminal instanceof Int) {
					dos.writeByte((byte) 'i');
					dos.writeInt(((Int) terminal).number);
				} else if (terminal instanceof Reference)
					dos.writeByte((byte) 'r');
				else if (terminal instanceof Str) {
					dos.writeByte((byte) 's');
					dos.writeUTF(((Str) terminal).value);
				} else
					throw new RuntimeException("Cannot persist " + terminal);
			}

			if (type == ReadType.TREE) {
				dos.writeUTF(ng.op.getName());
				dos.writeInt(children.get(0).t1 - index);
				dos.writeInt(children.get(1).t1 - index);
			}

			if (type == ReadType.DICT || type == ReadType.TUPLE) {
				dos.writeInt(children.size());

				for (IntPair child : children) {
					if (type == ReadType.DICT)
						dos.writeInt(child.t0 - index);
					dos.writeInt(child.t1 - index);
				}
			}
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();

		for (NodeGraph ng : ngs) {
			String s;
			switch (ng.type) {
			case DICT:
				s = Read.from(ng.children) //
						.map(p -> p.t0 + ":" + p.t1 + ", ") //
						.collect(As.joined("dict(", ", ", ")"));
				break;
			case TERM:
				s = Formatter.dump(ng.terminal);
				break;
			case TREE:
				s = "tree(" + ng.children.get(0).t1 + ng.op.getName() + ng.children.get(1).t1 + ")";
				break;
			case TUPLE:
				s = Read.from(ng.children) //
						.map(p -> p.t1 + ", ") //
						.collect(As.joined("tuple(", ", ", ")"));
				break;
			default:
				throw new RuntimeException();
			}
			sb.append(s + "\n");
		}

		sb.append("return(" + id + ")\n");
		return sb.toString();
	}

}
