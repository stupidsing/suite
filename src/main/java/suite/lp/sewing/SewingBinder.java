package suite.lp.sewing;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

import suite.lp.Journal;
import suite.lp.doer.Binder;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.util.FunUtil.Fun;
import suite.util.Util;

public class SewingBinder extends SewingGeneralizer {

	public static class BindEnv {
		private Journal journal;
		private Env env;

		public BindEnv(Journal journal, Env env) {
			this.journal = journal;
			this.env = env;
		}
	}

	public BiPredicate<BindEnv, Node> compileBind(Node node0) {
		Node node = node0.finalNode();
		Tree tree;

		if (node instanceof Atom) {
			String name = ((Atom) node0).name;
			if (isWildcard(name))
				return (be, n) -> true;
			else if (isVariable(name)) {
				int index = getVariableIndex(node0);
				return (be, n) -> {
					Reference ref = be.env.refs[index];
					if (ref.isFree()) {
						ref.bound(n);
						return true;
					} else
						return Binder.bind(n, ref, be.journal);
				};
			} else
				return boundIfPossible(node, n -> n == node);
		} else if (node instanceof Int) {
			int number = ((Int) node).number;
			return boundIfPossible(node, n -> n instanceof Int && ((Int) n).number == number);
		} else if (node instanceof Str) {
			String value = ((Str) node).value;
			return boundIfPossible(node, n -> n instanceof Str && Util.stringEquals(((Str) n).value, value));
		} else if ((tree = Tree.decompose(node)) != null) {
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
				else {
					Reference reference = (Reference) n_;
					if (reference.isFree()) {
						be.journal.addBind(reference, f.apply(be.env));
						return true;
					} else
						return Binder.bind(n, f.apply(be.env), be.journal);
				}
			};
		} else {
			Fun<Env, Node> f = compile(node);
			return (be, n) -> Binder.bind(n, f.apply(be.env), be.journal);
		}
	}

	private BiPredicate<BindEnv, Node> boundIfPossible(Node node, Predicate<Node> isEqual) {
		return (be, n) -> {
			Node n_ = n.finalNode();
			if (!(n_ instanceof Reference))
				return isEqual.test(n_);
			else {
				be.journal.addBind((Reference) n_, node);
				return true;
			}
		};
	}

}
