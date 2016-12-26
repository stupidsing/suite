package suite.jdk;

import java.io.PrintStream;
import java.lang.reflect.Method;
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
import suite.jdk.FunExpression.BinaryFunExpr;
import suite.jdk.FunExpression.ConstantFunExpr;
import suite.jdk.FunExpression.DoFunExpr;
import suite.jdk.FunExpression.FieldFunExpr;
import suite.jdk.FunExpression.FunExpr;
import suite.jdk.FunExpression.IfBooleanFunExpr;
import suite.jdk.FunExpression.InstanceOfFunExpr;
import suite.jdk.FunExpression.InvokeFunExpr;
import suite.jdk.FunExpression.ParameterFunExpr;
import suite.jdk.FunExpression.PrintlnFunExpr;
import suite.streamlet.Read;
import suite.util.Rethrow;
import suite.util.Util;

public class FunCreator<I> implements Opcodes {

	private static AtomicInteger counter = new AtomicInteger();

	public final Class<I> interfaceClass;
	public final Class<?> superClass;
	public final String className;
	public final Map<String, String> fields;
	public final String methodName;
	public final String returnType;
	public final List<String> parameters;

	private Class<? extends I> clazz;

	public static <I> FunCreator<I> of(Class<I> ic, String mn) {
		return of(ic, mn, new HashMap<>());
	}

	public static <I> FunCreator<I> of(Class<I> ic, String mn, Map<String, String> fs) {
		Method methods[] = Rethrow.reflectiveOperationException(() -> ic.getMethods());
		Method method = Read.from(methods).filter(m -> Util.stringEquals(m.getName(), mn)).uniqueResult();
		String rt = Type.getDescriptor(method.getReturnType());
		List<String> ps = Read.from(method.getParameterTypes()).map(Type::getDescriptor).toList();
		return new FunCreator<>(ic, mn, rt, ps, fs);
	}

	private FunCreator(Class<I> ic, String mn, String rt, List<String> ps, Map<String, String> fs) {
		interfaceClass = ic;
		superClass = Object.class;
		className = interfaceClass.getSimpleName() + counter.getAndIncrement();
		fields = fs;
		methodName = mn;
		returnType = rt;
		parameters = ps;
	}

	public void create(FunExpr expression) {
		clazz = Rethrow.ex(() -> create_(expression));
	}

	private Class<? extends I> create_(FunExpr expression) throws NoSuchMethodException {
		ClassWriter cw = new ClassWriter(0);

		List<Type> typeList = Read.from(parameters).map(Type::getType).toList();
		Type types[] = typeList.toArray(new Type[0]);

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

	public FunExpr add(FunExpr e0, FunExpr e1) {
		BinaryFunExpr expr = new BinaryFunExpr();
		expr.type = e0.type;
		expr.opcode = choose(expr.type, 0, DADD, FADD, IADD, LADD);
		expr.left = e0;
		expr.right = e1;
		return expr;
	}

	public FunExpr constant(int i) {
		return constant(i, int.class);
	}

	public FunExpr constant(Object object) {
		return constant(object, object != null ? object.getClass() : Object.class);
	}

	private FunExpr constant(Object object, Class<?> clazz) {
		ConstantFunExpr expr = new ConstantFunExpr();
		expr.type = Type.getDescriptor(clazz);
		expr.constant = object;
		return expr;
	}

	public FunExpr do_(FunExpr e0, FunExpr e1) {
		DoFunExpr expr = new DoFunExpr();
		expr.type = e0.type;
		expr.left = e0;
		expr.right = e1;
		return expr;
	}

	public FunExpr field(String field) {
		return this_().field(field, fields.get(field));
	}

	public FunExpr parameter(int number) { // 0 means this
		ParameterFunExpr expr = new ParameterFunExpr();
		expr.type = 0 < number ? parameters.get(number - 1) : className;
		expr.number = number;
		return expr;
	}

	public FunExpr this_() {
		return parameter(0);
	}

	public I instantiate() {
		return Rethrow.ex(clazz::newInstance);
	}

	public Class<? extends I> get() {
		return clazz;
	}

	private void visit(MethodVisitor mv, FunExpr e) {
		if (e instanceof BinaryFunExpr) {
			BinaryFunExpr expr = (BinaryFunExpr) e;
			visit(mv, expr.left);
			visit(mv, expr.right);
			mv.visitInsn(expr.opcode);
		} else if (e instanceof ConstantFunExpr) {
			ConstantFunExpr expr = (ConstantFunExpr) e;
			mv.visitLdcInsn(expr.constant);
		} else if (e instanceof DoFunExpr) {
			DoFunExpr expr = (DoFunExpr) e;
			visit(mv, expr.left);
			mv.visitInsn(POP);
			visit(mv, expr.right);
		} else if (e instanceof FieldFunExpr) {
			FieldFunExpr expr = (FieldFunExpr) e;
			visit(mv, expr.object);
			mv.visitFieldInsn(GETFIELD, className, expr.field, expr.type);
		} else if (e instanceof IfBooleanFunExpr) {
			IfBooleanFunExpr expr = (IfBooleanFunExpr) e;
			Label l0 = new Label();
			Label l1 = new Label();
			visit(mv, expr.if_);
			mv.visitJumpInsn(IFEQ, l0);
			visit(mv, expr.then);
			mv.visitLabel(l0);
			visit(mv, expr.else_);
			mv.visitLabel(l1);
		} else if (e instanceof InstanceOfFunExpr) {
			InstanceOfFunExpr expr = (InstanceOfFunExpr) e;
			visit(mv, expr.object);
			mv.visitTypeInsn(INSTANCEOF, Type.getInternalName(expr.instanceType));
		} else if (e instanceof InvokeFunExpr) {
			InvokeFunExpr expr = (InvokeFunExpr) e;
			if (expr.object != null)
				visit(mv, expr.object);
			for (FunExpr parameter : expr.parameters)
				visit(mv, parameter);
			List<Type> types = Read.from(expr.parameters).map(parameter -> Type.getType(parameter.type)).toList();
			Type array[] = types.toArray(new Type[0]);
			mv.visitMethodInsn( //
					expr.opcode, //
					expr.object.type, //
					expr.methodName, //
					Type.getMethodDescriptor(Type.getType(expr.type), array), //
					expr.opcode == Opcode.INVOKEINTERFACE);
		} else if (e instanceof ParameterFunExpr) {
			ParameterFunExpr expr = (ParameterFunExpr) e;
			mv.visitVarInsn(choose(expr.type, ALOAD, DLOAD, FLOAD, ILOAD, LLOAD), expr.number);
		} else if (e instanceof PrintlnFunExpr) {
			PrintlnFunExpr expr = (PrintlnFunExpr) e;
			mv.visitFieldInsn(GETSTATIC, Type.getInternalName(System.class), "out",
					Type.getDescriptor(PrintStream.class));
			visit(mv, expr.expression);
			mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(PrintStream.class), "println",
					Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)), false);
		} else
			throw new RuntimeException("Unknown expression " + e.getClass());
	}

	private int choose(String type, int a, int d, int f, int i, int l) {
		if (Util.stringEquals(type, Type.getDescriptor(double.class)))
			return d;
		else if (Util.stringEquals(type, Type.getDescriptor(boolean.class)))
			return i;
		else if (Util.stringEquals(type, Type.getDescriptor(float.class)))
			return f;
		else if (Util.stringEquals(type, Type.getDescriptor(int.class)))
			return i;
		else if (Util.stringEquals(type, Type.getDescriptor(long.class)))
			return l;
		else
			return a;
	}

}
