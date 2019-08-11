package suite.streamlet;

import static primal.statics.Fail.fail;
import static primal.statics.Fail.failBool;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import primal.Verbs.Build;
import primal.Verbs.New;
import primal.Verbs.Start;
import primal.adt.Pair;
import primal.adt.map.ListMultimap;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Sink;
import primal.fp.Funs.Source;
import primal.fp.Funs2.Fun2;
import primal.primitive.DblPrim.Obj_Dbl;
import primal.primitive.IntPrim;
import primal.primitive.IntPrim.IntSink;
import primal.primitive.IntPrim.Obj_Int;
import primal.primitive.Int_Dbl;
import primal.primitive.Int_Flt;
import primal.primitive.Int_Int;
import primal.primitive.adt.Bytes;
import primal.primitive.adt.Bytes.BytesBuilder;
import primal.primitive.adt.Chars;
import primal.primitive.adt.map.ObjIntMap;
import primal.primitive.fp.AsChr;
import primal.primitive.fp.AsFlt;
import primal.primitive.fp.AsInt;
import primal.primitive.puller.IntPuller;
import primal.primitive.streamlet.FltStreamlet;
import primal.primitive.streamlet.IntStreamlet;
import primal.puller.Puller;
import primal.puller.Puller2;
import primal.streamlet.Streamlet;
import suite.primitive.Bytes_;
import suite.util.To;

public class As {

	public interface Seq<I, O> {
		public O apply(int index, I i);
	}

	public static Fun<Puller<String>, String> conc(String delimiter) {
		return puller -> Build.string(sb -> puller.sink(s -> {
			sb.append(s);
			sb.append(delimiter);
		}));
	}

	public static <T> Streamlet<T> concat(Puller<Streamlet<T>> puller) {
		var list = new ArrayList<T>();
		puller.sink(st1 -> st1.sink(list::add));
		return Read.from(list);
	}

	public static Streamlet<String[]> csv(Puller<Bytes> puller) {
		return Read.from(puller.collect(As::lines_).map(As::csvLine).toList());
	}

	public static Pair<ObjIntMap<String>, Streamlet<String[]>> csvWithHeader(Puller<Bytes> puller) {
		var list = puller.collect(As::lines_).map(As::csvLine).toList();
		var headers = new ObjIntMap<String>();
		Read.from(list.get(0)).index().sink((i, field) -> headers.put(field, i));
		return Pair.of(headers, Read.from(list).skip(1));
	}

	public static <T> Fun<Puller<T>, Void> executeThreads(Sink<T> sink) {
		return puller -> puller.map(t -> New.thread(() -> sink.f(t))).collect(Start::thenJoin);
	}

	public static <T> Fun<IntPuller, Void> executeThreadsByInt(IntSink sink) {
		return puller -> puller.map(t -> New.thread(() -> sink.f(t))).collect(Start::thenJoin);
	}

	public static Fun<IntPuller, FltStreamlet> floats(Int_Flt fun0) {
		var fun1 = fun0.rethrow();
		return ts -> new FltStreamlet(AsFlt.build(b -> {
			int c;
			while ((c = ts.pull()) != IntPrim.EMPTYVALUE)
				b.append(fun1.apply(c));
		})::puller);
	}

	public static InputStream inputStream(Bytes bytes) {
		return new ByteArrayInputStream(bytes.bs, bytes.start, bytes.end - bytes.start);
	}

	public static Fun<IntPuller, IntStreamlet> ints(Int_Int fun0) {
		var fun1 = fun0.rethrow();
		return ts -> new IntStreamlet(AsInt.build(b -> {
			int c;
			while ((c = ts.pull()) != IntPrim.EMPTYVALUE)
				b.append(fun1.apply(c));
		})::puller);
	}

	public static Puller<String> lines(Puller<Bytes> puller) {
		return lines_(puller).map(t -> To.string(t).trim());
	}

	public static <K, V> Map<K, List<V>> listMap(Puller<Pair<K, V>> puller) {
		var map = new HashMap<K, List<V>>();
		puller.sink(pair -> map.computeIfAbsent(pair.k, k_ -> new ArrayList<>()).add(pair.v));
		return map;
	}

	public static <K, V> Map<K, V> map(Puller2<K, V> puller) {
		var map = new HashMap<K, V>();
		return puller.isAll((k, v) -> map.put(k, v) == null || failBool("duplicate key " + k)) ? map : null;
	}

	public static <T> Fun<Puller<T>, Integer> min(Obj_Int<T> fun) {
		return puller -> {
			var source = puller.source();
			var t = source.g();
			int result1;
			if (t != null) {
				var result = fun.apply(t);
				while ((t = source.g()) != null)
					if ((result1 = fun.apply(t)) < result)
						result = result1;
				return result;
			} else
				return null;
		};
	}

	public static <K, V> ListMultimap<K, V> multimap(Puller2<K, List<V>> puller) {
		return new ListMultimap<>(map(puller));
	}

	public static <K, V, T> Fun<Puller2<K, V>, Streamlet<T>> pairMap(Fun2<K, V, T> fun) {
		return puller -> new Streamlet<>(() -> puller.map(fun::apply));
	}

	public static <I, O> Fun<Puller<I>, Puller<O>> sequenced(Seq<I, O> seq) {
		return puller -> Puller.of(new Source<>() {
			private int index;

			public O g() {
				var i = puller.pull();
				return i != null ? seq.apply(index++, i) : null;
			}
		});
	}

	public static <K, V> Map<K, Set<V>> setMap(Puller<Pair<K, V>> puller) {
		var map = new HashMap<K, Set<V>>();
		puller.sink(pair -> map.computeIfAbsent(pair.k, k_ -> new HashSet<>()).add(pair.v));
		return map;
	}

	public static <T> Streamlet<T> streamlet(Puller<T> puller) {
		return Read.from(puller.toList());
	}

	public static String string(Puller<Bytes> puller) {
		return To.string(Bytes.of(puller));
	}

	public static Obj_Dbl<IntPuller> sum(Int_Dbl fun0) {
		var fun1 = fun0.rethrow();
		return puller -> {
			var source = puller.source();
			int c;
			var result = (double) 0;
			while ((c = source.g()) != IntPrim.EMPTYVALUE)
				result += fun1.apply(c);
			return result;
		};
	}

	public static Streamlet<String[]> table(Puller<Bytes> puller) {
		return Read.from(puller.collect(As::lines_) //
				.map(bytes -> To.string(bytes).split("\t")) //
				.toList());
	}

	public static Puller<Chars> utf8decode(Puller<Bytes> bytesPuller) {
		var source = bytesPuller.source();

		return Puller.of(new Source<>() {
			private BytesBuilder bb = new BytesBuilder();

			public Chars g() {
				Chars chars;
				while ((chars = decode()).size() == 0) {
					var bytes = source.g();
					if (bytes != null)
						bb.append(bytes);
					else if (bb.size() == 0)
						return null;
					else
						return fail();
				}
				return chars;
			}

			private Chars decode() {
				var bytes = bb.toBytes();

				return AsChr.build(cb -> {
					var s = 0;

					while (s < bytes.size()) {
						var b0 = Byte.toUnsignedInt(bytes.get(s++));
						int ch, e;
						if (b0 < 0x80) {
							ch = b0;
							e = s;
						} else if (b0 < 0xE0) {
							ch = b0 & 0x1F;
							e = s + 1;
						} else if (b0 < 0xF0) {
							ch = b0 & 0x0F;
							e = s + 2;
						} else if (b0 < 0xF8) {
							ch = b0 & 0x07;
							e = s + 3;
						} else if (b0 < 0xFC) {
							ch = b0 & 0x03;
							e = s + 4;
						} else if (b0 < 0xFE) {
							ch = b0 & 0x01;
							e = s + 5;
						} else
							throw new RuntimeException();
						if (e <= bytes.size()) {
							while (s < e) {
								var b = Byte.toUnsignedInt(bytes.get(s++));
								if ((b & 0xC0) == 0x80)
									ch = (ch << 6) + (b & 0x3F);
								else
									fail();
							}
							cb.append((char) ch);
						} else
							break;
					}

					bb = new BytesBuilder();
					bb.append(bytes.range(s));
				});
			}
		});
	}

	public static Puller<Bytes> utf8encode(Puller<Chars> charsPuller) {
		var source = charsPuller.source();

		return Puller.of(new Source<>() {
			public Bytes g() {
				var chars = source.g();
				if (chars != null) {
					var bb = new BytesBuilder();
					for (var i = 0; i < chars.size(); i++) {
						var ch = chars.get(i);
						if (ch < 0x80)
							bb.append((byte) ch);
						else if (ch < 0x800) {
							bb.append((byte) (0xC0 + ((ch >> 6) & 0x1F)));
							bb.append((byte) (0x80 + ((ch >> 0) & 0x3F)));
						} else if (ch < 0x10000) {
							bb.append((byte) (0xE0 + ((ch >> 12) & 0x0F)));
							bb.append((byte) (0x80 + ((ch >> 6) & 0x3F)));
							bb.append((byte) (0x80 + ((ch >> 0) & 0x3F)));
						} else {
							bb.append((byte) (0xF0 + ((ch >> 18) & 0x07)));
							bb.append((byte) (0x80 + ((ch >> 12) & 0x3F)));
							bb.append((byte) (0x80 + ((ch >> 6) & 0x3F)));
							bb.append((byte) (0x80 + ((ch >> 0) & 0x3F)));
						}
					}
					return bb.toBytes();
				} else
					return null;
			}
		});
	}

	private static String[] csvLine(Bytes bytes) {
		return csvLine(To.string(bytes));
	}

	private static String[] csvLine(String line) {
		var list = new ArrayList<String>();
		var sb = new StringBuilder();
		var length = line.length();
		var p = 0;
		if (0 < length) {
			while (p < length) {
				var ch = line.charAt(p++);
				if (ch == '"')
					while (p < length)
						if ((ch = line.charAt(p++)) == '"' && p < length && line.charAt(p) == '"') {
							sb.append(ch);
							p++;
						} else if (ch != '"')
							sb.append(ch);
						else
							break;
				else if (ch == ',') {
					list.add(sb.toString());
					sb.setLength(0);
				} else
					sb.append(ch);
			}
			list.add(sb.toString());
		}
		return list.toArray(new String[0]);
	}

	private static Puller<Bytes> lines_(Puller<Bytes> puller) {
		return Bytes_.split(Bytes.of((byte) 10)).apply(puller);
	}

}
