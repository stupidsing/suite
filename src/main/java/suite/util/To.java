package suite.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.util.FunUtil.Source;

public class To {

	private static final int bufferSize = 4096;
	private static final String hexDigits = "0123456789ABCDEF";

	public static Bytes bytes(String s) {
		return new Bytes(s.getBytes(FileUtil.charset));
	}

	public static Bytes bytes(InputStream is) throws IOException {
		BytesBuilder bb = new BytesBuilder();
		byte buffer[] = new byte[4096];
		int nBytesRead;

		while ((nBytesRead = is.read(buffer)) != -1)
			bb.append(buffer, 0, nBytesRead);

		return bb.toBytes();
	}

	public static Date date(String s) throws ParseException {
		return FormatUtil.dateTimeFormat.parse(s);
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

	@SafeVarargs
	public static <O> Source<O> source(final O... array) {
		return source(Arrays.asList(array));
	}

	public static <O> Source<O> source(Iterable<O> iterable) {
		final Iterator<O> iterator = iterable.iterator();

		return new Source<O>() {
			public O source() {
				return iterator.hasNext() ? iterator.next() : null;
			}
		};
	}

	public static String string(byte bs[]) {
		return new String(bs, FileUtil.charset);
	}

	public static String string(Bytes bytes) {
		return string(bytes.getBytes());
	}

	public static String string(Date date) {
		return FormatUtil.dateTimeFormat.format(date);
	}

	public static String string(File file) throws IOException {
		try (InputStream fis = new FileInputStream(file)) {
			return string(fis);
		}
	}

	public static String string(InputStream in) throws IOException {
		try (InputStream is = in;
				InputStreamReader isr = new InputStreamReader(is, FileUtil.charset);
				BufferedReader br = new BufferedReader(isr)) {
			return string(br);
		}
	}

	public static String string(Reader reader) throws IOException {
		try (Reader reader_ = reader) {
			char buffer[] = new char[bufferSize];
			StringBuilder sb = new StringBuilder();

			while (reader_.ready()) {
				int n = reader_.read(buffer);
				sb.append(new String(buffer, 0, n));
			}

			return sb.toString();
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
