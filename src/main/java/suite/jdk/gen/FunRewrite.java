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
import suite.jdk.gen.FunExpression.Declare1ParameterFunExpr;
import suite.jdk.gen.FunExpression.Declare2ParameterFunExpr;
import suite.jdk.gen.FunExpression.DeclareLocalFunExpr;
import suite.jdk.gen.FunExpression.DeclareParameterFunExpr;
import suite.jdk.gen.FunExpression.FieldFunExpr;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunExpression.InjectFunExpr;
import suite.jdk.gen.FunExpression.InvokeFunExpr;
import suite.jdk.gen.FunExpression.InvokeMethodFunExpr;
import suite.jdk.gen.FunExpression.ObjectFunExpr;
import suite.jdk.gen.FunExpression.PlaceholderFunExpr;
import suite.streamlet.Read;
import suite.util.Rethrow;
import suite.util.Util;

public class FunRewrite extends FunFactory {

	private static Inspect inspect = new Inspect();

	public final FunExpr expr;
	public final Map<String, Pair<Type, Object>> fields = new HashMap<>();

	private FunTypeInformation fti;
	private List<Type> localTypes;
	private Map<PlaceholderFunExpr, FunExpr> placeholders = new HashMap<>();

	public FunRewrite(FunTypeInformation fti, List<Type> parameterTypes, FunExpr expr0) {
		this.fti = fti;
		this.localTypes = new ArrayList<>(parameterTypes);
		this.expr = rewrite(expr0);
	}

	private FunExpr rewrite(FunExpr expr0) {
		return inspect.rewrite(FunExpr.class, new Object[] { fe, }, this::rewrite_, expr0);
	}

	private FunExpr rewrite_(FunExpr e) {
		if (e instanceof ApplyFunExpr) {
			ApplyFunExpr expr = (ApplyFunExpr) e;
			FunExpr object = rewrite(expr.object);
			FunExpr parameters[] = Read.from(expr.parameters).map(this::rewrite).toList().toArray(new FunExpr[0]);
			Method method = fti.methodOf(object);
			return object.invoke(method.getName(), parameters);
		} else if (e instanceof CastFunExpr) {
			CastFunExpr cfe = (CastFunExpr) e;
			FunExpr expr = cfe.expr;
			if (expr instanceof DeclareParameterFunExpr)
				fti.interfaceClasses.put((DeclareParameterFunExpr) expr, Type_.classOf(cfe.type));
			return null;
		} else if (e instanceof DeclareParameterFunExpr) {
			DeclareParameterFunExpr e_ = (DeclareParameterFunExpr) e;
			Class<?> pts[] = Type_.methodOf(fti.interfaceClasses.get(e_)).getParameterTypes();
			if (e instanceof Declare1ParameterFunExpr) {
				Declare1ParameterFunExpr expr = (Declare1ParameterFunExpr) e_;
				placeholders.put(expr.parameter, local(1, pts[0]));
				return rewrite(expr.do_);
			} else if (e instanceof Declare2ParameterFunExpr) {
				Declare2ParameterFunExpr expr = (Declare2ParameterFunExpr) e_;
				placeholders.put(expr.p0, local(1, pts[0]));
				placeholders.put(expr.p1, local(2, pts[1]));
				return rewrite(expr.do_);
			} else
				return null;
		} else if (e instanceof DeclareLocalFunExpr) {
			DeclareLocalFunExpr expr = (DeclareLocalFunExpr) e;
			FunExpr value = rewrite(expr.value);
			Type type = fti.typeOf(value);

			int index = localTypes.size();
			localTypes.add(type);

			AssignFunExpr afe = fe.new AssignFunExpr();
			afe.index = index;
			afe.value = value;

			placeholders.put(expr.var, local(index, type));
			return seq(afe, rewrite(expr.do_));
		} else if (e instanceof FieldFunExpr) {
			FieldFunExpr expr = (FieldFunExpr) e;
			FunExpr object = rewrite(expr.object);
			String fieldName = expr.field;
			Class<?> clazz = fti.classOf(object);
			Field field = Rethrow.reflectiveOperationException(() -> clazz.getField(fieldName));
			return object.cast(field.getDeclaringClass()).field(fieldName, Type.getType(field.getType()));
		} else if (e instanceof InvokeFunExpr) {
			InvokeFunExpr expr = (InvokeFunExpr) e;
			FunConfig<?> fun = expr.funConfig;

			InvokeMethodFunExpr imfe = fe.new InvokeMethodFunExpr();
			imfe.methodName = fun.lambdaClass.methodName;
			imfe.object = rewrite(object_(fun.newFun(), fun.lambdaClass.interfaceClass));
			imfe.parameters = expr.parameters;
			return rewrite(imfe);
		} else if (e instanceof InjectFunExpr) {
			InjectFunExpr expr = (InjectFunExpr) e;
			return rewrite(this_().field(expr.field));
		} else if (e instanceof ObjectFunExpr) {
			ObjectFunExpr expr = (ObjectFunExpr) e;
			String fieldName = "f" + Util.temp();
			Type type = Type.getType(expr.clazz);
			fields.put(fieldName, Pair.of(type, expr.object));
			return rewrite(this_().field(fieldName, type));
		} else if (e instanceof PlaceholderFunExpr)
			return placeholders.get((PlaceholderFunExpr) e);
		else
			return null;
	}

	private FunExpr this_() {
		return local(0, localTypes.get(0));
	}

}
