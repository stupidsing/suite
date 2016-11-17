package suite.jdk;

import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class FunExpression {

	public static abstract class FunExpr {
		protected String type; // Type.getDescriptor()

		public FunExpr field(String field, String type) {
			FieldFunExpr expr = new FieldFunExpr();
			expr.type = type;
			expr.field = field;
			expr.object = this;
			return expr;
		}

		public FunExpr instanceOf() {
			InstanceOfFunExpr expr = new InstanceOfFunExpr();
			expr.instanceType = boolean.class;
			expr.object = this;
			return expr;
		}

		public FunExpr invoke(FunCreator<?> cc, FunExpr... parameters) {
			return invoke(cc.methodName, cc.returnType, parameters);
		}

		public FunExpr invoke(String methodName, Class<?> clazz, FunExpr... parameters) {
			return invoke(methodName, Type.getDescriptor(clazz), parameters);
		}

		public FunExpr invoke(String methodName, String type, FunExpr... parameters) {
			InvokeFunExpr expr = new InvokeFunExpr();
			expr.type = type;
			expr.methodName = methodName;
			expr.object = this;
			expr.opcode = Opcodes.INVOKEINTERFACE;
			expr.parameters = Arrays.asList(parameters);
			return expr;
		}
	}

	public static class BinaryFunExpr extends FunExpr {
		public int opcode;
		public FunExpr left, right;
	}

	public static class ConstantFunExpr extends FunExpr {
		public Object constant;
	}

	public static class FieldFunExpr extends FunExpr {
		public FunExpr object;
		public String field;
	}

	public static class IfBooleanFunExpr extends FunExpr {
		public FunExpr if_, then, else_;
	}

	public static class InstanceOfFunExpr extends FunExpr {
		public FunExpr object;
		public Class<?> instanceType;
	}

	public static class InvokeFunExpr extends FunExpr {
		public int opcode;
		public String methodName;
		public FunExpr object;
		public List<FunExpr> parameters;
	}

	public static class ParameterFunExpr extends FunExpr {
		public int number;
	}

	public static class PrintlnFunExpr extends FunExpr {
		public FunExpr expression;
	}

	public static class ThisFunExpr extends FunExpr {
	}

}
