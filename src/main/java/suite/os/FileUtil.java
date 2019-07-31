package suite.os;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import suite.cfg.Defaults;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.ReadStream;
import suite.util.String_;
import suite.util.To;
import suite.util.WriteStream;

public class FileUtil {

	public static void delete(Path path) {
		ex(() -> {
			Files.delete(path);
			return path;
		});
	}

	public static void deleteIfExists(Path path) {
		ex(() -> {
			Files.deleteIfExists(path);
			return path;
		});
	}

	public static Path ext(Path path, String ext) {
		return path.resolveSibling(path.getFileName() + ext);
	}

	public static Streamlet<Path> findPaths(Path path) {
		return Read.from(() -> ex(() -> Files.walk(path).filter(Files::isRegularFile).iterator()));
	}

	public static String getFileExtension(Path path) {
		var filename = path.toString();
		return filename.substring(filename.lastIndexOf('.') + 1);
	}

	public static long getPid() {
		return ManagementFactory.getRuntimeMXBean().getPid();
	}

	public static ReadStream in(String filename) {
		return in_(Paths.get(filename));
	}

	public static ReadStream in(Path path) {
		return in_(path);
	}

	public static String jarFilename() {
		return ex(() -> FileUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI().getFragment());
	}

	public static String homeDir() {
		return System.getProperty("home.dir", ".");
	}

	public static List<String> listZip(ZipFile zipFile) {
		return Read.from(zipFile.entries()).map(ZipEntry::getName).toList();
	}

	/**
	 * Files.createDirectory() might fail with FileAlreadyExistsException in MacOSX,
	 * contrary to its documentation. This re-implementation would not.
	 */
	public static void mkdir(Path path) {
		if (path != null) {
			mkdir(path.getParent());
			if (!Files.isDirectory(path))
				ex(() -> Files.createDirectories(path));
		}
	}

	public static WriteStream out(String filename) {
		return out_(Paths.get(filename));
	}

	public static WriteStream out(Path path) {
		return out_(path);
	}

	public static String read(String filename) {
		return read(Paths.get(filename));
	}

	public static String read(InputStream in) {
		return String_.build(sb -> {
			try (var is = in; var isr = new InputStreamReader(is, Defaults.charset); var br = new BufferedReader(isr)) {
				var buffer = new char[Defaults.bufferSize];

				while (br.ready()) {
					var n = br.read(buffer);
					sb.append(new String(buffer, 0, n));
				}
			} catch (IOException ex) {
				fail(ex);
			}
		});
	}

	public static String read(Path path) {
		var bytes = FileUtil.in(path).readBytes();

		var isBomExist = 3 <= bytes.length //
				&& bytes[0] == (byte) 0xEF //
				&& bytes[1] == (byte) 0xBB //
				&& bytes[2] == (byte) 0xBF;

		if (!isBomExist)
			return To.string(bytes);
		else
			return new String(bytes, 3, bytes.length - 3, Defaults.charset);
	}

	private static ReadStream in_(Path path) {
		var is = ex(() -> Files.newInputStream(path));

		return ReadStream.of(is);
	}

	private static WriteStream out_(Path path) {
		var parent = path.getParent();
		var path1 = parent.resolve(path.getFileName() + ".new");

		mkdir(parent);
		var os = ex(() -> Files.newOutputStream(path1));

		return new WriteStream(os) {
			private boolean isClosed = false;

			public void close() throws IOException {
				if (!isClosed) {
					os.close();
					isClosed = true;
					Files.move(path1, path, ATOMIC_MOVE, REPLACE_EXISTING);
				}
			}
		};
	}

}
