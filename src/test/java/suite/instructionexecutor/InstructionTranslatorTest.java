package suite.instructionexecutor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.Test;

import suite.Suite;
import suite.instructionexecutor.TranslatedRunUtil.Thunk;
import suite.instructionexecutor.TranslatedRunUtil.TranslatedRun;
import suite.instructionexecutor.TranslatedRunUtil.TranslatedRunConfig;
import suite.instructionexecutor.thunk.ThunkUtil;
import suite.lp.kb.RuleSet;
import suite.lp.search.FindUtil;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.ProverBuilder.Finder;
import suite.lp.search.SewingProverBuilder2;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.util.FunUtil.Fun;
import suite.util.TempDir;

public class InstructionTranslatorTest {

	@Test
	public void testCut() throws IOException {
		assertLogical("(.v = 1; .v = 2), !, .v = 1", Atom.TRUE);
		assertLogical("(.v = 1; .v = 2), !, .v = 2", Atom.FALSE);
	}

	@Test
	public void testEagerFunctional() throws IOException {
		assertFunctional("1 + 2 * 3", false, Int.of(7));
	}

	@Test
	public void testLazyFunctional() throws IOException {
		assertFunctional("1 + 2 * 3", true, Int.of(7));
	}

	@Test
	public void testAtomString() throws IOException {
		boolean isLazy = false;
		String node = executeToString(compileFunctional(Suite.parse("" //
				+ "define atom-string := +get%i {atom:INTRN!BasicIntrinsics.atomString} >> " //
				+ "+call%i-v1 {atom-string} {atom:ATOM}"), isLazy), isLazy);
		assertEquals("ATOM", node);
	}

	@Test
	public void testError() throws IOException {
		String program = "error ()";
		try {
			assertFunctional(program, false, Int.of(9));
			assertTrue(false);
		} catch (RuntimeException ex) {
			assertEquals("Error termination", ex.getMessage());
		}
	}

	@Test
	public void testStandardLibrary() throws IOException {
		String program = "use source STANDARD >> 1; 2; 3; | map {`+ 1`} | fold-left {`+`} {0}";
		assertFunctional(program, false, Int.of(9));
	}

	@Test
	public void testLogical() throws IOException {
		assertLogical(".a = 1, (.a = 2; .a = 1), .b = .a, dump .b", Atom.TRUE);
	}

	private void assertFunctional(String program, boolean isLazy, Int result) throws IOException {
		Node code = compileFunctional(Suite.parse(program), isLazy);
		assertEquals(result, execute(code, isLazy));
	}

	private void assertLogical(String goal, Atom result) throws IOException {
		Node code = compileLogical(Suite.parse(goal));
		assertEquals(result, execute(code, false));
	}

	private Node compileFunctional(Node program, boolean isLazy) {
		RuleSet ruleSet = Suite.funCompilerRuleSet();
		Atom mode = Atom.of(isLazy ? "LAZY" : "EAGER");
		Node goal = Suite.substitute("source .in, compile-function .0 .in .out, sink .out", mode);
		return compile(ruleSet, goal, program);
	}

	private Node compileLogical(Node program) {
		RuleSet ruleSet = Suite.logicCompilerRuleSet();
		Node goal = Suite.parse("source .in, compile-logic .in .out, sink .out");
		return compile(ruleSet, goal, program);
	}

	private Node compile(RuleSet ruleSet, Node goal, Node program) {
		Builder builder = new SewingProverBuilder2();
		Finder compiler = builder.build(ruleSet).apply(goal);
		return FindUtil.collectSingle(compiler, program);
	}

	private Node execute(Node code, boolean isLazy) throws IOException {
		return execute(code, isLazy, exec -> exec.apply(new Thunk(null, 0)));
	}

	private String executeToString(Node code, boolean isLazy) throws IOException {
		return execute(code, isLazy, exec -> ThunkUtil.yawnString(exec, exec.apply(new Thunk(null, 0))));
	}

	private <T> T execute(Node code, boolean isLazy, Fun<Fun<Node, Node>, T> fun) throws IOException {
		Path basePath = TempDir.resolve(InstructionTranslator.class.getName());
		TranslatedRunConfig config = new TranslatedRunConfig(Suite.createRuleSet(), isLazy);

		try (InstructionTranslator instructionTranslator = new InstructionTranslator(basePath)) {
			TranslatedRun translatedRun = instructionTranslator.translate(code);

			Fun<Node, Node> exec = node -> {
				if (node instanceof Thunk)
					node = translatedRun.exec(config, (Thunk) node);
				return node;
			};

			return fun.apply(exec);
		}
	}

}
