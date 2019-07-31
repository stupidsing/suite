package suite.streamlet;

import static primal.statics.Rethrow.ex;

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

import suite.adt.pair.Pair;
import suite.cfg.Defaults;
import suite.http.HttpUtil;
import suite.primitive.Bytes;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.FunUtil2.Source2;
import suite.util.To;
import suite.util.Util;

public class Read {

	private static Streamlet<?> empty = from(() -> FunUtil.nullSource());
	private static Streamlet2<?, ?> empty2 = from2(() -> FunUtil2.nullSource());

	public static Streamlet<Bytes> bytes(Path path) {
		var file = path.toFile();

		return new Streamlet<>(() -> {
			InputStream is = ex(() -> new FileInputStream(file));
			return To.puller(is).closeAtEnd(is);
		});
	}

	public static Streamlet<Bytes> bytes(String data) {
		return new Streamlet<>(() -> To.puller(data));
	}

	public static Streamlet<Bytes> bytes(InputStream is) {
		return new Streamlet<>(() -> To.puller(is));
	}

	public static <T> Streamlet<T> empty() {
		@SuppressWarnings("unchecked")
		var st = (Streamlet<T>) empty;
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

	@SafeVarargs
	public static <K, V> Streamlet2<K, V> each2(Pair<K, V>... pairs) {
		return from2(Arrays.asList(pairs));
	}

	public static <T> Streamlet<T> from(T[] ts) {
		return new Streamlet<>(() -> Puller.of(ts));
	}

	public static <T> Streamlet<T> from(Enumeration<T> en) {
		return new Streamlet<>(() -> Puller.of(en));
	}

	public static <T> Streamlet<T> from(Iterable<T> col) {
		return new Streamlet<>(() -> Puller.of(col));
	}

	public static <T> Streamlet<T> from(Source<Source<T>> source) {
		return new Streamlet<>(() -> Puller.of(source.g()));
	}

	public static <K, V> Streamlet2<K, V> from2(Map<K, V> map) {
		return new Streamlet2<>(() -> Puller2.of(map));
	}

	public static <K, V> Streamlet2<K, V> from2(Iterable<Pair<K, V>> col) {
		return new Streamlet2<>(() -> Puller2.of(col));
	}

	public static <K, V> Streamlet2<K, V> from2(Source<Source2<K, V>> source) {
		return new Streamlet2<>(() -> Puller2.of(source.g()));
	}

	public static Streamlet<String> lines(Path path) {
		return lines(path.toFile());
	}

	public static Streamlet<String> lines(File file) {
		return new Streamlet<>(() -> lines(ex(() -> new FileInputStream(file))));
	}

	public static Puller<String> lines(InputStream is) {
		return lines(new InputStreamReader(is, Defaults.charset)).closeAtEnd(is);
	}

	public static Puller<String> lines(Reader reader) {
		var br = new BufferedReader(reader);
		return Puller.of(() -> ex(() -> Util.readLine(br))).closeAtEnd(br).closeAtEnd(reader);
	}

	public static Streamlet<Bytes> url(String url) {
		return new Streamlet<>(HttpUtil.get(url)::out);
	}

	public static <K, V, C extends Collection<V>> Streamlet2<K, V> multimap(Map<K, C> map) {
		return from2(map).concatMap2((k, l) -> from(l).map2(v -> k, v -> v));
	}

}
