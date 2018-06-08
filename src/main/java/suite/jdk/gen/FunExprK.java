package suite.jdk.gen;

import suite.adt.pair.Fixie_.FixieFun0;
import suite.adt.pair.Fixie_.FixieFun1;
import suite.adt.pair.Fixie_.FixieFun2;
import suite.adt.pair.Fixie_.FixieFun3;
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
		public <R> R apply(FixieFun0<R> fun) {
			return fun.apply();
		}

		public <R> R apply(FixieFun1<FunExpr, R> fun) {
			return fun.apply(do_);
		}
	}

	public static class Declare1ParameterFunExpr extends DeclareParameterFunExpr {
		public PlaceholderFunExpr parameter;

		public <R> R apply(FixieFun2<PlaceholderFunExpr, FunExpr, R> fun) {
			return fun.apply(parameter, do_);
		}
	}

	public static class Declare2ParameterFunExpr extends DeclareParameterFunExpr {
		public PlaceholderFunExpr p0, p1;

		public <R> R apply(FixieFun3<PlaceholderFunExpr, PlaceholderFunExpr, FunExpr, R> fun) {
			return fun.apply(p0, p1, do_);
		}
	}

	public static class PlaceholderFunExpr extends FunExpr {
		public int id = Util.temp();

		public <R> R apply(FixieFun0<R> fun) {
			return fun.apply();
		}
	}

}
