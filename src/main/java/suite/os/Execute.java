package suite.os;

import static suite.util.Friends.fail;
import static suite.util.Friends.rethrow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import suite.cfg.Defaults;
import suite.streamlet.Read;
import suite.util.Copy;
import suite.util.Fail.InterruptedRuntimeException;
import suite.util.Th;
import suite.util.To;

public class Execute {

	public final int code;
	public final String out;
	public final String err;

	public static String shell(String sh) {
		String[] command = null;

		for (var s : List.of("/bin/sh", "C:\\cygwin\\bin\\sh.exe", "C:\\cygwin64\\bin\\sh.exe"))
			if (Files.exists(Paths.get(s)))
				command = new String[] { s, };

		if (command != null) {
			LogUtil.info("START " + sh);
			var execute = new Execute(command, sh);
			LogUtil.info("END__ " + sh);
			return execute.code == 0 ? execute.out : fail(execute.toString());
		} else
			return fail("cannot find shell executable");
	}

	public Execute(String[] command) {
		this(command, "");
	}

	public Execute(String[] command, String in) {
		this(command, in.getBytes(Defaults.charset));
	}

	public Execute(String[] command, byte[] bytes) {
		var bis = new ByteArrayInputStream(bytes);
		var bos0 = new ByteArrayOutputStream();
		var bos1 = new ByteArrayOutputStream();

		var process = rethrow(() -> Runtime.getRuntime().exec(command));

		try {
			var pis = process.getInputStream();
			var pes = process.getErrorStream();
			var pos = process.getOutputStream();

			var threads = Read.each( //
					Copy.streamByThread(pis, bos0), //
					Copy.streamByThread(pes, bos1), //
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
		return "code = " + code //
				+ "\nout = " + out //
				+ "\nerr = " + err;
	}

}
