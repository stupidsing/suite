package suite.jdk.gen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.generic.Type;

import suite.adt.Pair;
import suite.inspect.Inspect;
import suite.jdk.gen.FunExpression.ApplyFunExpr;
import suite.jdk.gen.FunExpression.AssignFunExpr;
import suite.jdk.gen.FunExpression.CastFunExpr;
import suite.jdk.gen.FunExpression.Declare0ParameterFunExpr;
import suite.jdk.gen.FunExpression.Declare1ParameterFunExpr;
import suite.jdk.gen.FunExpression.Declare2ParameterFunExpr;
import suite.jdk.gen.FunExpression.DeclareLocalFunExpr;
import suite.jdk.gen.FunExpression.DeclareParameterFunExpr;
import suite.jdk.gen.FunExpression.FieldFunExpr;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunExpression.InjectFunExpr;
import suite.jdk.gen.FunExpression.InvokeFunExpr;
import suite.jdk.gen.FunExpression.ObjectFunExpr;
import suite.jdk.gen.FunExpression.PlaceholderFunExpr;
import suite.node.util.Singleton;
import suite.streamlet.Read;
import suite.util.Rethrow;
import suite.util.Util;

public class FunRewrite extends FunFactory {

	private static Inspect inspect = Singleton.get().getInspect();

	public final FunExpr expr;
	public final Map<String, Pair<Type, Object>> fieldTypeValues = new HashMap<>();
	public final FunTypeInformation fti;

	private List<Type> localTypes;
	private Map<String, Type> fieldTypes;
	private Map<PlaceholderFunExpr, FunExpr> placeholders = new HashMap<>();

	public FunRewrite(Map<String, Type> fieldTypes, List<Type> parameterTypes, FunExpr expr0) {
		this.fti = new FunTypeInformation(placeholders::get);
		this.fieldTypes = fieldTypes;
		this.localTypes = new ArrayList<>(parameterTypes);
		this.expr = rewrite(expr0);
	}

	private FunExpr rewrite(FunExpr expr0) {
		return inspect.rewrite(FunExpr.class, new Object[] { fe, }, this::rewrite_, expr0);
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
			FunExpr expr = e1.expr;
			if (expr instanceof DeclareParameterFunExpr)
				fti.interfaceClasses.put((DeclareParameterFunExpr) expr, Type_.classOf(e1.type));
			return null;
		} else if (e0 instanceof DeclareParameterFunExpr) {
			DeclareParameterFunExpr e1 = (DeclareParameterFunExpr) e0;
			Class<?> pts[] = Type_.methodOf(fti.interfaceClasses.get(e1)).getParameterTypes();
			if (e0 instanceof Declare0ParameterFunExpr) {
				Declare0ParameterFunExpr e2 = (Declare0ParameterFunExpr) e1;
				return rewrite(e2.do_);
			} else if (e0 instanceof Declare1ParameterFunExpr) {
				Declare1ParameterFunExpr e2 = (Declare1ParameterFunExpr) e1;
				placeholders.put(e2.parameter, local(1, pts[0]));
				return rewrite(e2.do_);
			} else if (e0 instanceof Declare2ParameterFunExpr) {
				Declare2ParameterFunExpr e2 = (Declare2ParameterFunExpr) e1;
				placeholders.put(e2.p0, local(1, pts[0]));
				placeholders.put(e2.p1, local(2, pts[1]));
				return rewrite(e2.do_);
			} else
				return null;
		} else if (e0 instanceof DeclareLocalFunExpr) {
			DeclareLocalFunExpr e1 = (DeclareLocalFunExpr) e0;
			FunExpr value = rewrite(e1.value);
			Type type = fti.typeOf(value);

			int index = localTypes.size();
			localTypes.add(type);

			AssignFunExpr afe = fe.new AssignFunExpr();
			afe.index = index;
			afe.value = value;

			placeholders.put(e1.var, local(index, type));
			return seq(afe, rewrite(e1.do_));
		} else if (e0 instanceof FieldFunExpr) {
			FieldFunExpr e1 = (FieldFunExpr) e0;
			FunExpr object = rewrite(e1.object);
			String fieldName = e1.field;
			Class<?> clazz = fti.classOf(object);
			Field field = Rethrow.reflectiveOperationException(() -> clazz.getField(fieldName));
			return object.cast(field.getDeclaringClass()).field(fieldName, Type.getType(field.getType()));
		} else if (e0 instanceof InvokeFunExpr) {
			InvokeFunExpr e1 = (InvokeFunExpr) e0;
			LambdaInstance<?> l_inst = e1.lambda;
			LambdaImplementation<?> l_impl = l_inst.lambdaImplementation;
			LambdaInterface<?> l_iface = l_impl.lambdaInterface;
			FunExpr object = object_(l_impl.newFun(l_inst.fieldValues), l_iface.interfaceClass);

			return rewrite(object.invoke(l_iface.interfaceClass, l_iface.methodName, e1.parameters));
		} else if (e0 instanceof InjectFunExpr) {
			InjectFunExpr e1 = (InjectFunExpr) e0;
			Type type = fieldTypes.get(e1.field);
			if (type != null)
				return rewrite(this_().field(e1.field, type));
			else
				throw new RuntimeException(e1.field);
		} else if (e0 instanceof ObjectFunExpr) {
			ObjectFunExpr e1 = (ObjectFunExpr) e0;
			String fieldName = "f" + Util.temp();
			Type type = e1.type;
			fieldTypeValues.put(fieldName, Pair.of(type, e1.object));
			return rewrite(this_().field(fieldName, type));
		} else if (e0 instanceof PlaceholderFunExpr) {
			PlaceholderFunExpr e1 = (PlaceholderFunExpr) e0;
			return placeholders.get(e1);
		} else
			return null;
	}

	private FunExpr this_() {
		return local(0, localTypes.get(0));
	}

}
