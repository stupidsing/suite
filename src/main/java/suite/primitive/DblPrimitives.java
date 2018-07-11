package suite.primitive; import static suite.util.Friends.fail;

import java.util.ArrayList;

import suite.adt.pair.Pair;
import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.adt.pair.DblObjPair;
import suite.primitive.streamlet.DblOutlet;
import suite.primitive.streamlet.DblStreamlet;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.FunUtil2.Source2;
import suite.streamlet.Outlet;
import suite.streamlet.Outlet2;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public class DblPrimitives {

	public interface DblComparator {
		int compare(double c0, double c1);
	}

	public interface Dbl_Obj<T> {
		public T apply(double c);

		public static <T> Fun<DblOutlet, Streamlet<T>> lift(Dbl_Obj<T> fun0) {
			Dbl_Obj<T> fun1 = fun0.rethrow();
			return s -> {
				var ts = new ArrayList<T>();
				double c;
				while ((c = s.next()) != DblFunUtil.EMPTYVALUE)
					ts.add(fun1.apply(c));
				return Read.from(ts);
			};
		}

		public default Dbl_Obj<T> rethrow() {
			return i -> {
				try {
					return apply(i);
				} catch (Exception ex) {
					return fail("for " + i, ex);
				}
			};
		}
	}

	public interface DblObj_Obj<X, Y> {
		public Y apply(double c, X x);

		public default DblObj_Obj<X, Y> rethrow() {
			return (x, y) -> {
				try {
					return apply(x, y);
				} catch (Exception ex) {
					return fail("for " + x + ":" + y, ex);
				}
			};
		}
	}

	public interface DblObjPredicate<T> {
		public boolean test(double c, T t);

		public default DblObjPredicate<T> rethrow() {
			return (c, t) -> {
				try {
					return test(c, t);
				} catch (Exception ex) {
					return fail("for " + c + ":" + t, ex);
				}
			};
		}
	}

	public interface DblObjSink<T> { // extends ObjCharConsumer<T>
		public void sink2(double c, T t);

		public default DblObjSink<T> rethrow() {
			return (c, t) -> {
				try {
					sink2(c, t);
				} catch (Exception ex) {
					fail("for " + t, ex);
				}
			};
		}
	}

	public interface DblObjSource<T> {
		public boolean source2(DblObjPair<T> pair);
	}

	public interface DblTest {
		public boolean test(double c);

		public default DblTest rethrow() {
			return c -> {
				try {
					return test(c);
				} catch (Exception ex) {
					return fail("for " + c, ex);
				}
			};
		}
	}

	public interface DblSink {
		public void sink(double c);

		public default DblSink rethrow() {
			return t -> {
				try {
					sink(t);
				} catch (Exception ex) {
					fail("for " + t, ex);
				}
			};
		}
	}

	public interface DblSource {
		public double source();
	}

	public interface Obj_Dbl<T> {
		public double apply(T t);

		public static <T> Fun<Outlet<T>, DblStreamlet> lift(Obj_Dbl<T> fun0) {
			Obj_Dbl<T> fun1 = fun0.rethrow();
			return ts -> {
				var b = new DoublesBuilder();
				T t;
				while ((t = ts.next()) != null)
					b.append(fun1.apply(t));
				return b.toDoubles().streamlet();
			};
		}

		public static <T> Obj_Dbl<Outlet<T>> sum(Obj_Dbl<T> fun0) {
			Obj_Dbl<T> fun1 = fun0.rethrow();
			return outlet -> {
				Source<T> source = outlet.source();
				T t;
				var result = (double) 0;
				while ((t = source.source()) != null)
					result += fun1.apply(t);
				return result;
			};
		}

		public default Obj_Dbl<T> rethrow() {
			return t -> {
				try {
					return apply(t);
				} catch (Exception ex) {
					return fail("for " + t, ex);
				}
			};
		}
	}

	public interface ObjObj_Dbl<X, Y> {
		public double apply(X x, Y y);

		public static <K, V> Obj_Dbl<Outlet2<K, V>> sum(ObjObj_Dbl<K, V> fun0) {
			ObjObj_Dbl<K, V> fun1 = fun0.rethrow();
			return outlet -> {
				Pair<K, V> pair = Pair.of(null, null);
				Source2<K, V> source = outlet.source();
				var result = (double) 0;
				while (source.source2(pair))
					result += fun1.apply(pair.t0, pair.t1);
				return result;
			};
		}

		public default ObjObj_Dbl<X, Y> rethrow() {
			return (x, y) -> {
				try {
					return apply(x, y);
				} catch (Exception ex) {
					return fail("for " + x + ":" + y, ex);
				}
			};
		}
	}

}
