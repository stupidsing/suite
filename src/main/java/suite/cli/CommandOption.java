package suite.cli;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import suite.Suite;
import suite.editor.Editor;
import suite.fp.FunCompilerConfig;
import suite.fp.PrecompileMain;
import suite.lp.doer.ProverConfig;
import suite.lp.kb.RuleSet;
import suite.node.Node;
import suite.util.FunUtil.Source;

/**
 * Command line interface environment.
 * 
 * @author ywsing
 */
public class CommandOption {

	// Command dispatching options
	private boolean isBackground = false;

	// Program type options
	private boolean isQuiet = false;
	private boolean isFilter = false;
	private boolean isFunctional = false;
	private boolean isLogical = false;

	// Program evaluation options
	private boolean isDumpCode = false;
	private boolean isLazy = true;
	private List<String> libraries = new ArrayList<>(Suite.libraries);
	private boolean isTrace = false;

	public boolean processOption(String arg, Source<String> source) {
		return processOption(arg, source, true);
	}

	private boolean processOption(String arg, Source<String> source, boolean on) {
		boolean result = true;
		String arg1;

		if (arg.equals("-background"))
			isBackground = on;
		else if (arg.equals("-dump-code"))
			isDumpCode = on;
		else if (arg.equals("-eager"))
			isLazy = !on;
		else if (arg.equals("-editor"))
			new Editor().open();
		else if (arg.equals("-filter"))
			isFilter = on;
		else if (arg.equals("-functional"))
			isFunctional = on;
		else if (arg.equals("-lazy"))
			isLazy = on;
		else if (arg.equals("-libraries") && (arg1 = source.source()) != null)
			libraries = Arrays.asList(arg1.split(","));
		else if (arg.equals("-logical"))
			isLogical = on;
		else if (arg.startsWith("-no-"))
			result &= processOption("-" + arg.substring(4), source, false);
		else if (arg.equals("-precompile") && (arg1 = source.source()) != null)
			for (String lib : arg1.split(","))
				result &= Suite.precompile(lib, pc(null));
		else if (arg.equals("-precompile-all"))
			try (PrecompileMain precompileMain = new PrecompileMain()) {
				result &= precompileMain.precompile();
			}
		else if (arg.equals("-quiet"))
			isQuiet = on;
		else if (arg.equals("-trace"))
			isTrace = on;
		else
			throw new RuntimeException("Unknown option " + arg);

		return result;
	}

	public FunCompilerConfig fcc(Node node) {
		ProverConfig pc = pc(Suite.createRuleSet());

		FunCompilerConfig fcc = new FunCompilerConfig(pc, libraries);
		fcc.setDumpCode(isDumpCode);
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

	public boolean isBackground() {
		return isBackground;
	}

	public boolean isDumpCode() {
		return isDumpCode;
	}

	public boolean isQuiet() {
		return isQuiet;
	}

	public boolean isFilter() {
		return isFilter;
	}

	public boolean isFunctional() {
		return isFunctional;
	}

	public boolean isLogical() {
		return isLogical;
	}

}
