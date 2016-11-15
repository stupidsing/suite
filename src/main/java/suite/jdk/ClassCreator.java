package suite.jdk;

import java.io.PrintStream;
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
import suite.jdk.ClassCreatorExpression.BinaryExpression;
import suite.jdk.ClassCreatorExpression.ConstantExpression;
import suite.jdk.ClassCreatorExpression.Expression;
import suite.jdk.ClassCreatorExpression.FieldExpression;
import suite.jdk.ClassCreatorExpression.IfBooleanExpression;
import suite.jdk.ClassCreatorExpression.InstanceOfExpression;
import suite.jdk.ClassCreatorExpression.InvokeExpression;
import suite.jdk.ClassCreatorExpression.ParameterExpression;
import suite.jdk.ClassCreatorExpression.PrintlnExpression;
import suite.streamlet.Read;
import suite.util.Rethrow;
import suite.util.Util;

public class ClassCreator<I> implements Opcodes {

	private static AtomicInteger counter = new AtomicInteger();

	public final Class<I> interfaceClass;
	public final Class<?> superClass;
	public final String className;
	public final Map<String, String> fields;
	public final String methodName;
	public final String returnType;
	public final List<String> parameters;

	public ClassCreator(Class<I> ic, String mn, String rt, List<String> ps) {
		this(ic, mn, rt, ps, new HashMap<>());
	}

	public ClassCreator(Class<I> ic, String mn, String rt, List<String> ps, Map<String, String> fs) {
		interfaceClass = ic;
		superClass = Object.class;
		className = interfaceClass.getSimpleName() + counter.getAndIncrement();
		fields = fs;
		methodName = mn;
		returnType = rt;
		parameters = ps;
	}

	public I create(Expression expression) {
		return Rethrow.ex(() -> clazz(expression).newInstance());
	}

	public Class<? extends I> clazz(Expression expression) {
		return Rethrow.ex(() -> clazz_(expression));
	}

	private Class<? extends I> clazz_(Expression expression) throws NoSuchMethodException {
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
					Type.getMethodDescriptor(Type.getType(returnType), types), //
					null, //
					null);

			visit(mv, expression);
			mv.visitInsn(choose(returnType, ARETURN, DRETURN, FRETURN, IRETURN, LRETURN));
			mv.visitMaxs(1 + parameters.size(), 1 + parameters.size());
			mv.visitEnd();
		}

		cw.visitEnd();

		byte bytes[] = cw.toByteArray();

		return new UnsafeUtil().defineClass(interfaceClass, className, bytes);
	}

	public Expression add(Expression e0, Expression e1) {
		BinaryExpression expr = new BinaryExpression();
		expr.type = e0.type;
		expr.opcode = choose(expr.type, 0, DADD, FADD, IADD, LADD);
		expr.left = e0;
		expr.right = e1;
		return expr;
	}

	public Expression constant(int i) {
		return constant(i, int.class);
	}

	public Expression constant(Object object) {
		return constant(object, object != null ? object.getClass() : Object.class);
	}

	private Expression constant(Object object, Class<?> clazz) {
		ConstantExpression expr = new ConstantExpression();
		expr.type = Type.getDescriptor(clazz);
		expr.constant = object;
		return expr;
	}

	public Expression field(String field) {
		return this_().field(field, fields.get(field));
	}

	public Expression parameter(int number) { // 0 means this
		ParameterExpression expr = new ParameterExpression();
		expr.type = 0 < number ? parameters.get(number - 1) : className;
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
			mv.visitFieldInsn(GETFIELD, className, expr.field, expr.type);
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
			mv.visitTypeInsn(INSTANCEOF, Type.getInternalName(expr.instanceType));
		} else if (e instanceof InvokeExpression) {
			InvokeExpression expr = (InvokeExpression) e;
			if (expr.object != null)
				visit(mv, expr.object);
			for (Expression parameter : expr.parameters)
				visit(mv, parameter);
			List<Type> types = Read.from(expr.parameters).map(parameter -> Type.getType(parameter.type)).toList();
			Type array[] = types.toArray(new Type[types.size()]);
			mv.visitMethodInsn( //
					expr.opcode, //
					expr.object.type, //
					expr.methodName, //
					Type.getMethodDescriptor(Type.getType(expr.type), array), //
					expr.opcode == Opcode.INVOKEINTERFACE);
		} else if (e instanceof ParameterExpression) {
			ParameterExpression expr = (ParameterExpression) e;
			mv.visitVarInsn(choose(expr.type, ALOAD, DLOAD, FLOAD, ILOAD, LLOAD), expr.number);
		} else if (e instanceof PrintlnExpression) {
			PrintlnExpression expr = (PrintlnExpression) e;
			mv.visitFieldInsn(GETSTATIC, Type.getInternalName(System.class), "out", Type.getDescriptor(PrintStream.class));
			visit(mv, expr.expression);
			mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(PrintStream.class), "println",
					Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)), false);
		} else
			throw new RuntimeException("Unknown expression " + e.getClass());
	}

	private int choose(String type, int a, int d, int f, int i, int l) {
		if (Util.stringEquals(type, Type.getDescriptor(double.class)))
			return d;
		else if (Util.stringEquals(type, Type.getDescriptor(float.class)))
			return f;
		else if (Util.stringEquals(type, Type.getDescriptor(boolean.class))
				|| Util.stringEquals(type, Type.getDescriptor(int.class)))
			return i;
		else if (Util.stringEquals(type, Type.getDescriptor(long.class)))
			return l;
		else
			return a;
	}

}
