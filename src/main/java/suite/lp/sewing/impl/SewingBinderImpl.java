package suite.lp.sewing.impl;

import primal.MoreVerbs.Read;
import suite.lp.doer.Binder;
import suite.lp.doer.BinderFactory;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.SwitchNode;

public class SewingBinderImpl extends SewingClonerImpl implements BinderFactory {

	private boolean isBindTrees;

	public SewingBinderImpl() {
		this(true);
	}

	public SewingBinderImpl(boolean isBindTrees) {
		this.isBindTrees = isBindTrees;
	}

	public Bind_ binder(Node node) {
		return new SwitchNode<Bind_>(node //
		).applyIf(Atom.class, n -> {
			return compileBindAtom(n);
		}).applyIf(Int.class, n -> {
			return compileBindInt(n);
		}).applyIf(Reference.class, n -> {
			var index = mapper().computeIndex(n);
			return (be, n_) -> Binder.bindReference(be.env.refs[index], n_, be.trail);
		}).applyIf(Str.class, n -> {
			return compileBindStr(n);
		}).applyTree((op, l, r) -> {
			var f = cloner(node);
			var c0 = binder(l);
			var c1 = binder(r);
			return (be, n) -> {
				var n_ = n.finalNode();
				Tree t;
				if (n_ instanceof Reference ref) {
					if (isBindTrees)
						be.trail.addBind(ref, f.apply(be.env));
					return isBindTrees;
				} else
					return (t = Tree.decompose(n_, op)) != null //
							&& c0.test(be, t.getLeft()) //
							&& c1.test(be, t.getRight());
			};
		}).applyIf(Tuple.class, tuple -> {
			var f = cloner(node);
			var cs = Read.from(tuple.nodes).map(this::binder).toArray(Bind_.class);
			var length = cs.length;
			return (be, n) -> {
				var n_ = n.finalNode();
				if (n_ instanceof Tuple tuple_) {
					var nodes = tuple_.nodes;
					var b = nodes.length == length;
					if (b)
						for (var i = 0; i < length; i++)
							if (!cs[i].test(be, nodes[i]))
								return false;
					return b;
				} else if (n_ instanceof Reference ref) {
					if (isBindTrees)
						be.trail.addBind(ref, f.apply(be.env));
					return isBindTrees;
				} else
					return false;
			};
		}).applyIf(Node.class, n -> {
			var f = cloner(node);
			return (be, n_) -> Binder.bind(n_, f.apply(be.env), be.trail);
		}).result();
	}

	private Bind_ compileBindAtom(Atom a) {
		return (be, n) -> {
			var n_ = n.finalNode();
			if (n_ instanceof Reference ref) {
				be.trail.addBind(ref, a);
				return true;
			} else
				return n_ == a;
		};
	}

	private Bind_ compileBindInt(Int i_) {
		var i = i_.number;
		return (be, n) -> {
			var n_ = n.finalNode();
			if (n_ instanceof Reference ref) {
				be.trail.addBind(ref, i_);
				return true;
			} else
				return n_ instanceof Int int_ && int_.number == i;
		};
	}

	private Bind_ compileBindStr(Str str) {
		var s = str.value;
		return (be, n) -> {
			var n_ = n.finalNode();
			if (n_ instanceof Reference ref) {
				be.trail.addBind(ref, str);
				return true;
			} else
				return n_ instanceof Str str_ && s.equals(str_.value);
		};
	}

}
