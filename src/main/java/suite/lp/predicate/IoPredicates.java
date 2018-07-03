package suite.lp.predicate;

import static suite.util.Friends.rethrow;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import suite.cfg.Defaults;
import suite.lp.doer.Cloner;
import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.node.Int;
import suite.node.Str;
import suite.node.io.Formatter;
import suite.node.tree.TreeAnd;
import suite.node.util.SuiteException;
import suite.os.FileUtil;
import suite.os.LogUtil;
import suite.primitive.Bytes.BytesBuilder;
import suite.util.To;

public class IoPredicates {

	public BuiltinPredicate dump = (prover, ps) -> {
		System.out.print(Formatter.dump(ps));
		return true;
	};

	public BuiltinPredicate dumpStack = (prover, ps) -> {
		var date = LocalDateTime.now().toString();
		var trace = prover.getTracer().getStackTrace();
		LogUtil.info("-- stack trace at " + date + " --\n" + trace);
		return true;
	};

	public BuiltinPredicate exec = PredicateUtil.p1((prover, p0) -> {
		if (p0 instanceof Str)
			try {
				var cmd = Str.str(p0);
				return Runtime.getRuntime().exec(cmd).waitFor() == 0;
			} catch (IOException | InterruptedException ex) {
				LogUtil.error(ex);
			}
		return false;
	});

	public BuiltinPredicate exit = PredicateUtil.sink(n -> System.exit(n instanceof Int ? Int.num(n) : 0));

	public BuiltinPredicate fileExists = PredicateUtil.bool(n -> Files.exists(Paths.get(Formatter.display(n))));

	public BuiltinPredicate fileRead = PredicateUtil.fun(n -> {
		var filename = Formatter.display(n);
		return new Str(FileUtil.read(filename));
	});

	public BuiltinPredicate fileTime = PredicateUtil.fun(n -> {
		return rethrow(() -> {
			var lastModifiedTime = Files.getLastModifiedTime(Paths.get(Formatter.display(n)));
			return Int.of((int) lastModifiedTime.to(TimeUnit.SECONDS));
		});
	});

	public BuiltinPredicate fileWrite = PredicateUtil.p2((prover, fn, contents) -> {
		var filename = Formatter.display(fn);
		var content = Formatter.display(contents);

		FileUtil.out(filename).writeAndClose(content.getBytes(Defaults.charset));
		return true;
	});

	public BuiltinPredicate jar = PredicateUtil.p1((prover, p0) -> prover.bind(new Str(FileUtil.jarFilename()), p0));

	public BuiltinPredicate homeDir = PredicateUtil.p1((prover, p0) -> prover.bind(new Str(FileUtil.homeDir()), p0));

	public BuiltinPredicate nl = PredicateUtil.run(System.out::println);

	public BuiltinPredicate readLine = PredicateUtil.p1((prover, p0) -> rethrow(() -> {
		var bb = new BytesBuilder();
		byte b;
		while (0 <= (b = (byte) System.in.read()) && b != 10)
			bb.append(b);
		var s = To.string(bb.toBytes());
		return prover.bind(new Str(s), p0);
	}));

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
				prover.setRemaining(TreeAnd.of(throw_, prover.getRemaining()));
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
