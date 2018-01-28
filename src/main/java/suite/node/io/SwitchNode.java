package suite.node.io;

import java.io.IOException;

import suite.BindArrayUtil.Pattern;
import suite.Suite;
import suite.adt.pair.Fixie_.FixieFun3;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.primitive.IoSink;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

public class SwitchNode<R> {

	private Node in;
	private R result;

	public SwitchNode(Node in) {
		this.in = in;
	}

	public <T extends Node> SwitchNode<R> applyIf(Class<T> c, Fun<T, R> fun) {
		if (result == null && c.isInstance(in))
			result = fun.apply(c.cast(in));
		return this;
	}

	public <T extends Node> SwitchNode<R> applyTree(FixieFun3<Operator, Node, Node, R> fun) {
		Tree tree = Tree.decompose(in);
		if (result == null && tree != null)
			result = fun.apply(tree.getOperator(), tree.getLeft(), tree.getRight());
		return this;
	}

	public <T extends Node> SwitchNode<R> doIf(Class<T> c, IoSink<T> fun) {
		return applyIf(c, t -> {
			try {
				fun.sink(t);
			} catch (IOException ex) {
				Fail.t(ex);
			}
			@SuppressWarnings("unchecked")
			R r = (R) t;
			return r;
		});
	}

	public SwitchNode<R> match(Atom node, Fun<Atom, R> fun) {
		if (result == null && in == node)
			result = fun.apply(node);
		return this;
	}

	public SwitchNode<R> match(Pattern pattern, Fun<Node[], R> fun) {
		Node[] m;
		if (result == null && (m = pattern.match(in)) != null)
			result = fun.apply(m);
		return this;
	}

	public SwitchNode<R> match(String pattern, Fun<Node[], R> fun) {
		return match(Suite.pattern(pattern), fun);
	}

	public R nonNullResult() {
		return result != null ? result : Fail.t("cannot handle " + in);
	}

	public R result() {
		return result;
	}

}
