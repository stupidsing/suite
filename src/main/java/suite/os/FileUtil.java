package suite.os;

import static primal.statics.Rethrow.ex;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import primal.MoreVerbs.Read;
import primal.streamlet.Streamlet;

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

}
