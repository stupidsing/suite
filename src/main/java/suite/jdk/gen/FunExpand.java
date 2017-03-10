package suite.jdk.gen;

import org.apache.bcel.generic.Type;

import suite.jdk.gen.FunExpression.ApplyFunExpr;
import suite.jdk.gen.FunExpression.CastFunExpr;
import suite.jdk.gen.FunExpression.ConstantFunExpr;
import suite.jdk.gen.FunExpression.Declare0ParameterFunExpr;
import suite.jdk.gen.FunExpression.Declare1ParameterFunExpr;
import suite.jdk.gen.FunExpression.Declare2ParameterFunExpr;
import suite.jdk.gen.FunExpression.DeclareLocalFunExpr;
import suite.jdk.gen.FunExpression.FieldInjectFunExpr;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunExpression.If1FunExpr;
import suite.jdk.gen.FunExpression.InvokeLambdaFunExpr;
import suite.util.Util;

public class FunExpand extends FunFactory {

	public FunExpr expand(FunExpr expr0, int depth) {
		if (0 < depth)
			return rewrite(expr -> expand_(expr, depth), expr0);
		else
			return expr0;
	}

	private FunExpr expand_(FunExpr e0, int depth) {
		if (e0 instanceof If1FunExpr) {
			If1FunExpr e1 = (If1FunExpr) e0;
			FunExpr if_ = e1.if_;
			if (if_ instanceof ConstantFunExpr) {
				ConstantFunExpr e2 = (ConstantFunExpr) if_;
				if (e2.type == Type.INT)
					return ((Integer) e2.constant).intValue() != 0 ? e1.then : e1.else_;
				else
					return null;
			} else
				return null;
		} else if (e0 instanceof ApplyFunExpr) {
			ApplyFunExpr e1 = (ApplyFunExpr) e0;
			FunExpr object0 = e1.object;
			FunExpr object1 = object0 instanceof CastFunExpr ? ((CastFunExpr) object0).expr : object0;
			if (object1 instanceof Declare0ParameterFunExpr) {
				Declare0ParameterFunExpr object_ = (Declare0ParameterFunExpr) object1;
				return expand(object_.do_, depth);
			} else if (object1 instanceof Declare1ParameterFunExpr) {
				Declare1ParameterFunExpr object_ = (Declare1ParameterFunExpr) object1;
				return expand(replace(object_.do_, object_.parameter, e1.parameters.get(0)), depth);
			} else if (object1 instanceof Declare2ParameterFunExpr) {
				Declare2ParameterFunExpr object_ = (Declare2ParameterFunExpr) object1;
				FunExpr do0 = object_.do_;
				FunExpr do1 = replace(do0, object_.p0, e1.parameters.get(0));
				FunExpr do2 = replace(do1, object_.p1, e1.parameters.get(1));
				return expand(do2, depth);
			} else
				return null;
		} else if (e0 instanceof DeclareLocalFunExpr) {
			DeclareLocalFunExpr e1 = (DeclareLocalFunExpr) e0;
			return expand(replace(e1.do_, e1.var, e1.value), depth);
		} else if (e0 instanceof InvokeLambdaFunExpr) {
			InvokeLambdaFunExpr e1 = (InvokeLambdaFunExpr) e0;
			LambdaInstance<?> l_inst = e1.lambda;
			LambdaImplementation<?> l_impl = l_inst.lambdaImplementation;
			LambdaInterface<?> l_iface = l_impl.lambdaInterface;
			FunExpr fe = l_impl.expr;
			for (String fieldName : l_impl.fieldTypes.keySet())
				fe = replaceFieldInject(fe, fieldName, object(l_inst.fieldValues.get(fieldName), l_impl.fieldTypes.get(fieldName)));
			return expand(fe.cast(l_iface.interfaceClass).apply(e1.parameters), depth - 1);
		} else
			return null;
	}

	private FunExpr replaceFieldInject(FunExpr expr0, String fieldName, FunExpr to) {
		return rewrite(e -> {
			if (e instanceof FieldInjectFunExpr && Util.stringEquals(((FieldInjectFunExpr) e).fieldName, fieldName))
				return to;
			else
				return null;
		}, expr0);
	}

}
