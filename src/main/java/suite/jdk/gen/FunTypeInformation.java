package suite.jdk.gen;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.generic.Type;

import suite.jdk.gen.FunExpression.ApplyFunExpr;
import suite.jdk.gen.FunExpression.AssignFunExpr;
import suite.jdk.gen.FunExpression.BinaryFunExpr;
import suite.jdk.gen.FunExpression.CastFunExpr;
import suite.jdk.gen.FunExpression.CheckCastFunExpr;
import suite.jdk.gen.FunExpression.ConstantFunExpr;
import suite.jdk.gen.FunExpression.DeclareLocalFunExpr;
import suite.jdk.gen.FunExpression.DeclareParameterFunExpr;
import suite.jdk.gen.FunExpression.FieldTypeFunExpr;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunExpression.IfFunExpr;
import suite.jdk.gen.FunExpression.InstanceOfFunExpr;
import suite.jdk.gen.FunExpression.InvokeMethodFunExpr;
import suite.jdk.gen.FunExpression.LocalFunExpr;
import suite.jdk.gen.FunExpression.PlaceholderFunExpr;
import suite.jdk.gen.FunExpression.PrintlnFunExpr;
import suite.jdk.gen.FunExpression.SeqFunExpr;
import suite.jdk.gen.FunExpression.StaticFunExpr;
import suite.streamlet.Read;
import suite.util.Rethrow;

public class FunTypeInformation {

	public final Map<DeclareParameterFunExpr, Class<?>> interfaceClasses = new HashMap<>();

	private Map<PlaceholderFunExpr, Type> placeholders = new HashMap<>();

	public Method invokeMethodOf(InvokeMethodFunExpr expr) {
		Type array[] = Read.from(expr.parameters) //
				.map(this::typeOf) //
				.toList() //
				.toArray(new Type[0]);

		@SuppressWarnings("unchecked")
		List<Class<?>> parameterTypes = (List<Class<?>>) (List<?>) Read.from(array) //
				.map(Type_::classOf) //
				.toList();

		return Rethrow.reflectiveOperationException(() -> {
			return classOf(expr.object).getMethod(expr.methodName, parameterTypes.toArray(new Class<?>[0]));
		});
	}

	public Method methodOf(FunExpr e) {
		return Type_.methodOf(classOf(e));
	}

	public Class<?> classOf(FunExpr e) {
		return Type_.classOf(typeOf(e));
	}

	public Type typeOf(FunExpr e) {
		if (e instanceof ApplyFunExpr) {
			ApplyFunExpr expr = (ApplyFunExpr) e;
			return Type.getType(methodOf(expr.object).getReturnType());
		} else if (e instanceof AssignFunExpr)
			return Type.getType(void.class);
		else if (e instanceof BinaryFunExpr) {
			BinaryFunExpr expr = (BinaryFunExpr) e;
			return typeOf(expr.left);
		} else if (e instanceof CastFunExpr) {
			CastFunExpr expr = (CastFunExpr) e;
			return expr.type;
		} else if (e instanceof CheckCastFunExpr) {
			CheckCastFunExpr expr = (CheckCastFunExpr) e;
			return expr.type;
		} else if (e instanceof ConstantFunExpr) {
			ConstantFunExpr expr = (ConstantFunExpr) e;
			return expr.type;
		} else if (e instanceof DeclareLocalFunExpr) {
			DeclareLocalFunExpr expr = (DeclareLocalFunExpr) e;
			return typeOf(expr.do_);
		} else if (e instanceof DeclareParameterFunExpr) {
			DeclareParameterFunExpr expr = (DeclareParameterFunExpr) e;
			return Type.getType(interfaceClasses.get(expr));
		} else if (e instanceof FieldTypeFunExpr) {
			FieldTypeFunExpr expr = (FieldTypeFunExpr) e;
			return expr.type;
		} else if (e instanceof IfFunExpr) {
			IfFunExpr expr = (IfFunExpr) e;
			return typeOf(expr.then);
		} else if (e instanceof InstanceOfFunExpr)
			return Type.BOOLEAN;
		else if (e instanceof InvokeMethodFunExpr) {
			InvokeMethodFunExpr expr = (InvokeMethodFunExpr) e;
			return Type.getType(invokeMethodOf(expr).getReturnType());
		} else if (e instanceof LocalFunExpr) {
			LocalFunExpr expr = (LocalFunExpr) e;
			return expr.type;
		} else if (e instanceof PlaceholderFunExpr)
			return placeholders.get((PlaceholderFunExpr) e);
		else if (e instanceof PrintlnFunExpr)
			return Type.VOID;
		else if (e instanceof SeqFunExpr) {
			SeqFunExpr expr = (SeqFunExpr) e;
			return typeOf(expr.right);
		} else if (e instanceof StaticFunExpr) {
			StaticFunExpr expr = (StaticFunExpr) e;
			return expr.type;
		} else
			throw new RuntimeException("Unknown expression " + e.getClass());
	}

}
