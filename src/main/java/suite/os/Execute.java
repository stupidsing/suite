package suite.os;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import primal.MoreVerbs.Read;
import primal.Nouns.Utf8;
import primal.Verbs.Th;
import primal.os.Log_;
import primal.statics.Fail.InterruptedRuntimeException;
import suite.util.Copy;
import suite.util.To;

public class Execute {

	public final int code;
	public final String out;
	public final String err;

	public static String shell(String sh) {
		var command = Read
				.each("/bin/sh", "C:\\cygwin\\bin\\sh.exe", "C:\\cygwin64\\bin\\sh.exe")
				.filter(s -> Files.exists(Paths.get(s)))
				.uniqueResult();

		Log_.info("START " + sh);
		var execute = new Execute(new String[] { command, }, sh);
		Log_.info("END__ " + sh);
		return execute.code == 0 ? execute.out : fail(execute.toString());
	}

	public Execute(String[] command) {
		this(command, "");
	}

	public Execute(String[] command, String in) {
		this(command, in.getBytes(Utf8.charset));
	}

	public Execute(String[] command, byte[] bytes) {
		var bis = new ByteArrayInputStream(bytes);
		var bos0 = new ByteArrayOutputStream();
		var bos1 = new ByteArrayOutputStream();

		var process = ex(() -> Runtime.getRuntime().exec(command));

		try {
			var pis = process.getInputStream();
			var pes = process.getErrorStream();
			var pos = process.getOutputStream();

			var threads = Read.each(
					Copy.streamByThread(pis, bos0),
					Copy.streamByThread(pes, bos1),
					Copy.streamByThread(bis, pos));

			threads.sink(Th::start);
			code = process.waitFor();
			threads.sink(Th::join_);
		} catch (InterruptedException ex) {
			throw new InterruptedRuntimeException(ex);
		} finally {
			process.destroy();
		}

		out = To.string(bos0.toByteArray());
		err = To.string(bos1.toByteArray());
	}

	@Override
	public String toString() {
		return "code = " + code
				+ "\nout = " + out
				+ "\nerr = " + err;
	}

}
