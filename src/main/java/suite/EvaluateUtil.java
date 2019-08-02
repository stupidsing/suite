package suite;

import static primal.statics.Fail.fail;

import java.io.IOException;

import primal.Verbs.Is;
import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Source;
import suite.fp.FunCompilerCfg;
import suite.instructionexecutor.EagerFunInstructionExecutor;
import suite.instructionexecutor.FunInstructionExecutor;
import suite.instructionexecutor.LazyFunInstructionExecutor;
import suite.instructionexecutor.thunk.ThunkUtil;
import suite.lp.Configuration.ProverCfg;
import suite.lp.doer.Specializer;
import suite.lp.kb.RuleSet;
import suite.lp.search.CompiledProverBuilder;
import suite.lp.search.InterpretedProverBuilder;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.ProverBuilder.Finder;
import suite.lp.search.SewingProverBuilder;
import suite.node.Atom;
import suite.node.Node;
import suite.os.LogUtil;
import suite.primitive.IoSink;
import suite.util.Memoize;

public class EvaluateUtil {

	private Fun<Boolean, Node> fccNodeFun = Memoize.fun(isLazy -> {
		var mode = Atom.of(isLazy ? "LAZY" : "EAGER");

		return new Specializer().specialize(Suite.substitute("" //
				+ "source .in, compile-function .0 .in .out, sink .out", mode));
	});

	// using level 1 CompiledProverBuilder would break the test case
	// FunRbTreeTest. it would blow up the stack in InstructionExecutor
	private Fun<Pair<ProverCfg, Node>, Finder> fccFinderFun = Memoize.fun(pair -> {
		var builder = new SewingProverBuilder(pair.k);
		// builder builder = new InterpretedProverBuilder(pair.t0);
		// builder builder = new CompiledProverBuilder.level1(pair.t0);
		return builder.build(Suite.funCompilerRuleSet()).apply(pair.v);
	});

	public boolean proveLogic(Node lp) {
		var builder = CompiledProverBuilder.level1(new ProverCfg());
		return proveLogic(builder, Suite.newRuleSet(), lp);
	}

	public boolean proveLogic(RuleSet rs, Node lp) {
		return proveLogic(new InterpretedProverBuilder(), rs, lp);
	}

	public boolean proveLogic(Builder builder, RuleSet rs, Node lp) {
		Node goal = Suite.substitute(".0, sink ()", lp);
		return evaluateLogic(builder, rs, goal).g() != null;
	}

	public Source<Node> evaluateLogic(Builder builder, RuleSet rs, Node lp) {
		return builder.build(rs).apply(lp).collect(Atom.NIL);
	}

	public Node evaluateFun(FunCompilerCfg fcc) {
		try (var executor = configureFunExecutor(fcc)) {
			var result = executor.execute();
			return fcc.isLazy() ? ThunkUtil.deepYawn(executor.getYawnFun(), result) : result;
		}
	}

	public void evaluateCallback(FunCompilerCfg fcc, IoSink<FunInstructionExecutor> sink) throws IOException {
		try (var executor = configureFunExecutor(fcc)) {
			sink.f(executor);
		}
	}

	public Node evaluateFunType(FunCompilerCfg fcc) {
		var node = Suite.parse("" //
				+ "source .in" //
				+ ", fc-parse .in .p" //
				+ ", fc-infer-type .p .t0" //
				+ ", graph.specialize .t0 .t1" //
				+ ", fc-parse-type .t2 .t1" //
				+ ", graph.generalize .t2 .out" //
				+ ", sink .out");

		var type = doFcc(node, fcc);

		if (type != null)
			return type;
		else
			return fail("type inference failure");
	}

	private FunInstructionExecutor configureFunExecutor(FunCompilerCfg fcc) {
		var node = fccNodeFun.apply(fcc.isLazy());
		var code = doFcc(node, fcc);

		if (code != null)
			if (fcc.isLazy())
				return new LazyFunInstructionExecutor(code);
			else
				return new EagerFunInstructionExecutor(code);
		else
			return fail("function compilation failure");
	}

	private Node doFcc(Node compileNode, FunCompilerCfg fcc) {
		return LogUtil.duration("Code compiled", () -> {
			var pc = fcc.getProverConfig();
			var finder = fccFinderFun.apply(Pair.of(pc, compileNode));
			return finder.collectSingle(appendLibraries(fcc));
		});
	}

	private Node appendLibraries(FunCompilerCfg fcc) {
		var node = fcc.getNode();
		var libraries = fcc.getLibraries();

		for (var library : libraries.reverse())
			if (!Is.blank(library))
				node = Suite.substitute("use .0 ~ .1", Atom.of(library), node);

		return node;
	}

}
