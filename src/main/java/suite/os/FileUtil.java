package suite.os;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import primal.MoreVerbs.Read;
import primal.Nouns.Buffer;
import primal.Nouns.Utf8;
import primal.Verbs.Build;
import primal.Verbs.ReadFile;
import primal.streamlet.Streamlet;
import suite.util.To;

public class FileUtil {

	public static Path ext(Path path, String ext) {
		return path.resolveSibling(path.getFileName() + ext);
	}

	public static Streamlet<Path> findPaths(Path path) {
		return Read.from(() -> ex(() -> Files.walk(path).filter(Files::isRegularFile).iterator()));
	}

	public static String homeDir() {
		return System.getProperty("home.dir", ".");
	}

	public static String jarFilename() {
		return ex(() -> FileUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI().getFragment());
	}

	public static List<String> listZip(ZipFile zipFile) {
		return Read.from(zipFile.entries()).map(ZipEntry::getName).toList();
	}

	public static String read(String filename) {
		return read(Paths.get(filename));
	}

	public static String read(InputStream in) {
		return Build.string(sb -> {
			try (var is = in; var isr = new InputStreamReader(is, Utf8.charset); var br = new BufferedReader(isr)) {
				var buffer = new char[Buffer.size];

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
		var bytes = ReadFile.from(path).readBytes();

		var isBomExist = 3 <= bytes.length //
				&& bytes[0] == (byte) 0xEF //
				&& bytes[1] == (byte) 0xBB //
				&& bytes[2] == (byte) 0xBF;

		if (!isBomExist)
			return To.string(bytes);
		else
			return new String(bytes, 3, bytes.length - 3, Utf8.charset);
	}

}
