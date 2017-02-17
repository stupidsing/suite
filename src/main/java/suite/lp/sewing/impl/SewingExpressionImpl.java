package suite.lp.sewing.impl;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.bcel.generic.Type;

import suite.Suite;
import suite.jdk.gen.FunCreator;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.LambdaClass;
import suite.lp.predicate.EvalPredicates;
import suite.lp.sewing.SewingCloner;
import suite.lp.sewing.SewingExpression;
import suite.lp.sewing.VariableMapper.Env;
import suite.node.Int;
import suite.node.Node;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;

public class SewingExpressionImpl implements SewingExpression {

	private static LambdaClass<Evaluate> lambdaClass = LambdaClass.of(Evaluate.class);

	private static String key = "key";
	private static Fun<Map<String, Object>, Evaluate> compiledNumber = compileNumber(key);
	private static Map<String, Fun<Map<String, Object>, Evaluate>> compiledByOp = new ConcurrentHashMap<>();
	private SewingCloner sc;

	public interface Evaluate {
		public int evaluate(Env env);
	}

	public SewingExpressionImpl(SewingCloner sc) {
		this.sc = sc;
	}

	public Evaluate compile(Node node) {
		Node m[];

		if ((m = Suite.matcher(".0 + .1").apply(node)) != null)
			return compileOperator(m, "+");
		else if ((m = Suite.matcher(".0 - .1").apply(node)) != null)
			return compileOperator(m, "-");
		else if ((m = Suite.matcher(".0 * .1").apply(node)) != null)
			return compileOperator(m, "*");
		else if ((m = Suite.matcher(".0 / .1").apply(node)) != null)
			return compileOperator(m, "/");
		else if ((m = Suite.matcher(".0 and .1").apply(node)) != null)
			return compileOperator(m, "&&");
		else if ((m = Suite.matcher(".0 or .1").apply(node)) != null)
			return compileOperator(m, "||");
		else if ((m = Suite.matcher(".0 shl .1").apply(node)) != null)
			return compileOperator(m, "<<");
		else if ((m = Suite.matcher(".0 shr .1").apply(node)) != null)
			return compileOperator(m, ">>");
		else if (node instanceof Int)
			return compiledNumber.apply(Collections.singletonMap(key, ((Int) node).number));
		else {
			Fun<Env, Node> f = sc.compile(node);
			return env -> new EvalPredicates().evaluate(f.apply(env));
		}
	}

	private static Fun<Map<String, Object>, Evaluate> compileNumber(String key) {
		FunCreator<Evaluate> fc = FunCreator.of(lambdaClass, Collections.singletonMap(key, Type.INT));
		return fc.create(fc.field(key));
	}

	private Evaluate compileOperator(Node m[], String op) {
		String e0 = "e0", e1 = "e1";

		Fun<Map<String, Object>, Evaluate> fun = compiledByOp //
				.computeIfAbsent(op, op_ -> {
					FunCreator<Evaluate> fc = FunCreator.of(lambdaClass,
							Read.<String, Type> empty2() //
									.cons(e0, Type.getType(Evaluate.class)) //
									.cons(e1, Type.getType(Evaluate.class)) //
									.toMap());
					return fc.create(fc.parameter(env -> {
						FunExpr v0 = fc.field(e0).apply(env);
						FunExpr v1 = fc.field(e1).apply(env);
						return fc.bi(op_, v0, v1);
					}));
				});

		return fun.apply(Read.<String, Object> empty2() //
				.cons(e0, compile(m[0])) //
				.cons(e1, compile(m[1])) //
				.toMap());
	}

}
