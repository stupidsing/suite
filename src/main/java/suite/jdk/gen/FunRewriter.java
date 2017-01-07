package suite.jdk.gen;

import java.lang.reflect.Method;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import suite.inspect.Inspect;
import suite.jdk.gen.FunExpression.ApplyFunExpr;
import suite.jdk.gen.FunExpression.AssignFunExpr;
import suite.jdk.gen.FunExpression.CastFunExpr;
import suite.jdk.gen.FunExpression.Declare1ParameterFunExpr;
import suite.jdk.gen.FunExpression.Declare2ParameterFunExpr;
import suite.jdk.gen.FunExpression.DeclareLocalFunExpr;
import suite.jdk.gen.FunExpression.DeclareParameterFunExpr;
import suite.jdk.gen.FunExpression.FunExpr;

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
				Method method = Helper.instance.method(expr.object);
				return expr.object.invoke( //
						Opcodes.INVOKEINTERFACE, //
						method.getName(), //
						Type.getDescriptor(method.getReturnType()), //
						expr.parameters);
			} else if (e instanceof CastFunExpr) {
				CastFunExpr cfe = (CastFunExpr) e;
				FunExpr expr = cfe.expr;
				if (expr instanceof DeclareParameterFunExpr) {
					((DeclareParameterFunExpr) expr).interfaceClass = Helper.instance.clazz(cfe.type);
					return rewrite(expr);
				} else
					return cfe;
			} else if (e instanceof DeclareParameterFunExpr) {
				DeclareParameterFunExpr e_ = (DeclareParameterFunExpr) e;
				Class<?> pts[] = Helper.instance.method(e_.interfaceClass).getParameterTypes();
				if (e instanceof Declare1ParameterFunExpr) {
					Declare1ParameterFunExpr expr = (Declare1ParameterFunExpr) e_;
					return fc.seq(fe.new NoOperationFunExpr(), expr.doFun.apply(fc.local(1, pts[0])));
				} else if (e instanceof Declare2ParameterFunExpr) {
					Declare2ParameterFunExpr expr = (Declare2ParameterFunExpr) e_;
					return fc.seq(fe.new NoOperationFunExpr(), expr.doFun.apply(fc.local(1, pts[0]), fc.local(2, pts[1])));
				} else
					return null;
			} else if (e instanceof DeclareLocalFunExpr) {
				DeclareLocalFunExpr expr = (DeclareLocalFunExpr) e;
				Type type = FunType.type(expr.value);

				int index = fc.localTypes.size();
				fc.localTypes.add(type);

				AssignFunExpr afe = fe.new AssignFunExpr();
				afe.index = index;
				afe.value = expr.value;

				return fc.seq(afe, expr.doFun.apply(fc.local(index, type)));
			} else
				return null;
		}, expr0);
	}

}
