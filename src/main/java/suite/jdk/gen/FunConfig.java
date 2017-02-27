package suite.jdk.gen;

import java.util.Map;

import org.apache.bcel.generic.Type;

import suite.jdk.gen.FunExpression.FunExpr;

public class FunConfig<I> {

	public final LambdaClass<I> lambdaClass;
	public final Map<String, Type> fieldTypes;
	public final FunExpr expr;

	public static <I> FunConfig<I> of(LambdaClass<I> lambdaClass, Map<String, Type> fieldTypes, FunExpr expr) {
		return new FunConfig<>(lambdaClass, fieldTypes, expr);
	}

	private FunConfig(LambdaClass<I> lambdaClass, Map<String, Type> fieldTypes, FunExpr expr) {
		this.lambdaClass = lambdaClass;
		this.fieldTypes = fieldTypes;
		this.expr = expr;
	}

	public I newFun(Map<String, Object> fieldValues) {
		return FunCreator.of(lambdaClass, fieldTypes).create(expr).apply(fieldValues);
	}

}
