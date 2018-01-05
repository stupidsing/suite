package suite.node.io;

import suite.Suite;
import suite.node.Atom;
import suite.node.Node;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;

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

	public <T extends Node> SwitchNode<R> doIf(Class<T> c, Sink<T> fun) {
		return applyIf(c, t -> {
			fun.sink(t);
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

	public SwitchNode<R> match(String pattern, Fun<Node[], R> fun) {
		Node[] m;
		if (result == null && (m = Suite.match(pattern).apply(in)) != null)
			result = fun.apply(m);
		return this;
	}

	public R nonNullResult() {
		if (result != null)
			return result;
		else
			throw new RuntimeException("cannot handle " + in);
	}

	public R result() {
		return result;
	}

}
