package suite.jdk.gen;

import suite.jdk.gen.FunExpression.FunExpr;
import suite.util.Util;

/**
 * Functional expressions that can be handled by FunExpand.java.
 *
 * @author ywsing
 */
public class FunExprK {

	public static class DeclareParameterFunExpr extends FunExpr {
		public FunExpr do_;
	}

	public static class Declare0ParameterFunExpr extends DeclareParameterFunExpr {
	}

	public static class Declare1ParameterFunExpr extends DeclareParameterFunExpr {
		public PlaceholderFunExpr parameter;
	}

	public static class Declare2ParameterFunExpr extends DeclareParameterFunExpr {
		public PlaceholderFunExpr p0, p1;
	}

	public static class PlaceholderFunExpr extends FunExpr {
		public int id = Util.temp();
	}

}
