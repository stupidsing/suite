package suite;

import java.io.IOException;
import java.util.List;

import suite.adt.pair.Pair;
import suite.fp.FunCompilerConfig;
import suite.instructionexecutor.EagerFunInstructionExecutor;
import suite.instructionexecutor.FunInstructionExecutor;
import suite.instructionexecutor.LazyFunInstructionExecutor;
import suite.instructionexecutor.thunk.ThunkUtil;
import suite.lp.Configuration.ProverConfig;
import suite.lp.doer.Specializer;
import suite.lp.kb.RuleSet;
import suite.lp.search.CompiledProverBuilder;
import suite.lp.search.FindUtil;
import suite.lp.search.InterpretedProverBuilder;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.ProverBuilder.Finder;
import suite.lp.search.SewingProverBuilder;
import suite.node.Atom;
import suite.node.Node;
import suite.os.LogUtil;
import suite.primitive.IoSink;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Memoize;
import suite.util.String_;

public class EvaluateUtil {

	private Fun<Boolean, Node> fccNodeFun = Memoize.fun(isLazy -> {
		Atom mode = Atom.of(isLazy ? "LAZY" : "EAGER");

		return new Specializer().specialize(Suite.substitute("" //
				+ "source .in, compile-function .0 .in .out, sink .out", mode));
	});

	// using level 1 CompiledProverBuilder would break the test case
	// FunRbTreeTest. it would blow up the stack in InstructionExecutor
	private Fun<Pair<ProverConfig, Node>, Finder> fccFinderFun = Memoize.fun(pair -> {
		Builder builder = new SewingProverBuilder(pair.t0);
		// builder builder = new InterpretedProverBuilder(pair.t0);
		// builder builder = new CompiledProverBuilder.level1(pair.t0);
		return builder.build(Suite.funCompilerRuleSet()).apply(pair.t1);
	});

	public boolean proveLogic(Node lp) {
		Builder builder = CompiledProverBuilder.level1(new ProverConfig());
		return proveLogic(builder, Suite.newRuleSet(), lp);
	}

	public boolean proveLogic(RuleSet rs, Node lp) {
		return proveLogic(new InterpretedProverBuilder(), rs, lp);
	}

	public boolean proveLogic(Builder builder, RuleSet rs, Node lp) {
		Node goal = Suite.substitute(".0, sink ()", lp);
		return evaluateLogic(builder, rs, goal).source() != null;
	}

	public Source<Node> evaluateLogic(Builder builder, RuleSet rs, Node lp) {
		return FindUtil.collect(builder.build(rs).apply(lp), Atom.NIL);
	}

	public Node evaluateFun(FunCompilerConfig fcc) {
		try (FunInstructionExecutor executor = configureFunExecutor(fcc)) {
			Node result = executor.execute();
			return fcc.isLazy() ? ThunkUtil.deepYawn(executor.getYawnFun(), result) : result;
		}
	}

	public void evaluateCallback(FunCompilerConfig fcc, IoSink<FunInstructionExecutor> sink) throws IOException {
		try (FunInstructionExecutor executor = configureFunExecutor(fcc)) {
			sink.sink(executor);
		}
	}

	public Node evaluateFunType(FunCompilerConfig fcc) {
		Node node = Suite.parse("" //
				+ "source .in" //
				+ ", fc-parse .in .p" //
				+ ", fc-infer-type .p .t0" //
				+ ", graph.specialize .t0 .t1" //
				+ ", fc-parse-type .t2 .t1" //
				+ ", graph.generalize .t2 .out" //
				+ ", sink .out");

		Node type = doFcc(node, fcc);

		if (type != null)
			return type;
		else
			throw new RuntimeException("type inference failure");
	}

	private FunInstructionExecutor configureFunExecutor(FunCompilerConfig fcc) {
		Node node = fccNodeFun.apply(fcc.isLazy());
		Node code = doFcc(node, fcc);

		if (code != null)
			if (fcc.isLazy())
				return new LazyFunInstructionExecutor(code);
			else
				return new EagerFunInstructionExecutor(code);
		else
			throw new RuntimeException("function compilation failure");
	}

	private Node doFcc(Node compileNode, FunCompilerConfig fcc) {
		return LogUtil.duration("Code compiled", () -> {
			ProverConfig pc = fcc.getProverConfig();
			Finder finder = fccFinderFun.apply(Pair.of(pc, compileNode));
			return FindUtil.collectSingle(finder, appendLibraries(fcc));
		});
	}

	private Node appendLibraries(FunCompilerConfig fcc) {
		Node node = fcc.getNode();
		List<String> libraries = fcc.getLibraries();

		for (int i = libraries.size() - 1; 0 <= i; i--) {
			String library = libraries.get(i);
			if (!String_.isBlank(library))
				node = Suite.substitute("use .0 >> .1", Atom.of(library), node);
		}

		return node;
	}

}
