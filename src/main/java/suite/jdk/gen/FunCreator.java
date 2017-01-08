package suite.jdk.gen;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import javassist.bytecode.Opcode;
import suite.adt.Pair;
import suite.jdk.UnsafeUtil;
import suite.jdk.gen.FunExpression.AssignFunExpr;
import suite.jdk.gen.FunExpression.BinaryFunExpr;
import suite.jdk.gen.FunExpression.CastFunExpr;
import suite.jdk.gen.FunExpression.CheckCastFunExpr;
import suite.jdk.gen.FunExpression.ConstantFunExpr;
import suite.jdk.gen.FunExpression.Declare1ParameterFunExpr;
import suite.jdk.gen.FunExpression.Declare2ParameterFunExpr;
import suite.jdk.gen.FunExpression.DeclareLocalFunExpr;
import suite.jdk.gen.FunExpression.FieldFunExpr;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunExpression.If1FunExpr;
import suite.jdk.gen.FunExpression.If2FunExpr;
import suite.jdk.gen.FunExpression.IfFunExpr;
import suite.jdk.gen.FunExpression.InstanceOfFunExpr;
import suite.jdk.gen.FunExpression.InvokeFunExpr;
import suite.jdk.gen.FunExpression.LocalFunExpr;
import suite.jdk.gen.FunExpression.NoOperationFunExpr;
import suite.jdk.gen.FunExpression.PrintlnFunExpr;
import suite.jdk.gen.FunExpression.SeqFunExpr;
import suite.jdk.gen.FunExpression.StaticFunExpr;
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
	public final Type returnType;
	public final List<Type> parameterTypes;
	public final List<Type> localTypes;

	private Map<String, Pair<Type, Object>> constants;
	private Map<String, Type> fields;
	private MethodCreator mc;
	private FunExpression fe;

	public static <I> FunCreator<I> of(Class<I> ic) {
		return of(ic, new HashMap<>());
	}

	public static <I> FunCreator<I> of(Class<I> ic, String mn) {
		return of(ic, mn, new HashMap<>());
	}

	public static <I> FunCreator<I> of(Class<I> ic, Map<String, Type> fs) {
		return of(ic, Read.from(ic.getDeclaredMethods()).uniqueResult().getName(), fs);
	}

	public static <I> FunCreator<I> of(Class<I> ic, String mn, Map<String, Type> fs) {
		Method methods[] = Rethrow.reflectiveOperationException(() -> ic.getMethods());
		Method method = Read.from(methods).filter(m -> Util.stringEquals(m.getName(), mn)).uniqueResult();
		Type rt = Type.getType(method.getReturnType());
		List<Type> pts = Read.from(method.getParameterTypes()).map(Type::getType).toList();
		return new FunCreator<>(ic, mn, rt, pts, fs);
	}

	private FunCreator(Class<I> ic, String mn, Type rt, List<Type> ps, Map<String, Type> fs) {
		interfaceClass = ic;
		superClass = Object.class;
		className = interfaceClass.getSimpleName() + counter.getAndIncrement();
		methodName = mn;
		returnType = rt;
		parameterTypes = ps;

		localTypes = new ArrayList<>();
		localTypes.add(Type.getObjectType(className));
		localTypes.addAll(parameterTypes);

		constants = new HashMap<>();
		fields = fs;
		mc = new MethodCreator();
		fe = new FunExpression(this);
	}

	public Fun<Map<String, Object>, I> create(FunExpr expr0) {
		FunExpr expr1 = new FunRewriter(fe).rewrite(expr0.cast(interfaceClass));
		Class<? extends I> clazz = Rethrow.reflectiveOperationException(() -> create_(expr1));

		return fields -> Rethrow.reflectiveOperationException(() -> {
			I t = clazz.newInstance();
			for (Entry<String, Object> entry : fields.entrySet())
				clazz.getDeclaredField(entry.getKey()).set(t, entry.getValue());
			return t;
		});
	}

	private Class<? extends I> create_(FunExpr expression) throws NoSuchMethodException {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		Type types[] = parameterTypes.toArray(new Type[0]);

		cw.visit(V1_8, //
				ACC_PUBLIC | ACC_SUPER, //
				className, //
				null, //
				Type.getInternalName(superClass), //
				new String[] { Type.getInternalName(interfaceClass), });

		for (Entry<String, Pair<Type, Object>> entry : constants.entrySet())
			cw.visitField(ACC_PUBLIC | ACC_STATIC, entry.getKey(), entry.getValue().t0.getDescriptor(), null, null)
					.visitEnd();

		for (Entry<String, Type> entry : fields.entrySet())
			cw.visitField(ACC_PUBLIC, entry.getKey(), entry.getValue().getDescriptor(), null, null).visitEnd();

		mc.create(cw, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), mv -> {
			String cd = Type.getConstructorDescriptor(superClass.getConstructor());

			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(superClass), "<init>", cd, false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
		});

		mc.create(cw, methodName, Type.getMethodDescriptor(returnType, types), mv -> {
			visit(mv, expression);
			mv.visitInsn(choose(returnType, ARETURN, DRETURN, FRETURN, IRETURN, LRETURN));
			mv.visitMaxs(0, localTypes.size());
		});

		cw.visitEnd();

		byte bytes[] = cw.toByteArray();

		Class<? extends I> clazz = new UnsafeUtil().defineClass(interfaceClass, className, bytes);

		for (Entry<String, Pair<Type, Object>> entry : constants.entrySet())
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
		return bi(e0, e1, choose(FunType.typeOf(e0), 0, DADD, FADD, IADD, LADD));
	}

	public FunExpr bi(FunExpr e0, FunExpr e1, int opcode) {
		BinaryFunExpr expr = fe.new BinaryFunExpr();
		expr.opcode = opcode;
		expr.left = e0;
		expr.right = e1;
		return expr;
	}

	public FunExpr constant(int i) {
		ConstantFunExpr expr = fe.new ConstantFunExpr();
		expr.type = Type.INT_TYPE;
		expr.constant = i;
		return expr;
	}

	public FunExpr constant(Object object) {
		return constantStatic(object, object != null ? object.getClass() : Object.class);
	}

	public FunExpr declare(FunExpr value, Fun<FunExpr, FunExpr> doFun) {
		DeclareLocalFunExpr expr = fe.new DeclareLocalFunExpr();
		expr.value = value;
		expr.doFun = doFun;
		return expr;
	}

	public FunExpr field(String field) {
		return this_().field(field, fields.get(field));
	}

	public FunExpr if_(FunExpr if_, FunExpr then_, FunExpr else_) {
		int ifInsn = Opcodes.IFEQ;

		If1FunExpr expr = fe.new If1FunExpr();
		expr.ifInsn = ifInsn;
		expr.if_ = if_;
		expr.then = then_;
		expr.else_ = else_;
		return expr;
	}

	public FunExpr ifeq(FunExpr left, FunExpr right, FunExpr then_, FunExpr else_) {
		int ifInsn = !Objects.equals(FunType.typeOf(left), Type.INT_TYPE) ? Opcodes.IF_ACMPNE : Opcodes.IF_ICMPNE;

		If2FunExpr expr = fe.new If2FunExpr();
		expr.ifInsn = ifInsn;
		expr.left = left;
		expr.right = right;
		expr.then = then_;
		expr.else_ = else_;
		return expr;
	}

	public FunExpr ifInstance(Class<?> clazz, FunExpr object, Fun<FunExpr, FunExpr> then_, FunExpr else_) {
		return if_(object.instanceOf(clazz), declare(object.checkCast(clazz), o_ -> then_.apply(o_)), else_);
	}

	public FunExpr local(int number, Class<?> clazz) {
		return local(number, Type.getType(clazz));
	}

	public FunExpr local(int number, Type type) { // 0 means this
		LocalFunExpr expr = fe.new LocalFunExpr();
		expr.type = type;
		expr.index = number;
		return expr;
	}

	public FunExpr parameter(Fun<FunExpr, FunExpr> doFun) {
		Declare1ParameterFunExpr expr = fe.new Declare1ParameterFunExpr();
		expr.doFun = doFun;
		return expr;
	}

	public FunExpr parameter2(BiFunction<FunExpr, FunExpr, FunExpr> doFun) {
		Declare2ParameterFunExpr expr = fe.new Declare2ParameterFunExpr();
		expr.doFun = doFun;
		return expr;
	}

	public FunExpr seq(FunExpr e0, FunExpr e1) {
		SeqFunExpr expr = fe.new SeqFunExpr();
		expr.left = e0;
		expr.right = e1;
		return expr;
	}

	public FunExpr this_() {
		return local(0, Type.getObjectType(className));
	}

	private FunExpr constantStatic(Object object, Class<?> clazz) {
		String field = "f" + counter.getAndIncrement();
		Type type = Type.getType(clazz);
		constants.put(field, Pair.of(type, object));

		StaticFunExpr expr = fe.new StaticFunExpr();
		expr.clazzType = className;
		expr.field = field;
		expr.type = type;
		return expr;
	}

	private void visit(MethodVisitor mv, FunExpr e) {
		if (e instanceof AssignFunExpr) {
			AssignFunExpr expr = (AssignFunExpr) e;
			visit(mv, expr.value);
			mv.visitVarInsn(choose(FunType.typeOf(expr.value), ASTORE, DSTORE, FSTORE, ISTORE, LSTORE), expr.index);
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
			mv.visitTypeInsn(CHECKCAST, expr.type.getDescriptor());
		} else if (e instanceof ConstantFunExpr) {
			ConstantFunExpr expr = (ConstantFunExpr) e;
			mc.visitLdc(mv, expr);
		} else if (e instanceof FieldFunExpr) {
			FieldFunExpr expr = (FieldFunExpr) e;
			visit(mv, expr.object);
			mv.visitFieldInsn(GETFIELD, FunType.typeDesc(expr.object), expr.field, FunType.typeDesc(expr));
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
					.map(FunType::typeOf) //
					.toList() //
					.toArray(new Type[0]);

			if (expr.object != null)
				visit(mv, expr.object);

			for (FunExpr parameter : expr.parameters)
				visit(mv, parameter);

			mv.visitMethodInsn( //
					expr.opcode, //
					FunType.typeDesc(expr.object), //
					expr.methodName, //
					Type.getMethodDescriptor(expr.type, array), //
					expr.opcode == Opcode.INVOKEINTERFACE);
		} else if (e instanceof LocalFunExpr) {
			LocalFunExpr expr = (LocalFunExpr) e;
			mv.visitVarInsn(choose(FunType.typeOf(expr), ALOAD, DLOAD, FLOAD, ILOAD, LLOAD), expr.index);
		} else if (e instanceof NoOperationFunExpr)
			;
		else if (e instanceof PrintlnFunExpr) {
			PrintlnFunExpr expr = (PrintlnFunExpr) e;
			String td = Type.getDescriptor(PrintStream.class);
			mv.visitFieldInsn(GETSTATIC, Type.getInternalName(System.class), "out", td);
			visit(mv, expr.expression);
			mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(PrintStream.class), "println",
					Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)), false);
		} else if (e instanceof SeqFunExpr) {
			SeqFunExpr expr = (SeqFunExpr) e;
			visit(mv, expr.left);
			if (!Objects.equals(FunType.typeOf(expr.left), Type.VOID_TYPE))
				mv.visitInsn(POP);
			visit(mv, expr.right);
		} else if (e instanceof StaticFunExpr) {
			StaticFunExpr expr = (StaticFunExpr) e;
			mv.visitFieldInsn(GETSTATIC, expr.clazzType, expr.field, expr.type.getDescriptor());
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

	private int choose(Type type, int a, int d, int f, int i, int l) {
		if (Objects.equals(type, Type.DOUBLE_TYPE))
			return d;
		else if (Objects.equals(type, Type.BOOLEAN_TYPE))
			return i;
		else if (Objects.equals(type, Type.FLOAT_TYPE))
			return f;
		else if (Objects.equals(type, Type.INT_TYPE))
			return i;
		else if (Objects.equals(type, Type.LONG_TYPE))
			return l;
		else
			return a;
	}

}
