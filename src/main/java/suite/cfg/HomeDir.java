package suite.cfg;

import java.nio.file.Path;
import java.nio.file.Paths;

import primal.Nouns.Tmp;
import primal.Verbs.Mk;
import primal.adt.Opt;
import primal.os.Env;

public class HomeDir {

	public static Path dir(String path0) {
		var path1 = resolve(path0);
		Mk.dir(path1);
		return path1;
	}

	public static Path priv(String path) {
		return resolve("private").resolve(path);
	}

	public static Path resolve(String path0) {
		return getHomePath().resolve(path0);
	}

	private static Path getHomePath() {
		return Opt //
				.<String> none() //
				.ifNone(Opt.of(Env.HOME)) //
				.ifNone(Opt.of(System.getenv("USERPROFILE"))) //
				.map(Paths::get) //
				.or(Tmp.root);
	}

}
