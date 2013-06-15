package org.instructionexecutor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.instructionexecutor.InstructionCompiler.CompiledRun;
import org.junit.Test;
import org.suite.Suite;
import org.suite.doer.Cloner;
import org.suite.kb.RuleSetUtil;
import org.suite.node.Node;
import org.suite.search.InterpretedProverBuilder;
import org.suite.search.ProverBuilder.Finder;
import org.util.FunUtil;

public class InstructionCompilerTest {

	@Test
	public void test() throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
		Node goal = Suite.parse(".a = 1, .b = .a, dump .b");

		InstructionCompiler instructionCompiler = new InstructionCompiler("src/main/java");
		instructionCompiler.compile(compileLogical(goal));

		String filename = instructionCompiler.getFilename();
		String className = instructionCompiler.getClassName();

		Class<?> clazz = compileJava(filename, className);

		System.out.println("Class has been successfully loaded");
		CompiledRun compiledRun = (CompiledRun) clazz.newInstance();
		System.out.println(compiledRun.exec(RuleSetUtil.create()));
	}

	@Test
	public void testDryRun() {
		System.out.println(new CompiledRun3().exec(RuleSetUtil.create()));
	}

	private Node compileLogical(Node program) {
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

	private Class<?> compileJava(String filename, String className) throws IOException, ClassNotFoundException {
		String binDir = "/tmp/" + InstructionCompiler.class.getName();
		new File(binDir).mkdirs();

		JavaCompiler jc = ToolProvider.getSystemJavaCompiler();

		try (StandardJavaFileManager sjfm = jc.getStandardFileManager(null, null, null)) {
			File file = new File(filename);

			jc.getTask(null //
					, null //
					, null //
					, Arrays.asList("-d", binDir) //
					, null //
					, sjfm.getJavaFileObjects(file)).call();

			URL urls[] = { new URL("file://" + binDir + "/") };

			try (URLClassLoader ucl = new URLClassLoader(urls)) {
				return ucl.loadClass(className);
			}
		}
	}

}
