package org.instructionexecutor;

import org.junit.Test;
import org.suite.Suite;
import org.suite.doer.Cloner;
import org.suite.node.Node;
import org.suite.search.InterpretedProverBuilder;
import org.suite.search.ProverBuilder.Finder;
import org.util.FunUtil;

public class InstructionCompilerTest {

	@Test
	public void test() {
		Node goal = Suite.parse(".a = 1, .b = .a, dump .b");
		new InstructionCompiler(compile(goal));

	}

	private Node compile(Node program) {
		InterpretedProverBuilder builder = new InterpretedProverBuilder();
		final Node holder[] = new Node[] { null };

		String compile = "source .in, compile-logic .in .out, sink .out";

		Finder compiler = builder.build(Suite.logicalCompilerRuleSet(), Suite.parse(compile));

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

}
