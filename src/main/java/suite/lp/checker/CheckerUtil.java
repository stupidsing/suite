package suite.lp.checker;

import java.util.Arrays;
import java.util.List;

import suite.Suite;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;

public class CheckerUtil {

	private List<Fun<Node, Node[]>> matchers = Arrays.asList( //
			Suite.matcher(".0, .1"), //
			Suite.matcher(".0; .1"), //
			Suite.matcher("find.all _ .0 _"), //
			Suite.matcher("if .0 then .1 else .2"), //
			Suite.matcher("not .0"), //
			Suite.matcher("once .0"), //
			Suite.matcher("try .0 _ .1"));

	public Streamlet<Node> scan(Node node) {
		Node m[] = null;

		for (Fun<Node, Node[]> matcher : matchers)
			if ((m = matcher.apply(node)) != null)
				return Read.from(m).concatMap(this::scan);

		if (Tree.decompose(node, TermOp.TUPLE_) != null || node instanceof Atom)
			return Read.from(node);
		else
			return Read.empty();
	}

	public int getNumberOfParameters(Node node) {
		int n = 0;
		Tree tree;
		while ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null) {
			node = tree.getRight();
			n++;
		}
		return n;
	}

}
