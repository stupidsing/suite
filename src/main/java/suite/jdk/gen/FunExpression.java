package suite.jdk.gen;

import java.util.Arrays;
import java.util.List;

import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import suite.inspect.Inspect;
import suite.jdk.gen.FunExprM.CastFunExpr;
import suite.jdk.gen.FunExprM.CheckCastFunExpr;
import suite.jdk.gen.FunExprM.FieldTypeFunExpr;
import suite.jdk.gen.FunExprM.IndexFunExpr;
import suite.jdk.gen.FunExprM.InstanceOfFunExpr;
import suite.jdk.gen.FunExprM.InvokeMethodFunExpr;
import suite.jdk.lambda.LambdaInstance;
import suite.node.util.Singleton;
import suite.util.Util;

public class FunExpression {

	private static Inspect inspect = Singleton.get().getInspect();

	public static abstract class FunExpr {
		public FunExpr apply(FunExpr... parameters) {
			return apply(Arrays.asList(parameters));
		}

		public FunExpr apply(List<FunExpr> list) {
			ApplyFunExpr expr = new ApplyFunExpr();
			expr.object = this;
			expr.parameters = list;
			return expr;
		}

		public FunExpr cast(Class<?> clazz) {
			CastFunExpr expr = new CastFunExpr();
			expr.type = Type.getType(clazz);
			expr.expr = this;
			return expr;
		}

		public FunExpr checkCast(Class<?> clazz) {
			CheckCastFunExpr expr = new CheckCastFunExpr();
			expr.type = ObjectType.getInstance(clazz.getName());
			expr.expr = this;
			return expr;
		}

		public FunExpr field(String fieldName) {
			FieldFunExpr expr = new FieldFunExpr();
			expr.fieldName = fieldName;
			expr.object = this;
			return expr;
		}

		public FunExpr field(String fieldName, Type fieldType) {
			FieldTypeFunExpr expr = new FieldTypeFunExpr();
			expr.fieldName = fieldName;
			expr.fieldType = fieldType;
			expr.object = this;
			return expr;
		}

		public FunExpr index(FunExpr index) {
			IndexFunExpr expr = new IndexFunExpr();
			expr.array = this;
			expr.index = index;
			return expr;
		}

		public FunExpr instanceOf(Class<?> clazz) {
			InstanceOfFunExpr expr = new InstanceOfFunExpr();
			expr.instanceType = (ReferenceType) Type.getType(clazz);
			expr.object = this;
			return expr;
		}

		public FunExpr invoke(String methodName, FunExpr... parameters) {
			return invoke(null, methodName, parameters);
		}

		public FunExpr invoke(Class<?> clazz, String methodName, FunExpr... parameters) {
			return invoke(clazz, methodName, Arrays.asList(parameters));
		}

		public FunExpr invoke(Class<?> clazz, String methodName, List<FunExpr> list) {
			InvokeMethodFunExpr expr = new InvokeMethodFunExpr();
			expr.clazz = clazz;
			expr.methodName = methodName;
			expr.object = this;
			expr.parameters = list;
			return expr;
		}

		@Override
		public boolean equals(Object object) {
			return Util.clazz(object) == getClass() && inspect.equals(this, object);
		}

		@Override
		public int hashCode() {
			return inspect.hashCode(this);
		}

		@Override
		public String toString() {
			return inspect.toString(this);
		}
	}

	public static class ApplyFunExpr extends FunExpr {
		public FunExpr object;
		public List<FunExpr> parameters;
	}

	public static class DeclareParameterFunExpr extends FunExpr {
		public FunExpr do_;
	}

	public static class Declare0ParameterFunExpr extends DeclareParameterFunExpr {
	}

	public static class Declare1ParameterFunExpr extends DeclareParameterFunExpr {
		public PlaceholderFunExpr parameter;
	}

	public static class Declare2ParameterFunExpr extends DeclareParameterFunExpr {
		public PlaceholderFunExpr p0, p1;
	}

	public static class DeclareLocalFunExpr extends FunExpr {
		public PlaceholderFunExpr var;
		public FunExpr value;
		public FunExpr do_;
	}

	public static class FieldFunExpr extends FunExpr {
		public String fieldName;
		public FunExpr object;
	}

	public static class FieldInjectFunExpr extends FunExpr {
		public String fieldName;
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

	public static class PlaceholderFunExpr extends FunExpr {
		public int id = Util.temp();
	}

}
