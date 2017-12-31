package suite.lp.compile.impl;

import java.util.Map;

import org.apache.bcel.generic.Type;

import suite.Suite;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.jdk.lambda.LambdaImplementation;
import suite.jdk.lambda.LambdaInstance;
import suite.jdk.lambda.LambdaInterface;
import suite.lp.doer.ClonerFactory;
import suite.lp.doer.ClonerFactory.Clone_;
import suite.lp.doer.EvaluatorFactory;
import suite.node.Int;
import suite.node.Node;
import suite.node.util.TreeUtil;

public class CompileExpressionImpl implements EvaluatorFactory {

	private static FunFactory f = new FunFactory();
	private static LambdaInterface<Evaluate_> lambdaInterface = LambdaInterface.of(Evaluate_.class);

	private ClonerFactory sc;

	public CompileExpressionImpl(ClonerFactory sc) {
		this.sc = sc;
	}

	public Evaluate_ evaluator(Node node) {
		return compile_(node).newFun();
	}

	private LambdaInstance<Evaluate_> compile_(Node node) {
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
			Clone_ n_ = sc.cloner(node);
			Evaluate_ evaluate = env -> TreeUtil.evaluate(n_.apply(env));
			return LambdaInstance.of(compiledEval, Map.of(keyEval, evaluate));
		}
	}

	private static String keyEval = "eval";
	private static String keyNumber = "key";
	private static LambdaImplementation<Evaluate_> compiledEval = compileEval(keyEval);
	private static LambdaImplementation<Evaluate_> compiledNumber = compileNumber(keyNumber);

	private static LambdaImplementation<Evaluate_> compileEval(String key) {
		FunExpr expr = f.parameter1(env -> f.inject(keyEval).invoke("evaluate", env));
		return LambdaImplementation.of(lambdaInterface, Map.of(key, Type.getType(Evaluate_.class)), expr);
	}

	private static LambdaImplementation<Evaluate_> compileNumber(String key) {
		FunExpr expr = f.parameter0(() -> f.inject(key));
		return LambdaImplementation.of(lambdaInterface, Map.of(key, Type.INT), expr);
	}

	private LambdaInstance<Evaluate_> compileOperator(Node[] m, String op) {
		LambdaInstance<Evaluate_> lambda0 = compile_(m[0]);
		LambdaInstance<Evaluate_> lambda1 = compile_(m[1]);

		return LambdaInstance.of(Evaluate_.class, env -> {
			FunExpr v0 = lambda0.invoke(env);
			FunExpr v1 = lambda1.invoke(env);
			return f.bi(op, v0, v1);
		});
	}

}
