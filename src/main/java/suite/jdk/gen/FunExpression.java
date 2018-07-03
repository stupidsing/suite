package suite.jdk.gen;

import java.util.List;

import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import suite.inspect.Dump;
import suite.inspect.Inspect;
import suite.jdk.gen.FunExprL.ApplyFunExpr;
import suite.jdk.gen.FunExprL.FieldFunExpr;
import suite.jdk.gen.FunExprL.FieldSetFunExpr;
import suite.jdk.gen.FunExprM.ArrayLengthFunExpr;
import suite.jdk.gen.FunExprM.CastFunExpr;
import suite.jdk.gen.FunExprM.CheckCastFunExpr;
import suite.jdk.gen.FunExprM.FieldTypeFunExpr;
import suite.jdk.gen.FunExprM.FieldTypeSetFunExpr;
import suite.jdk.gen.FunExprM.IndexFunExpr;
import suite.jdk.gen.FunExprM.InstanceOfFunExpr;
import suite.jdk.gen.FunExprM.InvokeMethodFunExpr;
import suite.node.util.Singleton;
import suite.object.AutoInterface;
import suite.object.Object_;

public class FunExpression {

	private static Inspect inspect = Singleton.me.inspect;

	public static abstract class FunExpr implements AutoInterface<FunExpr> {
		public FunExpr apply(FunExpr... parameters) {
			return apply(List.of(parameters));
		}

		public FunExpr apply(List<FunExpr> list) {
			var expr = new ApplyFunExpr();
			expr.object = this;
			expr.parameters = list;
			return expr;
		}

		public FunExpr cast_(Class<?> clazz) {
			var expr = new CastFunExpr();
			expr.type = Type.getType(clazz);
			expr.expr = this;
			return expr;
		}

		public FunExpr checkCast(Class<?> clazz) {
			var expr = new CheckCastFunExpr();
			expr.type = ObjectType.getInstance(clazz.getName());
			expr.expr = this;
			return expr;
		}

		public FunExpr field(String fieldName) {
			var expr = new FieldFunExpr();
			expr.fieldName = fieldName;
			expr.object = this;
			return expr;
		}

		public FunExpr field(String fieldName, Type fieldType) {
			var expr = new FieldTypeFunExpr();
			expr.fieldName = fieldName;
			expr.fieldType = fieldType;
			expr.object = this;
			return expr;
		}

		public FunExpr fieldSet(String fieldName, FunExpr value) {
			var expr = new FieldSetFunExpr();
			expr.fieldName = fieldName;
			expr.object = this;
			expr.value = value;
			return expr;
		}

		public FunExpr fieldSet(String fieldName, Type fieldType, FunExpr value) {
			var expr = new FieldTypeSetFunExpr();
			expr.fieldName = fieldName;
			expr.fieldType = fieldType;
			expr.object = this;
			expr.value = value;
			return expr;
		}

		public FunExpr index(FunExpr index) {
			var expr = new IndexFunExpr();
			expr.array = this;
			expr.index = index;
			return expr;
		}

		public FunExpr instanceOf(Class<?> clazz) {
			var expr = new InstanceOfFunExpr();
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
			var expr = new InvokeMethodFunExpr();
			expr.clazz = clazz;
			expr.methodName = methodName;
			expr.object = this;
			expr.parameters = list;
			return expr;
		}

		public FunExpr length() {
			var expr = new ArrayLengthFunExpr();
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
			return Dump.toLine(this);
		}
	}

}
