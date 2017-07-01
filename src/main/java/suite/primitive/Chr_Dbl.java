package suite.primitive;

import suite.primitive.ChrPrimitives.ChrSource;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.streamlet.ChrOutlet;
import suite.primitive.streamlet.DblStreamlet;
import suite.util.FunUtil.Fun;

@FunctionalInterface
public interface Chr_Dbl {

	public double apply(char c);

	public static Fun<ChrOutlet, DblStreamlet> lift(Chr_Dbl fun0) {
		Chr_Dbl fun1 = fun0.rethrow();
		return ts -> {
			DoublesBuilder b = new DoublesBuilder();
			char c;
			while ((c = ts.next()) != ChrFunUtil.EMPTYVALUE)
				b.append(fun1.apply(c));
			return b.toDoubles().streamlet();
		};
	}

	public static Obj_Dbl<ChrOutlet> sum(Chr_Dbl fun0) {
		Chr_Dbl fun1 = fun0.rethrow();
		return outlet -> {
			ChrSource source = outlet.source();
			char c;
			double result = (double) 0;
			while ((c = source.source()) != ChrFunUtil.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public default Chr_Dbl rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
