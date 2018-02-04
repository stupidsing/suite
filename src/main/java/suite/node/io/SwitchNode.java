package suite.node.io;

import java.io.IOException;

import suite.BindArrayUtil.Pattern;
import suite.Suite;
import suite.adt.pair.Fixie_.FixieFun1;
import suite.adt.pair.Fixie_.FixieFun2;
import suite.adt.pair.Fixie_.FixieFun3;
import suite.adt.pair.Fixie_.FixieFun4;
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
		return match_(pattern, fun);
	}

	public SwitchNode<R> match(String pattern, Fun<Node[], R> fun) {
		return match_(Suite.pattern(pattern), fun);
	}

	public SwitchNode<R> match1(Pattern pattern, FixieFun1<Node, R> fun) {
		return match1_(pattern, fun);
	}

	public SwitchNode<R> match1(String pattern, FixieFun1<Node, R> fun) {
		return match1_(Suite.pattern(pattern), fun);
	}

	public SwitchNode<R> match2(Pattern pattern, FixieFun2<Node, Node, R> fun) {
		return match2_(pattern, fun);
	}

	public SwitchNode<R> match2(String pattern, FixieFun2<Node, Node, R> fun) {
		return match2_(Suite.pattern(pattern), fun);
	}

	public SwitchNode<R> match3(String pattern, FixieFun3<Node, Node, Node, R> fun) {
		return match3_(Suite.pattern(pattern), fun);
	}

	public SwitchNode<R> match4(String pattern, FixieFun4<Node, Node, Node, Node, R> fun) {
		return match4_(Suite.pattern(pattern), fun);
	}

	public R nonNullResult() {
		return result != null ? result : Fail.t("cannot handle " + in);
	}

	public R result() {
		return result;
	}

	private SwitchNode<R> match1_(Pattern pattern, FixieFun1<Node, R> fun) {
		return match_(pattern, m -> fun.apply(m[0]));
	}

	private SwitchNode<R> match2_(Pattern pattern, FixieFun2<Node, Node, R> fun) {
		return match_(pattern, m -> fun.apply(m[0], m[1]));
	}

	private SwitchNode<R> match3_(Pattern pattern, FixieFun3<Node, Node, Node, R> fun) {
		return match_(pattern, m -> fun.apply(m[0], m[1], m[2]));
	}

	private SwitchNode<R> match4_(Pattern pattern, FixieFun4<Node, Node, Node, Node, R> fun) {
		return match_(pattern, m -> fun.apply(m[0], m[1], m[2], m[3]));
	}

	private SwitchNode<R> match_(Pattern pattern, Fun<Node[], R> fun) {
		Node[] m;
		if (result == null && (m = pattern.match(in)) != null)
			result = fun.apply(m);
		return this;
	}

}
