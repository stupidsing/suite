package suite.jdk.gen.pass;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.generic.Type;

import suite.adt.Pair;
import suite.jdk.gen.FunCreator;
import suite.jdk.gen.FunExprK.Declare0ParameterFunExpr;
import suite.jdk.gen.FunExprK.Declare1ParameterFunExpr;
import suite.jdk.gen.FunExprK.Declare2ParameterFunExpr;
import suite.jdk.gen.FunExprK.DeclareParameterFunExpr;
import suite.jdk.gen.FunExprK.PlaceholderFunExpr;
import suite.jdk.gen.FunExprL.ApplyFunExpr;
import suite.jdk.gen.FunExprL.DeclareLocalFunExpr;
import suite.jdk.gen.FunExprL.FieldFunExpr;
import suite.jdk.gen.FunExprL.FieldInjectFunExpr;
import suite.jdk.gen.FunExprL.InvokeLambdaFunExpr;
import suite.jdk.gen.FunExprL.ObjectFunExpr;
import suite.jdk.gen.FunExprM.AssignLocalFunExpr;
import suite.jdk.gen.FunExprM.CastFunExpr;
import suite.jdk.gen.FunExprM.FieldStaticFunExpr;
import suite.jdk.gen.FunExprM.NewFunExpr;
import suite.jdk.gen.FunExprM.ProfileFunExpr;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.jdk.gen.Type_;
import suite.jdk.lambda.LambdaImplementation;
import suite.jdk.lambda.LambdaInstance;
import suite.jdk.lambda.LambdaInterface;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.util.Rethrow;
import suite.util.Util;

public class FunRewrite extends FunFactory {

	public final FunExpr expr;
	public final Map<String, Pair<Type, Object>> fieldTypeValues = new HashMap<>();
	public final FunTypeInformation fti;

	private List<Type> localTypes;
	private Map<String, Type> fieldTypes;
	private Map<PlaceholderFunExpr, FunExpr> placeholders = new HashMap<>();

	public FunRewrite(Map<String, Type> fieldTypes, List<Type> parameterTypes, FunExpr expr0) {
		this.fieldTypes = fieldTypes;
		this.localTypes = new ArrayList<>(parameterTypes);
		this.fti = new FunTypeInformation(localTypes, placeholders::get);
		this.expr = rewriteFun(expr0);
	}

	private FunExpr rewriteFun(FunExpr e0) {
		if (e0 instanceof CastFunExpr) {
			CastFunExpr e1 = (CastFunExpr) e0;
			FunExpr e2 = e1.expr;
			if (e2 instanceof DeclareParameterFunExpr) {
				DeclareParameterFunExpr e3 = (DeclareParameterFunExpr) e2;
				if (e3 instanceof Declare0ParameterFunExpr) {
					Declare0ParameterFunExpr e4 = (Declare0ParameterFunExpr) e3;
					return rewrite(e4.do_);
				} else if (e3 instanceof Declare1ParameterFunExpr) {
					Declare1ParameterFunExpr e4 = (Declare1ParameterFunExpr) e3;
					placeholders.put(e4.parameter, local(1));
					return rewrite(e4.do_);
				} else if (e3 instanceof Declare2ParameterFunExpr) {
					Declare2ParameterFunExpr e4 = (Declare2ParameterFunExpr) e3;
					placeholders.put(e4.p0, local(1));
					placeholders.put(e4.p1, local(2));
					return rewrite(e4.do_);
				} else
					throw new RuntimeException("cannot rewrite " + e3.getClass());
			} else
				throw new RuntimeException("cannot rewrite " + e2.getClass());
		} else
			throw new RuntimeException("cannot rewrite " + e0.getClass());
	}

	private FunExpr rewrite(FunExpr expr0) {
		return rewrite(this::rewrite_, expr0);
	}

	private FunExpr rewrite_(FunExpr e0) {
		if (e0 instanceof ApplyFunExpr) {
			ApplyFunExpr e1 = (ApplyFunExpr) e0;
			FunExpr object = rewrite(e1.object);
			FunExpr parameters[] = Read.from(e1.parameters).map(this::rewrite).toArray(FunExpr.class);
			Method method = fti.methodOf(object);
			return object.invoke(method.getName(), parameters);
		} else if (e0 instanceof CastFunExpr) {
			CastFunExpr e1 = (CastFunExpr) e0;
			FunExpr e2 = e1.expr;

			if (e2 instanceof DeclareParameterFunExpr) {
				Class<?> interfaceClass = Type_.classOf(e1.type);
				Map<String, Type> fieldTypes = new HashMap<>();
				Map<String, FunExpr> fieldValues = new HashMap<>();

				FunExpr e3 = rewrite(e -> {
					FunExpr fieldValue;
					if (e instanceof FieldStaticFunExpr) {
						FieldStaticFunExpr e_ = (FieldStaticFunExpr) e;
						String fieldName = e_.fieldName;
						Type fieldType = fieldTypes.get(fieldName);
						fieldTypes.put(fieldName, fieldType);
						fieldValues.put(fieldName, e_);
						return e;
					} else if (e instanceof PlaceholderFunExpr && (fieldValue = placeholders.get(e)) != null) {
						String fieldName = "e" + Util.temp();
						Type fieldType = fti.typeOf(fieldValue);
						fieldTypes.put(fieldName, fieldType);
						fieldValues.put(fieldName, fieldValue);
						return this_().field(fieldName, fieldType);
					} else
						return null;
				}, e2);

				FunCreator<?>.CreateClass cc = FunCreator.of(LambdaInterface.of(interfaceClass), fieldTypes).create_(e3);
				Streamlet2<String, FunExpr> fieldValues0 = Read.from2(cc.fieldTypeValues).mapValue(tv -> objectField(tv.t1, tv.t0));
				Streamlet2<String, FunExpr> fieldValues1 = Read.from2(fieldValues);

				NewFunExpr e4 = new NewFunExpr();
				e4.className = cc.className;
				e4.fieldValues = Streamlet2.concat(fieldValues0, fieldValues1).toMap();
				e4.implementationClass = cc.clazz;
				e4.interfaceClass = interfaceClass;
				return e4;
			} else
				return null;
		} else if (e0 instanceof DeclareLocalFunExpr) {
			DeclareLocalFunExpr e1 = (DeclareLocalFunExpr) e0;
			FunExpr value = rewrite(e1.value);
			Type type = fti.typeOf(value);

			int index = localTypes.size();
			localTypes.add(type);

			AssignLocalFunExpr afe = new AssignLocalFunExpr();
			afe.index = index;
			afe.value = value;

			placeholders.put(e1.var, local(index));
			return seq(afe, rewrite(e1.do_));
		} else if (e0 instanceof FieldFunExpr) {
			FieldFunExpr e1 = (FieldFunExpr) e0;
			FunExpr object = rewrite(e1.object);
			String fieldName = e1.fieldName;
			Class<?> clazz = fti.classOf(object);
			Field field = Rethrow.ex(() -> clazz.getField(fieldName));
			return object.cast(field.getDeclaringClass()).field(fieldName, Type.getType(field.getType()));
		} else if (e0 instanceof FieldInjectFunExpr) {
			FieldInjectFunExpr e1 = (FieldInjectFunExpr) e0;
			Type type = fieldTypes.get(e1.fieldName);
			if (type != null)
				return rewrite(this_().field(e1.fieldName, type));
			else
				throw new RuntimeException(e1.fieldName);
		} else if (e0 instanceof InvokeLambdaFunExpr) {
			InvokeLambdaFunExpr e1 = (InvokeLambdaFunExpr) e0;
			LambdaInstance<?> l_inst = e1.lambda;
			LambdaImplementation<?> l_impl = l_inst.lambdaImplementation;
			LambdaInterface<?> l_iface = l_impl.lambdaInterface;
			FunExpr object = object_(l_impl.newFun(l_inst.fieldValues), l_iface.interfaceClass);

			return rewrite(object.invoke(l_iface.interfaceClass, l_iface.methodName, e1.parameters));
		} else if (e0 instanceof ObjectFunExpr) {
			ObjectFunExpr e1 = (ObjectFunExpr) e0;
			return objectField(e1.object, e1.type);
		} else if (e0 instanceof PlaceholderFunExpr) {
			PlaceholderFunExpr e1 = (PlaceholderFunExpr) e0;
			FunExpr e2 = placeholders.get(e1);
			if (e2 != null)
				return e2;
			else
				throw new RuntimeException("cannot resolve placeholder");
		} else if (e0 instanceof ProfileFunExpr) {
			ProfileFunExpr e1 = (ProfileFunExpr) e0;
			fieldTypeValues.put(e1.counterFieldName, Pair.of(Type.INT, 0));
			return null;
		} else
			return null;
	}

	private FunExpr objectField(Object object, Type type) {
		String fieldName = "o" + Util.temp();
		fieldTypeValues.put(fieldName, Pair.of(type, object));
		return rewrite(this_().field(fieldName, type));
	}

	private FunExpr this_() {
		return local(0);
	}

}
