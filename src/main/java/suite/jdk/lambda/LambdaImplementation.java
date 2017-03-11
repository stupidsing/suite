package suite.jdk.lambda;

import java.util.Map;

import org.apache.bcel.generic.Type;

import suite.jdk.gen.FunCreator;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.util.FunUtil.Fun;

public class LambdaImplementation<I> {

	public final LambdaInterface<I> lambdaInterface;
	public final Map<String, Type> fieldTypes;
	public final FunExpr expr;

	private Fun<Map<String, Object>, I> fun;

	public static <I> LambdaImplementation<I> of(LambdaInterface<I> lambdaInterface, Map<String, Type> fieldTypes, FunExpr expr) {
		return new LambdaImplementation<>(lambdaInterface, fieldTypes, expr);
	}

	private LambdaImplementation(LambdaInterface<I> lambdaInterface, Map<String, Type> fieldTypes, FunExpr expr) {
		this.lambdaInterface = lambdaInterface;
		this.fieldTypes = fieldTypes;
		this.expr = expr;
	}

	public I newFun(Map<String, Object> fieldValues) {
		if (fun == null)
			fun = FunCreator.of(lambdaInterface, fieldTypes).create(expr);
		return fun.apply(fieldValues);
	}

}
