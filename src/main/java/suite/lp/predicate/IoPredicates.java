package suite.lp.predicate;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;

import suite.lp.predicate.SystemPredicates.SystemPredicate;
import suite.node.Int;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.util.FileUtil;
import suite.util.LogUtil;
import suite.util.To;

public class IoPredicates {

	public static SystemPredicate dump = SystemPredicates.predicate(n -> System.out.print(Formatter.dump(n)));

	public static SystemPredicate dumpStack = (prover, ps) -> {
		String date = LocalDateTime.now().toString();
		String trace = prover.getTracer().getStackTrace();
		LogUtil.info("-- Stack trace at " + date + " --\n" + trace);
		return true;
	};

	public static SystemPredicate exec = (prover, ps) -> {
		if (ps instanceof Str)
			try {
				String cmd = ((Str) ps).getValue();
				return Runtime.getRuntime().exec(cmd).waitFor() == 0;
			} catch (Exception ex) { // IOException or InterruptedException
				LogUtil.error(ex);
			}
		return false;
	};

	public static SystemPredicate exit = SystemPredicates.predicate(n -> System.exit(n instanceof Int ? ((Int) n).getNumber() : 0));

	public static SystemPredicate fileExists = SystemPredicates.boolPredicate(n -> new File(Formatter.display(n)).exists());

	public static SystemPredicate fileRead = SystemPredicates.funPredicate(n -> {
		String filename = Formatter.display(n);
		try {
			return new Str(To.string(new File(filename)));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	});

	public static SystemPredicate fileWrite = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		String filename = Formatter.display(params[0]);
		String content = Formatter.display(params[1]);

		try (OutputStream fos = FileUtil.out(filename)) {
			fos.write(content.getBytes(FileUtil.charset));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		return true;
	};

	public static SystemPredicate homeDir = SystemPredicates.funPredicate(n -> {
		String homeDir = System.getProperty("home.dir");
		return new Str(homeDir != null ? homeDir : ".");
	});

	public static SystemPredicate nl = SystemPredicates.predicate(n -> System.out.println());

	public static SystemPredicate log = SystemPredicates.predicate(n -> LogUtil.info(Formatter.dump(n)));

	public static SystemPredicate sink = (prover, ps) -> {
		prover.config().getSink().sink(ps);
		return false;
	};

	public static SystemPredicate source = (prover, ps) -> {
		Node source = prover.config().getSource().source();
		return prover.bind(ps, source);
	};

	public static SystemPredicate throwPredicate = SystemPredicates.predicate(n -> {
		throw new RuntimeException(Formatter.dump(n.finalNode()));
	});

	public static SystemPredicate write(PrintStream printStream) {
		return SystemPredicates.predicate(n -> printStream.print(Formatter.display(n)));
	}

}
