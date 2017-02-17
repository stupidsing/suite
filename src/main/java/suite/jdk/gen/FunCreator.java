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

public class FunCreator<I> extends FunFactory {

	public final LambdaClass<I> lambdaClass;
	public final Class<?> superClass;
	public final String className;
	public final Type returnType;
	public final List<Type> parameterTypes;

	private Map<String, Pair<Type, Object>> constants;
	private Map<String, Type> fields;

	public static <I> FunCreator<I> of(LambdaClass<I> lc) {
		return of(lc, Collections.emptyMap());
	}

	public static <I> FunCreator<I> of(LambdaClass<I> lc, Map<String, Type> fs) {
		Method method = lc.method();
		Type rt = Type.getType(method.getReturnType());
		List<Type> pts = Read.from(method.getParameterTypes()).map(Type::getType).toList();
		return new FunCreator<>(lc, rt, pts, fs);
	}

	private FunCreator(LambdaClass<I> lc, Type rt, List<Type> ps, Map<String, Type> fs) {
		lambdaClass = lc;
		superClass = Object.class;
		className = lc.interfaceClass.getSimpleName() + Util.temp();
		returnType = rt;
		parameterTypes = ps;

		constants = new HashMap<>();
		fields = fs;
	}

	public Fun<Map<String, Object>, I> create(FunExpr expr0) {
		Class<I> interfaceClass = lambdaClass.interfaceClass;
		String methodName = lambdaClass.methodName;

		List<Type> localTypes = new ArrayList<>();
		localTypes.add(ObjectType.getInstance(className));
		localTypes.addAll(parameterTypes);

		ConstantPoolGen cp = new ConstantPoolGen();
		InstructionFactory factory = new InstructionFactory(cp);

		FunExpand fe = new FunExpand();
		FunTypeInformation fti = new FunTypeInformation();
		FunRewrite fr;
		FunGenerateBytecode fgb = new FunGenerateBytecode(fti, cp);

		FunExpr expr1 = fe.expand(expr0, 0);
		FunExpr expr2 = (fr = new FunRewrite(fti, localTypes, expr1.cast(interfaceClass))).expr;

		org.apache.bcel.classfile.Method m0, m1;
		Map<String, Pair<Type, Object>> fields1 = fr.fields;

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
			InstructionList il = fgb.visit(expr2, returnType);
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

		String ifs[] = new String[] { interfaceClass.getName(), };
		ClassGen cg = new ClassGen(className, superClass.getName(), ".java", ACC_PUBLIC | ACC_SUPER, ifs, cp);

		for (Entry<String, Pair<Type, Object>> e : constants.entrySet())
			cg.addField(new FieldGen(ACC_PUBLIC | ACC_STATIC, e.getValue().t0, e.getKey(), cp).getField());
		for (Entry<String, Type> e : fields.entrySet())
			cg.addField(new FieldGen(ACC_PUBLIC, e.getValue(), e.getKey(), cp).getField());
		for (Entry<String, Pair<Type, Object>> e : fields1.entrySet())
			cg.addField(new FieldGen(ACC_PUBLIC, e.getValue().t0, e.getKey(), cp).getField());

		cg.addMethod(m0);
		cg.addMethod(m1);

		byte bytes[] = cg.getJavaClass().getBytes();

		Class<? extends I> clazz = new UnsafeUtil().defineClass(interfaceClass, className, bytes);

		for (Entry<String, Pair<Type, Object>> e : constants.entrySet())
			try {
				clazz.getField(e.getKey()).set(null, e.getValue().t1);
			} catch (ReflectiveOperationException ex) {
				throw new RuntimeException(ex);
			}

		return fields -> Rethrow.reflectiveOperationException(() -> {
			I t = clazz.newInstance();
			for (Entry<String, Object> e : fields.entrySet())
				clazz.getDeclaredField(e.getKey()).set(t, e.getValue());
			for (Entry<String, Pair<Type, Object>> e : fields1.entrySet())
				clazz.getDeclaredField(e.getKey()).set(t, e.getValue().t1);
			return t;
		});
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
		String field = "f" + Util.temp();
		Type type = Type.getType(clazz);
		constants.put(field, Pair.of(type, object));

		StaticFunExpr expr = fe.new StaticFunExpr();
		expr.clazzType = className;
		expr.field = field;
		expr.type = type;
		return expr;
	}

}
