package suite.jdk.gen;

import java.util.Collections;
import java.util.Map;

import suite.jdk.gen.FunExpression.FunExpr;

public class LambdaInstance<I> {

	public final LambdaImplementation<I> lambdaImplementation;
	public final Map<String, Object> fieldValues;

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

	public I newFun() {
		return lambdaImplementation.newFun(fieldValues);
	}

}
