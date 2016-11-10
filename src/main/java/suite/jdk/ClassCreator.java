package suite.jdk;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import javassist.bytecode.Opcode;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.Rethrow;

public class ClassCreator implements Opcodes {

	public abstract class Expression {
		protected String clazz; // Type.getDescriptor()

		public Expression field(String field) {
			FieldExpression expr = new FieldExpression();
			expr.clazz = fields.get(field);
			expr.field = field;
			expr.object = this;
			return expr;
		}

		public Expression instanceOf() {
			InstanceOfExpression expr = new InstanceOfExpression();
			expr.instanceClazz = boolean.class;
			expr.object = this;
			return expr;
		}

		public Expression invoke(ClassCreator cc, Expression... parameters) {
			return invoke(cc.methodName, cc.className, parameters);
		}

		public Expression invoke(String methodName, Class<?> clazz, Expression... parameters) {
			return invoke(methodName, Type.getDescriptor(clazz), parameters);
		}

		private Expression invoke(String methodName, String className, Expression... parameters) {
			InvokeExpression expr = new InvokeExpression();
			expr.clazz = className;
			expr.methodName = methodName;
			expr.object = this;
			expr.opcode = INVOKEVIRTUAL;
			expr.parameters = Arrays.asList(parameters);
			return expr;
		}
	}

	public class BinaryExpression extends Expression {
		private int opcode;
		private Expression left, right;
	}

	public class ConstantExpression extends Expression {
		private Object constant;
	}

	public class FieldExpression extends Expression {
		private Expression object;
		private String field;
	}

	public class IfBooleanExpression extends Expression {
		private Expression if_, then, else_;
	}

	public class InstanceOfExpression extends Expression {
		private Expression object;
		private Class<?> instanceClazz;
	}

	public class InvokeExpression extends Expression {
		private int opcode;
		private String methodName;
		private Expression object;
		private List<Expression> parameters;
	}

	public class MethodParameterExpression extends Expression {
		private int number;
	}

	public class PrintlnExpression extends Expression {
		private Expression expression;
	}

	public class ThisExpression extends Expression {
	}

	private AtomicInteger counter = new AtomicInteger();
	private Map<String, String> fields;
	private List<String> parameters;

	@SuppressWarnings("rawtypes")
	private Class<Fun> interfaceClass = Fun.class;
	private Class<?> superClass = Object.class;
	private String className;
	private String methodName = "apply";

	public ClassCreator() {
		fields = new HashMap<>();
		parameters = Arrays.asList(Type.getDescriptor(Object.class));
	}

	public Object create(Expression expression) {
		className = "Fun" + counter.getAndIncrement();
		return Rethrow.ex(() -> create0(expression));
	}

	private Object create0(Expression expression) throws Exception {
		ClassWriter cw = new ClassWriter(0);

		List<Type> typeList = Read.from(parameters).map(Type::getType).toList();
		Type types[] = typeList.toArray(new Type[typeList.size()]);

		cw.visit(V1_8, //
				ACC_PUBLIC + ACC_SUPER, //
				className, //
				null, //
				Type.getInternalName(superClass), //
				new String[] { Type.getInternalName(interfaceClass), });

		for (Entry<String, String> entry : fields.entrySet())
			cw.visitField(ACC_PUBLIC, entry.getKey(), entry.getValue(), null, null).visitEnd();

		{
			MethodVisitor mv = cw.visitMethod( //
					ACC_PUBLIC, //
					"<init>", //
					Type.getMethodDescriptor(Type.VOID_TYPE), //
					null, //
					null);

			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(superClass), "<init>",
					Type.getConstructorDescriptor(superClass.getConstructor()), false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}

		{
			MethodVisitor mv = cw.visitMethod( //
					ACC_PUBLIC, //
					methodName, //
					Type.getMethodDescriptor(Type.getType(Object.class), types), //
					null, //
					null);

			visit(mv, expression);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(1 + parameters.size(), 1 + parameters.size());
			mv.visitEnd();
		}

		cw.visitEnd();

		byte bytes[] = cw.toByteArray();

		UnsafeUtil unsafeUtil = new UnsafeUtil();
		@SuppressWarnings("unchecked")
		Fun<Object, Object> fun = (Fun<Object, Object>) unsafeUtil.defineClass(interfaceClass, className, bytes).newInstance();
		return fun.apply("Hello");
	}

	public Expression constant(int i) {
		return constant(i, int.class);
	}

	public Expression constant(Object object) {
		return constant(object, object != null ? object.getClass() : Object.class);
	}

	private Expression constant(Object object, Class<?> clazz) {
		ConstantExpression expr = new ConstantExpression();
		expr.clazz = Type.getDescriptor(clazz);
		expr.constant = object;
		return expr;
	}

	public Expression parameter(int number) { // 0 means this
		MethodParameterExpression expr = new MethodParameterExpression();
		expr.clazz = 0 < expr.number ? parameters.get(expr.number - 1) : className;
		expr.number = number;
		return expr;
	}

	public Expression this_() {
		return parameter(0);
	}

	private void visit(MethodVisitor mv, Expression e) {
		if (e instanceof BinaryExpression) {
			BinaryExpression expr = (BinaryExpression) e;
			visit(mv, expr.left);
			visit(mv, expr.right);
			mv.visitInsn(expr.opcode);
		} else if (e instanceof ConstantExpression) {
			ConstantExpression expr = (ConstantExpression) e;
			mv.visitLdcInsn(expr.constant);
		} else if (e instanceof FieldExpression) {
			FieldExpression expr = (FieldExpression) e;
			visit(mv, expr.object);
			mv.visitFieldInsn(GETFIELD, className, expr.field, expr.clazz);
		} else if (e instanceof IfBooleanExpression) {
			IfBooleanExpression expr = (IfBooleanExpression) e;
			Label l0 = new Label();
			Label l1 = new Label();
			visit(mv, expr.if_);
			mv.visitJumpInsn(IFEQ, l0);
			visit(mv, expr.then);
			mv.visitLabel(l0);
			visit(mv, expr.else_);
			mv.visitLabel(l1);
		} else if (e instanceof InstanceOfExpression) {
			InstanceOfExpression expr = (InstanceOfExpression) e;
			visit(mv, expr.object);
			mv.visitTypeInsn(INSTANCEOF, Type.getInternalName(expr.instanceClazz));
		} else if (e instanceof InvokeExpression) {
			InvokeExpression expr = (InvokeExpression) e;
			if (expr.object != null)
				visit(mv, expr.object);
			for (Expression parameter : expr.parameters)
				visit(mv, parameter);
			List<Type> types = Read.from(expr.parameters).map(parameter -> Type.getType(parameter.clazz)).toList();
			Type array[] = types.toArray(new Type[types.size()]);
			mv.visitMethodInsn( //
					expr.opcode, //
					expr.object.clazz, //
					expr.methodName, //
					Type.getMethodDescriptor(Type.getType(e.clazz), array), //
					expr.opcode == Opcode.INVOKEINTERFACE);
		} else if (e instanceof MethodParameterExpression) {
			MethodParameterExpression expr = (MethodParameterExpression) e;
			mv.visitVarInsn(ALOAD, expr.number);
		} else if (e instanceof PrintlnExpression) {
			PrintlnExpression expr = (PrintlnExpression) e;
			mv.visitFieldInsn(GETSTATIC, Type.getInternalName(System.class), "out", Type.getDescriptor(PrintStream.class));
			visit(mv, expr.expression);
			mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(PrintStream.class), "println",
					Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)), false);
		} else
			throw new RuntimeException("Unknown expression " + e.getClass());
	}

}
