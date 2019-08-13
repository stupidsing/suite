package suite.cfg;

import java.nio.file.Path;
import java.nio.file.Paths;

import primal.Nouns.Tmp;
import primal.Verbs.Mk;
import primal.adt.Opt;

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
		return Opt //
				.<String> of(null) //
				.or(() -> System.getenv("HOME")) //
				.or(() -> System.getenv("USERPROFILE")) //
				.map(Paths::get) //
				.get(() -> Tmp.root);
	}

}
