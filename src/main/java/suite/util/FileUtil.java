package suite.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Deque;

import suite.util.FunUtil.Source;

public class FileUtil {

	public static final String tmp = "/tmp";
	public static final Charset charset = Charset.forName("UTF-8");

	public static void copyFile(File from, File to) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(to)) {
			// new FileOutputStream(f2, true); // Append
			Copy.stream(new FileInputStream(from), fos);
		}
	}

	public static Source<File> findFiles(final File file) {
		return new Source<File>() {
			private Deque<File> stack = new ArrayDeque<>();
			{
				stack.push(file);
			}

			public File source() {
				while (!stack.isEmpty()) {
					File f = stack.pop();

					if (f.isDirectory())
						for (File child : f.listFiles())
							stack.push(child);
					else
						return f;
				}

				return null;
			}
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

}
