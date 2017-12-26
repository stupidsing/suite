package suite.jdk.gen.pass;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.Type;

import suite.jdk.gen.FunExprK.PlaceholderFunExpr;
import suite.jdk.gen.FunExprL.ApplyFunExpr;
import suite.jdk.gen.FunExprL.DeclareLocalFunExpr;
import suite.jdk.gen.FunExprM.ArrayFunExpr;
import suite.jdk.gen.FunExprM.AssignLocalFunExpr;
import suite.jdk.gen.FunExprM.BinaryFunExpr;
import suite.jdk.gen.FunExprM.CastFunExpr;
import suite.jdk.gen.FunExprM.CheckCastFunExpr;
import suite.jdk.gen.FunExprM.ConstantFunExpr;
import suite.jdk.gen.FunExprM.FieldStaticFunExpr;
import suite.jdk.gen.FunExprM.FieldTypeFunExpr;
import suite.jdk.gen.FunExprM.IfFunExpr;
import suite.jdk.gen.FunExprM.IndexFunExpr;
import suite.jdk.gen.FunExprM.InstanceOfFunExpr;
import suite.jdk.gen.FunExprM.InvokeMethodFunExpr;
import suite.jdk.gen.FunExprM.LocalFunExpr;
import suite.jdk.gen.FunExprM.NewFunExpr;
import suite.jdk.gen.FunExprM.PrintlnFunExpr;
import suite.jdk.gen.FunExprM.ProfileFunExpr;
import suite.jdk.gen.FunExprM.SeqFunExpr;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.Type_;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.Rethrow;

public class FunTypeInformation {

	private List<Type> localTypes;
	private Fun<PlaceholderFunExpr, FunExpr> placeholderResolver;

	public FunTypeInformation(List<Type> localTypes, Fun<PlaceholderFunExpr, FunExpr> placeholderResolver) {
		this.localTypes = localTypes;
		this.placeholderResolver = placeholderResolver;
	}

	public Type typeOf(FunExpr e0) {
		if (e0 instanceof ArrayFunExpr) {
			ArrayFunExpr e1 = (ArrayFunExpr) e0;
			Type type = Type.getType(e1.clazz);
			for (FunExpr element : e1.elements)
				if (element != null && type != typeOf(element))
					throw new RuntimeException();
			return e1.clazz.isPrimitive() ? new ArrayType(type.getType(), 1) : new ArrayType(type, 1);
		} else if (e0 instanceof ApplyFunExpr) {
			ApplyFunExpr e1 = (ApplyFunExpr) e0;
			return Type.getType(methodOf(e1.object).getReturnType());
		} else if (e0 instanceof AssignLocalFunExpr)
			return Type.VOID;
		else if (e0 instanceof BinaryFunExpr) {
			BinaryFunExpr e1 = (BinaryFunExpr) e0;
			return typeOf(e1.left);
		} else if (e0 instanceof CastFunExpr) {
			CastFunExpr e1 = (CastFunExpr) e0;
			return e1.type;
		} else if (e0 instanceof CheckCastFunExpr) {
			CheckCastFunExpr e1 = (CheckCastFunExpr) e0;
			return e1.type;
		} else if (e0 instanceof ConstantFunExpr) {
			ConstantFunExpr e1 = (ConstantFunExpr) e0;
			return e1.type;
		} else if (e0 instanceof DeclareLocalFunExpr) {
			DeclareLocalFunExpr e1 = (DeclareLocalFunExpr) e0;
			return typeOf(e1.do_);
		} else if (e0 instanceof FieldTypeFunExpr) {
			FieldTypeFunExpr e1 = (FieldTypeFunExpr) e0;
			return e1.fieldType;
		} else if (e0 instanceof IfFunExpr) {
			IfFunExpr e1 = (IfFunExpr) e0;
			return typeOf(e1.then);
		} else if (e0 instanceof IndexFunExpr) {
			IndexFunExpr e1 = (IndexFunExpr) e0;
			return ((ArrayType) typeOf(e1.array)).getElementType();
		} else if (e0 instanceof InstanceOfFunExpr)
			return Type.BOOLEAN;
		else if (e0 instanceof InvokeMethodFunExpr) {
			InvokeMethodFunExpr e1 = (InvokeMethodFunExpr) e0;
			return Type.getType(invokeMethodOf(e1).getReturnType());
		} else if (e0 instanceof LocalFunExpr) {
			LocalFunExpr e1 = (LocalFunExpr) e0;
			return localTypes.get(e1.index);
		} else if (e0 instanceof NewFunExpr) {
			NewFunExpr e1 = (NewFunExpr) e0;
			return Type.getType(e1.interfaceClass);
		} else if (e0 instanceof PlaceholderFunExpr) {
			PlaceholderFunExpr e1 = (PlaceholderFunExpr) e0;
			return typeOf(placeholderResolver.apply(e1));
		} else if (e0 instanceof PrintlnFunExpr)
			return Type.VOID;
		else if (e0 instanceof ProfileFunExpr) {
			ProfileFunExpr e1 = (ProfileFunExpr) e0;
			return typeOf(e1.do_);
		} else if (e0 instanceof SeqFunExpr) {
			SeqFunExpr e1 = (SeqFunExpr) e0;
			return typeOf(e1.right);
		} else if (e0 instanceof FieldStaticFunExpr) {
			FieldStaticFunExpr e1 = (FieldStaticFunExpr) e0;
			return e1.fieldType;
		} else
			throw new RuntimeException("unknown expression " + e0.getClass());
	}

	public Method invokeMethodOf(InvokeMethodFunExpr expr) {
		Type[] array = Read //
				.from(expr.parameters) //
				.map(this::typeOf) //
				.toArray(Type.class);

		@SuppressWarnings("rawtypes")
		Class<?>[] parameterTypes = Read //
				.from(array) //
				.<Class> map(Type_::classOf) //
				.toArray(Class.class);

		return Rethrow.ex(() -> {
			Class<?> clazz0 = expr.clazz;
			Class<?> clazz1 = clazz0 != null ? clazz0 : classOf(expr.object);
			return clazz1.getMethod(expr.methodName, parameterTypes);
		});
	}

	public Method methodOf(FunExpr e) {
		return Type_.methodOf(classOf(e));
	}

	public Class<?> classOf(FunExpr e) {
		return Type_.classOf(typeOf(e));
	}

}
