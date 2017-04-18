package suite.util;

import java.nio.file.Path;
import java.nio.file.Paths;

import suite.Constants;

public class HomeDir {

	public static Path resolve(String path_) {
		return getHomePath().resolve(path_);
	}

	private static Path getHomePath() {
		String s = null;
		s = s != null ? s : System.getenv("HOME");
		s = s != null ? s : System.getenv("USERPROFILE");
		Path path = s != null ? Paths.get(s) : null;
		path = path != null ? path : Constants.tmp;
		return path;
	}

}
