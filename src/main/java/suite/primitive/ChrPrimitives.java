package suite.primitive;

import java.util.ArrayList;

import suite.adt.pair.Pair;
import suite.primitive.Chars.CharsBuilder;
import suite.primitive.adt.pair.ChrObjPair;
import suite.primitive.streamlet.ChrOutlet;
import suite.primitive.streamlet.ChrStreamlet;
import suite.streamlet.Outlet;
import suite.streamlet.Outlet2;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;

public class ChrPrimitives {

	public interface ChrComparator {
		int compare(char c0, char c1);
	}

	public interface Chr_Obj<T> {
		public T apply(char c);

		public static <T> Fun<ChrOutlet, Streamlet<T>> lift(Chr_Obj<T> fun0) {
			Chr_Obj<T> fun1 = fun0.rethrow();
			return s -> {
				var ts = new ArrayList<T>();
				char c;
				while ((c = s.next()) != ChrFunUtil.EMPTYVALUE)
					ts.add(fun1.apply(c));
				return Read.from(ts);
			};
		}

		public default Chr_Obj<T> rethrow() {
			return i -> {
				try {
					return apply(i);
				} catch (Exception ex) {
					return Fail.t("for " + i, ex);
				}
			};
		}
	}

	public interface ChrObj_Obj<X, Y> {
		public Y apply(char c, X x);

		public default ChrObj_Obj<X, Y> rethrow() {
			return (x, y) -> {
				try {
					return apply(x, y);
				} catch (Exception ex) {
					return Fail.t("for " + x + ":" + y, ex);
				}
			};
		}
	}

	public interface ChrObjPredicate<T> {
		public boolean test(char c, T t);

		public default ChrObjPredicate<T> rethrow() {
			return (c, t) -> {
				try {
					return test(c, t);
				} catch (Exception ex) {
					return Fail.t("for " + c + ":" + t, ex);
				}
			};
		}
	}

	public interface ChrObjSink<T> { // extends ObjCharConsumer<T>
		public void sink2(char c, T t);

		public default ChrObjSink<T> rethrow() {
			return (c, t) -> {
				try {
					sink2(c, t);
				} catch (Exception ex) {
					Fail.t("for " + t, ex);
				}
			};
		}
	}

	public interface ChrObjSource<T> {
		public boolean source2(ChrObjPair<T> pair);
	}

	public interface ChrTest {
		public boolean test(char c);

		public default ChrTest rethrow() {
			return c -> {
				try {
					return test(c);
				} catch (Exception ex) {
					return Fail.t("for " + c, ex);
				}
			};
		}
	}

	public interface ChrSink {
		public void sink(char c);

		public default ChrSink rethrow() {
			return t -> {
				try {
					sink(t);
				} catch (Exception ex) {
					Fail.t("for " + t, ex);
				}
			};
		}
	}

	public interface ChrSource {
		public char source();
	}

	public interface Obj_Chr<T> {
		public char apply(T t);

		public static <T> Fun<Outlet<T>, ChrStreamlet> lift(Obj_Chr<T> fun0) {
			Obj_Chr<T> fun1 = fun0.rethrow();
			return ts -> {
				var b = new CharsBuilder();
				T t;
				while ((t = ts.next()) != null)
					b.append(fun1.apply(t));
				return b.toChars().streamlet();
			};
		}

		public static <T> Obj_Chr<Outlet<T>> sum(Obj_Chr<T> fun0) {
			Obj_Chr<T> fun1 = fun0.rethrow();
			return outlet -> {
				Source<T> source = outlet.source();
				T t;
				var result = (char) 0;
				while ((t = source.source()) != null)
					result += fun1.apply(t);
				return result;
			};
		}

		public default Obj_Chr<T> rethrow() {
			return t -> {
				try {
					return apply(t);
				} catch (Exception ex) {
					return Fail.t("for " + t, ex);
				}
			};
		}
	}

	public interface ObjObj_Chr<X, Y> {
		public char apply(X x, Y y);

		public static <K, V> Obj_Chr<Outlet2<K, V>> sum(ObjObj_Chr<K, V> fun0) {
			ObjObj_Chr<K, V> fun1 = fun0.rethrow();
			return outlet -> {
				Pair<K, V> pair = Pair.of(null, null);
				Source2<K, V> source = outlet.source();
				var result = (char) 0;
				while (source.source2(pair))
					result += fun1.apply(pair.t0, pair.t1);
				return result;
			};
		}

		public default ObjObj_Chr<X, Y> rethrow() {
			return (x, y) -> {
				try {
					return apply(x, y);
				} catch (Exception ex) {
					return Fail.t("for " + x + ":" + y, ex);
				}
			};
		}
	}

}
