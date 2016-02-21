package suite.node.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.Suite;
import suite.adt.Pair;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.Rewriter.NodeRead;
import suite.node.io.Rewriter.NodeWrite;
import suite.node.io.Rewriter.ReadType;
import suite.util.Rethrow;
import suite.util.Util;

public class ReversePolish {

	public Node fromRpn(String s) {
		return Rethrow.ioException(() -> fromRpn(new StringReader(s)));
	}

	public Node fromRpn(Reader reader) throws IOException {
		BufferedReader br = new BufferedReader(reader);
		Map<String, Reference> references = new HashMap<>();
		Deque<Node> deque = new ArrayDeque<>();

		br.lines().filter(elem -> !elem.isEmpty()).forEach(elem -> {
			char type = elem.charAt(0);
			String s = elem.substring(1);
			Node n;

			if (type == '\\')
				n = Atom.of(s);
			else if (type == '^') {
				String a[] = s.split(":");
				int size = Integer.valueOf(a[3]);
				List<Pair<Node, Node>> children = new ArrayList<>();
				for (int i = 0; i < size; i++) {
					Node key = deque.pop();
					Node value = deque.pop();
					children.add(Pair.of(key, value));
				}
				n = new NodeWrite(ReadType.valueOf(a[0]), //
						!Util.stringEquals(a[1], "null") ? Suite.parse(a[1]) : null, //
						TermOp.valueOf(a[2]), //
						children) //
						.node;
				// n = Suite.parse(s);
			} else if (type == 'i')
				n = Int.of(Integer.parseInt(s));
			else if (type == 'r')
				n = references.computeIfAbsent(s, key -> new Reference());
			else if (type == 't') {
				TermOp op = TermOp.valueOf(s);
				Node left = deque.pop();
				Node right = deque.pop();
				n = Tree.of(op, left, right);
			} else
				throw new RuntimeException("RPN conversion error: " + elem);

			deque.push(n);
		});

		return deque.pop();
	}

	public String toRpn(Node node) {
		Deque<Node> deque = new ArrayDeque<>();
		deque.push(node);

		List<String> list = new ArrayList<>();

		while (!deque.isEmpty()) {
			Node n = deque.pop();
			String s;

			if (n instanceof Atom)
				s = "\\" + ((Atom) n).name;
			else if (n instanceof Int)
				s = "i" + ((Int) n).number;
			else if (n instanceof Reference)
				s = "r" + ((Reference) n).getId();
			else if (n instanceof Tree) {
				Tree tree = (Tree) n;
				s = "t" + tree.getOperator();
				deque.push(tree.getRight());
				deque.push(tree.getLeft());
			} else {
				NodeRead nr = NodeRead.of(n);
				for (Pair<Node, Node> pair : nr.children) {
					deque.push(pair.t1);
					deque.push(pair.t0);
				}
				s = "^" + nr.type + ":" + nr.terminal + ":" + nr.op + ":" + nr.children.size();
				// s = "^" + Formatter.dump(n);
			}

			list.add(s);
		}

		StringBuilder sb = new StringBuilder();

		for (int i = list.size() - 1; 0 <= i; i--)
			sb.append(list.get(i) + '\n');

		return sb.toString();
	}

}
