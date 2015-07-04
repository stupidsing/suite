package suite.lp.sewing.impl;

import java.util.function.BiPredicate;

import suite.lp.doer.Binder;
import suite.lp.sewing.SewingBinder;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.util.FunUtil.Fun;

public class SewingBinderImpl extends SewingClonerImpl implements SewingBinder {

	private boolean isBindTrees;

	public SewingBinderImpl() {
		this(true);
	}

	public SewingBinderImpl(boolean isBindTrees) {
		this.isBindTrees = isBindTrees;
	}

	public BiPredicate<BindEnv, Node> compileBind(Node node) {
		Tree tree;

		if (node instanceof Atom)
			return compileBindAtom((Atom) node);
		else if (node instanceof Int)
			return compileBindInt((Int) node);
		else if (node instanceof Str)
			return compileBindStr((Str) node);
		else if ((tree = Tree.decompose(node)) != null) {
			Operator operator = tree.getOperator();
			Fun<Env, Node> f = compile(node);
			BiPredicate<BindEnv, Node> c0 = compileBind(tree.getLeft());
			BiPredicate<BindEnv, Node> c1 = compileBind(tree.getRight());
			return (be, n) -> {
				Node n_ = n.finalNode();
				Tree t;
				if (!(n_ instanceof Reference))
					return (t = Tree.decompose(n_, operator)) != null //
							&& c0.test(be, t.getLeft()) //
							&& c1.test(be, t.getRight());
				else if (isBindTrees) {
					be.journal.addBind((Reference) n_, f.apply(be.env));
					return true;
				} else
					return false;
			};
		} else if (node instanceof Reference) {
			int index = findVariableIndex(node);
			return (be, n) -> Binder.bind(n, be.env.get(index), be.journal);
		} else {
			Fun<Env, Node> f = compile(node);
			return (be, n) -> Binder.bind(n, f.apply(be.env), be.journal);
		}
	}

	private BiPredicate<BindEnv, Node> compileBindAtom(Atom a) {
		return (be, n) -> {
			Node n_ = n.finalNode();
			if (n_ instanceof Reference) {
				be.journal.addBind((Reference) n_, a);
				return true;
			} else
				return n_ == a;
		};
	}

	private BiPredicate<BindEnv, Node> compileBindInt(Int i_) {
		int i = i_.number;
		return (be, n) -> {
			Node n_ = n.finalNode();
			if (n_ instanceof Reference) {
				be.journal.addBind((Reference) n_, i_);
				return true;
			} else
				return n_ instanceof Int && ((Int) n_).number == i;
		};
	}

	private BiPredicate<BindEnv, Node> compileBindStr(Str str) {
		String s = str.value;
		return (be, n) -> {
			Node n_ = n.finalNode();
			if (n_ instanceof Reference) {
				be.journal.addBind((Reference) n_, str);
				return true;
			} else
				return n_ instanceof Str && s.equals(((Str) n_).value);
		};
	}

}
