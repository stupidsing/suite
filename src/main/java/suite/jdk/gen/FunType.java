package suite.jdk.gen;

import org.objectweb.asm.Type;

import suite.jdk.gen.FunExpression.ApplyFunExpr;
import suite.jdk.gen.FunExpression.AssignFunExpr;
import suite.jdk.gen.FunExpression.BinaryFunExpr;
import suite.jdk.gen.FunExpression.CastFunExpr;
import suite.jdk.gen.FunExpression.CheckCastFunExpr;
import suite.jdk.gen.FunExpression.ConstantFunExpr;
import suite.jdk.gen.FunExpression.DeclareLocalFunExpr;
import suite.jdk.gen.FunExpression.DeclareParameterFunExpr;
import suite.jdk.gen.FunExpression.FieldFunExpr;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunExpression.If1FunExpr;
import suite.jdk.gen.FunExpression.If2FunExpr;
import suite.jdk.gen.FunExpression.InstanceOfFunExpr;
import suite.jdk.gen.FunExpression.InvokeFunExpr;
import suite.jdk.gen.FunExpression.LocalFunExpr;
import suite.jdk.gen.FunExpression.NoOperationFunExpr;
import suite.jdk.gen.FunExpression.PrintlnFunExpr;
import suite.jdk.gen.FunExpression.SeqFunExpr;
import suite.jdk.gen.FunExpression.StaticFunExpr;

public class FunType<I> {

	public static String typeDesc(FunExpr e) {
		return typeOf(e).getDescriptor();
	}

	public static Type typeOf(FunExpr e) {
		if (e instanceof ApplyFunExpr) {
			ApplyFunExpr expr = (ApplyFunExpr) e;
			return Type.getType(Helper.instance.method(expr.object).getReturnType());
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
			return typeOf(expr.doFun.apply(expr.value));
		} else if (e instanceof DeclareParameterFunExpr) {
			DeclareParameterFunExpr expr = (DeclareParameterFunExpr) e;
			return Type.getType(expr.interfaceClass);
		} else if (e instanceof FieldFunExpr) {
			FieldFunExpr expr = (FieldFunExpr) e;
			return expr.type;
		} else if (e instanceof If1FunExpr) {
			If1FunExpr expr = (If1FunExpr) e;
			return typeOf(expr.then);
		} else if (e instanceof If2FunExpr) {
			If2FunExpr expr = (If2FunExpr) e;
			return typeOf(expr.then);
		} else if (e instanceof InstanceOfFunExpr)
			return Type.getType(boolean.class);
		else if (e instanceof InvokeFunExpr) {
			InvokeFunExpr expr = (InvokeFunExpr) e;
			return Type.getType(expr.method().getReturnType());
		} else if (e instanceof LocalFunExpr) {
			LocalFunExpr expr = (LocalFunExpr) e;
			return expr.type;
		} else if (e instanceof NoOperationFunExpr)
			return Type.getType(void.class);
		else if (e instanceof PrintlnFunExpr)
			return Type.getType(void.class);
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
