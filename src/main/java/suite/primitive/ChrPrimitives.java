package suite.primitive;

import static suite.util.Friends.fail;

import java.util.ArrayList;

import suite.adt.pair.Pair;
import suite.primitive.Chars.CharsBuilder;
import suite.primitive.adt.pair.ChrObjPair;
import suite.primitive.streamlet.ChrPuller;
import suite.primitive.streamlet.ChrStreamlet;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.Puller;
import suite.streamlet.Puller2;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public class ChrPrimitives {

	public interface ChrComparator {
		int compare(char c0, char c1);
	}

	public interface Chr_Obj<T> {
		public T apply(char c);

		public static <T> Fun<ChrPuller, Streamlet<T>> lift(Chr_Obj<T> fun0) {
			var fun1 = fun0.rethrow();
			return s -> {
				var ts = new ArrayList<T>();
				char c;
				while ((c = s.pull()) != ChrFunUtil.EMPTYVALUE)
					ts.add(fun1.apply(c));
				return Read.from(ts);
			};
		}

		public default Chr_Obj<T> rethrow() {
			return i -> {
				try {
					return apply(i);
				} catch (Exception ex) {
					return fail("for " + i, ex);
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
					return fail("for " + x + ":" + y, ex);
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
					return fail("for " + c + ":" + t, ex);
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
					fail("for " + t, ex);
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
					return fail("for " + c, ex);
				}
			};
		}
	}

	public interface ChrSink {
		public void f(char c);

		public default ChrSink rethrow() {
			return t -> {
				try {
					f(t);
				} catch (Exception ex) {
					fail("for " + t, ex);
				}
			};
		}
	}

	public interface ChrSource {
		public char g();
	}

	public interface Obj_Chr<T> {
		public char apply(T t);

		public static <T> Fun<Puller<T>, ChrStreamlet> lift(Obj_Chr<T> fun0) {
			var fun1 = fun0.rethrow();
			return ts -> {
				var b = new CharsBuilder();
				T t;
				while ((t = ts.pull()) != null)
					b.append(fun1.apply(t));
				return b.toChars().streamlet();
			};
		}

		public static <T> Obj_Chr<Puller<T>> sum(Obj_Chr<T> fun0) {
			var fun1 = fun0.rethrow();
			return puller -> {
				var source = puller.source();
				T t;
				var result = (char) 0;
				while ((t = source.g()) != null)
					result += fun1.apply(t);
				return result;
			};
		}

		public default Obj_Chr<T> rethrow() {
			return t -> {
				try {
					return apply(t);
				} catch (Exception ex) {
					return fail("for " + t, ex);
				}
			};
		}
	}

	public interface ObjObj_Chr<X, Y> {
		public char apply(X x, Y y);

		public static <K, V> Obj_Chr<Puller2<K, V>> sum(ObjObj_Chr<K, V> fun0) {
			ObjObj_Chr<K, V> fun1 = fun0.rethrow();
			return puller -> {
				var pair = Pair.<K, V> of(null, null);
				var source = puller.source();
				var result = (char) 0;
				while (source.source2(pair))
					result += fun1.apply(pair.k, pair.v);
				return result;
			};
		}

		public default ObjObj_Chr<X, Y> rethrow() {
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
