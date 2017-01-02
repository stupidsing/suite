package suite.jdk.gen;

import java.lang.reflect.Method;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import suite.inspect.Inspect;
import suite.jdk.gen.FunExpression.ApplyFunExpr;
import suite.jdk.gen.FunExpression.AssignFunExpr;
import suite.jdk.gen.FunExpression.Declare1ParameterFunExpr;
import suite.jdk.gen.FunExpression.Declare2ParameterFunExpr;
import suite.jdk.gen.FunExpression.DeclareLocalFunExpr;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunExpression.LocalFunExpr;

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
				Method method = fc.method(expr.object);
				return expr.object.invoke( //
						Opcodes.INVOKEINTERFACE, //
						method.getName(), //
						Type.getDescriptor(method.getReturnType()), //
						expr.parameters);
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
