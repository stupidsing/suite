package suite.util;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.net.URL;

import primal.Nouns.Buffer;
import primal.Nouns.Utf8;
import primal.Verbs.Format;
import primal.Verbs.New;
import primal.Verbs.WriteFile;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Sink;
import primal.io.ReadStream;
import primal.primitive.DblPrim.Obj_Dbl;
import primal.primitive.Flt_Dbl;
import primal.primitive.IntInt_Dbl;
import primal.primitive.IntPrim.Int_Obj;
import primal.primitive.Int_Dbl;
import primal.primitive.adt.Bytes;
import primal.primitive.adt.Chars;
import primal.puller.Puller;
import suite.primitive.IoSink;
import suite.serialize.SerOutput;
import suite.streamlet.As;

public class To {

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
		return Bytes.of(s.getBytes(Utf8.charset));
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

	public static Fun<Puller<Bytes>, Boolean> file(String filename) {
		return puller -> {
			WriteFile.to(filename).doWrite(os -> Copy.stream(inputStream(puller), os));
			return true;
		};
	}

	public static ReadStream inputStream(Puller<Bytes> puller) {
		return new ReadStream(new InputStream() {
			private InputStream is;
			private boolean isOpen = true;

			public void close() throws IOException {
				if (isOpen) {
					var bs = new byte[Buffer.size];
					while (0 <= read(bs, 0, bs.length))
						;
				}
			}

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
						is = As.inputStream(bytes);
					else
						break;
				}
				return nBytesRead;
			}
		});
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

	public static Sink<String> sink(StringBuilder sb) {
		return s -> sb.append("\n" + s);
	}

	public static String string(Bytes bytes) {
		return string(bytes.toArray());
	}

	public static String string(byte[] bs) {
		return new String(bs, Utf8.charset);
	}

	public static String string(double d) {
		return Format.value(d);
	}

	public static String string(Throwable th) {
		var sw = new StringWriter();
		try (var sw_ = sw; var pw = new PrintWriter(sw_)) {
			th.printStackTrace(pw);
		} catch (IOException ex) {
		}
		return sw.toString();
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

}
