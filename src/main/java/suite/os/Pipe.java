package suite.os;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import primal.MoreVerbs.Read;
import primal.MoreVerbs.ReadLines;
import primal.Nouns.Utf8;
import primal.os.Log_;
import primal.streamlet.Streamlet;
import suite.util.Copy;

public class Pipe {

	public static Streamlet<String> shell(String sh) {
		var command = Read //
				.each("/bin/sh", "C:\\cygwin\\bin\\sh.exe", "C:\\cygwin64\\bin\\sh.exe") //
				.filter(s -> Files.exists(Paths.get(s))) //
				.uniqueResult();

		Log_.info("START " + sh);

		return ex(() -> {
			var bis = new ByteArrayInputStream(sh.getBytes(Utf8.charset));

			var process = ex(() -> Runtime.getRuntime().exec(command));

			var pis = process.getInputStream();
			var pes = process.getErrorStream();
			var pos = process.getOutputStream();

			var threads = new Thread[] { //
					Copy.streamByThread(pes, System.err, false), //
					Copy.streamByThread(bis, pos), };

			for (var thread : threads)
				thread.start();

			return ReadLines.from(pis).closeAtEnd(() -> ex(() -> {
				var code = process.waitFor();
				if (code == 0)
					for (var thread : threads)
						thread.join();
				else
					fail("code = " + code);
				process.destroy();
				Log_.info("END__ " + sh);
				return process;
			}));
		});
	}

}
