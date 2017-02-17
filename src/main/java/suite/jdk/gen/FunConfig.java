package suite.jdk.gen;

import java.util.Map;

import suite.jdk.gen.FunExpression.FunExpr;

public class FunConfig<I> {

	public final Class<I> interfaceClazz;
	public final String methodName;
	public final FunExpr expr;
	public final Map<String, Object> fields;

	public static <I> FunConfig<I> of(Class<I> interfaceClazz, FunExpr expr, Map<String, Object> fields) {
		return of(interfaceClazz, Type_.methodOf(interfaceClazz).getName(), expr, fields);
	}

	public static <I> FunConfig<I> of(Class<I> interfaceClazz, String methodName, FunExpr expr, Map<String, Object> fields) {
		return new FunConfig<>(interfaceClazz, methodName, expr, fields);
	}

	private FunConfig(Class<I> interfaceClazz, String methodName, FunExpr expr, Map<String, Object> fields) {
		this.interfaceClazz = interfaceClazz;
		this.methodName = methodName;
		this.expr = expr;
		this.fields = fields;
	}

	public I newFun() {
		return FunCreator.of(interfaceClazz, methodName).create(expr).apply(fields);
	}

}
