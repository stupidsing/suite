package suite.jdk.gen;

import suite.jdk.gen.FunExpression.FunExpr;

public class LambdaImplementation<I> {

	public final LambdaInterface<I> lambdaInterface;
	public final FunExpr expr;

	public static <I> LambdaImplementation<I> of(LambdaInterface<I> lambdaInterface, FunExpr expr) {
		return new LambdaImplementation<>(lambdaInterface, expr);
	}

	private LambdaImplementation(LambdaInterface<I> lambdaInterface, FunExpr expr) {
		this.lambdaInterface = lambdaInterface;
		this.expr = expr;
	}

}
