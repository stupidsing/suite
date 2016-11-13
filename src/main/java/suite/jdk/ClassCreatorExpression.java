package suite.jdk;

import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ClassCreatorExpression {

	public static abstract class Expression {
		protected String type; // Type.getDescriptor()

		public Expression field(String field, String type) {
			FieldExpression expr = new FieldExpression();
			expr.type = type;
			expr.field = field;
			expr.object = this;
			return expr;
		}

		public Expression instanceOf() {
			InstanceOfExpression expr = new InstanceOfExpression();
			expr.instanceType = boolean.class;
			expr.object = this;
			return expr;
		}

		public Expression invoke(ClassCreator cc, Expression... parameters) {
			return invoke(cc.methodName, cc.className, parameters);
		}

		public Expression invoke(String methodName, Class<?> clazz, Expression... parameters) {
			return invoke(methodName, Type.getDescriptor(clazz), parameters);
		}

		public Expression invoke(String methodName, String type, Expression... parameters) {
			InvokeExpression expr = new InvokeExpression();
			expr.type = type;
			expr.methodName = methodName;
			expr.object = this;
			expr.opcode = Opcodes.INVOKEVIRTUAL;
			expr.parameters = Arrays.asList(parameters);
			return expr;
		}
	}

	public static class BinaryExpression extends Expression {
		public int opcode;
		public Expression left, right;
	}

	public static class ConstantExpression extends Expression {
		public Object constant;
	}

	public static class FieldExpression extends Expression {
		public Expression object;
		public String field;
	}

	public static class IfBooleanExpression extends Expression {
		public Expression if_, then, else_;
	}

	public static class InstanceOfExpression extends Expression {
		public Expression object;
		public Class<?> instanceType;
	}

	public static class InvokeExpression extends Expression {
		public int opcode;
		public String methodName;
		public Expression object;
		public List<Expression> parameters;
	}

	public static class MethodParameterExpression extends Expression {
		public int number;
	}

	public static class PrintlnExpression extends Expression {
		public Expression expression;
	}

	public static class ThisExpression extends Expression {
	}

}
