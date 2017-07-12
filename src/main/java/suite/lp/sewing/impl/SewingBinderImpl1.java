package suite.lp.sewing.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.bcel.generic.Type;

import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.jdk.lambda.LambdaImplementation;
import suite.jdk.lambda.LambdaInstance;
import suite.jdk.lambda.LambdaInterface;
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
import suite.util.To;

public class SewingBinderImpl1 extends SewingClonerImpl implements SewingBinder {

	private static FunFactory f = new FunFactory();
	private static LambdaInterface<BindPredicate> lambdaClass = LambdaInterface.of(BindPredicate.class, "test");

	private boolean isBindTrees;

	public SewingBinderImpl1() {
		this(true);
	}

	public SewingBinderImpl1(boolean isBindTrees) {
		this.isBindTrees = isBindTrees;
	}

	public BindPredicate compileBind(Node node) {
		return compileBind_(node).newFun();
	}

	private LambdaInstance<BindPredicate> compileBind_(Node node) {
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
			LambdaInstance<BindPredicate> lambda0 = compileBind_(tree.getLeft());
			LambdaInstance<BindPredicate> lambda1 = compileBind_(tree.getRight());
			Fun<FunExpr, Fun<FunExpr, FunExpr>> bindRef = bindRef(compile(node));

			Fun<FunExpr, Fun<FunExpr, FunExpr>> bindTree = be -> n_ -> f //
					.declare(f.invokeStatic(Tree.class, "decompose", n_, f.object(operator)), t -> f //
							.ifNonNullAnd(t, f.and( //
									lambda0.invoke(be, t.invoke("getLeft")), //
									lambda1.invoke(be, t.invoke("getRight")))));

			return LambdaInstance.of(lambdaClass, ifRef(bindRef, bindTree));
		} else if (node instanceof Tuple) {
			List<LambdaInstance<BindPredicate>> lambdas = Read //
					.from(((Tuple) node).nodes) //
					.map(this::compileBind_) //
					.toList();

			Fun<FunExpr, Fun<FunExpr, FunExpr>> bindRef = bindRef(compile(node));

			Fun<FunExpr, Fun<FunExpr, FunExpr>> bindTuple = be -> n_ -> f //
					.ifInstanceAnd(Tuple.class, n_, tuple -> f //
							.declare(tuple.field("nodes"), nodes -> {
								List<FunExpr> cond = new ArrayList<>();
								for (int i = 0; i < lambdas.size(); i++) {
									LambdaInstance<BindPredicate> lambda = lambdas.get(i);
									FunExpr ni = nodes.index(f.int_(i));
									cond.add(lambda.invoke(be, ni));
								}
								return f.and(cond.toArray(new FunExpr[0]));
							}));

			return LambdaInstance.of(lambdaClass, ifRef(bindRef, bindTuple));
		} else {
			Clone_ n_ = compile(node);
			return compileBindPredicate((be, n) -> Binder.bind(n, n_.apply(be.getEnv()), be.getTrail()));
		}
	}

	private LambdaInstance<BindPredicate> compileBindPredicate(BindPredicate pred) {
		return LambdaInstance.of(LambdaImplementation.of( //
				lambdaClass, //
				To.map("pred", Type.getType(BindPredicate.class)), //
				f.parameter2((be, n) -> f.inject("pred").invoke("test", be, n))), //
				To.map("pred", pred));
	}

	private LambdaInstance<BindPredicate> compileBindAtom(Atom a) {
		return LambdaInstance.of(compiledBindAtom, To.map(key0, a));
	}

	private LambdaInstance<BindPredicate> compileBindInt(Int i) {
		return LambdaInstance.of(compiledBindInt, To.map(key0, i, key1, i.number));
	}

	private LambdaInstance<BindPredicate> compileBindStr(Str s) {
		return LambdaInstance.of(compiledBindStr, To.map(key0, s, key1, s.value));
	}

	private static String key0 = "k0";
	private static String key1 = "k1";
	private static LambdaImplementation<BindPredicate> compiledBindAtom = compileBindAtom_();
	private static LambdaImplementation<BindPredicate> compiledBindInt = compileBindInt_();
	private static LambdaImplementation<BindPredicate> compiledBindStr = compileBindStr_();

	private static LambdaImplementation<BindPredicate> compileBindAtom_() {
		Map<String, Type> fieldTypes = To.map(key0, Type.getType(Node.class));
		Fun<FunExpr, FunExpr> expr = n_ -> f.ifEquals(n_, f.inject(key0), f._true(), f._false());
		return bind(fieldTypes, expr);
	}

	private static LambdaImplementation<BindPredicate> compileBindInt_() {
		Map<String, Type> fieldTypes = To.map(key0, Type.getType(Node.class), key1, Type.INT);
		Fun<FunExpr, FunExpr> expr = n_ -> f.ifInstanceAnd( //
				Int.class, n_, //
				i -> f.ifEquals(i.field("number"), f.inject(key1), f._true(), f._false()));
		return bind(fieldTypes, expr);
	}

	private static LambdaImplementation<BindPredicate> compileBindStr_() {
		Map<String, Type> fieldTypes = To.map(key0, Type.getType(Node.class), key1, Type.STRING);
		Fun<FunExpr, FunExpr> expr = n_ -> f.ifInstanceAnd( //
				Str.class, n_, //
				i -> f.inject(key1).invoke("equals", i.field("value").cast(Object.class)));
		return bind(fieldTypes, expr);
	}

	private static LambdaImplementation<BindPredicate> bind(Map<String, Type> fieldTypes, Fun<FunExpr, FunExpr> compare) {
		return LambdaImplementation.of(lambdaClass, fieldTypes,
				ifRef(be -> ref -> bindRef(be, ref, f.inject(key0)), be -> compare));
	}

	private Fun<FunExpr, Fun<FunExpr, FunExpr>> bindRef(Clone_ n_) {
		Fun<FunExpr, Fun<FunExpr, FunExpr>> bindRef;
		if (isBindTrees)
			bindRef = be -> ref -> bindRef(be, ref, f.object(n_).apply(be.invoke("getEnv")));
		else
			bindRef = be -> ref -> f._false();
		return bindRef;
	}

	private static FunExpr bindRef(FunExpr bindEnv, FunExpr ref, FunExpr n1) {
		return f.seq(bindEnv.invoke("getTrail").invoke("addBind", ref, n1), f._true());
	}

	private static FunExpr ifRef(Fun<FunExpr, Fun<FunExpr, FunExpr>> bindRef, Fun<FunExpr, Fun<FunExpr, FunExpr>> bindTree) {
		return f.parameter2((be, n) -> f.declare( //
				n.invoke("finalNode"), //
				n_ -> f.ifInstance(Reference.class, n_, bindRef.apply(be), bindTree.apply(be).apply(n_))));
	}

}
