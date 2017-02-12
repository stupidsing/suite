package suite.jdk.gen;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import suite.adt.Pair;
import suite.jdk.UnsafeUtil;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunExpression.StaticFunExpr;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.Rethrow;
import suite.util.Util;

public class FunCreator<I> extends FunConstructor implements Opcodes {

	private static AtomicInteger counter = new AtomicInteger();

	public final Class<I> interfaceClass;
	public final Class<?> superClass;
	public final String className;
	public final String methodName;
	public final Type returnType;
	public final List<Type> parameterTypes;

	private Map<String, Pair<Type, Object>> constants;
	private Map<String, Type> fields;
	private Method_ mc;

	public static <I> FunCreator<I> of(Class<I> ic) {
		return of(ic, Collections.emptyMap());
	}

	public static <I> FunCreator<I> of(Class<I> ic, String mn) {
		return of(ic, mn, Collections.emptyMap());
	}

	public static <I> FunCreator<I> of(Class<I> ic, Map<String, Type> fs) {
		return of(ic, Read.each(ic.getDeclaredMethods()).uniqueResult().getName(), fs);
	}

	public static <I> FunCreator<I> of(Class<I> ic, String mn, Map<String, Type> fs) {
		Method methods[] = Rethrow.reflectiveOperationException(() -> ic.getMethods());
		Method method = Read.each(methods).filter(m -> Util.stringEquals(m.getName(), mn)).uniqueResult();
		Type rt = Type.getType(method.getReturnType());
		List<Type> pts = Read.each(method.getParameterTypes()).map(Type::getType).toList();
		return new FunCreator<>(ic, mn, rt, pts, fs);
	}

	private FunCreator(Class<I> ic, String mn, Type rt, List<Type> ps, Map<String, Type> fs) {
		interfaceClass = ic;
		superClass = Object.class;
		className = interfaceClass.getSimpleName() + counter.getAndIncrement();
		methodName = mn;
		returnType = rt;
		parameterTypes = ps;

		constants = new HashMap<>();
		fields = fs;
		mc = new Method_();
	}

	public Fun<Map<String, Object>, I> create(FunExpr expr0) {
		List<Type> localTypes = new ArrayList<>();
		localTypes.add(Type.getObjectType(className));
		localTypes.addAll(parameterTypes);

		FunExpand fe = new FunExpand();
		FunTypeInformation ft = new FunTypeInformation();
		FunRewrite fr = new FunRewrite(ft, localTypes);
		FunGenerateBytecode fgb = new FunGenerateBytecode(ft, mc);

		FunExpr expr1 = fe.expand(expr0, 0);
		FunExpr expr2 = fr.rewrite(expr1.cast(interfaceClass));
		Class<? extends I> clazz = Rethrow.reflectiveOperationException(() -> create(fgb, expr2));

		return fields -> Rethrow.reflectiveOperationException(() -> {
			I t = clazz.newInstance();
			for (Entry<String, Object> entry : fields.entrySet())
				clazz.getDeclaredField(entry.getKey()).set(t, entry.getValue());
			return t;
		});
	}

	private Class<? extends I> create(FunGenerateBytecode fgb, FunExpr expression) throws NoSuchMethodException {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		Type types[] = parameterTypes.toArray(new Type[0]);

		cw.visit(V1_8, //
				ACC_PUBLIC | ACC_SUPER, //
				className, //
				null, //
				Type.getInternalName(superClass), //
				new String[] { Type.getInternalName(interfaceClass), });

		for (Entry<String, Pair<Type, Object>> entry : constants.entrySet())
			cw.visitField(ACC_PUBLIC | ACC_STATIC, entry.getKey(), entry.getValue().t0.getDescriptor(), null, null).visitEnd();

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
			fgb.visit(mv, expression);
			mv.visitInsn(Type_.choose(returnType, ARETURN, DRETURN, FRETURN, IRETURN, LRETURN));
			mv.visitMaxs(0, 0); // localTypes.size()
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

	public FunExpr constant(Object object) {
		return constantStatic(object, object != null ? object.getClass() : Object.class);
	}

	public FunExpr field(String field) {
		return this_().field(field, fields.get(field));
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

}
