package suite.lp.doer;

import java.util.ArrayDeque;
import java.util.Deque;

import suite.lp.Suite;
import suite.lp.doer.TermParser.TermOp;
import suite.lp.node.Atom;
import suite.lp.node.Int;
import suite.lp.node.Node;
import suite.lp.node.Reference;
import suite.lp.node.Tree;

public class ReversePolish {

	public Node fromRpn(String rpn) {
		String elems[] = rpn.split("\n");
		Deque<Node> deque = new ArrayDeque<>();
		int index = elems.length;

		while (index > 0) {
			String elem = elems[--index];
			if (elem.isEmpty())
				continue;

			char type = elem.charAt(0);
			String s = elem.substring(1);
			Node n;

			if (type == '\\')
				n = Atom.create(s);
			else if (type == '^')
				n = Suite.parse(s);
			else if (type == 'i')
				n = Int.create(Integer.valueOf(s));
			else if (type == 't') {
				TermOp op = TermOp.valueOf(s);
				Node left = deque.pop();
				Node right = deque.pop();
				n = Tree.create(op, left, right);
			} else
				throw new RuntimeException("RPN conversion error: " + elem);

			deque.push(n);
		}

		return deque.pop();
	}

	public String toRpn(Node node) {
		StringBuilder sb = new StringBuilder();
		Deque<Node> deque = new ArrayDeque<>();

		deque.push(node);

		while (!deque.isEmpty()) {
			Node n = deque.pop().finalNode();
			String s;

			if (n instanceof Atom)
				s = "\\" + ((Atom) n).getName();
			else if (n instanceof Int)
				s = "i" + ((Int) n).getNumber();
			else if (n instanceof Reference)
				s = "\\." + ((Reference) n).getId();
			else if (n instanceof Tree) {
				Tree tree = (Tree) n;
				s = "t" + tree.getOperator();
				deque.push(tree.getRight());
				deque.push(tree.getLeft());
			} else
				s = "^" + Formatter.dump(n);

			sb.append(s);
			sb.append('\n');
		}

		return sb.toString();
	}

}
