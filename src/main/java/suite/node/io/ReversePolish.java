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
import suite.adt.pair.Pair;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.Rewrite_.NodeRead;
import suite.node.io.Rewrite_.NodeWrite;
import suite.node.io.Rewrite_.ReadType;
import suite.util.Fail;
import suite.util.Rethrow;
import suite.util.String_;

public class ReversePolish {

	public Node fromRpn(String s) {
		return Rethrow.ex(() -> fromRpn(new StringReader(s)));
	}

	public Node fromRpn(Reader reader) throws IOException {
		BufferedReader br = new BufferedReader(reader);
		Map<String, Reference> references = new HashMap<>();
		Deque<Node> deque = new ArrayDeque<>();

		br.lines().filter(elem -> !elem.isEmpty()).forEach(elem -> {
			var type = elem.charAt(0);
			var s = elem.substring(1);
			Node n;

			if (type == '\\')
				n = Atom.of(s);
			else if (type == '^') {
				String[] a = s.split(":");
				var size = Integer.valueOf(a[3]);
				List<Pair<Node, Node>> children = new ArrayList<>();
				for (int i = 0; i < size; i++) {
					Node key = deque.pop();
					Node value = deque.pop();
					children.add(Pair.of(key, value));
				}
				n = new NodeWrite( //
						ReadType.valueOf(a[0]), //
						!String_.equals(a[1], "null") ? Suite.parse(a[1]) : null, //
						TermOp.valueOf(a[2]), //
						children).node;
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
				n = Fail.t("RPN conversion error: " + elem);

			deque.push(n);
		});

		return deque.pop();
	}

	public String toRpn(Node node) {
		Deque<Node> deque = new ArrayDeque<>();
		deque.push(node);

		List<String> list = new ArrayList<>();

		while (!deque.isEmpty()) {
			var s = new SwitchNode<String>(deque.pop() //
			).applyIf(Atom.class, n_ -> {
				return "\\" + n_.name;
			}).applyIf(Int.class, n_ -> {
				return "i" + n_.number;
			}).applyIf(Reference.class, n_ -> {
				return "r" + n_.getId();
			}).applyIf(Tree.class, tree -> {
				deque.push(tree.getRight());
				deque.push(tree.getLeft());
				return "t" + tree.getOperator();
			}).applyIf(Node.class, n_ -> {
				NodeRead nr = NodeRead.of(n_);
				for (Pair<Node, Node> pair : nr.children) {
					deque.push(pair.t1);
					deque.push(pair.t0);
				}
				return "^" + nr.type + ":" + nr.terminal + ":" + nr.op + ":" + nr.children.size();
				// "^" + Formatter.dump(n);
			}).nonNullResult();

			list.add(s);
		}

		StringBuilder sb = new StringBuilder();

		for (int i = list.size() - 1; 0 <= i; i--)
			sb.append(list.get(i) + '\n');

		return sb.toString();
	}

}
