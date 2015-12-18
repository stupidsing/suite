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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.os.FileUtil;
import suite.util.FunUtil;
import suite.util.FunUtil.Source;

public class Read {

	public static <T> Streamlet<T> empty() {
		return Streamlet.from(FunUtil.nullSource());
	}

	public static <K, V> Streamlet2<K, V> from(Map<K, V> map) {
		Iterator<Entry<K, V>> iter = map.entrySet().iterator();
		return Streamlet2.from(pair -> {
			if (iter.hasNext()) {
				Entry<K, V> pair1 = iter.next();
				pair.t0 = pair1.getKey();
				pair.t1 = pair1.getValue();
				return true;
			} else
				return false;
		});
	}

	public static <K, V> Streamlet2<K, Collection<V>> from(ListMultimap<K, V> multimap) {
		Iterator<Pair<K, Collection<V>>> iter = multimap.listEntries().iterator();
		return Streamlet2.from(pair -> {
			if (iter.hasNext()) {
				Pair<K, Collection<V>> pair1 = iter.next();
				pair.t0 = pair1.t0;
				pair.t1 = pair1.t1;
				return true;
			} else
				return false;
		});
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
		return new Streamlet<Integer>(() -> {
			int i[] = new int[] { s };
			return Outlet.from(() -> i[0] < e ? i[0]++ : null);
		});
	}

	public static <K, V, C extends Collection<V>> Streamlet2<K, V> multimap(Map<K, C> map) {
		return from(map).concatMap2((k, l) -> from(l).map2(v -> k, v -> v));
	}

	public static <K, V> Streamlet2<K, V> multimap(ListMultimap<K, V> multimap) {
		return from(multimap).concatMapValue(Read::from);
	}

}
