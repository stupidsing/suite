package suite.jdk.gen.pass;

import static suite.util.Fail.fail;
import static suite.util.Rethrow.ex;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.Type;

import suite.jdk.gen.FunExprK.PlaceholderFunExpr;
import suite.jdk.gen.FunExprL.ApplyFunExpr;
import suite.jdk.gen.FunExprL.DeclareLocalFunExpr;
import suite.jdk.gen.FunExprM.ArrayFunExpr;
import suite.jdk.gen.FunExprM.ArrayLengthFunExpr;
import suite.jdk.gen.FunExprM.AssignLocalFunExpr;
import suite.jdk.gen.FunExprM.BinaryFunExpr;
import suite.jdk.gen.FunExprM.CastFunExpr;
import suite.jdk.gen.FunExprM.CheckCastFunExpr;
import suite.jdk.gen.FunExprM.ConstantFunExpr;
import suite.jdk.gen.FunExprM.FieldStaticFunExpr;
import suite.jdk.gen.FunExprM.FieldTypeFunExpr;
import suite.jdk.gen.FunExprM.FieldTypeSetFunExpr;
import suite.jdk.gen.FunExprM.IfFunExpr;
import suite.jdk.gen.FunExprM.IndexFunExpr;
import suite.jdk.gen.FunExprM.InstanceOfFunExpr;
import suite.jdk.gen.FunExprM.InvokeMethodFunExpr;
import suite.jdk.gen.FunExprM.LocalFunExpr;
import suite.jdk.gen.FunExprM.NewFunExpr;
import suite.jdk.gen.FunExprM.PrintlnFunExpr;
import suite.jdk.gen.FunExprM.ProfileFunExpr;
import suite.jdk.gen.FunExprM.SeqFunExpr;
import suite.jdk.gen.FunExprM.VoidFunExpr;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.Type_;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.Read;
import suite.util.Util;

public class FunTypeInformation {

	private List<Type> localTypes;
	private Fun<PlaceholderFunExpr, FunExpr> placeholderResolver;

	public FunTypeInformation(List<Type> localTypes, Fun<PlaceholderFunExpr, FunExpr> placeholderResolver) {
		this.localTypes = localTypes;
		this.placeholderResolver = placeholderResolver;
	}

	public Type typeOf(FunExpr e0) {
		return e0.<Type> switch_( //
		).applyIf(ArrayFunExpr.class, e1 -> e1.apply((clazz, elements) -> {
			var type = Type.getType(e1.clazz);
			for (var element : elements)
				if (element != null && type != typeOf(element))
					fail();
			return clazz.isPrimitive() ? new ArrayType(type.getType(), 1) : new ArrayType(type, 1);
		})).applyIf(ArrayLengthFunExpr.class, e1 -> {
			return Type.INT;
		}).applyIf(ApplyFunExpr.class, e1 -> e1.apply((object, parameters) -> {
			return Type.getType(methodOf(object).getReturnType());
		})).applyIf(AssignLocalFunExpr.class, e1 -> {
			return Type.VOID;
		}).applyIf(BinaryFunExpr.class, e1 -> {
			return typeOf(e1.left);
		}).applyIf(CastFunExpr.class, e1 -> {
			return e1.type;
		}).applyIf(CheckCastFunExpr.class, e1 -> {
			return e1.type;
		}).applyIf(ConstantFunExpr.class, e1 -> {
			return e1.type;
		}).applyIf(DeclareLocalFunExpr.class, e1 -> {
			return typeOf(e1.do_);
		}).applyIf(FieldStaticFunExpr.class, e1 -> {
			return e1.fieldType;
		}).applyIf(FieldTypeFunExpr.class, e1 -> {
			return e1.fieldType;
		}).applyIf(FieldTypeSetFunExpr.class, e1 -> {
			return Type.VOID;
		}).applyIf(IfFunExpr.class, e1 -> {
			return typeOf(e1.then);
		}).applyIf(IndexFunExpr.class, e1 -> {
			return ((ArrayType) typeOf(e1.array)).getElementType();
		}).applyIf(InstanceOfFunExpr.class, e1 -> {
			return Type.BOOLEAN;
		}).applyIf(InvokeMethodFunExpr.class, e1 -> {
			return Type.getType(invokeMethodOf(e1).getReturnType());
		}).applyIf(LocalFunExpr.class, e1 -> {
			return localTypes.get(e1.index);
		}).applyIf(NewFunExpr.class, e1 -> {
			return Type.getType(e1.interfaceClass);
		}).applyIf(PlaceholderFunExpr.class, e1 -> {
			return typeOf(placeholderResolver.apply(e1));
		}).applyIf(PrintlnFunExpr.class, e1 -> {
			return Type.VOID;
		}).applyIf(ProfileFunExpr.class, e1 -> {
			return typeOf(e1.do_);
		}).applyIf(SeqFunExpr.class, e1 -> {
			return typeOf(e1.left) == Type.VOID ? typeOf(e1.right) : fail();
		}).applyIf(VoidFunExpr.class, e1 -> {
			return Type.VOID;
		}).nonNullResult();
	}

	public Method invokeMethodOf(InvokeMethodFunExpr expr) {
		var array = Read.from(expr.parameters).map(this::typeOf).toArray(Type.class);

		@SuppressWarnings("rawtypes")
		var parameterTypes = Read.from(array).<Class> map(Type_::classOf).toArray(Class.class);

		return ex(() -> {
			var clazz0 = expr.clazz;
			var clazz1 = clazz0 != null ? clazz0 : classOf(expr.object);
			return clazz1.getMethod(expr.methodName, parameterTypes);
		});
	}

	public Method methodOf(FunExpr e) {
		return Util.methodOf(classOf(e));
	}

	public Class<?> classOf(FunExpr e) {
		return Type_.classOf(typeOf(e));
	}

}
