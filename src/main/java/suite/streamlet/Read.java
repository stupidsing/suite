package suite.streamlet;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;

import suite.Constants;
import suite.adt.Pair;
import suite.primitive.Bytes;
import suite.util.FunUtil;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2;
import suite.util.FunUtil2.Source2;
import suite.util.Rethrow;
import suite.util.Util;

public class Read {

	private static Streamlet<?> empty = Streamlet.from(FunUtil.nullSource());
	private static Streamlet2<?, ?> empty2 = Streamlet2.from(FunUtil2.nullSource());

	public static Streamlet<Bytes> bytes(Path path) {
		return bytes(path.toFile());
	}

	public static Streamlet<Bytes> bytes(File file) {
		return new Streamlet<>(() -> {
			InputStream is = Rethrow.ioException(() -> new FileInputStream(file));
			return bytes(is).closeAtEnd(is);
		});
	}

	public static Outlet<Bytes> bytes(InputStream is) {
		InputStream bis = new BufferedInputStream(is);
		return new Outlet<>(() -> {
			byte bs[] = new byte[Constants.bufferSize];
			int nBytesRead = Rethrow.ioException(() -> bis.read(bs));
			return 0 <= nBytesRead ? Bytes.of(bs, 0, nBytesRead) : null;
		}).closeAtEnd(bis).closeAtEnd(is);
	}

	public static <T> Streamlet<T> empty() {
		@SuppressWarnings("unchecked")
		Streamlet<T> st = (Streamlet<T>) empty;
		return st;
	}

	public static <K, V> Streamlet2<K, V> empty2() {
		@SuppressWarnings("unchecked")
		Streamlet2<K, V> st = (Streamlet2<K, V>) empty2;
		return st;
	}

	@SafeVarargs
	public static <T> Streamlet<T> each(T... ts) {
		return from(ts);
	}

	public static <T> Streamlet<T> from(T ts[]) {
		return new Streamlet<>(() -> Outlet.from(ts));
	}

	public static <T> Streamlet<T> from(Enumeration<T> en) {
		return new Streamlet<>(() -> Outlet.from(en));
	}

	public static <T> Streamlet<T> from(Iterable<T> col) {
		return new Streamlet<>(() -> Outlet.from(col));
	}

	public static <T> Streamlet<T> from(Source<T> source) {
		return Streamlet.from(source);
	}

	public static <K, V> Streamlet2<K, V> from2(Map<K, V> map) {
		return new Streamlet2<>(() -> Outlet2.from(map));
	}

	public static <K, V> Streamlet2<K, V> from2(K k, V v) {
		return from2(Arrays.asList(Pair.of(k, v)));
	}

	@SafeVarargs
	public static <K, V> Streamlet2<K, V> from2(Pair<K, V>... kvs) {
		return new Streamlet2<>(() -> Outlet2.from(kvs));
	}

	public static <K, V> Streamlet2<K, V> from2(Iterable<Pair<K, V>> col) {
		return new Streamlet2<>(() -> Outlet2.from(col));
	}

	public static <K, V> Streamlet2<K, V> from2(Source2<K, V> source) {
		return Streamlet2.from(source);
	}

	public static Streamlet<String> lines(Path path) {
		return lines(path.toFile());
	}

	public static Streamlet<String> lines(File file) {
		return new Streamlet<>(() -> {
			InputStream is = Rethrow.ioException(() -> new FileInputStream(file));
			return lines(is).closeAtEnd(is);
		});
	}

	public static Outlet<String> lines(InputStream is) {
		Reader reader = new InputStreamReader(is, Constants.charset);
		return lines(reader).closeAtEnd(reader);
	}

	public static Outlet<String> lines(Reader reader) {
		BufferedReader br = new BufferedReader(reader);
		return new Outlet<>(() -> Rethrow.ioException(() -> Util.readLine(br))).closeAtEnd(br);
	}

	public static Streamlet<Integer> range(int e) {
		return range(0, e);
	}

	public static Streamlet<Integer> range(int s, int e) {
		return new Streamlet<Integer>(() -> {
			int i[] = new int[] { s, };
			return Outlet.from(() -> i[0] < e ? i[0]++ : null);
		});
	}

	public static <K, V, C extends Collection<V>> Streamlet2<K, V> multimap(Map<K, C> map) {
		return from2(map).concatMap2((k, l) -> from(l).map2(v -> k, v -> v));
	}

}
