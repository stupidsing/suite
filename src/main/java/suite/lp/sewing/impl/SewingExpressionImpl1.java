package suite.lp.sewing.impl;

import java.util.Map;

import org.apache.bcel.generic.Type;

import suite.Suite;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.jdk.lambda.LambdaImplementation;
import suite.jdk.lambda.LambdaInstance;
import suite.jdk.lambda.LambdaInterface;
import suite.lp.sewing.SewingCloner;
import suite.lp.sewing.SewingCloner.Clone_;
import suite.lp.sewing.SewingExpression;
import suite.node.Int;
import suite.node.Node;
import suite.node.util.TreeUtil;

public class SewingExpressionImpl1 implements SewingExpression {

	private static FunFactory f = new FunFactory();
	private static LambdaInterface<Evaluate> lambdaInterface = LambdaInterface.of(Evaluate.class);

	private SewingCloner sc;

	public SewingExpressionImpl1(SewingCloner sc) {
		this.sc = sc;
	}

	public Evaluate compile(Node node) {
		return compile_(node).newFun();
	}

	private LambdaInstance<Evaluate> compile_(Node node) {
		Node[] m;

		if ((m = Suite.match(".0 + .1").apply(node)) != null)
			return compileOperator(m, "+");
		else if ((m = Suite.match(".0 - .1").apply(node)) != null)
			return compileOperator(m, "-");
		else if ((m = Suite.match(".0 * .1").apply(node)) != null)
			return compileOperator(m, "*");
		else if ((m = Suite.match(".0 / .1").apply(node)) != null)
			return compileOperator(m, "/");
		else if ((m = Suite.match(".0 and .1").apply(node)) != null)
			return compileOperator(m, "&&");
		else if ((m = Suite.match(".0 or .1").apply(node)) != null)
			return compileOperator(m, "||");
		else if ((m = Suite.match(".0 shl .1").apply(node)) != null)
			return compileOperator(m, "<<");
		else if ((m = Suite.match(".0 shr .1").apply(node)) != null)
			return compileOperator(m, ">>");
		else if (node instanceof Int)
			return LambdaInstance.of(compiledNumber, Map.of(keyNumber, ((Int) node).number));
		else {
			Clone_ n_ = sc.compile(node);
			Evaluate evaluate = env -> TreeUtil.evaluate(n_.apply(env));
			return LambdaInstance.of(compiledEval, Map.of(keyEval, evaluate));
		}
	}

	private static String keyEval = "eval";
	private static String keyNumber = "key";
	private static LambdaImplementation<Evaluate> compiledEval = compileEval(keyEval);
	private static LambdaImplementation<Evaluate> compiledNumber = compileNumber(keyNumber);

	private static LambdaImplementation<Evaluate> compileEval(String key) {
		FunExpr expr = f.parameter1(env -> f.inject(keyEval).invoke("evaluate", env));
		return LambdaImplementation.of(lambdaInterface, Map.of(key, Type.getType(Evaluate.class)), expr);
	}

	private static LambdaImplementation<Evaluate> compileNumber(String key) {
		FunExpr expr = f.parameter0(() -> f.inject(key));
		return LambdaImplementation.of(lambdaInterface, Map.of(key, Type.INT), expr);
	}

	private LambdaInstance<Evaluate> compileOperator(Node[] m, String op) {
		LambdaInstance<Evaluate> lambda0 = compile_(m[0]);
		LambdaInstance<Evaluate> lambda1 = compile_(m[1]);

		return LambdaInstance.of(Evaluate.class, env -> {
			FunExpr v0 = lambda0.invoke(env);
			FunExpr v1 = lambda1.invoke(env);
			return f.bi(op, v0, v1);
		});
	}

}
