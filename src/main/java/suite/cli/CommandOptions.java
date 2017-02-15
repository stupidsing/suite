package suite.cli;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import suite.Suite;
import suite.editor.Editor;
import suite.fp.FunCompilerConfig;
import suite.lp.Configuration.ProverConfig;
import suite.lp.kb.RuleSet;
import suite.node.Node;
import suite.util.FunUtil.Source;
import suite.util.Util;

/**
 * Command line interface environment.
 *
 * @author ywsing
 */
public class CommandOptions {

	// program type options
	private boolean isChars = false;
	private boolean isDo = false;
	private boolean isQuiet = false;

	// program evaluation options
	private boolean isLazy = true;
	private List<String> imports = Collections.emptyList();
	private List<String> libraries = new ArrayList<>(Suite.libraries);
	private boolean isTrace = false;

	public boolean processOption(String arg, Source<String> source) {
		return processOption(arg, source, true);
	}

	private boolean processOption(String arg, Source<String> source, boolean on) {
		boolean result = true;
		String arg1;

		if (Util.stringEquals(arg, "--do"))
			isDo = on;
		else if (Util.stringEquals(arg, "--chars"))
			isChars = on;
		else if (Util.stringEquals(arg, "--eager"))
			isLazy = !on;
		else if (Util.stringEquals(arg, "--editor"))
			new Editor().open();
		else if (Util.stringEquals(arg, "--imports") && (arg1 = source.source()) != null)
			imports = Arrays.asList(arg1.split(","));
		else if (Util.stringEquals(arg, "--lazy"))
			isLazy = on;
		else if (Util.stringEquals(arg, "--libraries") && (arg1 = source.source()) != null)
			libraries = Arrays.asList(arg1.split(","));
		else if (arg.startsWith("--no-"))
			result &= processOption("--" + arg.substring(5), source, false);
		else if (Util.stringEquals(arg, "--quiet"))
			isQuiet = on;
		else if (Util.stringEquals(arg, "--trace"))
			isTrace = on;
		else if (Util.stringEquals(arg, "--use") && (arg1 = source.source()) != null)
			libraries = Util.add(libraries, Arrays.asList(arg1.split(",")));
		else
			throw new RuntimeException("Unknown option " + arg);

		return result;
	}

	public FunCompilerConfig fcc(Node node) {
		ProverConfig pc = pc(Suite.newRuleSet());

		FunCompilerConfig fcc = new FunCompilerConfig(pc, libraries);
		fcc.setLazy(isLazy);
		fcc.setNode(node);

		return fcc;
	}

	public ProverConfig pc(RuleSet ruleSet) {
		ProverConfig pc = new ProverConfig(ruleSet);
		pc.setTrace(isTrace);
		return pc;
	}

	public PrintStream prompt() {
		try (OutputStream os = new OutputStream() {
			public void write(int c) {
			}
		}) {
			return !isQuiet ? System.out : new PrintStream(os);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public boolean isChars() {
		return isChars;
	}

	public boolean isDo() {
		return isDo;
	}

	public boolean isLazy() {
		return isLazy;
	}

	public boolean isQuiet() {
		return isQuiet;
	}

	public List<String> getImports() {
		return imports;
	}

}
