package suite.util;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import primal.String_;
import primal.Verbs.Close;
import primal.Verbs.New;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Sink;
import primal.fp.Funs.Source;
import primal.primitive.Flt_Dbl;
import primal.primitive.IntInt_Dbl;
import primal.primitive.Int_Dbl;
import suite.cfg.Defaults;
import suite.os.FileUtil;
import suite.primitive.Bytes;
import suite.primitive.Chars;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.primitive.IoSink;
import suite.serialize.SerOutput;
import suite.streamlet.As;
import suite.streamlet.Puller;

public class To {

	private static String hexDigits = "0123456789ABCDEF";

	public static <T> T[] array(int length, Class<T> clazz, Int_Obj<T> f) {
		var ts = New.array(clazz, length);
		for (var i = 0; i < length; i++)
			ts[i] = f.apply(i);
		return ts;
	}

	public static Object array_(int length, Class<?> clazz, Int_Obj<Object> f) {
		var ts = Array.newInstance(clazz, length);
		for (var i = 0; i < length; i++)
			Array.set(ts, i, f.apply(i));
		return ts;
	}

	public static Bytes bytes(String s) {
		return Bytes.of(s.getBytes(Defaults.charset));
	}

	public static Bytes bytes(IoSink<SerOutput> ioSink) {
		var baos = new ByteArrayOutputStream();
		try (var baos_ = baos) {
			ioSink.f(SerOutput.of(baos_));
		} catch (IOException ex) {
			fail(ex);
		}
		return Bytes.of(baos.toByteArray());
	}

	public static Chars chars(String s) {
		return Chars.of(s.toCharArray());
	}

	public static LocalDate date(String s) {
		return LocalDate.parse(s, Defaults.dateFormat);
	}

	public static Fun<Puller<Bytes>, Boolean> file(String filename) {
		return puller -> {
			FileUtil.out(filename).doWrite(os -> Copy.stream(inputStream(puller), os));
			return true;
		};
	}

	public static String hex(long i) {
		return Character.toString(hexDigits.charAt((int) (i & 0x0F)));
	}

	public static String hex2(long i) {
		return hex(i >>> 4) + hex(i);
	}

	public static String hex4(long i) {
		return hex2(i >>> 8 & 0xFF) + hex2(i & 0xFF);
	}

	public static String hex8(long i) {
		return hex4(i >>> 16 & 0xFFFF) + hex4(i & 0xFFFF);
	}

	public static ReadStream inputStream(Puller<Bytes> puller) {
		return new ReadStream(new InputStream() {
			private InputStream is;
			private boolean isOpen = true;

			public int read() throws IOException {
				var b = new byte[1];
				var nBytesRead = read(b, 0, 1);
				return 0 < nBytesRead ? b[0] : nBytesRead;
			}

			public int read(byte[] bs, int offset, int length) throws IOException {
				var nBytesRead = -1;
				while (is == null || (nBytesRead = is.read(bs, offset, length)) < 0) {
					var bytes = puller.pull();
					if (isOpen = (bytes != null))
						is = bytes.collect(As::inputStream);
					else
						break;
				}
				return nBytesRead;
			}

			public void close() throws IOException {
				if (isOpen) {
					var bs = new byte[Defaults.bufferSize];
					while (0 <= read(bs, 0, bs.length))
						;
				}
			}
		});
	}

	public static <T> List<T> list(Source<T> source) {
		return list(Puller.of(source));
	}

	public static <T> List<T> list(Iterable<T> i) {
		var list = new ArrayList<T>();
		var iter = i.iterator();
		while (iter.hasNext())
			list.add(iter.next());
		return list;
	}

	public static float[][] matrix(int height, int width_, IntInt_Dbl fun) {
		var matrix = new float[height][width_];
		for (var i = 0; i < height; i++)
			for (var j = 0; j < width_; j++)
				matrix[i][j] = (float) fun.apply(i, j);
		return matrix;
	}

	public static String percent(double d) {
		return String.format("%.1f", d * 100d) + "%";
	}

	public static Puller<Bytes> puller(String data) {
		return puller(new ByteArrayInputStream(data.getBytes(Defaults.charset)));
	}

	public static Puller<Bytes> puller(InputStream is) {
		var bis = new BufferedInputStream(is);
		return Puller.of(() -> {
			var bs = new byte[Defaults.bufferSize];
			var nBytesRead = ex(() -> bis.read(bs));
			return 0 <= nBytesRead ? Bytes.of(bs, 0, nBytesRead) : null;
		}).closeAtEnd(bis).closeAtEnd(is);
	}

	public static Sink<String> sink(StringBuilder sb) {
		return s -> sb.append("\n" + s);
	}

	@SafeVarargs
	public static <T> Source<T> source(T... array) {
		return new Source<>() {
			private int i;

			public T g() {
				return i < array.length ? array[i++] : null;
			}
		};
	}

	public static <T> Source<T> source(Enumeration<T> en) {
		return () -> en.hasMoreElements() ? en.nextElement() : null;
	}

	public static <T> Source<T> source(Iterable<T> iterable) {
		var iterator = iterable.iterator();
		return () -> iterator.hasNext() ? iterator.next() : null;
	}

	public static Source<Bytes> source(InputStream is) {
		return () -> {
			var bs = new byte[Defaults.bufferSize];
			var nBytesRead = ex(() -> is.read(bs));

			if (0 <= nBytesRead)
				return Bytes.of(bs, 0, nBytesRead);
			else {
				Close.quietly(is);
				return null;
			}
		};
	}

	public static String string(Bytes bytes) {
		return string(bytes.toArray());
	}

	public static String string(byte[] bs) {
		return new String(bs, Defaults.charset);
	}

	public static String string(double d) {
		if (d < 1d)
			return String.format("%.4f", d);
		else if (d < 10d)
			return String.format("%.3f", d);
		else if (d < 100d)
			return String.format("%.2f", d);
		else
			return String.format("%.1f", d);
	}

	public static String string(Instant instant) {
		return ymdHms(instant);
	}

	public static String string(LocalDate date) {
		return Defaults.dateFormat.format(date);
	}

	public static String string(LocalDateTime time) {
		return ymdHms(time);
	}

	public static String string(Sink<StringBuilder> sink) {
		return String_.build(sink);
	}

	public static String string(Throwable th) {
		var sw = new StringWriter();

		try (var sw_ = sw; var pw = new PrintWriter(sw_)) {
			th.printStackTrace(pw);
		} catch (IOException ex) {
		}

		return sw.toString();
	}

	public static LocalDateTime time(String s) {
		return LocalDateTime.parse(s, Defaults.dateTimeFormat);
	}

	public static URL url(String s) {
		return ex(() -> new URL(s));
	}

	public static float[] vector(float[] fs, Flt_Dbl fun) {
		return vector(fs.length, i -> fun.apply(fs[i]));
	}

	public static <T> float[] vector(T[] fs, Obj_Dbl<T> fun) {
		return vector(fs.length, i -> fun.apply(fs[i]));
	}

	public static float[] vector(int length, Int_Dbl f) {
		var fs = new float[length];
		for (var i = 0; i < length; i++)
			fs[i] = (float) f.apply(i);
		return fs;
	}

	public static String ymdHms(long time) {
		return ymdHms(LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()));
	}

	private static String ymdHms(TemporalAccessor ta) {
		return Defaults.dateTimeFormat.format(ta);
	}

}
