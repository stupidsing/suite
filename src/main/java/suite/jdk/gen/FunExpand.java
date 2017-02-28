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
import suite.jdk.gen.FunExpression.InjectFunExpr;
import suite.jdk.gen.FunExpression.InvokeFunExpr;
import suite.util.Util;

public class FunExpand extends FunFactory {

	private static Inspect inspect = new Inspect();

	public FunExpr expand(FunExpr expr0, int depth) {
		if (0 < depth)
			return inspect.rewrite(FunExpr.class, new Object[] { fe, }, expr -> expand_(expr, depth), expr0);
		else
			return expr0;
	}

	private FunExpr expand_(FunExpr e, int depth) {
		if (e instanceof If1FunExpr) {
			If1FunExpr expr = (If1FunExpr) e;
			FunExpr if_ = expr.if_;
			if (if_ instanceof ConstantFunExpr) {
				ConstantFunExpr cfe = (ConstantFunExpr) if_;
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
				return expand(replace(object_.do_, object_.parameter, expr.parameters.get(0)), depth);
			} else if (object instanceof Declare2ParameterFunExpr) {
				Declare2ParameterFunExpr object_ = (Declare2ParameterFunExpr) object;
				FunExpr do0 = object_.do_;
				FunExpr do1 = replace(do0, object_.p0, expr.parameters.get(0));
				FunExpr do2 = replace(do1, object_.p1, expr.parameters.get(1));
				return expand(do2, depth);
			} else
				return null;
		} else if (e instanceof DeclareLocalFunExpr) {
			DeclareLocalFunExpr expr = (DeclareLocalFunExpr) e;
			return expand(replace(expr.do_, expr.var, expr.value), depth);
		} else if (e instanceof InvokeFunExpr) {
			InvokeFunExpr expr = (InvokeFunExpr) e;
			LambdaInstance<?> l_inst = expr.lambda;
			LambdaImplementation<?> l_impl = l_inst.lambdaImplementation;
			FunExpr fe = l_impl.expr;
			for (String fieldName : l_impl.fieldTypes.keySet())
				fe = replaceInject(fe, fieldName, object(l_inst.fieldValues.get(fieldName), l_impl.fieldTypes.get(fieldName)));
			return expand(fe.apply(expr.parameters.toArray(new FunExpr[0])), depth - 1);
		} else
			return null;
	}

	private FunExpr replaceInject(FunExpr expr0, String fieldName, FunExpr to) {
		return inspect.rewrite(FunExpr.class, new Object[] { fe, }, e -> {
			if (e instanceof InjectFunExpr && Util.stringEquals(((InjectFunExpr) e).field, fieldName))
				return to;
			else
				return null;
		}, expr0);
	}

	private FunExpr replace(FunExpr expr0, FunExpr from, FunExpr to) {
		return inspect.rewrite(FunExpr.class, new Object[] { fe, }, e -> e.equals(from) ? to : null, expr0);
	}

}
