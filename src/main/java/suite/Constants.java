package suite;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

import suite.adt.pair.Fixie;
import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.Formatter;
import suite.util.Fail;
import suite.util.FunUtil.Source;
import suite.util.HomeDir;
import suite.util.Memoize;
import suite.util.To;

public class Constants {

	public static int bufferLimit = 65536;
	public static int bufferSize = 4096;
	public static Charset charset = StandardCharsets.UTF_8;
	public static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
	public static DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
	public static int nThreads = Runtime.getRuntime().availableProcessors();
	public static String separator = "________________________________________________________________________________\n";
	public static boolean testFlag = false; // for controlled experiments
	public static Path tmp = Paths.get("/tmp");

	public static Fixie<String, String, String, String, String, String, String, String, String, String> bindSecrets(
			String pattern) {
		var generalizer = new Generalizer();
		String[] m;

		if (secrets().prove(generalizer.generalize(Suite.parse(pattern)))) {
			var list = new ArrayList<>();
			var i = 0;
			Node n;
			while (!((n = generalizer.getVariable(Atom.of("." + i++))).finalNode() instanceof Reference))
				list.add(Formatter.display(n));
			m = list.toArray(new String[0]);
		} else
			m = null;

		if (m != null) {
			var length = m.length;
			return Fixie.of( //
					0 < length ? m[0] : null, //
					1 < length ? m[1] : null, //
					2 < length ? m[2] : null, //
					3 < length ? m[3] : null, //
					4 < length ? m[4] : null, //
					5 < length ? m[5] : null, //
					6 < length ? m[6] : null, //
					7 < length ? m[7] : null, //
					8 < length ? m[8] : null, //
					9 < length ? m[9] : null);
		} else
			return Fail.t("Cannot match " + pattern);
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
		return memoizeSecrets.source();
	}

	public static Path tmp(String path) {
		return tmp.resolve(path);
	}

	private static Source<Prover> memoizeSecrets = Memoize.source(() -> {
		var rs = Suite.newRuleSet();
		var text = To.string(HomeDir.resolve("private/secrets.sl"));
		Suite.importFrom(rs, Suite.parse(text));
		return new Prover(rs);
	});

}
