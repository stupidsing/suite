package suite.jdk.gen;

import static org.apache.bcel.Const.ACC_PUBLIC;
import static org.apache.bcel.Const.ACC_STATIC;
import static org.apache.bcel.Const.ACC_SUPER;
import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.Const;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.CPInstruction;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldGen;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import primal.MoreVerbs.Read;
import primal.Verbs.Get;
import primal.Verbs.New;
import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Iterate;
import primal.fp.Funs.Source;
import primal.fp.Funs2.BinOp;
import primal.jdk.UnsafeUtil;
import primal.os.Log_;
import suite.jdk.gen.FunExprM.FieldStaticFunExpr;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.pass.FunExpand;
import suite.jdk.gen.pass.FunGenerateBytecode;
import suite.jdk.gen.pass.FunRewrite;
import suite.jdk.lambda.LambdaInterface;

public class FunCreator<I> extends FunFactory {

	private static boolean isLog = false;

	public final LambdaInterface<I> lambdaClass;
	public final Class<?> superClass;
	public final Type returnType;
	public final List<Type> parameterTypes;

	private boolean isExpand;
	private Map<String, Pair<Type, Object>> fieldStaticTypeValues;
	private Map<String, Type> fieldTypes;

	public static <I> FunCreator<I> of(Class<I> clazz) {
		return of(clazz, true);
	}

	public static <I> FunCreator<I> of(Class<I> clazz, boolean isExpand) {
		var fc = of(LambdaInterface.of(clazz), Map.ofEntries());
		fc.isExpand = isExpand;
		return fc;
	}

	public static <I> FunCreator<I> of(LambdaInterface<I> lc, Map<String, Type> fs) {
		var method = lc.method();
		var rt = Type.getType(method.getReturnType());
		var pts = Read.from(method.getParameterTypes()).map(Type::getType).toList();
		return new FunCreator<>(lc, rt, pts, fs);
	}

	private FunCreator(LambdaInterface<I> lc, Type rt, List<Type> ps, Map<String, Type> fs) {
		lambdaClass = lc;
		superClass = Object.class;
		returnType = rt;
		parameterTypes = ps;

		fieldStaticTypeValues = new HashMap<>();
		fieldTypes = fs;
	}

	public Fun<Map<String, Object>, I> create(Source<FunExpr> expr) {
		return create(parameter0(expr));
	}

	public Fun<Map<String, Object>, I> create(Iterate<FunExpr> expr) {
		return create(parameter1(expr));
	}

	public Fun<Map<String, Object>, I> create(BinOp<FunExpr> expr) {
		return create(parameter2(expr));
	}

	public Fun<Map<String, Object>, I> create(FunExpr expr) {
		return create_(expr)::create;
	}

	public CreateClass create_(FunExpr expr) {
		return new CreateClass(expr);
	}

	public class CreateClass {
		public final Class<? extends I> clazz;
		public final Map<String, Pair<Type, Object>> fieldTypeValues;

		private CreateClass(FunExpr expr0) {
			var interfaceClass = lambdaClass.interfaceClass;
			var clsName = UnsafeUtil.class.getPackageName() + "." + interfaceClass.getSimpleName() + "_" + Get.temp();
			var methodName = lambdaClass.methodName;

			var localTypes = new ArrayList<Type>();
			localTypes.add(ObjectType.getInstance(clsName));
			localTypes.addAll(parameterTypes);

			var cpg = new ConstantPoolGen();
			var factory = new InstructionFactory(cpg);

			var fe = new FunExpand();
			FunRewrite fr;

			var expr1 = isExpand ? fe.expand(expr0, 3) : expr0;
			var expr2 = (fr = new FunRewrite(fieldTypes, localTypes, expr1.cast_(interfaceClass))).expr;

			org.apache.bcel.classfile.Method m0, m1;
			var ftvs = fr.fieldTypeValues;

			{
				var il = new InstructionList();
				try {
					il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
					il.append(factory.createInvoke(superClass.getName(), "<init>", Type.VOID, Type.NO_ARGS, Const.INVOKESPECIAL));
					il.append(InstructionFactory.createReturn(Type.VOID));

					var mg = new MethodGen(ACC_PUBLIC, Type.VOID, Type.NO_ARGS, new String[] {}, "<init>", clsName, il, cpg);
					mg.setMaxStack();
					mg.setMaxLocals();
					m0 = mg.getMethod();
				} finally {
					il.dispose();
				}
			}

			{
				var visit = new FunGenerateBytecode(clsName, fr.fti, cpg).visit(expr2, returnType);
				var il = visit.instructionList();
				var paramTypes = parameterTypes.toArray(new Type[0]);

				if (isLog) {
					Log_.info("expr0 = " + expr0);
					Log_.info("expr1 = " + expr1);
					Log_.info("expr2 = " + expr2);
					Log_.info("class = " + clsName + " implements " + interfaceClass.getName());
					Log_.info("fields = " + fieldTypes);
					var constantPool = cpg.getConstantPool();
					var instructions = il.getInstructions();

					for (var i = 0; i < instructions.length; i++) {
						var instruction = instructions[i];
						var s = instruction.toString(false);
						String p;
						if (instruction instanceof BranchInstruction)
							p = Integer.toString(visit.jumps.get(i));
						else if (instruction instanceof CPInstruction cpi)
							p = constantPool.constantToString(constantPool.getConstant(cpi.getIndex()));
						else
							p = "";
						Log_.info("(" + i + ") " + s + " " + p);
					}
				}

				try {
					var mg = new MethodGen(ACC_PUBLIC, returnType, paramTypes, null, methodName, clsName, il, cpg);
					mg.setMaxStack();
					mg.setMaxLocals();
					m1 = mg.getMethod();
				} finally {
					il.dispose();
				}
			}

			String[] ifs = { interfaceClass.getName(), };
			var cg = new ClassGen(clsName, superClass.getName(), ".java", ACC_PUBLIC | ACC_SUPER, ifs, cpg);

			for (var e : fieldStaticTypeValues.entrySet())
				cg.addField(new FieldGen(ACC_PUBLIC | ACC_STATIC, e.getValue().k, e.getKey(), cpg).getField());
			for (var e : fieldTypes.entrySet())
				cg.addField(new FieldGen(ACC_PUBLIC, e.getValue(), e.getKey(), cpg).getField());
			for (var e : ftvs.entrySet())
				cg.addField(new FieldGen(ACC_PUBLIC, e.getValue().k, e.getKey(), cpg).getField());

			cg.addMethod(m0);
			cg.addMethod(m1);

			var bytes = cg.getJavaClass().getBytes();

			clazz = new UnsafeUtil().defineClass(interfaceClass, clsName, bytes, null);
			fieldTypeValues = ftvs;

			for (var e : fieldStaticTypeValues.entrySet())
				try {
					clazz.getField(e.getKey()).set(null, e.getValue().v);
				} catch (ReflectiveOperationException ex) {
					fail(ex);
				}
		}

		private I create(Map<String, Object> fieldValues) {
			var t = New.clazz(clazz);

			return ex(() -> {
				for (var field : clazz.getDeclaredFields()) {
					var fieldName = field.getName();
					Pair<Type, Object> typeValue;
					Object value;

					if ((value = fieldValues.get(fieldName)) != null)
						field.set(t, value);
					else if ((typeValue = fieldTypeValues.get(fieldName)) != null)
						field.set(t, typeValue.v);
				}
				return t;
			});
		}
	}

	public FunExpr constant(Object object) {
		var fieldName = "s_" + Get.temp();
		var fieldType = object != null ? Type.getType(object.getClass()) : Type.OBJECT;
		fieldStaticTypeValues.put(fieldName, Pair.of(fieldType, object));

		var expr = new FieldStaticFunExpr();
		expr.fieldName = fieldName;
		expr.fieldType = fieldType;
		return expr;
	}

	public FunExpr field(String fieldName) {
		return local(0).field(fieldName, fieldTypes.get(fieldName));
	}

}
