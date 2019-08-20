package suite.cfg;

import static primal.statics.Fail.fail;

import java.util.ArrayList;

import primal.Verbs.ReadString;
import primal.adt.FixieArray;
import primal.fp.Funs.Source;
import suite.Suite;
import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.Formatter;
import suite.util.Memoize;

public class Defaults {

	public static int bufferLimit = 65536;
	public static int nThreads = Runtime.getRuntime().availableProcessors();
	public static boolean testFlag = false; // for controlled experiments

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

	private static Source<Prover> memoizeSecrets = Memoize.source(() -> {
		var rs = Suite.newRuleSet();
		var text = ReadString.from(HomeDir.resolve("private/secrets.sl"));
		rs.importFrom(Suite.parse(text));
		return new Prover(rs);
	});

}
