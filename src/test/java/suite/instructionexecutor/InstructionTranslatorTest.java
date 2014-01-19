package suite.instructionexecutor;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import suite.Suite;
import suite.instructionexecutor.TranslatedRunUtil.Closure;
import suite.instructionexecutor.TranslatedRunUtil.TranslatedRun;
import suite.instructionexecutor.TranslatedRunUtil.TranslatedRunConfig;
import suite.lp.doer.Cloner;
import suite.lp.kb.RuleSet;
import suite.lp.search.InterpretedProverBuilder;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.ProverBuilder.Finder;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.util.FileUtil;
import suite.util.FunUtil.Sink;
import suite.util.To;

public class InstructionTranslatorTest {

	@Test
	public void testCut() throws IOException {
		assertLogical("(.v = 1; .v = 2), !, .v = 1", Atom.TRUE);
		assertLogical("(.v = 1; .v = 2), !, .v = 2", Atom.FALSE);
	}

	@Test
	public void testEagerFunctional() throws IOException {
		assertFunctional("1 + 2 * 3", false, Int.create(7));
	}

	@Test
	public void testLazyFunctional() throws IOException {
		assertFunctional("1 + 2 * 3", true, Int.create(7));
	}

	@Test
	public void testAtomString() throws IOException {
		Node node = execute(compileFunctional(Suite.parse("" //
				+ "define atom-string = _ijavacls {CLASS!suite.lp.invocable.Invocables$AtomString} >> " //
				+ "_ijavaobj1 {atom-string} {ATOM}"), false));
		System.out.println(node);
	}

	@Test
	public void testStandardLibrary() throws IOException {
		String program = "using /STANDARD >> 1; 2; 3; | map {`+ 1`} | fold-left {`+`} {0}";
		assertFunctional(program, false, Int.create(9));
	}

	@Test
	public void testLogical() throws IOException {
		assertLogical(".a = 1, (.a = 2; .a = 1), .b = .a, dump .b", Atom.TRUE);
	}

	private void assertFunctional(String program, boolean isLazy, Int result) throws IOException {
		Node code = compileFunctional(Suite.parse(program), isLazy);
		assertEquals(result, execute(code));
	}

	private void assertLogical(String goal, Atom result) throws IOException {
		Node code = compileLogical(Suite.parse(goal));
		assertEquals(result, execute(code));
	}

	private Node compileFunctional(Node program, boolean isLazy) {
		RuleSet ruleSet = Suite.funCompilerRuleSet();
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

		compiler.find(To.source(program), new Sink<Node>() {
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
		String basePathName = FileUtil.tmp + "/" + InstructionTranslator.class.getName();

		TranslatedRunConfig config = new TranslatedRunConfig();
		config.ruleSet = Suite.createRuleSet();

		try (TranslatedRun translatedRun = new InstructionTranslator(basePathName).translate(code)) {
			return translatedRun.exec(config, new Closure(null, 0));
		}
	}

}
