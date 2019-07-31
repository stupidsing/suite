package suite.jdk.lambda;

import java.util.Map;

import primal.fp.Funs.Iterate;
import primal.fp.Funs.Source;
import primal.fp.Funs2.BinOp;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;

public class LambdaInstance<I> {

	private static FunFactory f = new FunFactory();

	public final LambdaImplementation<I> lambdaImplementation;
	public final Map<String, Object> fieldValueByNames;

	private I fun;

	public static <I> LambdaInstance<I> of(Class<I> clazz, Source<FunExpr> fun) {
		return of(LambdaInterface.of(clazz), f.parameter0(fun));
	}

	public static <I> LambdaInstance<I> of(Class<I> clazz, Iterate<FunExpr> fun) {
		return of(LambdaInterface.of(clazz), f.parameter1(fun));
	}

	public static <I> LambdaInstance<I> of(Class<I> clazz, BinOp<FunExpr> fun) {
		return of(LambdaInterface.of(clazz), f.parameter2(fun));
	}

	public static <I> LambdaInstance<I> of(LambdaInterface<I> lambdaInterface, FunExpr expr) {
		return of(LambdaImplementation.of(lambdaInterface, Map.ofEntries(), expr), Map.ofEntries());
	}

	public static <I> LambdaInstance<I> of(LambdaImplementation<I> lambdaImplementation, Map<String, Object> fieldValueByNames) {
		return new LambdaInstance<>(lambdaImplementation, fieldValueByNames);
	}

	private LambdaInstance(LambdaImplementation<I> lambdaImplementation, Map<String, Object> fieldValueByNames) {
		this.lambdaImplementation = lambdaImplementation;
		this.fieldValueByNames = fieldValueByNames;
	}

	public FunExpr invoke(FunExpr... parameters) {
		return f.invoke(this, parameters);
	}

	public I newFun() {
		if (fun == null)
			fun = lambdaImplementation.newFun(fieldValueByNames);
		return fun;
	}

}
