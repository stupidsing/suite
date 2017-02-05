package suite.util;

import java.nio.file.Path;

import suite.Constants;

public class TempDir {

	public static Path resolve(String path) {
		return Constants.tmp.resolve(path);
	}

}
