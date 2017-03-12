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

public class SewingBinderImpl extends SewingClonerImpl implements SewingBinder {

	private static FunFactory ff = new FunFactory();
	private static LambdaInterface<BindPredicate> lambdaClass = LambdaInterface.of(BindPredicate.class, "test");

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
			LambdaInstance<BindPredicate> lambda0 = compileBind0(tree.getLeft());
			LambdaInstance<BindPredicate> lambda1 = compileBind0(tree.getRight());
			Fun<FunExpr, Fun<FunExpr, FunExpr>> bindRef = bindRef(compile(node));

			Fun<FunExpr, Fun<FunExpr, FunExpr>> bindTree = be -> n_ -> ff //
					.declare(ff.invokeStatic(Tree.class, "decompose", n_, ff.object(operator, Operator.class)),
							t -> ff.ifNonNullAnd(t, //
									ff.and( //
											ff.invoke(lambda0, be, t.invoke("getLeft")), //
											ff.invoke(lambda1, be, t.invoke("getRight")))));

			return LambdaInstance.of(lambdaClass, ifRef(bindRef, bindTree));
		} else if (node instanceof Tuple) {
			List<LambdaInstance<BindPredicate>> lambdas = Read //
					.from(((Tuple) node).nodes) //
					.map(this::compileBind0) //
					.toList();

			Fun<FunExpr, Fun<FunExpr, FunExpr>> bindRef = bindRef(compile(node));

			Fun<FunExpr, Fun<FunExpr, FunExpr>> bindTuple = be -> n_ -> ff //
					.ifInstanceAnd(Tuple.class, n_, tuple -> ff //
							.declare(tuple.field("nodes"), nodes -> {
								List<FunExpr> cond = new ArrayList<>();
								for (int i = 0; i < lambdas.size(); i++)
									cond.add(ff.invoke(lambdas.get(i), //
											be, //
											nodes.invoke("get", ff.int_(i)).checkCast(Node.class)));
								return ff.and(cond.toArray(new FunExpr[0]));
							}));

			return LambdaInstance.of(lambdaClass, ifRef(bindRef, bindTuple));
		} else {
			Clone_ f = compile(node);
			return compileBindPredicate((be, n) -> Binder.bind(n, f.apply(be.getEnv()), be.getTrail()));
		}
	}

	private LambdaInstance<BindPredicate> compileBindPredicate(BindPredicate pred) {
		return LambdaInstance.of(
				LambdaImplementation.of( //
						lambdaClass, //
						To.map("pred", Type.getType(BindPredicate.class)), //
						ff.parameter2((be, n) -> ff.inject("pred").invoke("test", be, n))), //
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
		Fun<FunExpr, FunExpr> expr = n_ -> ff.ifEquals(n_, ff.inject(key0), ff._true(), ff._false());
		return bind(fieldTypes, expr);
	}

	private static LambdaImplementation<BindPredicate> compileBindInt_() {
		Map<String, Type> fieldTypes = To.map(key0, Type.getType(Node.class), key1, Type.INT);
		Fun<FunExpr, FunExpr> expr = n_ -> ff.ifInstanceAnd( //
				Int.class, n_, //
				i -> ff.ifEquals(i.field("number"), ff.inject(key1), ff._true(), ff._false()));
		return bind(fieldTypes, expr);
	}

	private static LambdaImplementation<BindPredicate> compileBindStr_() {
		Map<String, Type> fieldTypes = To.map(key0, Type.getType(Node.class), key1, Type.STRING);
		Fun<FunExpr, FunExpr> expr = n_ -> ff.ifInstanceAnd( //
				Str.class, n_, //
				i -> ff.inject(key1).invoke("equals", i.field("value").cast(Object.class)));
		return bind(fieldTypes, expr);
	}

	private static LambdaImplementation<BindPredicate> bind(Map<String, Type> fieldTypes, Fun<FunExpr, FunExpr> compare) {
		return LambdaImplementation.of(lambdaClass, fieldTypes,
				ifRef(be -> ref -> bindRef(be, ref, ff.inject(key0)), be -> compare));
	}

	private Fun<FunExpr, Fun<FunExpr, FunExpr>> bindRef(Clone_ f) {
		Fun<FunExpr, Fun<FunExpr, FunExpr>> bindRef;
		if (isBindTrees)
			bindRef = be -> ref -> bindRef(be, ref, ff.object(f, Clone_.class).apply(be.invoke("getEnv")));
		else
			bindRef = be -> ref -> ff._false();
		return bindRef;
	}

	private static FunExpr bindRef(FunExpr bindEnv, FunExpr ref, FunExpr n1) {
		return ff.seq(bindEnv.invoke("getTrail").invoke("addBind", ref, n1), ff._true());
	}

	private static FunExpr ifRef(Fun<FunExpr, Fun<FunExpr, FunExpr>> bindRef, Fun<FunExpr, Fun<FunExpr, FunExpr>> bindTree) {
		return ff.parameter2((be, n) -> ff.declare( //
				n.invoke("finalNode"), //
				n_ -> ff.ifInstance(Reference.class, n_, bindRef.apply(be), bindTree.apply(be).apply(n_))));
	}

}
