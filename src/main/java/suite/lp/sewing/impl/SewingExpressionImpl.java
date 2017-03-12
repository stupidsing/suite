package suite.lp.sewing.impl;

import org.apache.bcel.generic.Type;

import suite.Suite;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.jdk.lambda.LambdaImplementation;
import suite.jdk.lambda.LambdaInstance;
import suite.jdk.lambda.LambdaInterface;
import suite.lp.predicate.EvalPredicates;
import suite.lp.sewing.SewingCloner;
import suite.lp.sewing.SewingCloner.Clone_;
import suite.lp.sewing.SewingExpression;
import suite.node.Int;
import suite.node.Node;
import suite.util.To;

public class SewingExpressionImpl implements SewingExpression {

	private static FunFactory ff = new FunFactory();
	private static LambdaInterface<Evaluate> lambdaInterface = LambdaInterface.of(Evaluate.class);

	private SewingCloner sc;
	private EvalPredicates evalPredicates = new EvalPredicates();

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
			return LambdaInstance.of(compiledNumber, To.map(keyNumber, ((Int) node).number));
		else {
			Clone_ f = sc.compile(node);
			Evaluate evaluate = env -> evalPredicates.evaluate(f.apply(env));
			return LambdaInstance.of(compiledEval, To.map(keyEval, evaluate));
		}
	}

	private static String keyEval = "eval";
	private static String keyNumber = "key";
	private static LambdaImplementation<Evaluate> compiledEval = compileEval(keyEval);
	private static LambdaImplementation<Evaluate> compiledNumber = compileNumber(keyNumber);

	private static LambdaImplementation<Evaluate> compileEval(String key) {
		FunExpr expr = ff.parameter1(env -> ff.inject(keyEval).invoke("evaluate", env));
		return LambdaImplementation.of(lambdaInterface, To.map(key, Type.getType(Evaluate.class)), expr);
	}

	private static LambdaImplementation<Evaluate> compileNumber(String key) {
		FunExpr expr = ff.parameter0(() -> ff.inject(key));
		return LambdaImplementation.of(lambdaInterface, To.map(key, Type.INT), expr);
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
