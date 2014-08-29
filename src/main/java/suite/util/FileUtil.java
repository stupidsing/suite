package suite.util;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import suite.util.FunUtil.Source;

public class FileUtil {

	public static String tmp = "/tmp";
	public static Charset charset = Charset.forName("UTF-8");

	public static void copyFile(File from, File to) throws IOException {
		try (OutputStream fos = new FileOutputStream(to)) {
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

	public static List<String> listZip(ZipFile zipFile) {
		List<String> names = new ArrayList<>();
		Enumeration<? extends ZipEntry> e = zipFile.entries();
		while (e.hasMoreElements())
			names.add(e.nextElement().getName());
		return names;
	}

	public static void moveFile(File from, File to) throws IOException {

		// Serious problem that renameTo do not work across partitions in Linux!
		// We fall back to copy the file if renameTo() failed.
		if (!from.renameTo(to)) {
			copyFile(from, to);
			from.delete();
		}
	}

	public static OutputStream out(String filename) throws IOException {
		Path parentFile = Paths.get(filename).getParent();
		if (parentFile != null)
			Files.createDirectories(parentFile);

		String filename1 = filename + ".new";

		return new FileOutputStream(filename1) {
			private boolean isClosed = false;

			public void close() throws IOException {
				if (!isClosed) {
					super.close();
					isClosed = true;
					Files.move(Paths.get(filename1), Paths.get(filename), ATOMIC_MOVE, REPLACE_EXISTING);
				}
			}
		};
	}

}
