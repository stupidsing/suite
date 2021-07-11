package suite.node.io;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.util.List;

import primal.MoreVerbs.Read;
import primal.adt.FixieArray;
import primal.adt.Fixie_.FixieFun0;
import primal.adt.Fixie_.FixieFun1;
import primal.adt.Fixie_.FixieFun2;
import primal.adt.Fixie_.FixieFun3;
import primal.adt.Fixie_.FixieFun4;
import primal.adt.Fixie_.FixieFun5;
import primal.adt.Fixie_.FixieFun6;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Source;
import primal.parser.Operator;
import suite.BindArrayUtil.Pattern;
import suite.Suite;
import suite.fp.Matcher;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.primitive.IoSink;

public class SwitchNode<R> {

	private Node in;
	private R result;
	private Fun<String, Pattern> patternf;

	public SwitchNode(Node in) {
		this(in, Suite::pattern);
	}

	public SwitchNode(Node in, Fun<String, Pattern> patternf) {
		this.in = in;
		this.patternf = patternf;
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

	public <T extends Node> SwitchNode<R> doIf(Class<T> c, IoSink<T> sink) {
		return applyIf(c, t -> {
			@SuppressWarnings("unchecked")
			var r = (R) ex(() -> {
				sink.f(t);
				return t;
			});
			return r;
		});
	}

	public SwitchNode<R> match(Atom node, Source<R> fun) {
		if (result == null && in == node)
			result = fun.g();
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

	public SwitchNode<R> match(Pattern pattern, FixieFun3<Node, Node, Node, R> fun) {
		return match3_(pattern, fun);
	}

	public SwitchNode<R> match(String pattern, FixieFun0<R> fun) {
		return match0_(patternf.apply(pattern), fun);
	}

	public SwitchNode<R> match(String pattern, FixieFun1<Node, R> fun) {
		return match1_(patternf.apply(pattern), fun);
	}

	public SwitchNode<R> match(String pattern, FixieFun2<Node, Node, R> fun) {
		return match2_(patternf.apply(pattern), fun);
	}

	public SwitchNode<R> match(String pattern, FixieFun3<Node, Node, Node, R> fun) {
		return match3_(patternf.apply(pattern), fun);
	}

	public SwitchNode<R> match(String pattern, FixieFun4<Node, Node, Node, Node, R> fun) {
		return match4_(patternf.apply(pattern), fun);
	}

	public SwitchNode<R> match(String pattern, FixieFun5<Node, Node, Node, Node, Node, R> fun) {
		return match5_(patternf.apply(pattern), fun);
	}

	public SwitchNode<R> matchArray(Pattern pattern, Fun<Node[], R> fun) {
		return match_(pattern, fun);
	}

	public SwitchNode<R> matchArray(String pattern, Fun<Node[], R> fun) {
		return match_(patternf.apply(pattern), fun);
	}

	public R nonNullResult() {
		var result = result();
		return result != null ? result : fail("cannot handle " + in);
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

	private SwitchNode<R> match5_(Pattern pattern, FixieFun5<Node, Node, Node, Node, Node, R> fun) {
		return match_(pattern, m -> fun.apply(m[0], m[1], m[2], m[3], m[4]));
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
		return Read.from(m.getClass().getFields()).map(field -> (Node) ex(() -> field.get(m))).toList();
	}

}
