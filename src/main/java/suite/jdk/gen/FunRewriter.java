package suite.jdk.gen;

import java.lang.reflect.Method;

import org.objectweb.asm.Type;

import suite.inspect.Inspect;
import suite.jdk.gen.FunExpression.ApplyFunExpr;
import suite.jdk.gen.FunExpression.AssignFunExpr;
import suite.jdk.gen.FunExpression.CastFunExpr;
import suite.jdk.gen.FunExpression.Declare1ParameterFunExpr;
import suite.jdk.gen.FunExpression.Declare2ParameterFunExpr;
import suite.jdk.gen.FunExpression.DeclareLocalFunExpr;
import suite.jdk.gen.FunExpression.DeclareParameterFunExpr;
import suite.jdk.gen.FunExpression.FunExpr;

public class FunRewriter {

	private static Inspect inspect = new Inspect();

	private FunExpression fe;

	public FunRewriter(FunExpression fe) {
		this.fe = fe;
	}

	public FunExpr rewrite(FunExpr expr0) {
		FunCreator<?> fc = fe.fc;
		return inspect.rewrite(FunExpr.class, new Object[] { fe, }, e -> rewrite_(fc, e), expr0);
	}

	private FunExpr rewrite_(FunCreator<?> fc, FunExpr e) {
		if (e instanceof ApplyFunExpr) {
			ApplyFunExpr expr = (ApplyFunExpr) e;
			Method method = TypeHelper.instance.methodOf(expr.object);
			return expr.object.invoke(method.getName(), expr.parameters);
		} else if (e instanceof CastFunExpr) {
			CastFunExpr cfe = (CastFunExpr) e;
			FunExpr expr = cfe.expr;
			if (expr instanceof DeclareParameterFunExpr) {
				((DeclareParameterFunExpr) expr).interfaceClass = TypeHelper.instance.classOf(cfe.type);
				return rewrite(expr);
			} else
				return null;
		} else if (e instanceof DeclareParameterFunExpr) {
			DeclareParameterFunExpr e_ = (DeclareParameterFunExpr) e;
			Class<?> pts[] = TypeHelper.instance.methodOf(e_.interfaceClass).getParameterTypes();
			if (e instanceof Declare1ParameterFunExpr) {
				Declare1ParameterFunExpr expr = (Declare1ParameterFunExpr) e_;
				FunExpr f1 = expr.doFun.apply(fc.local(1, pts[0]));
				return fc.seq(fe.new NoOperationFunExpr(), rewrite(f1));
			} else if (e instanceof Declare2ParameterFunExpr) {
				Declare2ParameterFunExpr expr = (Declare2ParameterFunExpr) e_;
				FunExpr f1 = expr.doFun.apply(fc.local(1, pts[0]), fc.local(2, pts[1]));
				return fc.seq(fe.new NoOperationFunExpr(), rewrite(f1));
			} else
				return null;
		} else if (e instanceof DeclareLocalFunExpr) {
			DeclareLocalFunExpr expr = (DeclareLocalFunExpr) e;
			Type type = FunType.typeOf(expr.value);

			int index = fc.localTypes.size();
			fc.localTypes.add(type);

			AssignFunExpr afe = fe.new AssignFunExpr();
			afe.index = index;
			afe.value = expr.value;

			return fc.seq(afe, rewrite(expr.doFun.apply(fc.local(index, type))));
		} else
			return null;
	}

}
