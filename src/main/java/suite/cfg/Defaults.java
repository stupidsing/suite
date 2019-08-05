package suite.cfg;

import static primal.statics.Fail.fail;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

import primal.adt.FixieArray;
import primal.fp.Funs.Source;
import suite.Suite;
import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.Formatter;
import suite.os.FileUtil;
import suite.util.Memoize;

public class Defaults {

	public static int bufferLimit = 65536;
	public static int bufferSize = 4096;
	public static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
	public static DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
	public static int nThreads = Runtime.getRuntime().availableProcessors();
	public static String separator = "________________________________________________________________________________\n";
	public static boolean testFlag = false; // for controlled experiments
	public static Path tmp = Paths.get("/tmp");

	public static FixieArray<String> bindSecrets(String pattern) {
		var m = secrets(pattern);
		return m != null ? FixieArray.of(m) : fail("Cannot match " + pattern);
	}

	public static String[] secrets(String pattern) {
		var generalizer = new Generalizer();

		if (secrets().prove(generalizer.generalize(Suite.parse(pattern)))) {
			var list = new ArrayList<>();
			var i = 0;
			Node n;
			while (!((n = generalizer.getVariable(Atom.of("." + i++))).finalNode() instanceof Reference))
				list.add(Formatter.display(n));
			return list.toArray(new String[0]);
		} else
			return null;
	}

	public static Prover secrets() {
		return memoizeSecrets.g();
	}

	public static Path tmp(String path) {
		return tmp.resolve(path);
	}

	private static Source<Prover> memoizeSecrets = Memoize.source(() -> {
		var rs = Suite.newRuleSet();
		var text = FileUtil.read(HomeDir.resolve("private/secrets.sl"));
		rs.importFrom(Suite.parse(text));
		return new Prover(rs);
	});

}
