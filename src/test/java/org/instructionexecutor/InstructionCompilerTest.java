package org.instructionexecutor;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.instructionexecutor.CompiledRunUtil.Closure;
import org.instructionexecutor.CompiledRunUtil.CompiledRun;
import org.instructionexecutor.CompiledRunUtil.CompiledRunConfig;
import org.junit.Test;
import org.suite.Suite;
import org.suite.doer.Cloner;
import org.suite.kb.RuleSet;
import org.suite.kb.RuleSetUtil;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.search.InterpretedProverBuilder;
import org.suite.search.ProverBuilder.Builder;
import org.suite.search.ProverBuilder.Finder;
import org.util.FunUtil;

public class InstructionCompilerTest {

	@Test
	public void testEagerFunctional() throws IOException {
		Node goal = Suite.parse("1 + 2 * 3");
		Node code = compileFunctional(goal, false);
		assertEquals(Int.create(7), execute(code));
	}

	@Test
	public void testLazyFunctional() throws IOException {
		Node goal = Suite.parse("1 + 2 * 3");
		Node code = compileFunctional(goal, true);
		assertEquals(Int.create(7), execute(code));
	}

	@Test
	public void testStandardLibrary() throws IOException {
		Node goal = Suite.parse("using STANDARD >> 1, 2, 3, | map {`+ 1`} | fold-left {`+`} {0}");
		Node code = compileFunctional(goal, false);
		assertEquals(Int.create(9), execute(code));
	}

	@Test
	public void testLogical() throws IOException {
		Node goal = Suite.parse(".a = 1, .b = .a, dump .b");
		Node code = compileLogical(goal);
		assertEquals(Atom.TRUE, execute(code));
	}

	private Node compileFunctional(Node program, boolean isLazy) {
		RuleSet ruleSet = Suite.funCompilerRuleSet(isLazy);
		Atom mode = Atom.create(isLazy ? "LAZY" : "EAGER");
		Node goal = Suite.substitute("source .in, compile-function .0 .in .out, sink .out", mode);
		return compile(ruleSet, goal, program);
	}

	private Node compileLogical(Node program) {
		RuleSet ruleSet = Suite.logicCompilerRuleSet();
		Node goal = Suite.parse("source .in, compile-logic .in .out, sink .out");
		return compile(ruleSet, goal, program);
	}

	private Node compile(RuleSet ruleSet, Node goal, Node program) {
		Builder builder = new InterpretedProverBuilder();
		Finder compiler = builder.build(ruleSet, goal);
		final Node holder[] = new Node[] { null };

		compiler.find(FunUtil.source(program), new FunUtil.Sink<Node>() {
			public void sink(Node node) {
				holder[0] = new Cloner().clone(node);
			}
		});

		Node code = holder[0];

		if (code != null)
			return code;
		else
			throw new RuntimeException("Compilation error");
	}

	private Node execute(Node code) throws IOException {
		String basePathName = "/tmp/" + InstructionCompiler.class.getName();

		CompiledRunConfig config = new CompiledRunConfig();
		config.ruleSet = RuleSetUtil.create();

		try (CompiledRun compiledRun = new InstructionCompiler(basePathName).compile(code)) {
			return compiledRun.exec(config, new Closure(null, 0));
		}
	}

}
