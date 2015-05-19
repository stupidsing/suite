package suite.node.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import suite.adt.IdentityKey;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;

public class Persister {

	public static class Loader {
		private List<Node> nodes = new ArrayList<>();

		public Node load(InputStream is) throws IOException {
			try (GZIPInputStream gis = new GZIPInputStream(is); DataInputStream dis = new DataInputStream(gis)) {
				while (true) {
					char type = dis.readChar();
					Node node;

					switch (type) {
					case 'a':
						node = Atom.of(dis.readUTF());
						break;
					case 'f':
						return nodes.get(dis.readInt());
					case 'i':
						node = Int.of(dis.readInt());
						break;
					case 'r':
						node = new Reference();
						break;
					case 's':
						node = new Str(dis.readUTF());
						break;
					case 't':
						TermOp oper = TermOp.find(dis.readUTF());
						Node node0 = nodes.get(dis.readInt());
						Node node1 = nodes.get(dis.readInt());
						node = Tree.of(oper, node0, node1);
						break;
					default:
						throw new RuntimeException("Unknown type " + type);
					}

					nodes.add(node);
				}
			}
		}
	}

	public static class Saver {
		private int counter;
		private Map<IdentityKey<Node>, Integer> nodes = new HashMap<>();

		public void save(OutputStream os, Node node) throws IOException {
			try (GZIPOutputStream gos = new GZIPOutputStream(os); DataOutputStream dos = new DataOutputStream(gos)) {
				int key = save0(dos, node);
				dos.writeChar('f');
				dos.writeInt(key);
			}
		}

		private int save0(DataOutputStream dos, Node node) throws IOException {
			node = node.finalNode();
			Tree tree;
			Integer id;

			if ((id = nodes.get(IdentityKey.of(node))) == null) {
				if (node instanceof Atom) {
					dos.writeChar('a');
					dos.writeUTF(((Atom) node).name);
				} else if (node instanceof Int) {
					dos.writeChar('i');
					dos.writeInt(((Int) node).number);
				} else if (node instanceof Reference)
					dos.writeChar('r');
				else if (node instanceof Str) {
					dos.writeChar('s');
					dos.writeUTF(((Str) node).value);
				} else if ((tree = Tree.decompose(node)) != null) {
					int key0 = save0(dos, tree.getLeft());
					int key1 = save0(dos, tree.getRight());
					dos.writeChar('t');
					dos.writeUTF(tree.getOperator().getName());
					dos.writeInt(key0);
					dos.writeInt(key1);
				} else
					throw new RuntimeException("Cannot persist " + node);

				id = counter++;
				nodes.put(IdentityKey.of(node), id);
			}

			return id;
		}
	}

}
