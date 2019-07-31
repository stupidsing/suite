package suite.jdk.gen;

import java.util.List;

import org.apache.bcel.generic.Type;

import primal.adt.Fixie_.FixieFun0;
import primal.adt.Fixie_.FixieFun1;
import primal.adt.Fixie_.FixieFun2;
import primal.adt.Fixie_.FixieFun3;
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

		public <R> R apply(FixieFun2<FunExpr, List<FunExpr>, R> fun) {
			return fun.apply(object, parameters);
		}
	}

	public static class DeclareLocalFunExpr extends FunExpr {
		public PlaceholderFunExpr var;
		public FunExpr value;
		public FunExpr do_;

		public <R> R apply(FixieFun3<PlaceholderFunExpr, FunExpr, FunExpr, R> fun) {
			return fun.apply(var, value, do_);
		}
	}

	public static class FieldFunExpr_ extends FunExpr {
		public String fieldName;
		public FunExpr object;

		public <R> R apply(FixieFun2<String, FunExpr, R> fun) {
			return fun.apply(fieldName, object);
		}
	}

	public static class FieldFunExpr extends FieldFunExpr_ {
		public <R> R apply(FixieFun0<R> fun) {
			return fun.apply();
		}
	}

	public static class FieldInjectFunExpr extends FunExpr {
		public String fieldName;

		public <R> R apply(FixieFun1<String, R> fun) {
			return fun.apply(fieldName);
		}
	}

	public static class FieldSetFunExpr extends FieldFunExpr_ {
		public FunExpr value;

		public <R> R apply(FixieFun1<FunExpr, R> fun) {
			return fun.apply(value);
		}
	}

	public static class InvokeLambdaFunExpr extends FunExpr {
		public boolean isExpand;
		public LambdaInstance<?> lambda;
		public List<FunExpr> parameters;

		public <R> R apply(FixieFun3<Boolean, LambdaInstance<?>, List<FunExpr>, R> fun) {
			return fun.apply(isExpand, lambda, parameters);
		}
	}

	public static class ObjectFunExpr extends FunExpr {
		public Type type;
		public Object object;

		public <R> R apply(FixieFun2<Type, Object, R> fun) {
			return fun.apply(type, object);
		}
	}

}
