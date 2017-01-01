package suite.jdk;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import suite.util.Rethrow;

public class FunExpression {

	public static abstract class FunExpr {
		protected String type; // type.getDescriptor()

		public FunExpr cast(Class<?> clazz) {
			CastFunExpr expr = new CastFunExpr();
			expr.type = Type.getDescriptor(clazz);
			expr.expr = this;
			return expr;
		}

		public FunExpr checkCast(Class<?> clazz) {
			CheckCastFunExpr expr = new CheckCastFunExpr();
			expr.type = Type.getDescriptor(clazz);
			expr.expr = this;
			return expr;
		}

		public FunExpr field(String fieldName) {
			return Rethrow.reflectiveOperationException(() -> {
				Field field = clazz().getField(fieldName);
				return cast(field.getDeclaringClass()).field(fieldName, Type.getDescriptor(field.getType()));
			});
		}

		public FunExpr field(String fieldName, String type) {
			FieldFunExpr expr = new FieldFunExpr();
			expr.type = type;
			expr.field = fieldName;
			expr.object = this;
			return expr;
		}

		public FunExpr instanceOf(Class<?> clazz) {
			InstanceOfFunExpr expr = new InstanceOfFunExpr();
			expr.type = Type.getDescriptor(boolean.class);
			expr.instanceType = clazz;
			expr.object = this;
			return expr;
		}

		public FunExpr invoke(String methodName, FunExpr... parameters) {
			Method method = Rethrow.reflectiveOperationException(() -> {
				List<Class<?>> parameterTypes = new ArrayList<>();
				for (FunExpr parameter : parameters)
					parameterTypes.add(parameter.clazz());
				return clazz().getMethod(methodName, parameterTypes.toArray(new Class<?>[0]));
			});

			FunExpr cast = cast(method.getDeclaringClass());
			String returnType = Type.getDescriptor(method.getReturnType());

			if (method.getDeclaringClass().isInterface())
				return cast.invoke(Opcodes.INVOKEINTERFACE, methodName, returnType, parameters);
			else
				return cast.invoke(Opcodes.INVOKEVIRTUAL, methodName, returnType, parameters);
		}

		public FunExpr invoke(FunCreator<?> cc, FunExpr... parameters) {
			return invoke(Opcodes.INVOKEINTERFACE, cc.methodName, cc.returnType, parameters);
		}

		private FunExpr invoke(int opcode, String methodName, String returnType, FunExpr... parameters) {
			InvokeFunExpr expr = new InvokeFunExpr();
			expr.type = returnType;
			expr.methodName = methodName;
			expr.object = this;
			expr.opcode = opcode;
			expr.parameters = Arrays.asList(parameters);
			return expr;
		}

		private Class<?> clazz() throws ClassNotFoundException {
			return Class.forName(Type.getType(type).getClassName());
		}
	}

	public static class AssignFunExpr extends FunExpr {
		public int index;
		public FunExpr value;
		public FunExpr do_;
	}

	public static class BinaryFunExpr extends FunExpr {
		public int opcode;
		public FunExpr left, right;
	}

	public static class CastFunExpr extends FunExpr {
		public FunExpr expr;
	}

	public static class CheckCastFunExpr extends FunExpr {
		public FunExpr expr;
	}

	public static class ConstantFunExpr extends FunExpr {
		public Object constant; // primitives, class, handles etc.
	}

	public static class FieldFunExpr extends FunExpr {
		public FunExpr object;
		public String field;
	}

	public static class IfFunExpr extends FunExpr {
		public int ifInsn;
		public FunExpr then, else_;
	}

	public static class If1FunExpr extends IfFunExpr {
		public FunExpr if_;
	}

	public static class If2FunExpr extends IfFunExpr {
		public FunExpr left, right;
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

	public static class LocalFunExpr extends FunExpr {
		public int index;
	}

	public static class PrintlnFunExpr extends FunExpr {
		public FunExpr expression;
	}

	public static class SeqFunExpr extends FunExpr {
		public FunExpr left, right;
	}

	public static class StaticFunExpr extends FunExpr {
		public String clazzType;
		public String field;
	}

	public static class ThisFunExpr extends FunExpr {
	}

}
