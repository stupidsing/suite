package suite.jdk.gen;

import java.util.Map;

import org.apache.bcel.generic.Type;

public class FunConfig<I> {

	public final LambdaImplementation<I> lambda;
	public final Map<String, Type> fieldTypes;

	public static <I> FunConfig<I> of(LambdaImplementation<I> lambda, Map<String, Type> fieldTypes) {
		return new FunConfig<>(lambda, fieldTypes);
	}

	private FunConfig(LambdaImplementation<I> lambda, Map<String, Type> fieldTypes) {
		this.lambda = lambda;
		this.fieldTypes = fieldTypes;
	}

	public I newFun(Map<String, Object> fieldValues) {
		return FunCreator.of(lambda.lambdaInterface, fieldTypes) //
				.create(lambda.expr) //
				.apply(fieldValues);
	}

}
