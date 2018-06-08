package suite.jdk.gen.pass;

import org.apache.bcel.generic.Type;

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
import suite.primitive.IntPrimitives.Obj_Int;
import suite.streamlet.Read;
import suite.util.Rethrow;
import suite.util.String_;
import suite.util.Switch;

public class FunExpand extends FunFactory {

	private static Inspect inspect = Singleton.me.inspect;

	public FunExpr expand(FunExpr expr0, int depth) {
		if (0 < depth)
			return rewrite(expr -> expand_(expr, depth), expr0);
		else
			return expr0;
	}

	private FunExpr expand_(FunExpr e0, int depth) {
		return e0.<FunExpr> switch_( //
		).applyIf(ApplyFunExpr.class, e1 -> e1.apply((object0, parameters) -> {
			var object1 = object0 instanceof CastFunExpr ? ((CastFunExpr) object0).expr : object0;
			return new Switch<FunExpr>(object1 //
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
					for (var fieldName : l_impl.fieldTypes.keySet())
						fe = replaceFieldInject(fe, fieldName,
								object(l_inst.fieldValues.get(fieldName), l_impl.fieldTypes.get(fieldName)));
					return expand(fe.cast_(l_iface.interfaceClass).apply(ps), depth - 1);
				} else
					return null;
			} else
				return null;
		})).applyIf(If1FunExpr.class, e1 -> e1.apply(if_ -> {
			return new Switch<FunExpr>(if_).applyIf(ConstantFunExpr.class, e2 -> e2.apply((type, constant) -> {
				if (type == Type.INT)
					return ((Integer) constant).intValue() != 0 ? e1.then : e1.else_;
				else
					return null;
			})).result();
		})).result();
	}

	private FunExpr replaceFieldInject(FunExpr expr0, String fieldName, FunExpr to) {
		return rewrite(e -> {
			var b = e instanceof FieldInjectFunExpr && String_.equals(((FieldInjectFunExpr) e).fieldName, fieldName);
			return b ? to : null;
		}, expr0);
	}

	private int weight(FunExpr e0) {
		if (e0 instanceof CastFunExpr) {
			var e1 = (CastFunExpr) e0;
			return weight(e1.expr);
		} else
			return Read //
					.from(inspect.fields(e0.getClass())) //
					.toInt(Obj_Int.sum(field -> {
						var e1 = Rethrow.ex(() -> field.get(e0));
						if (e1 instanceof FunExpr)
							return weight_(e1);
						else if (e1 instanceof Iterable<?>) {
							Iterable<?> iter = (Iterable<?>) e1;
							var sum = 0;
							for (var e2 : iter)
								sum += weight_(e2);
							return sum;
						} else
							return 0;
					})) + 1;
	}

	private int weight_(Object object) {
		return object instanceof FunExpr ? weight((FunExpr) object) : 0;
	}

}
