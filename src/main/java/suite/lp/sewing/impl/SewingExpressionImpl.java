package suite.lp.sewing.impl;

import java.util.Collections;

import org.apache.bcel.generic.Type;

import suite.Suite;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.jdk.gen.LambdaImplementation;
import suite.jdk.gen.LambdaInstance;
import suite.jdk.gen.LambdaInterface;
import suite.lp.predicate.EvalPredicates;
import suite.lp.sewing.SewingCloner;
import suite.lp.sewing.SewingCloner.Clone_;
import suite.lp.sewing.SewingExpression;
import suite.lp.sewing.VariableMapper.Env;
import suite.node.Int;
import suite.node.Node;

public class SewingExpressionImpl implements SewingExpression {

	private static FunFactory ff = new FunFactory();
	private static LambdaInterface<Evaluate> lambdaInterface = LambdaInterface.of(Evaluate.class);

	private static String key = "key";
	private static LambdaImplementation<Evaluate> compiledNumber = compileNumber(key);

	private SewingCloner sc;

	public interface Evaluate {
		public int evaluate(Env env);
	}

	public SewingExpressionImpl(SewingCloner sc) {
		this.sc = sc;
	}

	public Evaluate compile(Node node) {
		return compile0(node).newFun();
	}

	private LambdaInstance<Evaluate> compile0(Node node) {
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
			return LambdaInstance.of(compiledNumber, Collections.singletonMap(key, ((Int) node).number));
		else {
			Clone_ f = sc.compile(node);
			return compileEvaluate(env -> new EvalPredicates().evaluate(f.apply(env)));
		}
	}

	private LambdaInstance<Evaluate> compileEvaluate(Evaluate evaluate) {
		String key = "eval";

		return LambdaInstance.of(
				LambdaImplementation.of( //
						lambdaInterface, //
						Collections.singletonMap(key, Type.getType(Evaluate.class)), //
						ff.parameter1(env -> ff.inject(key).invoke("evaluate", env))), //
				Collections.singletonMap(key, evaluate));
	}

	private static LambdaImplementation<Evaluate> compileNumber(String key) {
		FunExpr expr = ff.parameter0(() -> ff.inject(key));
		return LambdaImplementation.of(lambdaInterface, Collections.singletonMap(key, Type.INT), expr);
	}

	private LambdaInstance<Evaluate> compileOperator(Node m[], String op) {
		LambdaInstance<Evaluate> lambda0 = compile0(m[0]);
		LambdaInstance<Evaluate> lambda1 = compile0(m[1]);

		return LambdaInstance.of(lambdaInterface, ff.parameter1(env -> {
			FunExpr v0 = ff.invoke(lambda0, env);
			FunExpr v1 = ff.invoke(lambda1, env);
			return ff.bi(op, v0, v1);
		}));
	}

}
