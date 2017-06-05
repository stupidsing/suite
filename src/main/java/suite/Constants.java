package suite;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import suite.lp.doer.Prover;
import suite.lp.kb.RuleSet;
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

	public static Prover secrets() {
		return memoizeSecrets.source();
	}
}
