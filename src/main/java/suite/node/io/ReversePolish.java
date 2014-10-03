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
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.Tuple;

public class ReversePolish {

	public Node fromRpn(String s) {
		try {
			return fromRpn(new StringReader(s));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public Node fromRpn(Reader reader) throws IOException {
		BufferedReader br = new BufferedReader(reader);
		Map<String, Reference> references = new HashMap<>();
		Deque<Node> deque = new ArrayDeque<>();
		String elem;

		while ((elem = br.readLine()) != null) {
			if (elem.isEmpty())
				continue;

			char type = elem.charAt(0);
			String s = elem.substring(1);
			Node n;

			if (type == '\\')
				n = Atom.of(s);
			else if (type == '^')
				n = Suite.parse(s);
			else if (type == 'i')
				n = Int.of(Integer.parseInt(s));
			else if (type == 'r')
				n = references.computeIfAbsent(s, key -> new Reference());
			else if (type == 't') {
				TermOp op = TermOp.valueOf(s);
				Node left = deque.pop();
				Node right = deque.pop();
				n = Tree.of(op, left, right);
			} else if (type == 'u') {
				int size = Integer.valueOf(s);
				List<Node> nodes = new ArrayList<>();
				for (int i = 0; i < size; i++)
					nodes.add(deque.pop());
				return new Tuple(nodes);
			} else
				throw new RuntimeException("RPN conversion error: " + elem);

			deque.push(n);
		}

		return deque.pop();
	}

	public String toRpn(Node node) {
		Deque<Node> deque = new ArrayDeque<>();
		deque.push(node);

		List<String> list = new ArrayList<>();

		while (!deque.isEmpty()) {
			Node n = deque.pop().finalNode();
			String s;

			if (n instanceof Atom)
				s = "\\" + ((Atom) n).name;
			else if (n instanceof Int)
				s = "i" + ((Int) n).number;
			else if (n instanceof Reference)
				s = "r" + ((Reference) n).getId();
			// s = "\\." + ((Reference) n).getId();
			else if (n instanceof Tree) {
				Tree tree = (Tree) n;
				s = "t" + tree.getOperator();
				deque.push(tree.getRight());
				deque.push(tree.getLeft());
			} else if (n instanceof Tuple) {
				List<Node> nodes = ((Tuple) n).nodes;
				int size = nodes.size();
				s = "u" + size;
				for (int i = size - 1; i >= 0; i++)
					deque.push(nodes.get(i));
			} else
				s = "^" + Formatter.dump(n);

			list.add(s);
		}

		StringBuilder sb = new StringBuilder();

		for (int i = list.size() - 1; i >= 0; i--)
			sb.append(list.get(i) + '\n');

		return sb.toString();
	}

}
