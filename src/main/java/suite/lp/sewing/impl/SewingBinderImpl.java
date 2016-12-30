package suite.lp.sewing.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import suite.jdk.FunCreator;
import suite.jdk.FunExpression.FunExpr;
import suite.lp.doer.Binder;
import suite.lp.sewing.SewingBinder;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.Operator;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;

public class SewingBinderImpl extends SewingClonerImpl implements SewingBinder {

	private static String key0 = "k0";
	private static String key1 = "k1";
	private static Fun<Map<String, Object>, BindPredicate> compiledBindAtom = compileBindAtom_();
	private static Fun<Map<String, Object>, BindPredicate> compiledBindInt = compileBindInt_();
	private static Fun<Map<String, Object>, BindPredicate> compiledBindStr = compileBindStr_();

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
			Fun<Env, Node> f = compile(node);
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
			Fun<Env, Node> f = compile(node);
			List<BindPredicate> cs = Read.from(((Tuple) node).nodes).map(this::compileBind).toList();
			int size = cs.size();
			return (be, n) -> {
				Node n_ = n.finalNode();
				if (n_ instanceof Tuple) {
					List<Node> nodes = ((Tuple) n_).nodes;
					if (nodes.size() == size) {
						for (int i = 0; i < size; i++)
							if (!cs.get(i).test(be, nodes.get(i)))
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
			Fun<Env, Node> f = compile(node);
			return (be, n) -> Binder.bind(n, f.apply(be.getEnv()), be.getTrail());
		}
	}

	private BindPredicate compileBindAtom(Atom a) {
		return compiledBindAtom.apply(Collections.singletonMap(key0, a));
	}

	private BindPredicate compileBindInt(Int i) {
		return compiledBindInt.apply(Read.<String, Object> empty2().cons(key0, i).cons(key1, i.number).toMap());
	}

	private BindPredicate compileBindStr(Str s) {
		return compiledBindStr.apply(Read.<String, Object> empty2().cons(key0, s).cons(key1, s.value).toMap());
	}

	private static Fun<Map<String, Object>, BindPredicate> compileBindAtom_() {
		Map<String, Class<?>> fields = Collections.singletonMap(key0, Node.class);

		FunCreator<BindPredicate> fc = FunCreator.of(BindPredicate.class, "test", fields);

		return bind(fc, //
				n_ -> fc.ifeq(n_, fc.field(key0), fc.true_(), fc.false_()));
	}

	private static Fun<Map<String, Object>, BindPredicate> compileBindInt_() {
		Map<String, Class<?>> fields = Read.<String, Class<?>> empty2() //
				.cons(key0, Node.class) //
				.cons(key1, int.class) //
				.toMap();

		FunCreator<BindPredicate> fc = FunCreator.of(BindPredicate.class, "test", fields);

		return bind(fc, //
				n_ -> fc.ifInstance(Int.class, n_, //
						i -> fc.ifeq(i.field("number"), fc.field(key1), fc.true_(), fc.false_()), //
						fc.false_()));
	}

	private static Fun<Map<String, Object>, BindPredicate> compileBindStr_() {
		Map<String, Class<?>> fields = Read.<String, Class<?>> empty2() //
				.cons(key0, Node.class) //
				.cons(key1, String.class) //
				.toMap();

		FunCreator<BindPredicate> fc = FunCreator.of(BindPredicate.class, "test", fields);

		return bind(fc, //
				n_ -> fc.ifInstance(Str.class, n_, //
						i -> fc.field(key1).invoke("equals", i.field("value").cast(Object.class)), //
						fc.false_()));
	}

	private static Fun<Map<String, Object>, BindPredicate> bind(FunCreator<BindPredicate> fc, Fun<FunExpr, FunExpr> compare) {
		FunExpr be = fc.parameter(1);
		FunExpr n = fc.parameter(2);
		FunExpr k0 = fc.field(key0);

		return fc.create(fc.local(n.invoke("finalNode"), //
				n_ -> fc.ifInstance(Reference.class, n_, //
						ref -> {
							FunExpr addBind = be.invoke("getTrail").invoke("addBind", ref, k0);
							return fc.seq(addBind, fc.true_());
						}, //
						compare.apply(n_))));
	}

	@SuppressWarnings("unused")
	private BindPredicate compileBindAtom0(Atom a) {
		return (be, n) -> {
			Node n_ = n.finalNode();
			if (n_ instanceof Reference) {
				be.getTrail().addBind((Reference) n_, a);
				return true;
			} else
				return n_ == a;
		};
	}

	@SuppressWarnings("unused")
	private BindPredicate compileBindInt0(Int i_) {
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

	@SuppressWarnings("unused")
	private BindPredicate compileBindStr0(Str str) {
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
