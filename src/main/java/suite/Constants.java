package suite;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.lp.kb.RuleSet;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.Formatter;
import suite.util.FunUtil.Source;
import suite.util.HomeDir;
import suite.util.Memoize;
import suite.util.To;

public class Constants {

	public static int bufferSize = 4096;
	public static Charset charset = StandardCharsets.UTF_8;
	public static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
	public static DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
	public static int nThreads = Runtime.getRuntime().availableProcessors();
	public static String separator = "________________________________________________________________________________\n";
	public static boolean testFlag = false; // for controlled experiments
	public static Path tmp = Paths.get("/tmp");

	private static Source<Prover> memoizeSecrets = Memoize.source(() -> {
		RuleSet rs = Suite.newRuleSet();
		String text = To.string(HomeDir.resolve("private/secrets.sl"));
		Suite.importFrom(rs, Suite.parse(text));
		return new Prover(rs);
	});

	public static String[] secrets(String pattern) {
		Generalizer generalizer = new Generalizer();

		if (secrets().prove(generalizer.generalize(Suite.parse(pattern)))) {
			List<String> list = new ArrayList<>();
			int i = 0;
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

}
