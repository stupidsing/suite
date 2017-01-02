package suite.jdk;

import org.objectweb.asm.Opcodes;

import suite.inspect.Inspect;
import suite.jdk.FunExpression.ApplyFunExpr;
import suite.jdk.FunExpression.AssignFunExpr;
import suite.jdk.FunExpression.Declare1ParameterFunExpr;
import suite.jdk.FunExpression.Declare2ParameterFunExpr;
import suite.jdk.FunExpression.DeclareLocalFunExpr;
import suite.jdk.FunExpression.FunExpr;
import suite.jdk.FunExpression.LocalFunExpr;

public class FunRewriter {

	private static Inspect inspect = new Inspect();

	private FunExpression fe;

	public FunRewriter(FunExpression fe) {
		this.fe = fe;
	}

	public FunExpr rewrite(FunExpr expr0) {
		FunCreator<?> fc = fe.fc;

		return inspect.rewrite(FunExpr.class, new Object[] { fe, }, e -> {
			if (e instanceof ApplyFunExpr) {
				ApplyFunExpr expr = (ApplyFunExpr) e;
				return expr.object.invoke(Opcodes.INVOKEINTERFACE, expr.fun.methodName, expr.fun.returnType, expr.parameters);
			} else if (e instanceof Declare1ParameterFunExpr)
				return fc.seq(fe.new NoOperationFunExpr(), ((Declare1ParameterFunExpr) e).doFun.apply(local(1)));
			else if (e instanceof Declare2ParameterFunExpr)
				return fc.seq(fe.new NoOperationFunExpr(), ((Declare2ParameterFunExpr) e).doFun.apply(local(1), local(2)));
			else if (e instanceof DeclareLocalFunExpr) {
				DeclareLocalFunExpr expr = (DeclareLocalFunExpr) e;
				int index = fc.localTypes.size();
				fc.localTypes.add(fc.type(expr.value));

				AssignFunExpr afe = fe.new AssignFunExpr();
				afe.index = index;
				afe.value = expr.value;

				return fc.seq(afe, expr.doFun.apply(local(index)));
			} else
				return null;
		}, expr0);
	}

	private FunExpr local(int number) { // 0 means this
		LocalFunExpr expr = fe.new LocalFunExpr();
		expr.index = number;
		return expr;
	}

}
