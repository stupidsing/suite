package suite.jdk.gen;

import java.util.Map;

import org.apache.bcel.generic.Type;

import suite.jdk.gen.FunExpression.FunExpr;

public class FunConfig<I> {

	public final LambdaClass<I> lambdaClass;
	public final FunExpr expr;
	public final Map<String, Type> fieldTypes;
	public final Map<String, Object> fieldValues;

	public static <I> FunConfig<I> of(LambdaClass<I> lambdaClass, FunExpr expr, Map<String, Type> fieldTypes,
			Map<String, Object> fieldValues) {
		return new FunConfig<>(lambdaClass, expr, fieldTypes, fieldValues);
	}

	private FunConfig(LambdaClass<I> lambdaClass, FunExpr expr, Map<String, Type> fieldTypes, Map<String, Object> fieldValues) {
		this.lambdaClass = lambdaClass;
		this.expr = expr;
		this.fieldTypes = fieldTypes;
		this.fieldValues = fieldValues;
	}

	public I newFun() {
		return FunCreator.of(lambdaClass, fieldTypes).create(expr).apply(fieldValues);
	}

}
