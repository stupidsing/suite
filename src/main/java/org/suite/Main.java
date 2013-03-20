package org.suite;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.suite.SuiteUtil.FunCompilerConfig;
import org.suite.doer.Formatter;
import org.suite.doer.Generalizer;
import org.suite.doer.Prover;
import org.suite.doer.Station;
import org.suite.doer.TermParser;
import org.suite.doer.TermParser.TermOp;
import org.suite.kb.RuleSet;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.node.Tree;
import org.util.IoUtil;
import org.util.LogUtil;
import org.util.Util;

/**
 * Logic interpreter and functional interpreter. Likes Prolog and Haskell.
 * 
 * @author ywsing
 */
public class Main {

	private boolean isLazy = true;
	private boolean isDefaultLibrary = true;
	private List<String> libraries = Util.createList();
	private boolean isTrace = false;
	private boolean isDumpCode = false;

	public static void main(String args[]) {
		LogUtil.initLog4j();

		try {
			new Main().run(args);
		} catch (Throwable ex) {
			log.error(Main.class, ex);
		}
	}

	private void run(String args[]) throws IOException {
		int code = 0;

		boolean isFilter = false;
		boolean isFunctional = false;
		boolean isLogical = false;
		List<String> inputs = Util.createList();
		Iterator<String> iter = Arrays.asList(args).iterator();

		while (iter.hasNext()) {
			String arg = iter.next();

			if (arg.startsWith("-dump-code"))
				isDumpCode = true;
			else if (arg.startsWith("-eager"))
				isLazy = false;
			else if (arg.startsWith("-filter"))
				isFilter = true;
			else if (arg.startsWith("-functional"))
				isFunctional = true;
			else if (arg.startsWith("-lazy"))
				isLazy = true;
			else if (arg.startsWith("-library") && iter.hasNext())
				libraries.addAll(Arrays.asList(iter.next().split(",")));
			else if (arg.startsWith("-logical"))
				isLogical = true;
			else if (arg.startsWith("-no-default-library"))
				isDefaultLibrary = false;
			else if (arg.startsWith("-precompile") && iter.hasNext())
				for (String lib : iter.next().split(","))
					runPrecompile(lib);
			else if (arg.startsWith("-trace"))
				isTrace = true;
			else
				inputs.add(arg);
		}

		if (isFilter)
			code = runFilter(inputs, isLazy) ? 0 : 1;
		else if (isFunctional)
			code = runFunctional(inputs, isLazy) ? 0 : 1;
		else if (isLogical)
			code = runLogical(inputs) ? 0 : 1;
		else
			run(inputs);

		System.exit(code);
	}

	public enum InputType {
		FACT, QUERY, ELABORATE, EVALUATE, EVALUATEDUMP, EVALUATESTR, EVALUATETYPE
	};

	public void run(List<String> importFilenames) throws IOException {
		RuleSet rs = new RuleSet();
		SuiteUtil.importResource(rs, "auto.sl");

		for (String importFilename : importFilenames)
			SuiteUtil.importFile(rs, importFilename);

		Prover prover = new Prover(rs);
		prover.setEnableTrace(isTrace);

		InputStreamReader is = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(is);

		quit: while (true)
			try {
				StringBuilder sb = new StringBuilder();
				String line;

				do {
					System.out.print((sb.length() == 0) ? "=> " : "   ");

					if ((line = br.readLine()) != null)
						sb.append(line + "\n");
					else
						break quit;
				} while (!line.isEmpty() && !line.endsWith("#"));

				String input = sb.toString();
				InputType type;

				if (Util.isBlank(input))
					continue;

				if (input.startsWith("?")) {
					type = InputType.QUERY;
					input = input.substring(1);
				} else if (input.startsWith("/")) {
					type = InputType.ELABORATE;
					input = input.substring(1);
				} else if (input.startsWith("\\e")) {
					type = InputType.EVALUATE;
					input = input.substring(2);
				} else if (input.startsWith("\\s")) {
					type = InputType.EVALUATESTR;
					input = input.substring(2);
				} else if (input.startsWith("\\t")) {
					type = InputType.EVALUATETYPE;
					input = input.substring(2);
				} else if (input.startsWith("\\")) {
					type = InputType.EVALUATEDUMP;
					input = input.substring(1);
				} else
					type = InputType.FACT;

				input = input.trim();
				if (input.endsWith("#"))
					input = input.substring(0, input.length() - 1);

				final int count[] = { 0 };
				Node node = new TermParser().parse(input.trim());
				String prog;
				FunCompilerConfig fcc;

				switch (type) {
				case FACT:
					rs.addRule(node);
					break;
				case EVALUATE:
					fcc = SuiteUtil.fcc(node, isLazy);
					configureFunCompiler(fcc);
					Node result = SuiteUtil.evaluateFunctional(fcc);
					System.out.println(Formatter.dump(result));
					break;
				case EVALUATEDUMP:
					prog = applyFilter("anything => dump {" + input + "}");
					fcc = SuiteUtil.fcc(prog, isLazy);
					configureFunCompiler(fcc);
					SuiteUtil.evaluateFunctional(fcc);
					System.out.println();
					break;
				case EVALUATESTR:
					prog = applyFilter("anything => " + input);
					fcc = SuiteUtil.fcc(prog, isLazy);
					configureFunCompiler(fcc);
					SuiteUtil.evaluateFunctional(fcc);
					System.out.println();
					break;
				case EVALUATETYPE:
					fcc = SuiteUtil.fcc(node);
					configureFunCompiler(fcc);
					node = SuiteUtil.evaluateFunctionalType(fcc);
					System.out.println(Formatter.dump(node));
					break;
				default:
					final Generalizer generalizer = new Generalizer();
					node = generalizer.generalize(node);

					if (type == InputType.QUERY) {
						boolean q = prover.prove(node);
						System.out.println(q ? "Yes\n" : "No\n");
					} else if (type == InputType.ELABORATE) {
						Node elab = new Station() {
							public boolean run() {
								String dump = generalizer.dumpVariables();
								if (!dump.isEmpty())
									System.out.println(dump);

								count[0]++;
								return false;
							}
						};

						prover.prove(Tree.create(TermOp.AND___, node, elab));

						if (count[0] == 1)
							System.out.println(count[0] + " solution\n");
						else
							System.out.println(count[0] + " solutions\n");
					}
				}
			} catch (Throwable ex) {
				LogUtil.error(Main.class, ex);
			}
	}

	public boolean runLogical(List<String> files) throws IOException {
		boolean result = true;

		RuleSet rs = new RuleSet();
		result &= SuiteUtil.importResource(rs, "auto.sl");

		for (String file : files)
			result &= SuiteUtil.importFile(rs, file);

		return result;
	}

	public boolean runFilter(List<String> inputs, boolean isLazy)
			throws IOException {
		StringBuilder sb = new StringBuilder();
		for (String input : inputs)
			sb.append(input + " ");

		SuiteUtil.evaluateFunctional(applyFilter(sb.toString()), isLazy);
		return true;
	}

	public boolean runFunctional(List<String> files, boolean isLazy)
			throws IOException {
		if (files.size() == 1) {
			FileInputStream is = new FileInputStream(files.get(0));
			String expression = IoUtil.readStream(is);
			Node result = SuiteUtil.evaluateFunctional(expression, isLazy);
			return result == Atom.true_;
		} else
			throw new RuntimeException("Only one evaluation is allowed");
	}

	public void runPrecompile(String libraryName) {
		System.out.println("Pre-compiling " + libraryName + "... ");
		String imports[] = { "auto.sl", "fc-precompile.sl" };

		Prover prover = SuiteUtil.getProver(imports);
		prover.setEnableTrace(isTrace);

		String goal = "fc-setup-precompile " + libraryName;
		Node node = SuiteUtil.parse(goal);

		if (prover.prove(node))
			System.out.println("Pre-compilation success\n");
		else
			System.out.println("Pre-compilation failed");
	}

	// Public to be called by test case FilterTest.java
	public static String applyFilter(String func) {
		return "" //
				+ "define af-in = (start => \n" //
				+ "    define c = fgetc {start} >> \n" //
				+ "    if (c >= 0) then (c, af-in {start + 1}) else () \n" //
				+ ") >> \n" //
				+ "define af-out = (p => \n" //
				+ "    if-match:: \\c, \\cs \n" //
				+ "    then:: fputc {p} {c} {af-out {p + 1} {cs}} \n" //
				+ "    else:: () \n" //
				+ ") >> \n" //
				+ "define af-flush = (i => fflush {i}) >> \n" //
				+ "0 | af-in | (" + func + ") | af-out {0} | af-flush";
	}

	private void configureFunCompiler(FunCompilerConfig c) {
		if (!isDefaultLibrary)
			c.setLibraries(new ArrayList<String>());

		c.addLibraries(libraries);
		c.setTrace(isTrace);
		c.setDumpCode(isDumpCode);
		c.setIn(new ByteArrayInputStream(new byte[0]));
	}

	private static Log log = LogFactory.getLog(Util.currentClass());

}
