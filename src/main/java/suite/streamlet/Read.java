package suite.streamlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;

import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.os.FileUtil;
import suite.util.FunUtil;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.Rethrow;

public class Read {

	public static <T> Streamlet<T> empty() {
		return Streamlet.from(FunUtil.nullSource());
	}

	@SafeVarargs
	public static <T> Streamlet<T> from(T... ts) {
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

	public static <K, V> Streamlet2<K, Collection<V>> from2(ListMultimap<K, V> multimap) {
		return new Streamlet2<>(() -> Outlet2.from(multimap));
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

	public static Streamlet<String> lines(Path path) throws IOException {
		return lines(path.toFile());
	}

	public static Streamlet<String> lines(File file) throws FileNotFoundException {
		return new Streamlet<>(() -> {
			InputStream is = Rethrow.ioException(() -> new FileInputStream(file));
			return lines(is).closeAtEnd(is);
		});
	}

	public static Outlet<String> lines(InputStream is) {
		Reader reader = new InputStreamReader(is, FileUtil.charset);
		return lines(reader).closeAtEnd(reader);
	}

	public static Outlet<String> lines(Reader reader) {
		BufferedReader br = new BufferedReader(reader);
		return new Outlet<>(() -> Rethrow.ioException(() -> br.readLine())).closeAtEnd(br);
	}

	public static Streamlet<Integer> range(int s, int e) {
		return new Streamlet<Integer>(() -> {
			int i[] = new int[] { s };
			return Outlet.from(() -> i[0] < e ? i[0]++ : null);
		});
	}

	public static <K, V, C extends Collection<V>> Streamlet2<K, V> multimap(Map<K, C> map) {
		return from2(map).concatMap2((k, l) -> from(l).map2(v -> k, v -> v));
	}

	public static <K, V> Streamlet2<K, V> from(ListMultimap<K, V> multimap) {
		return from2(multimap).concatMapValue(Read::from);
	}

}
