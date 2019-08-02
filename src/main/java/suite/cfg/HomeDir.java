package suite.cfg;

import java.nio.file.Path;
import java.nio.file.Paths;

import primal.Verbs.Mk;

public class HomeDir {

	public static Path dir(String path_) {
		var path = resolve(path_);
		Mk.dir(path);
		return path;
	}

	public static Path resolve(String path_) {
		return getHomePath().resolve(path_);
	}

	private static Path getHomePath() {
		String s = null;
		s = s != null ? s : System.getenv("HOME");
		s = s != null ? s : System.getenv("USERPROFILE");
		var path = s != null ? Paths.get(s) : null;
		return path != null ? path : Defaults.tmp;
	}

}
