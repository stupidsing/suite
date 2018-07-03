package suite.node.io;

import static suite.util.Friends.rethrow;

import java.util.List;

import suite.BindArrayUtil.Pattern;
import suite.Suite;
import suite.adt.pair.FixieArray;
import suite.adt.pair.Fixie_.FixieFun0;
import suite.adt.pair.Fixie_.FixieFun1;
import suite.adt.pair.Fixie_.FixieFun2;
import suite.adt.pair.Fixie_.FixieFun3;
import suite.adt.pair.Fixie_.FixieFun4;
import suite.adt.pair.Fixie_.FixieFun5;
import suite.adt.pair.Fixie_.FixieFun6;
import suite.fp.Matcher;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.primitive.IoSink;
import suite.streamlet.Read;
import suite.util.Fail;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

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
		var tree = Tree.decompose(in);
		if (result == null && tree != null)
			result = fun.apply(tree.getOperator(), tree.getLeft(), tree.getRight());
		return this;
	}

	public <T extends Node> SwitchNode<R> doIf(Class<T> c, IoSink<T> fun) {
		return applyIf(c, t -> {
			@SuppressWarnings("unchecked")
			var r = (R) rethrow(() -> {
				fun.sink(t);
				return t;
			});
			return r;
		});
	}

	public SwitchNode<R> match(Atom node, Source<R> fun) {
		if (result == null && in == node)
			result = fun.source();
		return this;
	}

	public <T> SwitchNode<R> match(Matcher<T> matcher, FixieFun0<R> fun) {
		return result == null ? match_(matcher, m -> FixieArray.of(nodes(m)).map(fun)) : this;
	}

	public <T> SwitchNode<R> match(Matcher<T> matcher, FixieFun1<Node, R> fun) {
		return result == null ? match_(matcher, m -> FixieArray.of(nodes(m)).map(fun)) : this;
	}

	public <T> SwitchNode<R> match(Matcher<T> matcher, FixieFun2<Node, Node, R> fun) {
		return result == null ? match_(matcher, m -> FixieArray.of(nodes(m)).map(fun)) : this;
	}

	public <T> SwitchNode<R> match(Matcher<T> matcher, FixieFun3<Node, Node, Node, R> fun) {
		return result == null ? match_(matcher, m -> FixieArray.of(nodes(m)).map(fun)) : this;
	}

	public <T> SwitchNode<R> match(Matcher<T> matcher, FixieFun4<Node, Node, Node, Node, R> fun) {
		return result == null ? match_(matcher, m -> FixieArray.of(nodes(m)).map(fun)) : this;
	}

	public <T> SwitchNode<R> match(Matcher<T> matcher, FixieFun5<Node, Node, Node, Node, Node, R> fun) {
		return result == null ? match_(matcher, m -> FixieArray.of(nodes(m)).map(fun)) : this;
	}

	public <T> SwitchNode<R> match(Matcher<T> matcher, FixieFun6<Node, Node, Node, Node, Node, Node, R> fun) {
		return result == null ? match_(matcher, m -> FixieArray.of(nodes(m)).map(fun)) : this;
	}

	public SwitchNode<R> match(Pattern pattern, FixieFun0<R> fun) {
		return match0_(pattern, fun);
	}

	public SwitchNode<R> match(Pattern pattern, FixieFun1<Node, R> fun) {
		return match1_(pattern, fun);
	}

	public SwitchNode<R> match(Pattern pattern, FixieFun2<Node, Node, R> fun) {
		return match2_(pattern, fun);
	}

	public SwitchNode<R> match(String pattern, FixieFun0<R> fun) {
		return match0_(Suite.pattern(pattern), fun);
	}

	public SwitchNode<R> match(String pattern, FixieFun1<Node, R> fun) {
		return match1_(Suite.pattern(pattern), fun);
	}

	public SwitchNode<R> match(String pattern, FixieFun2<Node, Node, R> fun) {
		return match2_(Suite.pattern(pattern), fun);
	}

	public SwitchNode<R> match(String pattern, FixieFun3<Node, Node, Node, R> fun) {
		return match3_(Suite.pattern(pattern), fun);
	}

	public SwitchNode<R> match(String pattern, FixieFun4<Node, Node, Node, Node, R> fun) {
		return match4_(Suite.pattern(pattern), fun);
	}

	public SwitchNode<R> matchArray(Pattern pattern, Fun<Node[], R> fun) {
		return match_(pattern, fun);
	}

	public SwitchNode<R> matchArray(String pattern, Fun<Node[], R> fun) {
		return match_(Suite.pattern(pattern), fun);
	}

	public R nonNullResult() {
		return result != null ? result : Fail.t("cannot handle " + in);
	}

	public R result() {
		return result;
	}

	private SwitchNode<R> match0_(Pattern pattern, FixieFun0<R> fun) {
		return match_(pattern, m -> fun.apply());
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

	private <T> SwitchNode<R> match_(Matcher<T> matcher, Fun<T, R> fun) {
		T t;
		if (result == null && (t = matcher.match(in)) != null)
			result = fun.apply(t);
		return this;
	}

	private SwitchNode<R> match_(Pattern pattern, Fun<Node[], R> fun) {
		Node[] m;
		if (result == null && (m = pattern.match(in)) != null)
			result = fun.apply(m);
		return this;
	}

	private List<Node> nodes(Object m) {
		return Read.from(m.getClass().getFields()).map(field -> (Node) rethrow(() -> field.get(m))).toList();
	}

}
