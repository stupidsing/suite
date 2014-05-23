package suite.instructionexecutor;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import suite.Suite;
import suite.instructionexecutor.TranslatedRunUtil.Closure;
import suite.instructionexecutor.TranslatedRunUtil.TranslatedRun;
import suite.instructionexecutor.TranslatedRunUtil.TranslatedRunConfig;
import suite.lp.kb.RuleSet;
import suite.lp.search.FindUtil;
import suite.lp.search.InterpretedProverBuilder;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.ProverBuilder.Finder;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.util.FileUtil;
import suite.util.FunUtil.Fun;

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
				+ "define atom-string := +getintrn {atom:CLASS!suite.lp.intrinsic.Intrinsics$AtomString} >> " //
				+ "+callintrn1 {atom-string} {atom:ATOM}"), isLazy), isLazy);
		assertEquals("ATOM", node);
	}

	@Test
	public void testStandardLibrary() throws IOException {
		String program = "using source STANDARD >> 1; 2; 3; | map {`+ 1`} | fold-left {`+`} {0}";
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
		Builder builder = new InterpretedProverBuilder();
		Finder compiler = builder.build(ruleSet, goal);
		return FindUtil.collectSingle(compiler, program);
	}

	private Node execute(Node code, boolean isLazy) throws IOException {
		return execute(code, isLazy, exec -> exec.apply(new Closure(null, 0)));
	}

	private String executeToString(Node code, boolean isLazy) throws IOException {
		return execute(code, isLazy, exec -> ExpandUtil.expandString(exec, exec.apply(new Closure(null, 0))));
	}

	private <T> T execute(Node code, boolean isLazy, Fun<Fun<Node, Node>, T> fun) throws IOException {
		String basePathName = FileUtil.tmp + "/" + InstructionTranslator.class.getName();
		TranslatedRunConfig config = new TranslatedRunConfig(Suite.createRuleSet(), isLazy);

		try (InstructionTranslator instructionTranslator = new InstructionTranslator(basePathName)) {
			TranslatedRun translatedRun = instructionTranslator.translate(code);

			Fun<Node, Node> exec = node -> {
				if (node instanceof Closure)
					node = translatedRun.exec(config, (Closure) node);
				return node;
			};

			return fun.apply(exec);
		}
	}

}
