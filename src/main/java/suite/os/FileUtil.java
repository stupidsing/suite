package suite.os;

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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Copy;
import suite.util.Rethrow;

public class FileUtil {

	public static String tmp = "/tmp";
	public static Charset charset = StandardCharsets.UTF_8;

	public static void copyFile(File from, File to) throws IOException {
		try (OutputStream fos = new FileOutputStream(to)) {
			// new FileOutputStream(f2, true); // Append
			Copy.stream(new FileInputStream(from), fos);
		}
	}

	public static Streamlet<Path> findPaths(Path path) {
		return Read.from(() -> Rethrow.ioException(() -> Files.walk(path).filter(p -> Files.isRegularFile(p)).iterator()));
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

	public static String homeDir() {
		String homeDir = System.getProperty("home.dir");
		return homeDir != null ? homeDir : ".";
	}

	public static List<String> listZip(ZipFile zipFile) {
		return Read.from(zipFile.entries()).map(ZipEntry::getName).toList();
	}

	/**
	 * Files.createDirectory() might fail with FileAlreadyExistsException in
	 * MacOSX, contrary to its documentation. This re-implementation would not.
	 */
	public static void mkdir(Path path) {
		if (path != null) {
			mkdir(path.getParent());
			if (!Files.exists(path))
				mkdir(path);
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

	public static OutputStream out(String filename) throws IOException {
		Path parentFile = Paths.get(filename).getParent();
		if (parentFile != null && !Files.isDirectory(parentFile))
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

	public static String read(String filename) throws IOException {
		return read(Paths.get(filename));
	}

	public static String read(Path path) throws IOException {
		byte bytes[] = Files.readAllBytes(path);
		boolean isBomExist = 3 <= bytes.length //
				&& bytes[0] == (byte) 0xEF //
				&& bytes[1] == (byte) 0xBB //
				&& bytes[2] == (byte) 0xBF;

		if (!isBomExist)
			return new String(bytes, FileUtil.charset);
		else
			return new String(bytes, 3, bytes.length - 3, FileUtil.charset);
	}

}
