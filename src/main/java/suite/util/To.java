package suite.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import suite.Constants;
import suite.adt.Pair;
import suite.primitive.Bytes;
import suite.primitive.Chars;
import suite.primitive.PrimitiveFun.Float_Float;
import suite.primitive.PrimitiveFun.IntInt_Float;
import suite.primitive.PrimitiveFun.Int_Float;
import suite.primitive.PrimitiveFun.Int_Int;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;

public class To {

	private static String hexDigits = "0123456789ABCDEF";
	private static final Field field;

	static {
		field = Rethrow.ex(() -> {
			Field field_ = String.class.getDeclaredField("value");
			field_.setAccessible(true);
			return field_;
		});
	}

	public static <T> T[] array(Class<T> clazz, int length, IntFunction<T> f) {
		@SuppressWarnings("unchecked")
		T[] ts = (T[]) Array.newInstance(clazz, length);
		for (int i = 0; i < length; i++)
			ts[i] = f.apply(i);
		return ts;
	}

	public static Bytes bytes(String s) {
		return Bytes.of(s.getBytes(Constants.charset));
	}

	public static Bytes bytes(InputStream is) {
		return outlet(is).collect(As::bytes);
	}

	/**
	 * Get characters in a string without copying overhead. Do not modify the
	 * returned array!
	 */
	public static char[] charArray(String s) {
		return Rethrow.ex(() -> (char[]) field.get(s)); // s.toCharArray()
	}

	public static Chars chars(String s) {
		return Chars.of(charArray(s));
	}

	public static float[] floatArray(float[] fs, Float_Float fun) {
		return floatArray(fs.length, i -> fun.apply(fs[i]));
	}

	public static float[] floatArray(int length, Int_Float fun) {
		float[] floats = new float[length];
		for (int i = 0; i < length; i++)
			floats[i] = fun.apply(i);
		return floats;
	}

	public static float[][] floatArray(int height, int width, IntInt_Float fun) {
		float[][] m = new float[height][width];
		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++)
				m[i][j] = fun.apply(i, j);
		return m;
	}

	public static String hex(int i) {
		return "" + hexDigits.charAt(i & 0x0F);
	}

	public static String hex2(int i) {
		return "" //
				+ hexDigits.charAt(i >>> 4 & 0x0F) //
				+ hexDigits.charAt(i & 0x0F);
	}

	public static String hex4(int i) {
		return hex2(i >>> 8 & 0xFF) + hex2(i & 0xFF);
	}

	public static String hex8(int i) {
		return hex4(i >>> 16 & 0xFFFF) + hex4(i & 0xFFFF);
	}

	public static InputStream inputStream(Outlet<Bytes> outlet) {
		return new InputStream() {
			private InputStream is;

			public int read() throws IOException {
				byte[] b = new byte[1];
				int nBytesRead = read(b, 0, 1);
				return 0 < nBytesRead ? b[0] : nBytesRead;
			}

			public int read(byte[] bs, int offset, int length) throws IOException {
				int nBytesRead = -1;
				while (is == null || (nBytesRead = is.read(bs, offset, length)) < 0) {
					Bytes bytes = outlet.next();
					if (bytes != null)
						is = bytes.asInputStream();
					else
						break;
				}
				return nBytesRead;
			}
		};
	}

	public static int[] intArray(int length, Int_Int f) {
		int[] ints = new int[length];
		for (int i = 0; i < length; i++)
			ints[i] = f.apply(i);
		return ints;
	}

	public static <T> List<T> list(Iterable<T> iter) {
		return list(iter.iterator());
	}

	public static <T> List<T> list(Iterator<T> iter) {
		List<T> list = new ArrayList<>();
		while (iter.hasNext())
			list.add(iter.next());
		return list;
	}

	public static <T> List<T> list(Source<T> source) {
		List<T> list = new ArrayList<>();
		T t;
		while ((t = source.source()) != null)
			list.add(t);
		return list;
	}

	public static <K, V> Map<K, V> map(K k, V v) {
		return Collections.singletonMap(k, v);
	}

	public static <K, V> Map<K, V> map(K k0, V v0, K k1, V v1) {
		HashMap<K, V> map = new HashMap<>();
		map.put(k0, v0);
		map.put(k1, v1);
		return map;
	}

	@SafeVarargs
	public static <K, V> Map<K, V> map(Pair<K, V>... pairs) {
		Map<K, V> map = new HashMap<>();
		for (Pair<K, V> pair : pairs)
			if (pair != null)
				map.put(pair.t0, pair.t1);
		return map;
	}

	public static Outlet<Bytes> outlet(String data) {
		return outlet(new ByteArrayInputStream(data.getBytes(Constants.charset)));
	}

	public static Outlet<Bytes> outlet(InputStream is) {
		InputStream bis = new BufferedInputStream(is);
		return Outlet.of(() -> {
			byte[] bs = new byte[Constants.bufferSize];
			int nBytesRead = Rethrow.ex(() -> bis.read(bs));
			return 0 <= nBytesRead ? Bytes.of(bs, 0, nBytesRead) : null;
		}).closeAtEnd(bis).closeAtEnd(is);
	}

	public static Sink<String> sink(StringBuilder sb) {
		return s -> {
			sb.append(s);
			sb.append("\n");
		};
	}

	@SafeVarargs
	public static <T> Source<T> source(T... array) {
		return new Source<T>() {
			private int i;

			public T source() {
				return i < array.length ? array[i++] : null;
			}
		};
	}

	public static <T> Source<T> source(Enumeration<T> en) {
		return () -> en.hasMoreElements() ? en.nextElement() : null;
	}

	public static <T> Source<T> source(Iterable<T> iterable) {
		Iterator<T> iterator = iterable.iterator();
		return () -> iterator.hasNext() ? iterator.next() : null;
	}

	public static Source<Bytes> source(InputStream is) {
		return () -> {
			byte[] bs = new byte[Constants.bufferSize];
			int nBytesRead = Rethrow.ex(() -> is.read(bs));

			if (0 <= nBytesRead)
				return Bytes.of(bs, 0, nBytesRead);
			else {
				Util.closeQuietly(is);
				return null;
			}
		};
	}

	public static String string(Bytes bytes) {
		return string(bytes.toByteArray());
	}

	public static String string(byte[] bs) {
		return new String(bs, Constants.charset);
	}

	public static String string(long time) {
		return FormatUtil.formatDateTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()));
	}

	public static String string(InputStream in) {
		try (InputStream is = in;
				InputStreamReader isr = new InputStreamReader(is, Constants.charset);
				BufferedReader br = new BufferedReader(isr)) {
			return string(br);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static String string(Reader reader) {
		try (Reader reader_ = reader) {
			char[] buffer = new char[Constants.bufferSize];
			StringBuilder sb = new StringBuilder();

			while (reader_.ready()) {
				int n = reader_.read(buffer);
				sb.append(new String(buffer, 0, n));
			}

			return sb.toString();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static String string(Throwable th) {
		StringWriter sw = new StringWriter();

		try (Writer sw_ = sw; PrintWriter pw = new PrintWriter(sw_)) {
			th.printStackTrace(pw);
		} catch (IOException ex) {
		}

		return sw.toString();
	}

}
