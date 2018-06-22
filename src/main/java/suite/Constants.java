package suite;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

import suite.adt.pair.FixieArray;
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

	public static FixieArray<String> bindSecrets(String pattern) {
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
			m = Fail.t("Cannot match " + pattern);

		return FixieArray.of(m);
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
		rs.importFrom(Suite.parse(text));
		return new Prover(rs);
	});

}
