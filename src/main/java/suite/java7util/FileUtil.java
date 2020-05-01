package suite.java7util;

import primal.fp.Funs.Source;
import suite.util.Copy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;

import static primal.statics.Fail.fail;

public class FileUtil {

	public static void copyFile(File from, File to) {
		try (var fos = new FileOutputStream(to)) {
			Copy.stream(new FileInputStream(from), fos);
		} catch (IOException ex) {
			fail(ex);
		}
	}

	public static Source<File> findFiles(File file) {
		var stack = new ArrayDeque<File>();
		stack.push(file);

		return new Source<File>() {
			public File g() {
				while (!stack.isEmpty()) {
					var f = stack.pop();

					if (f.isDirectory())
						for (var child : f.listFiles())
							stack.push(child);
					else
						return f;
				}

				return null;
			}
		};
	}

	public static void moveFile(File from, File to) {

		// serious problem that renameTo do not work across partitions in Linux!
		// we fall back to copy the file if renameTo() failed.
		if (!from.renameTo(to)) {
			copyFile(from, to);
			from.delete();
		}
	}

}
