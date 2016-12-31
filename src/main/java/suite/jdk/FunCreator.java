package suite.jdk;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
import suite.adt.Pair;
import suite.jdk.FunExpression.AssignFunExpr;
import suite.jdk.FunExpression.BinaryFunExpr;
import suite.jdk.FunExpression.CastFunExpr;
import suite.jdk.FunExpression.CheckCastFunExpr;
import suite.jdk.FunExpression.ConstantFunExpr;
import suite.jdk.FunExpression.FieldFunExpr;
import suite.jdk.FunExpression.FunExpr;
import suite.jdk.FunExpression.If1FunExpr;
import suite.jdk.FunExpression.If2FunExpr;
import suite.jdk.FunExpression.IfFunExpr;
import suite.jdk.FunExpression.InstanceOfFunExpr;
import suite.jdk.FunExpression.InvokeFunExpr;
import suite.jdk.FunExpression.ParameterFunExpr;
import suite.jdk.FunExpression.PrintlnFunExpr;
import suite.jdk.FunExpression.SeqFunExpr;
import suite.jdk.FunExpression.StaticFunExpr;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.Rethrow;
import suite.util.Util;

public class FunCreator<I> implements Opcodes {

	private static AtomicInteger counter = new AtomicInteger();

	public final Class<I> interfaceClass;
	public final Class<?> superClass;
	public final String className;
	public final String methodName;
	public final String returnType;
	public final List<Class<?>> parameterTypes;
	public final List<String> localTypes;

	private Map<String, Pair<String, Object>> constants;
	private Map<String, String> fields;
	private MethodCreator mc = new MethodCreator();

	public static <I> FunCreator<I> of(Class<I> ic) {
		return of(ic, new HashMap<>());
	}

	public static <I> FunCreator<I> of(Class<I> ic, String mn) {
		return of(ic, mn, new HashMap<>());
	}

	public static <I> FunCreator<I> of(Class<I> ic, Map<String, Class<?>> fs) {
		return of(ic, Read.from(ic.getDeclaredMethods()).uniqueResult().getName(), fs);
	}

	public static <I> FunCreator<I> of(Class<I> ic, String mn, Map<String, Class<?>> fs) {
		Method methods[] = Rethrow.reflectiveOperationException(() -> ic.getMethods());
		Method method = Read.from(methods).filter(m -> Util.stringEquals(m.getName(), mn)).uniqueResult();
		Class<?> rt = method.getReturnType();
		Class<?> pts[] = method.getParameterTypes();
		String rt1 = Type.getDescriptor(rt);
		List<Class<?>> pts1 = Arrays.asList(pts);
		return new FunCreator<>(ic, mn, rt1, pts1, fs);
	}

	private FunCreator(Class<I> ic, String mn, String rt, List<Class<?>> ps, Map<String, Class<?>> fs) {
		interfaceClass = ic;
		superClass = Object.class;
		className = interfaceClass.getSimpleName() + counter.getAndIncrement();
		methodName = mn;
		returnType = rt;
		parameterTypes = ps;
		localTypes = new ArrayList<>();
		constants = new HashMap<>();
		fields = Read.from2(fs).mapValue(Type::getDescriptor).toMap();
	}

	public Fun<Map<String, Object>, I> create(FunExpr expression) {
		Class<? extends I> clazz = Rethrow.ex(() -> create_(expression));

		return fields -> Rethrow.reflectiveOperationException(() -> {
			I t = clazz.newInstance();
			for (Entry<String, Object> entry : fields.entrySet())
				clazz.getDeclaredField(entry.getKey()).set(t, entry.getValue());
			return t;
		});

	}

	private Class<? extends I> create_(FunExpr expression) throws NoSuchMethodException {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		Type types[] = Read.from(parameterTypes).map(Type::getType).toList().toArray(new Type[0]);

		cw.visit(V1_8, //
				ACC_PUBLIC + ACC_SUPER, //
				className, //
				null, //
				Type.getInternalName(superClass), //
				new String[] { Type.getInternalName(interfaceClass), });

		for (Entry<String, Pair<String, Object>> entry : constants.entrySet())
			cw.visitField(ACC_PUBLIC | ACC_STATIC, entry.getKey(), entry.getValue().t0, null, null).visitEnd();

		for (Entry<String, String> entry : fields.entrySet())
			cw.visitField(ACC_PUBLIC, entry.getKey(), entry.getValue(), null, null).visitEnd();

		mc.create(cw, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false, mv -> {
			String cd = Type.getConstructorDescriptor(superClass.getConstructor());

			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(superClass), "<init>", cd, false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
		});

		mc.create(cw, methodName, Type.getMethodDescriptor(Type.getType(returnType), types), true, mv -> {
			visit(mv, expression);
			mv.visitInsn(choose(returnType, ARETURN, DRETURN, FRETURN, IRETURN, LRETURN));
			mv.visitMaxs(0, 1 + parameterTypes.size() + localTypes.size());
		});

		cw.visitEnd();

		byte bytes[] = cw.toByteArray();

		Class<? extends I> clazz = new UnsafeUtil().defineClass(interfaceClass, className, bytes);

		for (Entry<String, Pair<String, Object>> entry : constants.entrySet())
			try {
				clazz.getField(entry.getKey()).set(null, entry.getValue().t1);
			} catch (ReflectiveOperationException ex) {
				throw new RuntimeException(ex);
			}

		return clazz;
	}

	public FunExpr _true() {
		return constant(1);
	}

	public FunExpr _false() {
		return constant(0);
	}

	public FunExpr add(FunExpr e0, FunExpr e1) {
		return bi(e0, e1, choose(e0.type, 0, DADD, FADD, IADD, LADD));
	}

	public FunExpr bi(FunExpr e0, FunExpr e1, int opcode) {
		BinaryFunExpr expr = new BinaryFunExpr();
		expr.type = e0.type;
		expr.opcode = opcode;
		expr.left = e0;
		expr.right = e1;
		return expr;
	}

	public FunExpr constant(int i) {
		ConstantFunExpr expr = new ConstantFunExpr();
		expr.type = Type.getDescriptor(int.class);
		expr.constant = i;
		return expr;
	}

	public FunExpr constant(Object object) {
		return constantStatic(object, object != null ? object.getClass() : Object.class);
	}

	public FunExpr field(String field) {
		return this_().field(field, fields.get(field));
	}

	public FunExpr if_(FunExpr if_, FunExpr then_, FunExpr else_) {
		int ifInsn = Opcodes.IFEQ;

		If1FunExpr expr = new If1FunExpr();
		expr.type = then_.type;
		expr.ifInsn = ifInsn;
		expr.if_ = if_;
		expr.then = then_;
		expr.else_ = else_;
		return expr;
	}

	public FunExpr ifeq(FunExpr left, FunExpr right, FunExpr then_, FunExpr else_) {
		int ifInsn = !Util.stringEquals(left.type, Type.getDescriptor(int.class)) ? Opcodes.IF_ACMPNE
				: Opcodes.IF_ICMPNE;

		If2FunExpr expr = new If2FunExpr();
		expr.type = then_.type;
		expr.ifInsn = ifInsn;
		expr.left = left;
		expr.right = right;
		expr.then = then_;
		expr.else_ = else_;
		return expr;
	}

	public FunExpr ifInstance(Class<?> clazz, FunExpr object, Fun<FunExpr, FunExpr> then_, FunExpr else_) {
		return if_(object.instanceOf(clazz), local(object.checkCast(clazz), o_ -> then_.apply(o_)), else_);
	}

	public FunExpr local(FunExpr value, Fun<FunExpr, FunExpr> doFun) {
		int index = 1 + parameterTypes.size() + localTypes.size();
		localTypes.add(value.type);

		ParameterFunExpr pe = new ParameterFunExpr();
		pe.type = value.type;
		pe.index = index;

		FunExpr do_ = doFun.apply(pe);

		AssignFunExpr expr = new AssignFunExpr();
		expr.type = do_.type;
		expr.index = index;
		expr.value = value;
		expr.do_ = do_;
		return expr;
	}

	public FunExpr parameter(int number) { // 0 means this
		ParameterFunExpr expr = new ParameterFunExpr();
		expr.type = 0 < number ? Type.getDescriptor(parameterTypes.get(number - 1)) : className;
		expr.index = number;
		return expr;
	}

	public FunExpr seq(FunExpr e0, FunExpr e1) {
		SeqFunExpr expr = new SeqFunExpr();
		expr.type = e0.type;
		expr.left = e0;
		expr.right = e1;
		return expr;
	}

	public FunExpr this_() {
		return parameter(0);
	}

	private FunExpr constantStatic(Object object, Class<?> clazz) {
		String field = "f" + counter.getAndIncrement();
		String type = Type.getDescriptor(clazz);
		constants.put(field, Pair.of(type, object));

		StaticFunExpr expr = new StaticFunExpr();
		expr.clazzType = className;
		expr.field = field;
		expr.type = type;
		return expr;
	}

	private void visit(MethodVisitor mv, FunExpr e) {
		if (e instanceof AssignFunExpr) {
			AssignFunExpr expr = (AssignFunExpr) e;
			visit(mv, expr.value);
			mv.visitVarInsn(choose(expr.value.type, ASTORE, DSTORE, FSTORE, ISTORE, LSTORE), expr.index);
			visit(mv, expr.do_);
		} else if (e instanceof BinaryFunExpr) {
			BinaryFunExpr expr = (BinaryFunExpr) e;
			visit(mv, expr.left);
			visit(mv, expr.right);
			mv.visitInsn(expr.opcode);
		} else if (e instanceof CastFunExpr) {
			CastFunExpr expr = (CastFunExpr) e;
			visit(mv, expr.expr);
		} else if (e instanceof CheckCastFunExpr) {
			CheckCastFunExpr expr = (CheckCastFunExpr) e;
			visit(mv, expr.expr);
			mv.visitTypeInsn(CHECKCAST, expr.type);
		} else if (e instanceof ConstantFunExpr) {
			ConstantFunExpr expr = (ConstantFunExpr) e;
			mc.visitLdc(mv, expr);
		} else if (e instanceof FieldFunExpr) {
			FieldFunExpr expr = (FieldFunExpr) e;
			visit(mv, expr.object);
			mv.visitFieldInsn(GETFIELD, expr.object.type, expr.field, expr.type);
		} else if (e instanceof If1FunExpr) {
			If1FunExpr expr = (If1FunExpr) e;
			visit(mv, expr.if_);
			visitIf(mv, expr);
		} else if (e instanceof If2FunExpr) {
			If2FunExpr expr = (If2FunExpr) e;
			visit(mv, expr.left);
			visit(mv, expr.right);
			visitIf(mv, expr);
		} else if (e instanceof InstanceOfFunExpr) {
			InstanceOfFunExpr expr = (InstanceOfFunExpr) e;
			visit(mv, expr.object);
			mv.visitTypeInsn(INSTANCEOF, Type.getInternalName(expr.instanceType));
		} else if (e instanceof InvokeFunExpr) {
			InvokeFunExpr expr = (InvokeFunExpr) e;
			Type array[] = Read.from(expr.parameters) //
					.map(parameter -> Type.getType(parameter.type)) //
					.toList() //
					.toArray(new Type[0]);

			if (expr.object != null)
				visit(mv, expr.object);

			for (FunExpr parameter : expr.parameters)
				visit(mv, parameter);

			mv.visitMethodInsn( //
					expr.opcode, //
					expr.object.type, //
					expr.methodName, //
					Type.getMethodDescriptor(Type.getType(expr.type), array), //
					expr.opcode == Opcode.INVOKEINTERFACE);
		} else if (e instanceof ParameterFunExpr) {
			ParameterFunExpr expr = (ParameterFunExpr) e;
			mv.visitVarInsn(choose(expr.type, ALOAD, DLOAD, FLOAD, ILOAD, LLOAD), expr.index);
		} else if (e instanceof PrintlnFunExpr) {
			PrintlnFunExpr expr = (PrintlnFunExpr) e;
			String td = Type.getDescriptor(PrintStream.class);
			mv.visitFieldInsn(GETSTATIC, Type.getInternalName(System.class), "out", td);
			visit(mv, expr.expression);
			mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(PrintStream.class), "println",
					Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)), false);
		} else if (e instanceof SeqFunExpr) {
			SeqFunExpr expr = (SeqFunExpr) e;
			visit(mv, expr.left);
			if (!Util.stringEquals(expr.left.type, Type.getDescriptor(void.class)))
				mv.visitInsn(POP);
			visit(mv, expr.right);
		} else if (e instanceof StaticFunExpr) {
			StaticFunExpr expr = (StaticFunExpr) e;
			mv.visitFieldInsn(GETSTATIC, expr.clazzType, expr.field, expr.type);
		} else
			throw new RuntimeException("Unknown expression " + e.getClass());
	}

	private void visitIf(MethodVisitor mv, IfFunExpr expr) {
		Label l0 = new Label();
		Label l1 = new Label();
		mv.visitJumpInsn(expr.ifInsn, l0);
		visit(mv, expr.then);
		mv.visitJumpInsn(GOTO, l1);
		mv.visitLabel(l0);
		visit(mv, expr.else_);
		mv.visitLabel(l1);
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
