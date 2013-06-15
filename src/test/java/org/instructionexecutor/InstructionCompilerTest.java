package org.instructionexecutor;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.instructionexecutor.CompiledRunUtil.CompiledRun;
import org.junit.Test;
import org.suite.Suite;
import org.suite.doer.Cloner;
import org.suite.kb.RuleSetUtil;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.search.InterpretedProverBuilder;
import org.suite.search.ProverBuilder.Finder;
import org.util.FunUtil;

public class InstructionCompilerTest {

	public void testEagerFunctional() throws IOException {
		Node goal = Suite.parse("using STANDARD >> 1, 2, 3, | map {`+ 1`} | fold-left {`+`} {0}");
		Node code = compileEagerFunctional(goal);
		assertEquals(Int.create(9), execute(code));
	}

	@Test
	public void testLogical() throws IOException {
		Node goal = Suite.parse(".a = 1, .b = .a, dump .b");
		Node code = compileLogical(goal);
		assertEquals(Atom.TRUE, execute(code));
	}

	private Node compileEagerFunctional(Node program) {
		InterpretedProverBuilder builder = new InterpretedProverBuilder();
		final Node holder[] = new Node[] { null };

		Node goal = Suite.parse("source .in, compile-function EAGER .in .out, sink .out");
		Finder compiler = builder.build(Suite.eagerFunCompilerRuleSet(), goal);

		compiler.find(FunUtil.source(program), new FunUtil.Sink<Node>() {
			public void sink(Node node) {
				holder[0] = new Cloner().clone(node);
			}
		});

		Node code = holder[0];

		if (code != null)
			return code;
		else
			throw new RuntimeException("Functional compilation error");
	}

	private Node compileLogical(Node program) {
		InterpretedProverBuilder builder = new InterpretedProverBuilder();
		final Node holder[] = new Node[] { null };

		Node goal = Suite.parse("source .in, compile-logic .in .out, sink .out");
		Finder compiler = builder.build(Suite.logicalCompilerRuleSet(), goal);

		compiler.find(FunUtil.source(program), new FunUtil.Sink<Node>() {
			public void sink(Node node) {
				holder[0] = new Cloner().clone(node);
			}
		});

		Node code = holder[0];

		if (code != null)
			return code;
		else
			throw new RuntimeException("Logic compilation error");
	}

	private Node execute(Node code) throws IOException {
		String basePathName = "/tmp/" + InstructionCompiler.class.getName();

		try (CompiledRun compiledRun = new InstructionCompiler(basePathName).compile(code)) {
			return compiledRun.exec(RuleSetUtil.create());
		}
	}

}
