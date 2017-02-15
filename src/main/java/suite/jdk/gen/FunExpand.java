package suite.jdk.gen;

import org.apache.bcel.generic.Type;

import suite.inspect.Inspect;
import suite.jdk.gen.FunExpression.ApplyFunExpr;
import suite.jdk.gen.FunExpression.ConstantFunExpr;
import suite.jdk.gen.FunExpression.Declare1ParameterFunExpr;
import suite.jdk.gen.FunExpression.Declare2ParameterFunExpr;
import suite.jdk.gen.FunExpression.DeclareLocalFunExpr;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunExpression.If1FunExpr;

public class FunExpand extends FunFactory {

	private static Inspect inspect = new Inspect();

	public FunExpr expand(FunExpr expr0, int depth) {
		if (0 < depth)
			return inspect.rewrite(FunExpr.class, new Object[] { fe, }, expr -> expand_(expr, depth), expr0);
		else
			return expr0;
	}

	private FunExpr expand_(FunExpr e, int depth) {
		int depth1 = depth - 1;

		if (e instanceof If1FunExpr) {
			If1FunExpr expr = (If1FunExpr) e;
			if (expr.if_ instanceof ConstantFunExpr) {
				ConstantFunExpr cfe = (ConstantFunExpr) expr.if_;
				if (cfe.type == Type.INT)
					return ((Integer) cfe.constant).intValue() == 1 ? expr.then : expr.else_;
				else
					return null;
			} else
				return null;
		} else if (e instanceof ApplyFunExpr) {
			ApplyFunExpr expr = (ApplyFunExpr) e;
			FunExpr object = expr.object;
			if (object instanceof Declare1ParameterFunExpr) {
				Declare1ParameterFunExpr object_ = (Declare1ParameterFunExpr) object;
				return expand(object_.doFun.apply(expr.parameters.get(0)), depth1);
			} else if (e instanceof Declare2ParameterFunExpr) {
				Declare2ParameterFunExpr object_ = (Declare2ParameterFunExpr) object;
				return expand(object_.doFun.apply(expr.parameters.get(0), expr.parameters.get(1)), depth1);
			} else
				return null;
		} else if (e instanceof DeclareLocalFunExpr) {
			DeclareLocalFunExpr expr = (DeclareLocalFunExpr) e;
			return expand(expr.apply(expr.value), depth1);
		} else
			return null;
	}

}
