package suite.jdk.gen;

import java.util.Map;

public class LambdaInstance<I> {

	public final LambdaImplementation<I> lambdaImplementation;
	public final Map<String, Object> fieldValues;

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
