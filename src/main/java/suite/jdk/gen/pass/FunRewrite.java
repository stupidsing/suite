package suite.jdk.gen.pass;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.generic.Type;

import suite.adt.pair.Pair;
import suite.jdk.gen.FunCreator;
import suite.jdk.gen.FunExprK.Declare0ParameterFunExpr;
import suite.jdk.gen.FunExprK.Declare1ParameterFunExpr;
import suite.jdk.gen.FunExprK.Declare2ParameterFunExpr;
import suite.jdk.gen.FunExprK.DeclareParameterFunExpr;
import suite.jdk.gen.FunExprK.PlaceholderFunExpr;
import suite.jdk.gen.FunExprL.ApplyFunExpr;
import suite.jdk.gen.FunExprL.DeclareLocalFunExpr;
import suite.jdk.gen.FunExprL.FieldFunExpr_;
import suite.jdk.gen.FunExprL.FieldInjectFunExpr;
import suite.jdk.gen.FunExprL.FieldSetFunExpr;
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
import suite.jdk.lambda.LambdaInterface;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.util.Switch;
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
		return e0.sw( //
		).applyIf(CastFunExpr.class, //
				e1 -> e1.expr.sw( //
				).applyIf(Declare0ParameterFunExpr.class, e2 -> e2.apply(do_ -> {
					return rewrite(e2.do_);
				})).applyIf(Declare1ParameterFunExpr.class, e2 -> e2.apply((parameter, do_) -> {
					placeholders.put(parameter, local(1));
					return rewrite(do_);
				})).applyIf(Declare2ParameterFunExpr.class, e2 -> e2.apply((p0, p1, do_) -> {
					placeholders.put(p0, local(1));
					placeholders.put(p1, local(2));
					return rewrite(do_);
				})).nonNullResult() //
		).nonNullResult();
	}

	private FunExpr rewrite(FunExpr expr0) {
		return rewrite(this::rewrite_, expr0);
	}

	private FunExpr rewrite_(FunExpr e0) {
		return e0.sw( //
		).applyIf(ApplyFunExpr.class, e1 -> e1.apply((object, parameters) -> {
			var object1 = rewrite(object);
			var parameters1 = Read.from(parameters).map(this::rewrite).toArray(FunExpr.class);
			var method = fti.methodOf(object1);
			return object1.invoke(method.getName(), parameters1);
		})).applyIf(CastFunExpr.class, e1 -> e1.apply((type, e2) -> {
			if (e2 instanceof DeclareParameterFunExpr) {
				var interfaceClass = Type_.classOf(type);
				var fieldTypes = new HashMap<String, Type>();
				var fieldValues = new HashMap<String, FunExpr>();

				var e3 = rewrite(e -> {
					return new Switch<FunExpr>(e //
					).applyIf(FieldStaticFunExpr.class, e4 -> e4.apply((fieldName, ft) -> {
						var fieldType = fieldTypes.get(fieldName);
						fieldTypes.put(fieldName, fieldType);
						fieldValues.put(fieldName, e4);
						return e4;
					})).applyIf(PlaceholderFunExpr.class, e4 -> {
						FunExpr fieldValue = placeholders.get(e);
						if (fieldValue != null) {
							var fieldName = "e_" + Util.temp();
							var fieldType = fti.typeOf(fieldValue);
							fieldTypes.put(fieldName, fieldType);
							fieldValues.put(fieldName, fieldValue);
							return this_().field(fieldName, fieldType);
						} else
							return null;
					}).result();
				}, e2);

				var cc = FunCreator.of(LambdaInterface.of(interfaceClass), fieldTypes).create_(e3);
				var fieldValues0 = Read.from2(cc.fieldTypeValues).mapValue(tv -> objectField(tv.v, tv.k));
				var fieldValues1 = Read.from2(fieldValues);

				var e4 = new NewFunExpr();
				e4.className = cc.className;
				e4.fieldValues = Streamlet2.concat(fieldValues0, fieldValues1).toMap();
				e4.implementationClass = cc.clazz;
				e4.interfaceClass = interfaceClass;
				return e4;
			} else
				return null;
		})).applyIf(DeclareLocalFunExpr.class, e1 -> e1.apply((var, value, do_) -> {
			var value1 = rewrite(value);
			var lfe = local(localTypes.size());
			localTypes.add(fti.typeOf(value1));

			var alfe = new AssignLocalFunExpr();
			alfe.var = lfe;
			alfe.value = value1;

			placeholders.put(var, lfe);
			return seq(alfe, rewrite(do_));
		})).applyIf(FieldFunExpr_.class, e1 -> e1.apply((fieldName, object) -> {
			var set = e1 instanceof FieldSetFunExpr ? ((FieldSetFunExpr) e1).value : null;
			var object0 = rewrite(object);
			var clazz = fti.classOf(object0);
			var field = ex(() -> clazz.getField(fieldName));
			var object1 = object0.cast_(field.getDeclaringClass());
			var fieldType = Type.getType(field.getType());
			return set == null ? object1.field(fieldName, fieldType) : object1.fieldSet(fieldName, fieldType, set);
		})).applyIf(FieldInjectFunExpr.class, e1 -> e1.apply(fieldName -> {
			var type = fieldTypes.get(fieldName);
			return type != null ? rewrite(this_().field(fieldName, type)) : fail(e1.fieldName);
		})).applyIf(InvokeLambdaFunExpr.class, e1 -> e1.apply((isExpand, l_inst, ps) -> {
			var l_impl = l_inst.lambdaImplementation;
			var l_iface = l_impl.lambdaInterface;
			var object = object_(l_impl.newFun(l_inst.fieldValueByNames), l_iface.interfaceClass);
			return rewrite(object.invoke(l_iface.interfaceClass, l_iface.methodName, ps));
		})).applyIf(ObjectFunExpr.class, e1 -> e1.apply((type, object) -> {
			return objectField(object, type);
		})).applyIf(PlaceholderFunExpr.class, e1 -> {
			var e2 = placeholders.get(e1);
			return e2 != null ? e2 : fail("cannot resolve placeholder");
		}).applyIf(ProfileFunExpr.class, e1 -> e1.apply((counterFieldName, do_) -> {
			fieldTypeValues.put(counterFieldName, Pair.of(Type.INT, 0));
			return null;
		})).result();
	}

	private FunExpr objectField(Object object, Type type) {
		var fieldName = "o_" + Util.temp();
		fieldTypeValues.put(fieldName, Pair.of(type, object));
		return rewrite(this_().field(fieldName, type));
	}

	private FunExpr this_() {
		return local(0);
	}

}
