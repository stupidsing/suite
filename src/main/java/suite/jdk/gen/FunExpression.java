package suite.jdk.gen;

import java.util.List;

import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import suite.inspect.Inspect;
import suite.jdk.gen.FunExprL.ApplyFunExpr;
import suite.jdk.gen.FunExprL.FieldFunExpr;
import suite.jdk.gen.FunExprM.ArrayLengthFunExpr;
import suite.jdk.gen.FunExprM.CastFunExpr;
import suite.jdk.gen.FunExprM.CheckCastFunExpr;
import suite.jdk.gen.FunExprM.FieldTypeFunExpr;
import suite.jdk.gen.FunExprM.IndexFunExpr;
import suite.jdk.gen.FunExprM.InstanceOfFunExpr;
import suite.jdk.gen.FunExprM.InvokeMethodFunExpr;
import suite.node.util.Singleton;
import suite.util.Object_;

public class FunExpression {

	private static Inspect inspect = Singleton.me.inspect;

	public static abstract class FunExpr {
		public FunExpr apply(FunExpr... parameters) {
			return apply(List.of(parameters));
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
			return invoke(clazz, methodName, List.of(parameters));
		}

		public FunExpr invoke(Class<?> clazz, String methodName, List<FunExpr> list) {
			InvokeMethodFunExpr expr = new InvokeMethodFunExpr();
			expr.clazz = clazz;
			expr.methodName = methodName;
			expr.object = this;
			expr.parameters = list;
			return expr;
		}

		public FunExpr length() {
			ArrayLengthFunExpr expr = new ArrayLengthFunExpr();
			expr.expr = this;
			return expr;
		}

		@Override
		public boolean equals(Object object) {
			return Object_.clazz(object) == getClass() && inspect.equals(this, object);
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

}
