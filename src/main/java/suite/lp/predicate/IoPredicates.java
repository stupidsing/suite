package suite.lp.predicate;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import suite.Constants;
import suite.lp.doer.Cloner;
import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.node.Int;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.TermOp;
import suite.node.util.SuiteException;
import suite.os.FileUtil;
import suite.os.LogUtil;
import suite.primitive.Bytes.BytesBuilder;
import suite.util.Rethrow;

public class IoPredicates {

	public BuiltinPredicate dump = (prover, ps) -> {
		System.out.print(Formatter.dump(ps));
		return true;
	};

	public BuiltinPredicate dumpStack = (prover, ps) -> {
		String date = LocalDateTime.now().toString();
		String trace = prover.getTracer().getStackTrace();
		LogUtil.info("-- Stack trace at " + date + " --\n" + trace);
		return true;
	};

	public BuiltinPredicate exec = PredicateUtil.p1((prover, p0) -> {
		if (p0 instanceof Str)
			try {
				String cmd = ((Str) p0).value;
				return Runtime.getRuntime().exec(cmd).waitFor() == 0;
			} catch (Exception ex) { // IOException or InterruptedException
				LogUtil.error(ex);
			}
		return false;
	});

	public BuiltinPredicate exit = PredicateUtil.sink(n -> System.exit(n instanceof Int ? ((Int) n).number : 0));

	public BuiltinPredicate fileExists = PredicateUtil.bool(n -> Files.exists(Paths.get(Formatter.display(n))));

	public BuiltinPredicate fileRead = PredicateUtil.fun(n -> {
		String filename = Formatter.display(n);
		return Rethrow.ioException(() -> new Str(FileUtil.read(filename)));
	});

	public BuiltinPredicate fileTime = PredicateUtil.fun(n -> {
		return Rethrow.ioException(() -> {
			FileTime lastModifiedTime = Files.getLastModifiedTime(Paths.get(Formatter.display(n)));
			return Int.of((int) lastModifiedTime.to(TimeUnit.SECONDS));
		});
	});

	public BuiltinPredicate fileWrite = PredicateUtil.p2((prover, fn, contents) -> {
		String filename = Formatter.display(fn);
		String content = Formatter.display(contents);

		try (OutputStream fos = FileUtil.out(filename)) {
			fos.write(content.getBytes(Constants.charset));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		return true;
	});

	public BuiltinPredicate jar = PredicateUtil.p1((prover, p0) -> prover.bind(new Str(FileUtil.jarFilename()), p0));

	public BuiltinPredicate homeDir = PredicateUtil.p1((prover, p0) -> prover.bind(new Str(FileUtil.homeDir()), p0));

	public BuiltinPredicate nl = PredicateUtil.run(() -> System.out.println());

	public BuiltinPredicate readLine = PredicateUtil.p1((prover, p0) -> {
		return Rethrow.ioException(() -> {
			BytesBuilder bb = new BytesBuilder();
			byte b;
			while (0 <= (b = (byte) System.in.read()) && b != 10)
				bb.append(b);
			String s = new String(bb.toBytes().toBytes(), Constants.charset);
			return prover.bind(new Str(s), p0);
		});
	});

	public BuiltinPredicate log = PredicateUtil.sink(n -> LogUtil.info(Formatter.dump(n)));

	public BuiltinPredicate sink = PredicateUtil.p1((prover, p0) -> {
		prover.config().getSink().sink(p0);
		return false;
	});

	public BuiltinPredicate source = PredicateUtil.p1((prover, p0) -> prover.bind(p0, prover.config().getSource().source()));

	public BuiltinPredicate throwPredicate = PredicateUtil.sink(n -> {
		throw new SuiteException(new Cloner().clone(n));
	});

	public BuiltinPredicate tryPredicate = PredicateUtil.p3((prover, try_, catch_, throw_) -> {
		try {
			return PredicateUtil.tryProve(prover, prover1 -> prover1.prove0(try_));
		} catch (SuiteException ex) {
			if (prover.bind(catch_, ex.getNode())) {
				prover.setRemaining(Tree.of(TermOp.AND___, throw_, prover.getRemaining()));
				return true;
			} else
				throw ex;
		}
	});

	public BuiltinPredicate write(PrintStream printStream) {
		return (prover, ps) -> {
			printStream.print(Formatter.display(ps));
			return true;
		};
	}

}
