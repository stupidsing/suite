package suite.jdk.lambda;

import java.util.Map;

import org.apache.bcel.generic.Type;

import suite.jdk.gen.FunCreator;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.streamlet.FunUtil.Fun;

public class LambdaImplementation<I> {

	public final LambdaInterface<I> lambdaInterface;
	public final Map<String, Type> fieldTypeByNames;
	public final FunExpr expr;

	private Fun<Map<String, Object>, I> fun;

	public static <I> LambdaImplementation<I> of(LambdaInterface<I> l_intf, Map<String, Type> fieldTypeByNames, FunExpr expr) {
		return new LambdaImplementation<>(l_intf, fieldTypeByNames, expr);
	}

	private LambdaImplementation(LambdaInterface<I> lambdaInterface, Map<String, Type> fieldTypeByNames, FunExpr expr) {
		this.lambdaInterface = lambdaInterface;
		this.fieldTypeByNames = fieldTypeByNames;
		this.expr = expr;
	}

	public I newFun(Map<String, Object> fieldValueByFieldNames) {
		if (fun == null)
			fun = FunCreator.of(lambdaInterface, fieldTypeByNames).create(expr);
		return fun.apply(fieldValueByFieldNames);
	}

}
