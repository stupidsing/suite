package suite.lp.compile.impl;

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
import suite.lp.doer.BinderFactory;
import suite.lp.sewing.impl.SewingClonerImpl;
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
import suite.util.FunUtil.Iterate;

public class CompileBinderImpl extends SewingClonerImpl implements BinderFactory {

	private static FunFactory f = new FunFactory();
	private static LambdaInterface<Bind_> lambdaClass = LambdaInterface.of(Bind_.class, "test");

	private boolean isBindTrees;

	public CompileBinderImpl() {
		this(true);
	}

	public CompileBinderImpl(boolean isBindTrees) {
		this.isBindTrees = isBindTrees;
	}

	public Bind_ binder(Node node) {
		return compileBind_(node).newFun();
	}

	private LambdaInstance<Bind_> compileBind_(Node node) {
		Tree tree;

		if (node instanceof Atom)
			return compileBindAtom((Atom) node);
		else if (node instanceof Int)
			return compileBindInt((Int) node);
		else if (node instanceof Reference) {
			int index = computeIndex(node);
			return compileBindPredicate((be, n) -> Binder.bind(n, be.env.get(index), be.trail));
		} else if (node instanceof Str)
			return compileBindStr((Str) node);
		else if ((tree = Tree.decompose(node)) != null) {
			Operator operator = tree.getOperator();
			LambdaInstance<Bind_> lambda0 = compileBind_(tree.getLeft());
			LambdaInstance<Bind_> lambda1 = compileBind_(tree.getRight());
			Fun<FunExpr, Iterate<FunExpr>> bindRef = bindRef(cloner(node));

			Fun<FunExpr, Iterate<FunExpr>> bindTree = be -> n_ -> f //
					.declare(f.invokeStatic(Tree.class, "decompose", n_, f.object(operator)), t -> f //
							.ifNonNullAnd(t, f.and( //
									lambda0.invoke(be, t.invoke("getLeft")), //
									lambda1.invoke(be, t.invoke("getRight")))));

			return LambdaInstance.of(lambdaClass, ifRef(bindRef, bindTree));
		} else if (node instanceof Tuple) {
			List<LambdaInstance<Bind_>> lambdas = Read //
					.from(((Tuple) node).nodes) //
					.map(this::compileBind_) //
					.toList();

			Fun<FunExpr, Iterate<FunExpr>> bindRef = bindRef(cloner(node));

			Fun<FunExpr, Iterate<FunExpr>> bindTuple = be -> n_ -> f //
					.ifInstanceAnd(Tuple.class, n_, tuple -> f //
							.declare(tuple.field("nodes"), nodes -> {
								List<FunExpr> cond = new ArrayList<>();
								for (int i = 0; i < lambdas.size(); i++) {
									LambdaInstance<Bind_> lambda = lambdas.get(i);
									FunExpr ni = nodes.index(f.int_(i));
									cond.add(lambda.invoke(be, ni));
								}
								return f.and(cond.toArray(new FunExpr[0]));
							}));

			return LambdaInstance.of(lambdaClass, ifRef(bindRef, bindTuple));
		} else {
			Clone_ n_ = cloner(node);
			return compileBindPredicate((be, n) -> Binder.bind(n, n_.apply(be.env), be.trail));
		}
	}

	private LambdaInstance<Bind_> compileBindPredicate(Bind_ pred) {
		return LambdaInstance.of(LambdaImplementation.of( //
				lambdaClass, //
				Map.of("pred", Type.getType(Bind_.class)), //
				f.parameter2((be, n) -> f.inject("pred").invoke("test", be, n))), //
				Map.of("pred", pred));
	}

	private LambdaInstance<Bind_> compileBindAtom(Atom a) {
		return LambdaInstance.of(compiledBindAtom, Map.of(key0, a));
	}

	private LambdaInstance<Bind_> compileBindInt(Int i) {
		return LambdaInstance.of(compiledBindInt, Map.of(key0, i, key1, i.number));
	}

	private LambdaInstance<Bind_> compileBindStr(Str s) {
		return LambdaInstance.of(compiledBindStr, Map.of(key0, s, key1, s.value));
	}

	private static String key0 = "k0";
	private static String key1 = "k1";
	private static LambdaImplementation<Bind_> compiledBindAtom = compileBindAtom_();
	private static LambdaImplementation<Bind_> compiledBindInt = compileBindInt_();
	private static LambdaImplementation<Bind_> compiledBindStr = compileBindStr_();

	private static LambdaImplementation<Bind_> compileBindAtom_() {
		Map<String, Type> fieldTypes = Map.of(key0, Type.getType(Node.class));
		Iterate<FunExpr> expr = n_ -> f.ifEquals(n_, f.inject(key0), f._true(), f._false());
		return bind(fieldTypes, expr);
	}

	private static LambdaImplementation<Bind_> compileBindInt_() {
		Map<String, Type> fieldTypes = Map.of(key0, Type.getType(Node.class), key1, Type.INT);
		Iterate<FunExpr> expr = n_ -> f.ifInstanceAnd( //
				Int.class, n_, //
				i -> f.ifEquals(i.field("number"), f.inject(key1), f._true(), f._false()));
		return bind(fieldTypes, expr);
	}

	private static LambdaImplementation<Bind_> compileBindStr_() {
		Map<String, Type> fieldTypes = Map.of(key0, Type.getType(Node.class), key1, Type.STRING);
		Iterate<FunExpr> expr = n_ -> f.ifInstanceAnd( //
				Str.class, n_, //
				i -> f.inject(key1).invoke("equals", i.field("value").cast(Object.class)));
		return bind(fieldTypes, expr);
	}

	private static LambdaImplementation<Bind_> bind(Map<String, Type> fieldTypes, Iterate<FunExpr> compare) {
		return LambdaImplementation.of(lambdaClass, fieldTypes,
				ifRef(be -> ref -> bindRef(be, ref, f.inject(key0)), be -> compare));
	}

	private Fun<FunExpr, Iterate<FunExpr>> bindRef(Clone_ n_) {
		Fun<FunExpr, Iterate<FunExpr>> bindRef;
		if (isBindTrees)
			bindRef = be -> ref -> bindRef(be, ref, f.object(n_).apply(be.field("env")));
		else
			bindRef = be -> ref -> f._false();
		return bindRef;
	}

	private static FunExpr bindRef(FunExpr bindEnv, FunExpr ref, FunExpr n1) {
		return f.seq(bindEnv.field("trail").invoke("addBind", ref, n1), f._true());
	}

	private static FunExpr ifRef(Fun<FunExpr, Iterate<FunExpr>> bindRef, Fun<FunExpr, Iterate<FunExpr>> bindTree) {
		return f.parameter2((be, n) -> f.declare( //
				n.invoke("finalNode"), //
				n_ -> f.ifInstance(Reference.class, n_, bindRef.apply(be), bindTree.apply(be).apply(n_))));
	}

}
