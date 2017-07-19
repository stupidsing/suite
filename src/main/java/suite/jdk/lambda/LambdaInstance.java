package suite.jdk.lambda;

import java.util.Collections;
import java.util.Map;

import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Fun2;

public class LambdaInstance<I> {

	private static FunFactory f = new FunFactory();

	public final LambdaImplementation<I> lambdaImplementation;
	public final Map<String, Object> fieldValues;

	private I fun;

	public static <I> LambdaInstance<I> of(Class<I> clazz, Source<FunExpr> fun) {
		return of(LambdaInterface.of(clazz), f.parameter0(fun));
	}

	public static <I> LambdaInstance<I> of(Class<I> clazz, Iterate<FunExpr> fun) {
		return of(LambdaInterface.of(clazz), f.parameter1(fun));
	}

	public static <I> LambdaInstance<I> of(Class<I> clazz, Fun2<FunExpr, FunExpr, FunExpr> fun) {
		return of(LambdaInterface.of(clazz), f.parameter2(fun));
	}

	public static <I> LambdaInstance<I> of(LambdaInterface<I> lambdaInterface, FunExpr expr) {
		return of(LambdaImplementation.of(lambdaInterface, Collections.emptyMap(), expr), Collections.emptyMap());
	}

	public static <I> LambdaInstance<I> of(LambdaImplementation<I> lambdaImplementation, Map<String, Object> fieldValues) {
		return new LambdaInstance<>(lambdaImplementation, fieldValues);
	}

	private LambdaInstance(LambdaImplementation<I> lambdaImplementation, Map<String, Object> fieldValues) {
		this.lambdaImplementation = lambdaImplementation;
		this.fieldValues = fieldValues;
	}

	public FunExpr invoke(FunExpr... parameters) {
		return f.invoke(this, parameters);
	}

	public I newFun() {
		if (fun == null)
			fun = lambdaImplementation.newFun(fieldValues);
		return fun;
	}

}
