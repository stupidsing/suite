package suite.jdk.gen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.objectweb.asm.Type;

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
import suite.streamlet.Read;
import suite.util.Rethrow;

public class FunRewriter extends FunConstructor {

	private static Inspect inspect = new Inspect();

	private List<Type> localTypes;
	private FunExpression fe;
	private FunType ft;

	public FunRewriter(FunCreator<?> fc, FunType ft) {
		this.localTypes = fc.localTypes;
		this.fe = fc.fe;
		this.ft = ft;
	}

	public FunExpr rewrite(FunExpr expr0) {
		return inspect.rewrite(FunExpr.class, new Object[] { fe, }, this::rewrite_, expr0);
	}

	private FunExpr rewrite_(FunExpr e) {
		if (e instanceof ApplyFunExpr) {
			ApplyFunExpr expr = (ApplyFunExpr) e;
			FunExpr object = rewrite(expr.object);
			FunExpr parameters[] = Read.from(expr.parameters).map(this::rewrite).toList().toArray(new FunExpr[0]);
			Method method = ft.methodOf(object);
			return object.invoke(method.getName(), parameters);
		} else if (e instanceof CastFunExpr) {
			CastFunExpr cfe = (CastFunExpr) e;
			FunExpr expr = cfe.expr;
			if (expr instanceof DeclareParameterFunExpr)
				((DeclareParameterFunExpr) expr).interfaceClass = TypeHelper.classOf(cfe.type);
			return null;
		} else if (e instanceof DeclareParameterFunExpr) {
			DeclareParameterFunExpr e_ = (DeclareParameterFunExpr) e;
			Class<?> pts[] = TypeHelper.methodOf(e_.interfaceClass).getParameterTypes();
			if (e instanceof Declare1ParameterFunExpr) {
				Declare1ParameterFunExpr expr = (Declare1ParameterFunExpr) e_;
				return rewrite(expr.doFun.apply(local(1, pts[0])));
			} else if (e instanceof Declare2ParameterFunExpr) {
				Declare2ParameterFunExpr expr = (Declare2ParameterFunExpr) e_;
				return rewrite(expr.doFun.apply(local(1, pts[0]), local(2, pts[1])));
			} else
				return null;
		} else if (e instanceof DeclareLocalFunExpr) {
			DeclareLocalFunExpr expr = (DeclareLocalFunExpr) e;
			FunExpr value = rewrite(expr.value);
			Type type = ft.typeOf(value);

			int index = localTypes.size();
			localTypes.add(type);

			AssignFunExpr afe = fe.new AssignFunExpr();
			afe.index = index;
			afe.value = value;

			return seq(afe, rewrite(expr.doFun.apply(local(index, type))));
		} else if (e instanceof FieldFunExpr) {
			FieldFunExpr expr = (FieldFunExpr) e;
			FunExpr object = rewrite(expr.object);
			String fieldName = expr.field;
			Class<?> clazz = ft.classOf(object);
			Field field = Rethrow.reflectiveOperationException(() -> clazz.getField(fieldName));
			return object.cast(field.getDeclaringClass()).field(fieldName, Type.getType(field.getType()));

		} else
			return null;
	}

}
