package suite.jdk.gen;

import static org.apache.bcel.Const.ACC_PUBLIC;
import static org.apache.bcel.Const.ACC_STATIC;
import static org.apache.bcel.Const.ACC_SUPER;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.bcel.Const;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldGen;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import suite.adt.Pair;
import suite.jdk.UnsafeUtil;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunExpression.StaticFunExpr;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.Rethrow;
import suite.util.Util;

public class FunCreator<I> extends FunConstructor {

	private static AtomicInteger counter = new AtomicInteger();

	public final Class<I> interfaceClass;
	public final Class<?> superClass;
	public final String className;
	public final String methodName;
	public final Type returnType;
	public final List<Type> parameterTypes;

	private Map<String, Pair<Type, Object>> constants;
	private Map<String, Type> fields;

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
	}

	public Fun<Map<String, Object>, I> create(FunExpr expr0) {
		List<Type> localTypes = new ArrayList<>();
		localTypes.add(ObjectType.getInstance(className));
		localTypes.addAll(parameterTypes);

		FunExpand fe = new FunExpand();
		FunTypeInformation fti = new FunTypeInformation();
		FunRewrite fr = new FunRewrite(fti, localTypes);

		FunExpr expr1 = fe.expand(expr0, 0);
		FunExpr expr2 = fr.rewrite(expr1.cast(interfaceClass));
		Class<? extends I> clazz = Rethrow.reflectiveOperationException(() -> create0(fti, expr2));

		return fields -> Rethrow.reflectiveOperationException(() -> {
			I t = clazz.newInstance();
			for (Entry<String, Object> entry : fields.entrySet())
				clazz.getDeclaredField(entry.getKey()).set(t, entry.getValue());
			return t;
		});
	}

	private Class<? extends I> create0(FunTypeInformation fti, FunExpr expression) throws NoSuchMethodException {
		String ifs[] = new String[] { interfaceClass.getName(), };
		ConstantPoolGen cp = new ConstantPoolGen();
		FunGenerateBytecode fgb = new FunGenerateBytecode(fti, cp);
		ClassGen cg = new ClassGen(className, superClass.getName(), ".java", ACC_PUBLIC | ACC_SUPER, ifs, cp);
		InstructionFactory factory = new InstructionFactory(cg);
		org.apache.bcel.classfile.Method m0, m1;

		for (Entry<String, Pair<Type, Object>> entry : constants.entrySet())
			cg.addField(new FieldGen(ACC_PUBLIC | ACC_STATIC, entry.getValue().t0, entry.getKey(), cp).getField());

		for (Entry<String, Type> entry : fields.entrySet())
			cg.addField(new FieldGen(ACC_PUBLIC, entry.getValue(), entry.getKey(), cp).getField());

		{
			InstructionList il = new InstructionList();
			try {
				il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
				il.append(factory.createInvoke(superClass.getName(), "<init>", Type.VOID, Type.NO_ARGS, Const.INVOKESPECIAL));
				il.append(InstructionFactory.createReturn(Type.VOID));

				MethodGen mg0 = new MethodGen(ACC_PUBLIC, Type.VOID, Type.NO_ARGS, new String[] {}, "<init>", className, il, cp);
				mg0.setMaxStack();
				mg0.setMaxLocals();
				m0 = mg0.getMethod();
			} finally {
				il.dispose();
			}
		}

		{
			InstructionList il = fgb.visit(expression, returnType);
			Type types[] = parameterTypes.toArray(new Type[0]);

			try {
				MethodGen mg1 = new MethodGen(ACC_PUBLIC, returnType, types, null, methodName, className, il, cp);
				mg1.setMaxStack();
				mg1.setMaxLocals();
				m1 = mg1.getMethod();
			} finally {
				il.dispose();
			}
		}

		cg.addMethod(m0);
		cg.addMethod(m1);

		byte bytes[] = cg.getJavaClass().getBytes();

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
		return local(0, ObjectType.getInstance(className));
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
