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
import java.util.Map;

import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.os.FileUtil;
import suite.util.FunUtil;
import suite.util.FunUtil.Source;

public class Read {

	public static <T> Streamlet<T> empty() {
		return Streamlet.from(FunUtil.nullSource());
	}

	public static <K, V> Streamlet<Pair<K, V>> from(Map<K, V> map) {
		return from(map.entrySet()).map(e -> Pair.of(e.getKey(), e.getValue()));
	}

	public static <K, V> Streamlet<Pair<K, Collection<V>>> from(ListMultimap<K, V> multimap) {
		return from(multimap.listEntries()).map(p -> Pair.of(p.t0, p.t1));
	}

	@SafeVarargs
	public static <T> Streamlet<T> from(T... col) {
		return from(Arrays.asList(col));
	}

	public static <T> Streamlet<T> from(Iterable<T> col) {
		return new Streamlet<>(() -> Outlet.from(col));
	}

	public static <T> Streamlet<T> from(Source<T> source) {
		return Streamlet.from(source);
	}

	public static Streamlet<String> lines(Path path) throws IOException {
		return lines(path.toFile());
	}

	public static Streamlet<String> lines(File file) throws FileNotFoundException {
		return new Streamlet<>(() -> {
			InputStream is;
			try {
				is = new FileInputStream(file);
			} catch (FileNotFoundException ex) {
				throw new RuntimeException(ex);
			}
			return lines(is).closeAtEnd(is);
		});
	}

	public static Outlet<String> lines(InputStream is) {
		Reader reader = new InputStreamReader(is, FileUtil.charset);
		return lines(reader).closeAtEnd(reader);
	}

	public static Outlet<String> lines(Reader reader) {
		BufferedReader br = new BufferedReader(reader);
		return new Outlet<>(() -> {
			try {
				return br.readLine();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}).closeAtEnd(br);
	}

	public static Streamlet<Integer> range(int s, int e) {
		return new Streamlet<>(() -> {
			int i[] = new int[] { s };
			return Outlet.from(() -> i[0] < e ? i[0]++ : null);
		});
	}

	public static <K, V, C extends Collection<V>> Streamlet<Pair<K, V>> multimap(Map<K, C> map) {
		return Read.from(map).concatMap(p -> Read.from(p.t1).map(v -> Pair.of(p.t0, v)));
	}

}
