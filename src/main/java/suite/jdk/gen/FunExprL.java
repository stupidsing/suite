package suite.jdk.gen;

import java.util.List;

import org.apache.bcel.generic.Type;

import suite.jdk.gen.FunExprK.PlaceholderFunExpr;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.lambda.LambdaInstance;

/**
 * Functional expressions that can be handled by FunRewrite.java.
 *
 * @author ywsing
 */
public class FunExprL {

	public static class ApplyFunExpr extends FunExpr {
		public FunExpr object;
		public List<FunExpr> parameters;
	}

	public static class DeclareLocalFunExpr extends FunExpr {
		public PlaceholderFunExpr var;
		public FunExpr value;
		public FunExpr do_;
	}

	public static class FieldFunExpr_ extends FunExpr {
		public String fieldName;
		public FunExpr object;
	}

	public static class FieldFunExpr extends FieldFunExpr_ {
	}

	public static class FieldInjectFunExpr extends FunExpr {
		public String fieldName;
	}

	public static class FieldSetFunExpr extends FieldFunExpr_ {
		public FunExpr value;
	}

	public static class InvokeLambdaFunExpr extends FunExpr {
		public boolean isExpand;
		public LambdaInstance<?> lambda;
		public List<FunExpr> parameters;
	}

	public static class ObjectFunExpr extends FunExpr {
		public Type type;
		public Object object;
	}

}
