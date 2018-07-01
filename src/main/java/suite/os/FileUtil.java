package suite.os;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import suite.Defaults;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Fail;
import suite.util.Rethrow;
import suite.util.Rethrow.SinkEx;
import suite.util.To;

public class FileUtil {

	public static Path ext(Path path, String ext) {
		return path.resolveSibling(path.getFileName() + ext);
	}

	public static Streamlet<Path> findPaths(Path path) {
		return Read.from(() -> Rethrow.ex(() -> Files.walk(path).filter(Files::isRegularFile).iterator()));
	}

	public static String getFileExtension(Path path) {
		var filename = path.toString();
		return filename.substring(filename.lastIndexOf('.') + 1);
	}

	public static long getPid() {
		return ManagementFactory.getRuntimeMXBean().getPid();
	}

	public static String jarFilename() {
		return Rethrow.ex(() -> FileUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI().getFragment());
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
				try {
					Files.createDirectories(path);
				} catch (IOException ex) {
					Fail.t(ex);
				}
		}
	}

	public static OutputStreamW out(String filename) {
		return out_(Paths.get(filename));
	}

	public static OutputStreamW out(Path path) {
		return out_(path);
	}

	public static String read(String filename) {
		return To.string(Paths.get(filename));
	}

	private static OutputStreamW out_(Path path) {
		var parent = path.getParent();
		var path1 = parent.resolve(path.getFileName() + ".new");

		mkdir(parent);
		var os = Rethrow.ex(() -> Files.newOutputStream(path1));

		return new OutputStreamW() {
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

	public static abstract class OutputStreamW extends OutputStream {
		public void writeData(byte[] content) {
			write(os -> os.write(content));
		}

		public void writeData(String content) {
			writeContents(w -> w.write(content));
		}

		public void writeContents(SinkEx<OutputStreamWriter, IOException> sink) {
			write(os -> {
				try (var w = new OutputStreamWriter(os, Defaults.charset)) {
					sink.sink(w);
				}
			});
		}

		public void write(SinkEx<OutputStream, IOException> sink) {
			try {
				sink.sink(this);
			} catch (IOException ex) {
				Fail.t(ex);
			}
		}
	}
}
