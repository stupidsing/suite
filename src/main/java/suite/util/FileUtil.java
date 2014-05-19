package suite.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Deque;

import suite.util.FunUtil.Source;

public class FileUtil {

	public static String tmp = "/tmp";
	public static Charset charset = Charset.forName("UTF-8");

	public static void copyFile(File from, File to) throws IOException {
		try (OutputStream fos = out(to)) {
			// new FileOutputStream(f2, true); // Append
			Copy.stream(new FileInputStream(from), fos);
		}
	}

	public static Source<File> findFiles(File file) {
		Deque<File> stack = new ArrayDeque<>();
		stack.push(file);

		return () -> {
			while (!stack.isEmpty()) {
				File f = stack.pop();

				if (f.isDirectory())
					for (File child : f.listFiles())
						stack.push(child);
				else
					return f;
			}

			return null;
		};
	}

	public static int getPid() {
		try {
			RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

			Field jvm = runtime.getClass().getDeclaredField("jvm");
			jvm.setAccessible(true);

			Object vmm = jvm.get(runtime);

			Method method = vmm.getClass().getDeclaredMethod("getProcessId");
			method.setAccessible(true);

			return (Integer) method.invoke(jvm.get(runtime));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static void moveFile(File from, File to) throws IOException {

		// Serious problem that renameTo do not work across partitions in Linux!
		// We fall back to copy the file if renameTo() failed.
		if (!from.renameTo(to)) {
			copyFile(from, to);
			from.delete();
		}
	}

	public static OutputStream out(File file) throws FileNotFoundException {
		File parentFile = file.getParentFile();
		if (parentFile != null)
			parentFile.mkdirs();

		if (file.exists()) {
			File file0 = new File(file.getPath() + ".old");
			File file1 = new File(file.getPath() + ".new");

			return new FileOutputStream(file1) {
				private boolean isClosed = false;

				public void close() throws IOException {
					if (!isClosed) {
						super.close();
						isClosed = true;

						if (file0.exists() && !file0.delete())
							throw new IOException("Failed to delete old file");
						if (!file.renameTo(file0) || !file1.renameTo(file))
							throw new IOException("Failed to rename file");
					}
				}
			};
		} else
			return new FileOutputStream(file);
	}

}
