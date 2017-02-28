package suite.lp.sewing.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.bcel.generic.Type;

import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.jdk.gen.LambdaImplementation;
import suite.jdk.gen.LambdaInstance;
import suite.jdk.gen.LambdaInterface;
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

	private static FunFactory ff = new FunFactory();
	private static LambdaInterface<BindPredicate> lambdaClass = LambdaInterface.of(BindPredicate.class, "test");
	private static String key0 = "k0";
	private static String key1 = "k1";
	private static LambdaImplementation<BindPredicate> compiledBindAtom = compileBindAtom_();
	private static LambdaImplementation<BindPredicate> compiledBindInt = compileBindInt_();
	private static LambdaImplementation<BindPredicate> compiledBindStr = compileBindStr_();

	private boolean isBindTrees;

	public SewingBinderImpl() {
		this(true);
	}

	public SewingBinderImpl(boolean isBindTrees) {
		this.isBindTrees = isBindTrees;
	}

	public BindPredicate compileBind(Node node) {
		return compileBind0(node).newFun();
	}

	private LambdaInstance<BindPredicate> compileBind0(Node node) {
		Tree tree;

		if (node instanceof Atom)
			return compileBindAtom((Atom) node);
		else if (node instanceof Int)
			return compileBindInt((Int) node);
		else if (node instanceof Reference) {
			int index = findVariableIndex(node);
			return compileBindPredicate((be, n) -> Binder.bind(n, be.getEnv().get(index), be.getTrail()));
		} else if (node instanceof Str)
			return compileBindStr((Str) node);
		else if ((tree = Tree.decompose(node)) != null) {
			Operator operator = tree.getOperator();
			Clone_ f = compile(node);
			BindPredicate c0 = compileBind(tree.getLeft());
			BindPredicate c1 = compileBind(tree.getRight());
			return compileBindPredicate((be, n) -> {
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
			});
		} else if (node instanceof Tuple) {
			Clone_ f = compile(node);
			List<BindPredicate> cs = Read.from(((Tuple) node).nodes) //
					.map(this::compileBind0) //
					.map(LambdaInstance::newFun) //
					.toList();
			int size = cs.size();
			return compileBindPredicate((be, n) -> {
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
			});
		} else {
			Clone_ f = compile(node);
			return compileBindPredicate((be, n) -> Binder.bind(n, f.apply(be.getEnv()), be.getTrail()));
		}
	}

	private LambdaInstance<BindPredicate> compileBindPredicate(BindPredicate pred) {
		return LambdaInstance.of(
				LambdaImplementation.of( //
						lambdaClass, //
						Collections.singletonMap("pred", Type.getType(BindPredicate.class)), //
						ff.parameter2((be, n) -> ff.inject("pred").invoke("test", new FunExpr[] { be, n, }))), //
				Collections.singletonMap("pred", pred));
	}

	private LambdaInstance<BindPredicate> compileBindAtom(Atom a) {
		return LambdaInstance.of(compiledBindAtom, Collections.singletonMap(key0, a));
	}

	private LambdaInstance<BindPredicate> compileBindInt(Int i) {
		return LambdaInstance.of(compiledBindInt, Read.<String, Object> empty2().cons(key0, i).cons(key1, i.number).toMap());
	}

	private LambdaInstance<BindPredicate> compileBindStr(Str s) {
		return LambdaInstance.of(compiledBindStr, Read.<String, Object> empty2().cons(key0, s).cons(key1, s.value).toMap());
	}

	private static LambdaImplementation<BindPredicate> compileBindAtom_() {
		Map<String, Type> fieldTypes = Collections.singletonMap(key0, Type.getType(Node.class));

		return bind(fieldTypes, //
				n_ -> ff.ifEquals(n_, ff.inject(key0), ff._true(), ff._false()));
	}

	private static LambdaImplementation<BindPredicate> compileBindInt_() {
		Map<String, Type> fieldTypes = Read.<String, Type> empty2() //
				.cons(key0, Type.getType(Node.class)) //
				.cons(key1, Type.INT) //
				.toMap();

		return bind(fieldTypes, //
				n_ -> ff.ifInstance(Int.class, n_, //
						i -> ff.ifEquals(i.field("number"), ff.inject(key1), ff._true(), ff._false()), //
						ff._false()));
	}

	private static LambdaImplementation<BindPredicate> compileBindStr_() {
		Map<String, Type> fieldTypes = Read.<String, Type> empty2() //
				.cons(key0, Type.getType(Node.class)) //
				.cons(key1, Type.getType(String.class)) //
				.toMap();

		return bind(fieldTypes, //
				n_ -> ff.ifInstance(Str.class, n_, //
						i -> ff.inject(key1).invoke("equals", i.field("value").cast(Object.class)), //
						ff._false()));
	}

	private static LambdaImplementation<BindPredicate> bind(Map<String, Type> fieldTypes, Fun<FunExpr, FunExpr> compare) {
		return LambdaImplementation.of(lambdaClass, fieldTypes,
				ifRef(be -> ref -> ff.seq( //
						be.invoke("getTrail").invoke("addBind", ref, ff.inject(key0)), //
						ff._true()), //
						be -> compare));
	}

	private static FunExpr ifRef(Fun<FunExpr, Fun<FunExpr, FunExpr>> bindRef, Fun<FunExpr, Fun<FunExpr, FunExpr>> bindTree) {
		return ff.parameter2((be, n) -> ff.declare( //
				n.invoke("finalNode"), //
				n_ -> ff.ifInstance(Reference.class, n_, bindRef.apply(be), bindTree.apply(be).apply(n_))));
	}

}
