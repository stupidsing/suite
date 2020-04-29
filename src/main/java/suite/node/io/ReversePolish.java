package suite.node.io;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

import primal.Verbs.Build;
import primal.Verbs.Equals;
import primal.adt.Pair;
import suite.Suite;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.Rewrite_.NodeRead;
import suite.node.io.Rewrite_.NodeWrite;
import suite.node.io.Rewrite_.ReadType;

public class ReversePolish {

	public Node fromRpn(String s) {
		return ex(() -> fromRpn(new StringReader(s)));
	}

	public Node fromRpn(Reader reader) throws IOException {
		var br = new BufferedReader(reader);
		var references = new HashMap<String, Reference>();
		var deque = new ArrayDeque<Node>();

		br.lines().filter(elem -> !elem.isEmpty()).forEach(elem -> {
			var type = elem.charAt(0);
			var s = elem.substring(1);
			Node n;

			if (type == '\\')
				n = Atom.of(s);
			else if (type == '^') {
				var a = s.split(":");
				var size = Integer.valueOf(a[3]);
				var children = new ArrayList<Pair<Node, Node>>();
				for (var i = 0; i < size; i++) {
					var key = deque.pop();
					var value = deque.pop();
					children.add(Pair.of(key, value));
				}
				n = new NodeWrite(
						ReadType.valueOf(a[0]),
						!Equals.string(a[1], "null") ? Suite.parse(a[1]) : null,
						TermOp.valueOf(a[2]),
						children).node;
				// n = Suite.parse(s);
			} else if (type == 'i')
				n = Int.of(Integer.parseInt(s));
			else if (type == 'r')
				n = references.computeIfAbsent(s, key -> new Reference());
			else if (type == 't') {
				var op = TermOp.valueOf(s);
				var left = deque.pop();
				var right = deque.pop();
				n = Tree.of(op, left, right);
			} else
				n = fail("RPN conversion error: " + elem);

			deque.push(n);
		});

		return deque.pop();
	}

	public String toRpn(Node node) {
		var deque = new ArrayDeque<Node>();
		deque.push(node);

		var list = new ArrayList<String>();

		while (!deque.isEmpty()) {
			var s = new SwitchNode<String>(deque.pop()
			).applyIf(Atom.class, n_ -> {
				return "\\" + n_.name;
			}).applyIf(Int.class, n_ -> {
				return "i" + n_.number;
			}).applyIf(Reference.class, n_ -> {
				return "r" + n_.getId();
			}).applyTree((op, l, r) -> {
				deque.push(r);
				deque.push(l);
				return "t" + op;
			}).applyIf(Node.class, n_ -> {
				var nr = NodeRead.of(n_);
				for (var pair : nr.children) {
					deque.push(pair.v);
					deque.push(pair.k);
				}
				return "^" + nr.type + ":" + nr.terminal + ":" + nr.op + ":" + nr.children.size();
				// "^" + Formatter.dump(n);
			}).nonNullResult();

			list.add(s);
		}

		return Build.string(sb -> {
			for (var i = list.size() - 1; 0 <= i; i--)
				sb.append(list.get(i) + '\n');
		});
	}

}
