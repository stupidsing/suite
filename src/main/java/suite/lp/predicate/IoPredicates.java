package suite.lp.predicate;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import suite.lp.predicate.PredicateUtil.SystemPredicate;
import suite.node.Int;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.util.FileUtil;
import suite.util.LogUtil;

public class IoPredicates {

	public SystemPredicate dump = PredicateUtil.predicate(n -> System.out.print(Formatter.dump(n)));

	public SystemPredicate dumpStack = (prover, ps) -> {
		String date = LocalDateTime.now().toString();
		String trace = prover.getTracer().getStackTrace();
		LogUtil.info("-- Stack trace at " + date + " --\n" + trace);
		return true;
	};

	public SystemPredicate exec = (prover, ps) -> {
		if (ps instanceof Str)
			try {
				String cmd = ((Str) ps).getValue();
				return Runtime.getRuntime().exec(cmd).waitFor() == 0;
			} catch (Exception ex) { // IOException or InterruptedException
				LogUtil.error(ex);
			}
		return false;
	};

	public SystemPredicate exit = PredicateUtil.predicate(n -> System.exit(n instanceof Int ? ((Int) n).getNumber() : 0));

	public SystemPredicate fileExists = PredicateUtil.boolPredicate(n -> Files.exists(Paths.get(Formatter.display(n))));

	public SystemPredicate fileRead = PredicateUtil.funPredicate(n -> {
		String filename = Formatter.display(n);
		try {
			return new Str(FileUtil.read(filename));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	});

	public SystemPredicate fileWrite = (prover, ps) -> {
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

	public SystemPredicate homeDir = (prover, ps) -> {
		return prover.bind(new Str(FileUtil.homeDir()), ps);
	};

	public SystemPredicate nl = PredicateUtil.predicate(n -> System.out.println());

	public SystemPredicate log = PredicateUtil.predicate(n -> LogUtil.info(Formatter.dump(n)));

	public SystemPredicate sink = (prover, ps) -> {
		prover.config().getSink().sink(ps);
		return false;
	};

	public SystemPredicate source = (prover, ps) -> {
		Node source = prover.config().getSource().source();
		return prover.bind(ps, source);
	};

	public SystemPredicate throwPredicate = PredicateUtil.predicate(n -> {
		throw new RuntimeException(Formatter.dump(n.finalNode()));
	});

	public SystemPredicate write(PrintStream printStream) {
		return PredicateUtil.predicate(n -> printStream.print(Formatter.display(n)));
	}

}
