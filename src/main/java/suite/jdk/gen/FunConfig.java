package suite.jdk.gen;

import java.util.Map;

import suite.jdk.gen.FunExpression.FunExpr;

public class FunConfig<I> {

	public final LambdaClass<I> lambdaClass;
	public final FunExpr expr;
	public final Map<String, Object> fields;

	public static <I> FunConfig<I> of(LambdaClass<I> lambdaClass, FunExpr expr, Map<String, Object> fields) {
		return new FunConfig<>(lambdaClass, expr, fields);
	}

	private FunConfig(LambdaClass<I> lambdaClass, FunExpr expr, Map<String, Object> fields) {
		this.lambdaClass = lambdaClass;
		this.expr = expr;
		this.fields = fields;
	}

	public I newFun() {
		return FunCreator.of(lambdaClass).create(expr).apply(fields);
	}

}
