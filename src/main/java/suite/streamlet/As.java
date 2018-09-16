package suite.streamlet;

import static suite.util.Friends.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.adt.map.ListMultimap;
import suite.adt.pair.Pair;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.primitive.Bytes_;
import suite.primitive.Chars;
import suite.primitive.Chars.CharsBuilder;
import suite.primitive.IntPrimitives.IntSink;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.streamlet.IntOutlet;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.FunUtil2.Fun2;
import suite.util.Fail;
import suite.util.Thread_;
import suite.util.To;

public class As {

	public interface Seq<I, O> {
		public O apply(int index, I i);
	}

	public static Fun<Outlet<String>, String> conc(String delimiter) {
		return outlet -> {
			var sb = new StringBuilder();
			outlet.sink(new Sink<>() {
				public void sink(String s) {
					sb.append(s);
					sb.append(delimiter);
				}
			});
			return sb.toString();
		};
	}

	public static <T> Streamlet<T> concat(Outlet<Streamlet<T>> outlet) {
		var list = new ArrayList<T>();
		outlet.sink(st1 -> st1.sink(list::add));
		return Read.from(list);
	}

	public static Streamlet<String[]> csv(Outlet<Bytes> outlet) {
		return Read.from(outlet.collect(As::lines_).map(As::csvLine).toList());
	}

	public static <T> Fun<Outlet<T>, Void> executeThreads(Sink<T> sink) {
		return outlet -> outlet.map(t -> Thread_.newThread(() -> sink.sink(t))).collect(Thread_::startJoin);
	}

	public static <T> Fun<IntOutlet, Void> executeThreadsByInt(IntSink sink) {
		return outlet -> outlet.map(t -> Thread_.newThread(() -> sink.sink(t))).collect(Thread_::startJoin);
	}

	public static InputStream inputStream(Bytes bytes) {
		return new ByteArrayInputStream(bytes.bs, bytes.start, bytes.end - bytes.start);
	}

	public static <T> String joined(Outlet<T> outlet) {
		return As.<T> joinedBy("").apply(outlet);
	}

	public static <T> Fun<Outlet<T>, String> joinedBy(String delimiter) {
		return joinedBy("", delimiter, "");
	}

	public static <T> Fun<Outlet<T>, String> joinedBy(String before, String delimiter, String after) {
		return outlet -> {
			var sb = new StringBuilder();
			outlet.sink(s -> {
				if (0 < sb.length())
					sb.append(delimiter);
				sb.append(s);
			});
			return before + sb + after;
		};
	}

	public static Outlet<String> lines(Outlet<Bytes> outlet) {
		return lines_(outlet).map(t -> To.string(t).trim());
	}

	public static <K, V> Map<K, List<V>> listMap(Outlet<Pair<K, V>> outlet) {
		var map = new HashMap<K, List<V>>();
		outlet.sink(pair -> map.computeIfAbsent(pair.t0, k_ -> new ArrayList<>()).add(pair.t1));
		return map;
	}

	public static <K, V> Map<K, V> map(Outlet2<K, V> outlet) {
		var map = new HashMap<K, V>();
		return outlet.isAll((k, v) -> map.put(k, v) == null || Fail.b("duplicate key " + k)) ? map : null;
	}

	public static <T> Fun<Outlet<T>, Integer> min(Obj_Int<T> fun) {
		return outlet -> {
			var source = outlet.source();
			var t = source.source();
			int result1;
			if (t != null) {
				var result = fun.apply(t);
				while ((t = source.source()) != null)
					if ((result1 = fun.apply(t)) < result)
						result = result1;
				return result;
			} else
				return null;
		};
	}

	public static <K, V> ListMultimap<K, V> multimap(Outlet2<K, List<V>> outlet) {
		return new ListMultimap<>(map(outlet));
	}

	public static <K, V, T> Fun<Outlet2<K, V>, Streamlet<T>> pairMap(Fun2<K, V, T> fun) {
		return outlet -> new Streamlet<>(() -> outlet.map(fun::apply));
	}

	public static <I, O> Fun<Outlet<I>, Outlet<O>> sequenced(Seq<I, O> seq) {
		return outlet -> Outlet.of(new Source<>() {
			private int index;

			public O source() {
				var i = outlet.next();
				return i != null ? seq.apply(index++, i) : null;
			}
		});
	}

	public static <K, V> Map<K, Set<V>> setMap(Outlet<Pair<K, V>> outlet) {
		var map = new HashMap<K, Set<V>>();
		outlet.sink(pair -> map.computeIfAbsent(pair.t0, k_ -> new HashSet<>()).add(pair.t1));
		return map;
	}

	public static <T> Streamlet<T> streamlet(Outlet<T> outlet) {
		return Read.from(outlet.toList());
	}

	public static String string(Outlet<Bytes> outlet) {
		return To.string(Bytes.of(outlet));
	}

	public static Streamlet<String[]> table(Outlet<Bytes> outlet) {
		return Read.from(outlet.collect(As::lines_) //
				.map(bytes -> To.string(bytes).split("\t")) //
				.toList());
	}

	public static Outlet<Chars> utf8decode(Outlet<Bytes> bytesOutlet) {
		Source<Bytes> source = bytesOutlet.source();

		return Outlet.of(new Source<>() {
			private BytesBuilder bb = new BytesBuilder();

			public Chars source() {
				Chars chars;
				while ((chars = decode()).size() == 0) {
					var bytes = source.source();
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
				var cb = new CharsBuilder();
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

				return cb.toChars();
			}
		});
	}

	public static Outlet<Bytes> utf8encode(Outlet<Chars> charsOutlet) {
		Source<Chars> source = charsOutlet.source();

		return Outlet.of(new Source<>() {
			public Bytes source() {
				var chars = source.source();
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

	private static Outlet<Bytes> lines_(Outlet<Bytes> outlet) {
		return Bytes_.split(Bytes.of((byte) 10)).apply(outlet);
	}

}
