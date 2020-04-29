package suite.jdk.gen.pass;

import static primal.statics.Rethrow.ex;

import org.apache.bcel.generic.Type;

import primal.MoreVerbs.Read;
import primal.Verbs.Equals;
import primal.primitive.fp.AsInt;
import suite.inspect.Inspect;
import suite.jdk.gen.FunExprK.Declare0ParameterFunExpr;
import suite.jdk.gen.FunExprK.Declare1ParameterFunExpr;
import suite.jdk.gen.FunExprK.Declare2ParameterFunExpr;
import suite.jdk.gen.FunExprL.ApplyFunExpr;
import suite.jdk.gen.FunExprL.DeclareLocalFunExpr;
import suite.jdk.gen.FunExprL.FieldInjectFunExpr;
import suite.jdk.gen.FunExprL.InvokeLambdaFunExpr;
import suite.jdk.gen.FunExprM.CastFunExpr;
import suite.jdk.gen.FunExprM.ConstantFunExpr;
import suite.jdk.gen.FunExprM.If1FunExpr;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.node.util.Singleton;
import suite.util.Switch;

public class FunExpand extends FunFactory {

	private static Inspect inspect = Singleton.me.inspect;

	public FunExpr expand(FunExpr expr0, int depth) {
		return 0 < depth ? rewrite(expr -> expand_(expr, depth), expr0) : expr0;
	}

	private FunExpr expand_(FunExpr e0, int depth) {
		return e0.sw(
		).applyIf(ApplyFunExpr.class, e1 -> e1.apply((object0, parameters) -> {
			var object1 = object0 instanceof CastFunExpr ? ((CastFunExpr) object0).expr : object0;
			return new Switch<FunExpr>(object1
			).applyIf(Declare0ParameterFunExpr.class, e2 -> e2.apply(do_ -> {
				return expand(do_, depth);
			})).applyIf(Declare1ParameterFunExpr.class, e2 -> e2.apply((p0, do_) -> {
				return expand(replace(do_, p0, parameters.get(0)), depth);
			})).applyIf(Declare2ParameterFunExpr.class, e2 -> e2.apply((p0, p1, do0) -> {
				var do1 = replace(do0, p0, parameters.get(0));
				var do2 = replace(do1, p1, parameters.get(1));
				return expand(do2, depth);
			})).result();
		})).applyIf(DeclareLocalFunExpr.class, e1 -> e1.apply((var, value, do_) -> {
			return expand(replace(do_, var, value), depth);
		})).applyIf(InvokeLambdaFunExpr.class, e1 -> e1.apply((isExpand, l_inst, ps) -> {
			if (Boolean.FALSE) {
				var l_impl = l_inst.lambdaImplementation;
				if (isExpand || weight(l_impl.expr) <= 5) {
					var l_iface = l_impl.lambdaInterface;
					var fe = l_impl.expr;
					for (var n : l_impl.fieldTypeByNames.keySet())
						fe = replaceFieldInject(fe, n, object(l_inst.fieldValueByNames.get(n), l_impl.fieldTypeByNames.get(n)));
					return expand(fe.cast_(l_iface.interfaceClass).apply(ps), depth - 1);
				} else
					return null;
			} else
				return null;
		})).applyIf(If1FunExpr.class, e1 -> e1.apply(if_ -> {
			return if_.cast(ConstantFunExpr.class, e2 -> e2.apply((type, constant) -> {
				if (type == Type.INT)
					return ((Integer) constant).intValue() != 0 ? e1.then : e1.else_;
				else
					return null;
			}));
		})).result();
	}

	private FunExpr replaceFieldInject(FunExpr expr0, String fieldName, FunExpr to) {
		return rewrite(e -> {
			var inj = e.cast(FieldInjectFunExpr.class);
			return inj != null && Equals.string(inj.fieldName, fieldName) ? to : null;
		}, expr0);
	}

	private int weight(FunExpr e0) {
		var cast = e0.cast(CastFunExpr.class);
		if (cast != null)
			return weight(cast.expr);
		else
			return inspect
					.fields(e0.getClass())
					.toInt(AsInt.sum(field -> {
						var e1 = ex(() -> field.get(e0));
						if (e1 instanceof FunExpr)
							return weight_(e1);
						else if (e1 instanceof Iterable<?>)
							return Read.from((Iterable<?>) e1).toInt(AsInt.sum(e2 -> weight_(e2)));
						else
							return 0;
					})) + 1;
	}

	private int weight_(Object object) {
		return object instanceof FunExpr ? weight((FunExpr) object) : 0;
	}

}
