package suite.os;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import suite.Constants;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Rethrow;
import suite.util.To;

public class FileUtil {

	public static Path ext(Path path, String ext) {
		return path.resolveSibling(path.getFileName() + ext);
	}

	public static Streamlet<Path> findPaths(Path path) {
		return Read.from(() -> Rethrow.ex(() -> Files.walk(path).filter(p -> Files.isRegularFile(p)).iterator()));
	}

	public static String getFileExtension(Path path) {
		String filename = path.toString();
		return filename.substring(filename.lastIndexOf('.') + 1);
	}

	public static int getPid() {
		RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

		return Rethrow.ex(() -> {
			Field jvm = runtime.getClass().getDeclaredField("jvm");
			jvm.setAccessible(true);

			Object vmm = jvm.get(runtime);

			Method method = vmm.getClass().getDeclaredMethod("getProcessId");
			method.setAccessible(true);

			return (Integer) method.invoke(jvm.get(runtime));
		});
	}

	public static String jarFilename() {
		return Rethrow.ex(() -> FileUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI().getFragment());
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
			if (!Files.isDirectory(path))
				try {
					Files.createDirectories(path);
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
		}
	}

	public static OutputStream out(String filename) throws IOException {
		Path path = Paths.get(filename);
		return out(path);
	}

	public static OutputStream out(Path path) throws IOException {
		Path parent = path.getParent();
		Path path1 = parent.resolve(path.getFileName() + ".new");

		mkdir(parent);
		OutputStream os = Files.newOutputStream(path1);

		return new OutputStream() {
			private boolean isClosed = false;

			public void close() throws IOException {
				if (!isClosed) {
					os.close();
					isClosed = true;
					Files.move(path1, path, ATOMIC_MOVE, REPLACE_EXISTING);
				}
			}

			public void write(int b) throws IOException {
				os.write(b);
			}

			public void write(byte[] bs, int off, int len) throws IOException {
				os.write(bs, off, len);
			}
		};
	}

	public static String read(String filename) {
		return Rethrow.ex(() -> read_(Paths.get(filename)));
	}

	public static String read(Path path) {
		return Rethrow.ex(() -> read_(path));
	}

	private static String read_(Path path) throws IOException {
		byte[] bytes = Files.readAllBytes(path);
		boolean isBomExist = 3 <= bytes.length //
				&& bytes[0] == (byte) 0xEF //
				&& bytes[1] == (byte) 0xBB //
				&& bytes[2] == (byte) 0xBF;

		if (!isBomExist)
			return To.string(bytes);
		else
			return new String(bytes, 3, bytes.length - 3, Constants.charset);
	}

}
