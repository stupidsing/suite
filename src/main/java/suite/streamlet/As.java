package suite.streamlet;

import static primal.statics.Fail.failBool;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import primal.MoreVerbs.Read;
import primal.Verbs.Build;
import primal.Verbs.New;
import primal.Verbs.Start;
import primal.adt.Pair;
import primal.adt.map.ListMultimap;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Sink;
import primal.fp.Funs2.Fun2;
import primal.primitive.DblPrim.Obj_Dbl;
import primal.primitive.IntPrim;
import primal.primitive.IntPrim.IntSink;
import primal.primitive.IntPrim.Obj_Int;
import primal.primitive.Int_Dbl;
import primal.primitive.Int_Flt;
import primal.primitive.Int_Int;
import primal.primitive.adt.Bytes;
import primal.primitive.adt.Floats;
import primal.primitive.adt.Ints;
import primal.primitive.adt.map.ObjIntMap;
import primal.primitive.puller.IntPuller;
import primal.primitive.streamlet.FltStreamlet;
import primal.primitive.streamlet.IntStreamlet;
import primal.puller.Puller;
import primal.puller.Puller2;
import primal.streamlet.Streamlet;
import suite.primitive.Bytes_;
import suite.util.To;

public class As {

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
		return ts -> new FltStreamlet(Floats.build(b -> {
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
		return ts -> new IntStreamlet(Ints.build(b -> {
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
