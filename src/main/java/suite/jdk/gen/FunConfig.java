package suite.jdk.gen;

import java.util.Map;

import org.apache.bcel.generic.Type;

import suite.util.FunUtil.Fun;

public class FunConfig<I> {

	public final LambdaImplementation<I> lambda;
	public final Map<String, Type> fieldTypes;

	private Fun<Map<String, Object>, I> fun;

	public static <I> FunConfig<I> of(LambdaImplementation<I> lambda, Map<String, Type> fieldTypes) {
		return new FunConfig<>(lambda, fieldTypes);
	}

	private FunConfig(LambdaImplementation<I> lambda, Map<String, Type> fieldTypes) {
		this.lambda = lambda;
		this.fieldTypes = fieldTypes;

		fun = FunCreator.of(lambda.lambdaInterface, fieldTypes).create(lambda.expr);
	}

	public I newFun(Map<String, Object> fieldValues) {
		return fun.apply(fieldValues);
	}

}
