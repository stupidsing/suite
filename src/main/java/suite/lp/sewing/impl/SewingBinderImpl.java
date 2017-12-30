package suite.lp.sewing.impl;

import java.util.List;

import suite.lp.doer.Binder;
import suite.lp.doer.BinderFactory;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.Operator;
import suite.streamlet.Read;

public class SewingBinderImpl extends SewingClonerImpl implements BinderFactory {

	private boolean isBindTrees;

	public SewingBinderImpl() {
		this(true);
	}

	public SewingBinderImpl(boolean isBindTrees) {
		this.isBindTrees = isBindTrees;
	}

	public BindPredicate compileBind(Node node) {
		Tree tree;

		if (node instanceof Atom)
			return compileBindAtom((Atom) node);
		else if (node instanceof Int)
			return compileBindInt((Int) node);
		else if (node instanceof Reference) {
			int index = findVariableIndex(node);
			return (be, n) -> Binder.bind(n, be.getEnv().get(index), be.getTrail());
		} else if (node instanceof Str)
			return compileBindStr((Str) node);
		else if ((tree = Tree.decompose(node)) != null) {
			Operator operator = tree.getOperator();
			Clone_ f = compile(node);
			BindPredicate c0 = compileBind(tree.getLeft());
			BindPredicate c1 = compileBind(tree.getRight());
			return (be, n) -> {
				Node n_ = n.finalNode();
				Tree t;
				if (n_ instanceof Reference)
					if (isBindTrees) {
						be.getTrail().addBind((Reference) n_, f.apply(be.getEnv()));
						return true;
					} else
						return false;
				else
					return (t = Tree.decompose(n_, operator)) != null //
							&& c0.test(be, t.getLeft()) //
							&& c1.test(be, t.getRight());
			};
		} else if (node instanceof Tuple) {
			Clone_ f = compile(node);
			List<BindPredicate> cs = Read.from(((Tuple) node).nodes).map(this::compileBind).toList();
			int size = cs.size();
			return (be, n) -> {
				Node n_ = n.finalNode();
				if (n_ instanceof Tuple) {
					Node[] nodes = ((Tuple) n_).nodes;
					if (nodes.length == size) {
						for (int i = 0; i < size; i++)
							if (!cs.get(i).test(be, nodes[i]))
								return false;
						return true;
					} else
						return false;
				} else if (n_ instanceof Reference)
					if (isBindTrees) {
						be.getTrail().addBind((Reference) n_, f.apply(be.getEnv()));
						return true;
					} else
						return false;
				else
					return false;
			};
		} else {
			Clone_ f = compile(node);
			return (be, n) -> Binder.bind(n, f.apply(be.getEnv()), be.getTrail());
		}
	}

	private BindPredicate compileBindAtom(Atom a) {
		return (be, n) -> {
			Node n_ = n.finalNode();
			if (n_ instanceof Reference) {
				be.getTrail().addBind((Reference) n_, a);
				return true;
			} else
				return n_ == a;
		};
	}

	private BindPredicate compileBindInt(Int i_) {
		int i = i_.number;
		return (be, n) -> {
			Node n_ = n.finalNode();
			if (n_ instanceof Reference) {
				be.getTrail().addBind((Reference) n_, i_);
				return true;
			} else
				return n_ instanceof Int && ((Int) n_).number == i;
		};
	}

	private BindPredicate compileBindStr(Str str) {
		String s = str.value;
		return (be, n) -> {
			Node n_ = n.finalNode();
			if (n_ instanceof Reference) {
				be.getTrail().addBind((Reference) n_, str);
				return true;
			} else
				return n_ instanceof Str && s.equals(((Str) n_).value);
		};
	}

}
