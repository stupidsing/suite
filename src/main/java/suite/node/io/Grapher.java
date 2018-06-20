package suite.node.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import suite.adt.IdentityKey;
import suite.adt.pair.Pair;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.lp.doer.ProverConstant;
import suite.node.Atom;
import suite.node.Dict;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.Rewrite_.NodeHead;
import suite.node.io.Rewrite_.NodeRead;
import suite.node.io.Rewrite_.ReadType;
import suite.primitive.IntFunUtil;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.map.ObjIntMap;
import suite.primitive.adt.pair.IntIntPair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.Fail;

/**
 * Converts a node into graph representation. The nodes link to other nodes via
 * an integer key.
 */
public class Grapher {

	private List<GN> gns = new ArrayList<>();
	private int id;

	private class GN extends NodeHead {
		private List<IntIntPair> children;

		private GN(Node terminal) {
			this(ReadType.TERM, terminal, null, List.of());
		}

		private GN(ReadType type, Node terminal, Operator op, List<IntIntPair> children) {
			super(type, terminal, op);
			this.children = children;
		}
	}

	public void graph(Node node) {
		id = graph_(new ObjIntMap<>(), node);
	}

	public Node ungraph() {
		return ungraph_(id);
	}

	private int graph_(ObjIntMap<IdentityKey<Node>> ids, Node node) {
		var key = IdentityKey.of(node);
		var id = ids.get(key);

		if (id == IntFunUtil.EMPTYVALUE) {
			ids.put(key, id = gns.size());
			gns.add(null);

			var nr = NodeRead.of(node);

			var children = Read //
					.from(nr.children) //
					.map(p -> IntIntPair.of(graph_(ids, p.t0), graph_(ids, p.t1))) //
					.toList();

			gns.set(id, new GN(nr.type, nr.terminal, nr.op, children));
		}

		return id;
	}

	private Node ungraph_(int id) {
		var size = gns.size();

		var nodes = Read //
				.from(gns) //
				.map(gn -> {
					switch (gn.type) {
					case DICT:
						return Dict.of();
					case TERM:
						return gn.terminal;
					case TREE:
						return Tree.of(gn.op, null, null);
					case TUPLE:
						return Tuple.of(new Node[gn.children.size()]);
					default:
						return Fail.t();
					}
				}) //
				.toList();

		for (var i = 0; i < size; i++) {
			var gn = gns.get(i);
			var node = nodes.get(i);
			var children = Read.from(gn.children).map(p -> Pair.of(nodes.get(p.t0), nodes.get(p.t1))).toList();

			switch (gn.type) {
			case DICT:
				Dict.m(node).putAll(Read.from2(children).mapValue(Reference::of).collect(As::map));
				break;
			case TERM:
				break;
			case TREE:
				var tree = (Tree) node;
				Tree.forceSetLeft(tree, children.get(0).t1);
				Tree.forceSetRight(tree, children.get(1).t1);
				break;
			case TUPLE:
				var list = Tuple.t(node);
				for (var j = 0; j < children.size(); j++)
					list[j] = children.get(j).t1;
			}
		}

		return nodes.get(id);
	}

	public static boolean bind(Node n0, Node n1, Trail trail) {
		var mapn0 = new ObjIntMap<IdentityKey<Node>>();
		var mapn1 = new ObjIntMap<IdentityKey<Node>>();
		var g0 = new Grapher();
		var g1 = new Grapher();
		g0.id = g0.graph_(mapn0, n0);
		g1.id = g1.graph_(mapn1, n1);

		var mapi0 = new IntObjMap<IdentityKey<Node>>();
		var mapi1 = new IntObjMap<IdentityKey<Node>>();
		for (var e : mapn0.streamlet())
			mapi0.put(e.t0, e.t1);
		for (var e : mapn1.streamlet())
			mapi1.put(e.t0, e.t1);

		var set = new HashSet<>();
		var deque = new ArrayDeque<IntIntPair>();
		deque.add(IntIntPair.of(g0.id, g1.id));
		IntIntPair pair;

		while ((pair = deque.pollLast()) != null)
			if (set.add(pair)) {
				var gn0 = g0.gns.get(pair.t0);
				var gn1 = g1.gns.get(pair.t1);

				if (gn0.type == ReadType.TERM //
						&& gn0.terminal instanceof Reference //
						&& Binder.bind(gn0.terminal, mapi1.get(pair.t1).key, trail))
					;
				else if (gn1.type == ReadType.TERM //
						&& gn1.terminal instanceof Reference //
						&& Binder.bind(gn1.terminal, mapi0.get(pair.t0).key, trail))
					;
				else if (gn0.type == gn1.type && Objects.equals(gn0.terminal, gn1.terminal) && gn0.op == gn1.op) {
					var children0 = gn0.children;
					var children1 = gn1.children;
					var size0 = children0.size();
					var size1 = children1.size();
					if (size0 == size1)
						for (var i = 0; i < size0; i++) {
							var p0 = children0.get(i);
							var p1 = children1.get(i);
							deque.addLast(IntIntPair.of(p0.t0, p1.t0));
							deque.addLast(IntIntPair.of(p0.t1, p1.t1));
						}
					else
						return false;
				} else
					return false;
			}

		return true;
	}

	public void generalize() {
		gns = Read //
				.from(gns) //
				.map(gn -> {
					Node node;
					GN gn1;
					if (gn.type == ReadType.TERM //
							&& (node = gn.terminal) instanceof Atom //
							&& Atom.name(node).startsWith(ProverConstant.variablePrefix))
						gn1 = new GN(new Reference());
					else
						gn1 = gn;
					return gn1;
				}) //
				.toList();
	}

	public void specialize() {
		gns = Read //
				.from(gns) //
				.map(gn -> {
					Node node;
					GN gn1;
					if (gn.type == ReadType.TERM && (node = gn.terminal) instanceof Reference)
						gn1 = new GN(Atom.of(((Reference) node).name()));
					else
						gn1 = gn;
					return gn1;
				}) //
				.toList();
	}

	public static Node replace(Node fr, Node to, Node node) {
		var ids = new ObjIntMap<IdentityKey<Node>>();

		var grapher = new Grapher();
		var n0 = grapher.graph_(ids, fr);
		var nx = grapher.graph_(ids, to);
		var id = grapher.graph_(ids, node);

		grapher.gns.set(n0, grapher.gns.get(nx));
		return grapher.ungraph_(id);
	}

	public void load(DataInputStream dis) throws IOException {
		var size = dis.readInt();
		id = dis.readInt();

		for (var index = 0; index < size; index++) {
			var type = ReadType.of(dis.readByte());
			Node terminal;
			Operator op;
			var children = new ArrayList<IntIntPair>();

			if (type == ReadType.TERM) {
				var ch = (char) dis.readByte();

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
					terminal = Fail.t("unknown type " + ch);
				}
			} else
				terminal = null;

			if (type == ReadType.TREE) {
				op = TermOp.find(dis.readUTF());
				children.add(IntIntPair.of(0, dis.readInt() + index));
				children.add(IntIntPair.of(0, dis.readInt() + index));
			} else
				op = null;

			if (type == ReadType.DICT || type == ReadType.TUPLE) {
				var size1 = dis.readInt();
				for (var i = 0; i < size1; i++) {
					var i0 = type != ReadType.DICT ? 0 : dis.readInt() + index;
					var i1 = dis.readInt() + index;
					children.add(IntIntPair.of(i0, i1));
				}
			}

			gns.add(new GN(type, terminal, op, children));
		}
	}

	public void save(DataOutputStream dos) throws IOException {
		var size = gns.size();
		dos.writeInt(size);
		dos.writeInt(id);

		for (var index = 0; index < size; index++) {
			var gn = gns.get(index);
			var type = gn.type;
			var children = gn.children;

			dos.writeByte(type.value);

			if (type == ReadType.TERM)
				new SwitchNode<Node>(gn.terminal //
				).doIf(Atom.class, n -> {
					dos.writeByte((byte) 'a');
					dos.writeUTF(n.name);
				}).doIf(Int.class, n -> {
					dos.writeByte((byte) 'i');
					dos.writeInt(n.number);
				}).doIf(Reference.class, n -> {
					dos.writeByte((byte) 'r');
				}).doIf(Str.class, n -> {
					dos.writeByte((byte) 's');
					dos.writeUTF(n.value);
				}).nonNullResult();
			else if (type == ReadType.TREE) {
				dos.writeUTF(gn.op.name_());
				dos.writeInt(children.get(0).t1 - index);
				dos.writeInt(children.get(1).t1 - index);
			} else if (type == ReadType.DICT || type == ReadType.TUPLE) {
				dos.writeInt(children.size());

				for (var child : children) {
					if (type == ReadType.DICT)
						dos.writeInt(child.t0 - index);
					dos.writeInt(child.t1 - index);
				}
			}
		}
	}

	public String toString() {
		var sb = new StringBuilder();

		for (var gn : gns) {
			String s;
			switch (gn.type) {
			case DICT:
				s = Read //
						.from(gn.children) //
						.map(p -> p.t0 + ":" + p.t1 + ", ") //
						.collect(As.joinedBy("dict(", ", ", ")"));
				break;
			case TERM:
				s = Formatter.dump(gn.terminal);
				break;
			case TREE:
				s = "tree(" + gn.children.get(0).t1 + gn.op.name_() + gn.children.get(1).t1 + ")";
				break;
			case TUPLE:
				s = Read //
						.from(gn.children) //
						.map(p -> p.t1 + ", ") //
						.collect(As.joinedBy("tuple(", ", ", ")"));
				break;
			default:
				s = Fail.t();
			}
			sb.append(s + "\n");
		}

		sb.append("return(" + id + ")\n");
		return sb.toString();
	}

}
