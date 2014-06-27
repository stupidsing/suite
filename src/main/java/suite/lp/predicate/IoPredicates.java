package suite.lp.predicate;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;

import suite.lp.doer.Prover;
import suite.lp.predicate.SystemPredicates.SystemPredicate;
import suite.node.Int;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.util.FileUtil;
import suite.util.FunUtil.Fun;
import suite.util.LogUtil;
import suite.util.To;

public class IoPredicates {

	public static SystemPredicate dump = (prover, ps) -> {
		System.out.print(Formatter.dump(ps));
		return true;
	};

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

	public static SystemPredicate exit = (prover, ps) -> {
		System.exit(ps instanceof Int ? ((Int) ps).getNumber() : 0);
		return true;
	};

	public static SystemPredicate fileExists = (prover, ps) -> {
		return new File(Formatter.display(ps)).exists();
	};

	public static SystemPredicate fileRead = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		String filename = Formatter.display(params[0]);
		try {
			String content = To.string(new File(filename));
			return prover.bind(new Str(content), params[1]);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	};

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

	public static SystemPredicate homeDir = (prover, ps) -> {
		String homeDir = System.getProperty("home.dir");
		homeDir = homeDir != null ? homeDir : ".";
		return prover.bind(new Str(homeDir), ps);
	};

	public static SystemPredicate nl = (prover, ps) -> {
		System.out.println();
		return true;
	};

	public static SystemPredicate log = (prover, ps) -> {
		LogUtil.info(Formatter.dump(ps));
		return true;
	};

	public static SystemPredicate sink = (prover, ps) -> {
		prover.config().getSink().sink(ps);
		return false;
	};

	public static SystemPredicate source = (prover, ps) -> {
		Node source = prover.config().getSource().source();
		return prover.bind(ps, source);
	};

	public static SystemPredicate throwPredicate = (prover, ps) -> {
		throw new RuntimeException(Formatter.dump(ps.finalNode()));
	};

	public static Fun<PrintStream, SystemPredicate> write = printStream -> new SystemPredicate() {
		public boolean prove(Prover prover, Node ps) {
			printStream.print(Formatter.display(ps));
			return true;
		}
	};

}
